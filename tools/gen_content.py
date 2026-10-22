#!/usr/bin/env python3
"""Generate lesson JSON and reference tests from note 05 + markup sidecars.

Usage:
  python3 tools/gen_content.py                      regenerate committed output
  python3 tools/gen_content.py --check              exit 1 and list stale/orphan files, write nothing
  python3 tools/gen_content.py --allow-removed l0.9 allow a lesson id to leave index.json

Inputs: the bundled note 05 (files/kb/n07ed966c.md, refreshed by sync_notes.py,
never the vault) and content/authoring/<id>.md. Outputs: files/content/index.json,
files/content/lessons/<id>.json, commonTest/.../lesson/ReferenceSolutionsTest.kt.
Any validation error (G1-G8) prints `ERROR <file>: <rule>: <detail>` and writes nothing.

Sidecar grammar (content/authoring/l0.N.md):

  # l0.N                         lesson id, must equal the file stem
  note: 0.N <heading>            selects `## 0.N <heading>` in note 05
  title: / statement: / trains:  optional one-line overrides of note fields
  pitfalls: / variations:        optional bullet-list overrides (solution.*)
  examples:                      >=1 bullets, each a Kotlin statement run against every variant
  - assertEquals(2, f(intArrayOf(1, 3)))

  ## theory <card title>         theory card, keys: signals: (bullets), skeleton: (``` fence),
                                 pitfalls: (bullets), body: (one line)

  ## eK                          exercise <id>.eK; one ```kotlin fence with the FULL function,
                                 holes written as ⟦ok | ok2 | ~acceptable⟧ (first non-~ is ok[0])
  - `T`: why                     distractor chip with a why (must contain a `code` counterexample)
  - `T`                          distractor without chip why: needs an @H override for every hole
  - @H `T`: why                  holes[H-1].wrongWhy[T] (T in bank, not correct for hole H)
  - ~`T`: note                   note for the acceptable text T
  ? hint                         hint, in order

Bank order: ok[0] of each hole (with multiplicity), other ok/acceptable texts, distractors.
"""
import argparse
import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = "shared/src/commonMain/composeResources/files"
NOTE = RES + "/kb/n07ed966c.md"
AUTHORING = "content/authoring"
INDEX = RES + "/content/index.json"
LESSONS = RES + "/content/lessons"
TEST_KT = "shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt"
MAX_BANK = 10

HOLE = re.compile(r"⟦(.*?)⟧")
# ponytail: " | " separates alternatives; level 0 has no chip containing " | ", revisit if one appears
ALT_SEP = " | "
BULLET = re.compile(r"^- (?:@(\d+) |(~))?`(.+?)`(?:: ?(.*))?$")
KEY = re.compile(r"^([a-z]+):\s*(.*)$")
CARD_KEYS = ("signals", "skeleton", "pitfalls", "body")
# expected value shape per key: one line, bullet list or ``` fence
TOP_KINDS = {"note": "line", "title": "line", "statement": "line", "trains": "line",
             "pitfalls": "list", "variations": "list", "examples": "list"}
CARD_KINDS = {"signals": "list", "skeleton": "fence", "pitfalls": "list", "body": "line"}


class GenError(Exception):
    def __init__(self, file, rule, detail):
        Exception.__init__(self, "%s: %s: %s" % (file, rule, detail))
        self.file, self.rule, self.detail = file, rule, detail


# ---------- note 05 ----------

def unquote(lines):
    """Strip callout prefixes; never lstrip('>') because code has -> and >=."""
    out = []
    for ln in lines:
        if ln == ">":
            out.append("")
        elif ln.startswith("> "):
            out.append(ln[2:])
        else:
            break
    return out


def section(note_lines, heading):
    if "## " + heading not in note_lines:
        raise GenError(NOTE, "G7", "heading not found: ## " + heading)
    start = note_lines.index("## " + heading) + 1
    end = next((i for i in range(start, len(note_lines))
                if note_lines[i].startswith(("## ", "# ", "---"))), len(note_lines))
    return note_lines[start:end]


def split_items(text):
    parts = re.split(r";\s+|(?<=\.)\s+(?=[А-ЯЁA-Z`])", text.strip())
    return [p[:-1] if p.endswith(".") else p for p in (p.strip() for p in parts) if p]


def parse_note_section(note_lines, heading):
    sec = section(note_lines, heading)
    res = {"title": heading.split(" ", 1)[1], "statement": "", "trains": None, "targetTimeSec": None,
           "code": None, "time": None, "space": None, "pitfalls": [], "variations": [], "hints": []}
    statement, in_statement, i = [], True, 0
    while i < len(sec):
        ln = sec[i]
        i += 1
        if ln.startswith("> [!"):
            body = unquote(sec[i:])
            i += len(body)
            if "[!question]" in ln:
                res["hints"].append("\n".join(body).strip())
            elif "[!success]" in ln:
                parse_success(body, res)
            continue
        if ln.startswith("- "):
            in_statement = False
        m = re.match(r"^- 🎯\s*\*\*Тренируем:\*\*\s*(.*?)\.?$", ln)
        if m:
            res["trains"] = m.group(1)
        m = re.match(r"^- ⏱️?\s*\*\*Цель:\*\*\s*([\d.]+)\s*минут", ln)
        if m:
            res["targetTimeSec"] = int(round(float(m.group(1)) * 60))
        if in_statement:
            statement.append(ln)
    res["statement"] = "\n".join(statement).strip()
    if res["code"] is None or res["time"] is None:
        raise GenError(NOTE, "G7", "## %s: needs a [!success] callout with ```kotlin and **Сложность:**" % heading)
    return res


def parse_success(body, res):
    if "```kotlin" not in body or "```" not in body[body.index("```kotlin") + 1:]:
        raise GenError(NOTE, "G7", "%s: [!success] has no closed ```kotlin fence" % res["title"])
    start = body.index("```kotlin")
    end = body.index("```", start + 1)
    res["code"] = "\n".join(body[start + 1:end])
    for ln in body[end + 1:]:
        if "**Сложность:**" in ln:
            # one level of nesting: O(min(n, m))
            found = re.findall(r"O\((?:[^()]|\([^()]*\))*\)", ln)
            if len(found) < 2:
                raise GenError(NOTE, "G7", "%s: **Сложность:** needs time and space O(...)" % res["title"])
            res["time"], res["space"] = found[:2]
        m = re.search(r"⚠️?\s*\*\*Грабли[^*]*:\*\*\s*(.*)", ln)
        if m:
            # None = multi-line list the parser cannot read; build_lesson requires a sidecar override
            res["pitfalls"] = split_items(m.group(1)) or None
        m = re.search(r"🔁\s*\*\*Вариации[^*]*:\*\*\s*(.*)", ln)
        if m:
            res["variations"] = split_items(m.group(1)) or None


# ---------- sidecar ----------

def parse_keys(lines, file, kinds):
    out, i = {}, 0
    while i < len(lines):
        ln = lines[i]
        i += 1
        if not ln.strip():
            continue
        m = KEY.match(ln)
        if not m:
            raise GenError(file, "syntax", "unexpected line: " + ln)
        key, val = m.groups()
        kind = "line" if val else "fence" if i < len(lines) and lines[i].startswith("```") else "list"
        if key in kinds and kinds[key] != kind:
            raise GenError(file, "syntax", "%s: expected %s, got %s" % (key, kinds[key], kind))
        if val:
            out[key] = val
        elif i < len(lines) and lines[i].startswith("```"):
            try:
                end = lines.index("```", i + 1)
            except ValueError:
                raise GenError(file, "syntax", "unclosed fence after " + key + ":")
            out[key] = "\n".join(lines[i + 1:end])
            i = end + 1
        else:
            items = []
            while i < len(lines) and lines[i].startswith("- "):
                items.append(lines[i][2:])
                i += 1
            out[key] = items
    return out


def parse_exercise(name, lines, file):
    ex = {"name": name, "code": None, "bullets": [], "hints": []}
    i = 0
    while i < len(lines):
        ln = lines[i]
        i += 1
        if ln.startswith("```kotlin") and ex["code"] is None:
            try:
                end = lines.index("```", i)
            except ValueError:
                raise GenError(file, "syntax", name + ": unclosed kotlin fence")
            ex["code"] = "\n".join(lines[i:end])
            i = end + 1
        elif ln.startswith("? "):
            ex["hints"].append(ln[2:].strip())
        elif ln.startswith("- "):
            m = BULLET.match(ln)
            if not m:
                raise GenError(file, "syntax", name + ": bad bullet: " + ln)
            hole, tilde, text, why = m.groups()
            kind = "@" if hole else ("~" if tilde else "-")
            ex["bullets"].append((kind, int(hole) if hole else None, text, (why or "").strip() or None))
        elif ln.strip():
            raise GenError(file, "syntax", name + ": unexpected line: " + ln)
    if ex["code"] is None:
        raise GenError(file, "syntax", name + ": no ```kotlin fence")
    return ex


def parse_sidecar(text, stem):
    file = "%s/%s.md" % (AUTHORING, stem)
    lines = text.splitlines()
    if not lines or not lines[0].startswith("# "):
        raise GenError(file, "G7", "first line must be '# <lesson id>'")
    lid = lines[0][2:].strip()
    if lid != stem:
        raise GenError(file, "G7", "id %r != file stem %r" % (lid, stem))
    if not re.match(r"^l\d+\.\d+$", lid):
        raise GenError(file, "G7", "id %r is not l<level>.<n>" % lid)
    starts = [i for i, ln in enumerate(lines) if ln.startswith("## ")] + [len(lines)]
    top = parse_keys(lines[1:starts[0]], file, TOP_KINDS)
    unknown = set(top) - set(TOP_KINDS)
    if unknown:
        raise GenError(file, "syntax", "unknown keys: " + ", ".join(sorted(unknown)))
    if "note" not in top:
        raise GenError(file, "G7", "missing note: heading")
    sc = {"id": lid, "file": file, "top": top, "theory": [], "exercises": []}
    for a, b in zip(starts, starts[1:]):
        head = lines[a][3:].strip()
        body = lines[a + 1:b]
        if head.startswith("theory "):
            card = parse_keys(body, file, CARD_KINDS)
            bad = set(card) - set(CARD_KEYS)
            if bad:
                raise GenError(file, "syntax", "## %s: unknown keys: %s" % (head, ", ".join(sorted(bad))))
            if not any(card.get(k) for k in CARD_KEYS):
                raise GenError(file, "G7", "## %s is empty" % head)
            card["title"] = head[len("theory "):].strip()
            sc["theory"].append(card)
        elif re.match(r"^e\d+$", head):
            sc["exercises"].append(parse_exercise(head, body, file))
        else:
            raise GenError(file, "syntax", "unknown section: ## " + head)
    return sc


# ---------- build + validate ----------

def split_holes(code):
    segs, holes, pos = [], [], 0
    for m in HOLE.finditer(code):
        segs.append(code[pos:m.start()])
        pos = m.end()
        alts = [a.strip() for a in m.group(1).split(ALT_SEP)]
        holes.append(([a for a in alts if not a.startswith("~")],
                      [a[1:].strip() for a in alts if a.startswith("~")],
                      alts))
    segs.append(code[pos:])
    return segs, holes


def assemble(segs, fills):
    return "".join(s + f for s, f in zip(segs, fills)) + segs[-1]


def norm(code):
    # ponytail: drops `//` comments naively; assumes no "//" inside string literals (true for level 0)
    return [re.sub(r"\s*//.*$", "", ln).rstrip() for ln in code.split("\n")]


def build_lesson(sc, note_lines):
    file, lid, top = sc["file"], sc["id"], sc["top"]
    if "## " + top["note"] not in note_lines:
        raise GenError(file, "G7", "note heading not found: ## " + top["note"])
    note = parse_note_section(note_lines, top["note"])
    if not top.get("examples"):
        raise GenError(file, "G8", "no examples: lines")

    parsed = []
    for ex in sc["exercises"]:
        segs, holes = split_holes(ex["code"])
        if not holes:
            raise GenError(file, "G1", ex["name"] + ": no ⟦⟧ holes")
        for h, (ok, acc, alts) in enumerate(holes, 1):
            if not ok or any(not a or a == "~" for a in alts):
                raise GenError(file, "G1", "%s hole %d: needs a correct entry and no empty entry" % (ex["name"], h))
        parsed.append((ex, segs, holes))

    counts = [len(p[2]) for p in parsed]
    if not 2 <= len(parsed) <= 3:
        raise GenError(file, "G7", "need 2..3 exercises, got %d" % len(parsed))
    if any(b <= a for a, b in zip(counts, counts[1:])):
        raise GenError(file, "G7", "hole counts must strictly increase: %s" % counts)

    codes = [assemble(segs, [h[0][0] for h in holes]) for _, segs, holes in parsed]
    for (ex, _, _), code in zip(parsed, codes):
        if norm(code) != norm(note["code"]):
            raise GenError(file, "G5", ex["name"] + ": code without holes != note reference")
    if len(set(codes)) != 1:
        raise GenError(file, "G5", "exercises do not produce the same code")

    exercises = [build_exercise(lid, file, ex, segs, holes) for ex, segs, holes in parsed]

    lesson = {"schema": 1, "id": lid, "level": int(re.match(r"l(\d+)", lid).group(1)),
              "title": top.get("title", note["title"]),
              "statement": top.get("statement", note["statement"])}
    trains = top.get("trains", note["trains"])
    if trains:
        lesson["trains"] = trains
    if note["targetTimeSec"]:
        lesson["targetTimeSec"] = note["targetTimeSec"]
    lesson["theory"] = [{k: c[k] for k in ("title",) + CARD_KEYS if c.get(k)}
                        for c in sc["theory"]]
    lesson["exercises"] = exercises
    lesson["solution"] = {"code": codes[0], "time": note["time"], "space": note["space"],
                          "pitfalls": top.get("pitfalls", note["pitfalls"]),
                          "variations": top.get("variations", note["variations"])}
    for k in ("pitfalls", "variations"):
        if lesson["solution"][k] is None:
            raise GenError(file, "G7", "note %s is a multi-line list; set %s: in the sidecar" % (k, k))
    return lesson


def build_exercise(lid, file, ex, segs, holes):
    where = ex["name"]
    correct = [set(ok) | set(acc) for ok, acc, _ in holes]
    any_correct = set().union(*correct)
    acc_texts = set(t for _, acc, _ in holes for t in acc)
    wrong = [dict() for _ in holes]
    notes, distractors = {}, []
    for kind, h, text, why in ex["bullets"]:
        if kind == "~":
            if text not in acc_texts:
                raise GenError(file, "G6", "%s: ~`%s` is not acceptable in any hole" % (where, text))
            if not why:
                raise GenError(file, "G6", "%s: ~`%s` has a blank note" % (where, text))
            notes[text] = why
        elif kind == "@":
            if not 1 <= h <= len(holes):
                raise GenError(file, "G6", "%s: @%d out of range (1..%d)" % (where, h, len(holes)))
            if text in correct[h - 1]:
                raise GenError(file, "G6", "%s: @%d `%s` is correct for that hole" % (where, h, text))
            if not why:
                raise GenError(file, "G6", "%s: @%d `%s` has a blank why" % (where, h, text))
            wrong[h - 1][text] = why
        else:
            if text in any_correct or text in [d[0] for d in distractors]:
                raise GenError(file, "G6", "%s: distractor `%s` is correct or duplicated" % (where, text))
            distractors.append((text, why))

    bank = [{"text": ok[0]} for ok, _, _ in holes]
    for ok, acc, _ in holes:
        for t in ok[1:] + acc:
            if t not in [c["text"] for c in bank]:
                bank.append({"text": t})
    for text, why in distractors:
        bank.append({"text": text, "why": why} if why else {"text": text})
    bank_texts = set(c["text"] for c in bank)

    for h, ww in enumerate(wrong, 1):
        for text in ww:
            if text not in bank_texts:
                raise GenError(file, "G6", "%s: @%d `%s` is not in the bank" % (where, h, text))
    for t in sorted(acc_texts - set(notes)):
        raise GenError(file, "G6", "%s: acceptable `%s` has no ~ note" % (where, t))
    for text, why in distractors:
        if not why and not all(text in ww for ww in wrong):
            raise GenError(file, "G2", "%s: distractor `%s` has no why for every hole" % (where, text))
    for text, why in distractors:
        if why and "`" not in why:
            raise GenError(file, "G3", "%s: why for `%s` has no `counterexample`" % (where, text))
    for h, ww in enumerate(wrong, 1):
        for text, why in ww.items():
            if "`" not in why:
                raise GenError(file, "G3", "%s: @%d why for `%s` has no `counterexample`" % (where, h, text))
    if len(bank) > MAX_BANK:
        raise GenError(file, "G7", "%s: bank has %d chips > %d" % (where, len(bank), MAX_BANK))

    out_holes = []
    for (ok, acc, _), ww in zip(holes, wrong):
        hole = {"ok": ok}
        if acc:
            hole["acceptable"] = [{"text": t, "note": notes[t]} for t in acc]
        if ww:
            hole["wrongWhy"] = ww
        out_holes.append(hole)
    return {"id": "%s.%s" % (lid, ex["name"]), "segments": segs, "holes": out_holes, "bank": bank,
            "hints": ex["hints"]}


# ---------- render ----------

def dump(obj):
    return json.dumps(obj, ensure_ascii=False, indent=2) + "\n"


def id_key(lid):
    return tuple(int(n) for n in re.findall(r"\d+", lid))


def variants(lesson):
    out = [(lesson["solution"]["code"], "reference")]
    for ex in lesson["exercises"]:
        base = [h["ok"][0] for h in ex["holes"]]
        for i, h in enumerate(ex["holes"]):
            alts = [(t, "ok") for t in h["ok"][1:]] + [(a["text"], "acceptable") for a in h.get("acceptable", [])]
            for text, kind in alts:
                fills = list(base)
                fills[i] = text
                code = assemble(ex["segments"], fills)
                if code not in [c for c, _ in out]:
                    out.append((code, "%s hole %d = %s (%s)" % (ex["id"], i + 1, text, kind)))
    return out


def render_kt(items):
    objs, tests = [], []
    for lesson, examples in items:
        prefix = "L" + lesson["id"][1:].replace(".", "_")
        for n, (code, label) in enumerate(variants(lesson)):
            name = "%s_v%d" % (prefix, n)
            body = "\n".join("    " + ln if ln else ln for ln in code.split("\n"))
            objs.append("private object %s { // %s\n%s\n}\n" % (name, label, body))
            tests.append("    @Test\n    fun %s() {\n        with(%s) {\n%s        }\n    }\n"
                         % (name.lower(), name, "".join("            %s\n" % e for e in examples)))
    return ("// GENERATED by tools/gen_content.py from content/authoring/*.md. Do not edit; rerun the generator.\n"
            "package com.tlenparty.algolingo.lesson\n\nimport kotlin.test.*\n\n"
            + "\n".join(objs) + "\nclass ReferenceSolutionsTest {\n" + "\n".join(tests) + "}\n")


def render_all(repo_root, allow_removed=()):
    root = Path(repo_root)
    note_lines = (root / NOTE).read_text(encoding="utf-8").splitlines()
    items = []
    for p in sorted((root / AUTHORING).glob("*.md"), key=lambda p: id_key(p.stem)):
        sc = parse_sidecar(p.read_text(encoding="utf-8"), p.stem)
        items.append((build_lesson(sc, note_lines), sc["top"]["examples"]))
    ids = [lesson["id"] for lesson, _ in items]
    old = root / INDEX
    if old.is_file():
        try:
            old_ids = [e["id"] for e in json.loads(old.read_text(encoding="utf-8"))["lessons"]]
        except (ValueError, KeyError, TypeError) as e:
            raise GenError(INDEX, "syntax", "cannot read lesson ids: %r" % e)
        for oid in old_ids:
            if oid not in ids and oid not in allow_removed:
                raise GenError(INDEX, "G4", "lesson id %s disappeared (pass --allow-removed %s)" % (oid, oid))
    out = {INDEX: dump({"schema": 1, "lessons": [{"id": l["id"], "title": l["title"]} for l, _ in items]})}
    for lesson, _ in items:
        out["%s/%s.json" % (LESSONS, lesson["id"])] = dump(lesson)
    out[TEST_KT] = render_kt(items)
    return out


def main(argv=None):
    ap = argparse.ArgumentParser(description="Generate lesson JSON from note 05 + sidecars.")
    ap.add_argument("--check", action="store_true", help="list stale outputs, exit 1 if any, write nothing")
    ap.add_argument("--allow-removed", action="append", default=[], metavar="ID")
    ap.add_argument("--root", default=str(ROOT), help=argparse.SUPPRESS)
    args = ap.parse_args(argv)
    root = Path(args.root)
    try:
        out = render_all(root, args.allow_removed)
    except GenError as e:
        print("ERROR " + str(e), file=sys.stderr)
        return 1
    stale = [rel for rel, text in out.items()
             if not (root / rel).is_file() or (root / rel).read_text(encoding="utf-8") != text]
    orphans = ["%s/%s" % (LESSONS, p.name) for p in sorted((root / LESSONS).glob("*.json"))
               if "%s/%s" % (LESSONS, p.name) not in out]
    if args.check:
        for rel in stale:
            print("stale: " + rel)
        for rel in orphans:
            print("orphan: " + rel)
        return 1 if stale or orphans else 0
    for rel in orphans:
        (root / rel).unlink()
        print("removed " + rel)
    for rel in stale:
        path = root / rel
        path.parent.mkdir(parents=True, exist_ok=True)
        with open(str(path), "w", encoding="utf-8", newline="\n") as f:
            f.write(out[rel])
        print("wrote " + rel)
    print("%d file(s) written, %d unchanged" % (len(stale), len(out) - len(stale)))
    return 0


if __name__ == "__main__":
    sys.exit(main())

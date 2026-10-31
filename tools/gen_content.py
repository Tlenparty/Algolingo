#!/usr/bin/env python3
"""Generate lesson JSON and reference tests from note 05 + markup sidecars.

Usage:
  python3 tools/gen_content.py                      regenerate committed output
  python3 tools/gen_content.py --check              exit 1 and list stale/orphan files, write nothing
  python3 tools/gen_content.py --allow-removed l0.9 allow a lesson id to leave index.json

Inputs: the bundled note 05 (files/kb/n07ed966c.md) and content/authoring/<id>.md. Outputs: files/content/index.json,
files/content/lessons/<id>.json, commonTest/.../lesson/ReferenceSolutionsTest.kt (reference/ok/
acceptable variants, JVM + Native) and androidHostTest/resources/wrong_variants.json (one full
program per wrong (hole, chip) pair plus one "control" reference per lesson, classified on JVM by
WrongChipsTest).
Any validation error (G1-G14; G12-G14 run the python harness) prints `ERROR <file>: <rule>: <detail>` and writes nothing.

Sidecar grammar (content/authoring/l0.N.md):

  # l0.N                         lesson id, must equal the file stem
  note: 0.N <heading>            selects `## 0.N <heading>` in note 05
  title: / statement: / trains:  optional one-line overrides of note fields
  pitfalls: / variations:        optional bullet-list overrides (solution.*)
  examples:                      >=1 bullets, each a Kotlin statement run inside every variant
                                 object (member scope, so nested classes in the note code resolve)
  - assertEquals(2, f(intArrayOf(1, 3)))
  checks:                        required ```kotlin fence, test-only (never in lesson JSON): edge cases
                                 and oracle loops, each check wrapped in case { ... }; `rnd`
                                 (Random(1234)) and pw(b, e) are in scope; no Math./java./String( (G10)

  ## theory <card title>         theory card, keys: signals: (bullets), skeleton: (``` fence),
                                 pitfalls: (bullets), body: (one line)

  ## eK                          exercise <id>.eK; one ```kotlin fence with the FULL function,
                                 holes written as ⟦ok | ok2 | ~acceptable⟧ (first non-~ is ok[0])
  - `T` [kinds]: why             distractor chip with a why (must contain a `code` counterexample)
  - `T`                          distractor without chip why: needs an @H override for every hole
  - @H `T` [kind]: why           holes[H-1].wrongWhy[T] (T in bank, not correct for hole H)
  - ~`T`: note                   note for the acceptable text T (no tag)
  ? hint                         hint, in order

Kind tags (G9, test-only, never in lesson JSON) say how the variant breaks: compile | wrong |
crash | hang. @H bullets carry exactly one; a distractor with a why carries a set ([compile,wrong]),
the kinds seen across its holes; a distractor without a why takes none (tag its @H overrides).

Every claim is a hard gate: no variant is skipped or parked (tools/test_gen_content.py guards it).

Python sidecar (content/authoring/py/<id>.md): same grammar plus `lang: python`; every fence is
```python; pitfalls: and variations: are required; the note gives title/statement/complexity only
(no G5 against the Kotlin note code). Needs a Kotlin sidecar with the same id. Output goes to
files/content/lessons/py/<id>.json and adds langs ["kotlin","python"] to the index entry.

Bank order: ok[0] of each hole (with multiplicity), other ok/acceptable texts, distractors.
"""
import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import tempfile
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
RES = "shared/src/commonMain/composeResources/files"
NOTE = RES + "/kb/n07ed966c.md"
AUTHORING = "content/authoring"
INDEX = RES + "/content/index.json"
LESSONS = RES + "/content/lessons"
LESSONS_PY = RES + "/content/lessons/py"
AUTHORING_PY = AUTHORING + "/py"
TEST_KT = "shared/src/commonTest/kotlin/com/tlenparty/algolingo/lesson/ReferenceSolutionsTest.kt"
WRONG_JSON = "shared/src/androidHostTest/resources/wrong_variants.json"
MAX_BANK = 10

HOLE = re.compile(r"⟦(.*?)⟧")
# ponytail: " | " separates alternatives; level 0 has no chip containing " | ", revisit if one appears
ALT_SEP = " | "
BULLET = re.compile(r"^- (?:@(\d+) |(~))?`(.+?)`(?: \[([a-z,]+)\])?(?:: ?(.*))?$")
KINDS = ("compile", "wrong", "crash", "hang")
# ponytail: substring ban, not a parser; enough to keep checks: common-Kotlin (Native-compilable)
CHECKS_BANNED = re.compile(r"\b(Math\.|java\.|String\()")  # \b: concatToString() is fine
KEY = re.compile(r"^([a-z]+):\s*(.*)$")
CARD_KEYS = ("signals", "skeleton", "pitfalls", "body")
# expected value shape per key: one line, bullet list or ``` fence
TOP_KINDS = {"lang": "line", "note": "line", "title": "line", "statement": "line", "trains": "line",
             "pitfalls": "list", "variations": "list", "examples": "list", "checks": "fence"}
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
    text = text.strip()
    code = [m.span() for m in re.finditer(r"`[^`]*`", text)]
    parts, pos = [], 0
    for m in re.finditer(r";\s+|(?<=\.)\s+(?=[А-ЯЁA-Z`])", text):
        if not any(a <= m.start() < b for a, b in code):  # never split inside `inline code`
            parts.append(text[pos:m.start()])
            pos = m.end()
    parts.append(text[pos:])
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

def parse_keys(lines, file, kinds, fence="kotlin"):
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
            if lines[i] != "```" + fence:
                raise GenError(file, "syntax", "%s: fence must be ```%s, got %s" % (key, fence, lines[i]))
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


def parse_exercise(name, lines, file, fence="kotlin"):
    ex = {"name": name, "code": None, "bullets": [], "hints": []}
    i = 0
    while i < len(lines):
        ln = lines[i]
        i += 1
        if ln.startswith("```") and ln != "```" and ex["code"] is None:
            if ln != "```" + fence:
                raise GenError(file, "syntax", "%s: fence must be ```%s, got %s" % (name, fence, ln))
            try:
                end = lines.index("```", i)
            except ValueError:
                raise GenError(file, "syntax", "%s: unclosed %s fence" % (name, fence))
            ex["code"] = "\n".join(lines[i:end])
            i = end + 1
        elif ln.startswith("? "):
            ex["hints"].append(ln[2:].strip())
        elif ln.startswith("- "):
            m = BULLET.match(ln)
            if not m:
                raise GenError(file, "syntax", name + ": bad bullet: " + ln)
            hole, tilde, text, tags, why = m.groups()
            kind = "@" if hole else ("~" if tilde else "-")
            ex["bullets"].append((kind, int(hole) if hole else None, text, (why or "").strip() or None,
                                  tuple(tags.split(",")) if tags else ()))
        elif ln.strip():
            raise GenError(file, "syntax", name + ": unexpected line: " + ln)
    if ex["code"] is None:
        raise GenError(file, "syntax", "%s: no ```%s fence" % (name, fence))
    return ex


def parse_sidecar(text, stem, lang="kotlin"):
    """lang comes from the folder: content/authoring/<id>.md is kotlin, content/authoring/py/<id>.md python."""
    file = "%s/%s.md" % (AUTHORING_PY if lang == "python" else AUTHORING, stem)
    lines = text.splitlines()
    if not lines or not lines[0].startswith("# "):
        raise GenError(file, "G7", "first line must be '# <lesson id>'")
    lid = lines[0][2:].strip()
    if lid != stem:
        raise GenError(file, "G7", "id %r != file stem %r" % (lid, stem))
    if not re.match(r"^l\d+\.\d+$", lid):
        raise GenError(file, "G7", "id %r is not l<level>.<n>" % lid)
    starts = [i for i, ln in enumerate(lines) if ln.startswith("## ")] + [len(lines)]
    # the lang: line must be read before the fences it selects
    declared = next((ln[5:].strip() for ln in lines[1:starts[0]] if ln.startswith("lang:")), None)
    if declared not in (None, "python"):
        raise GenError(file, "syntax", "lang: only python is allowed, got %r" % declared)
    if (declared or "kotlin") != lang:
        raise GenError(file, "syntax", "lang: %s sidecar must live in %s/" % (
            declared or "kotlin", AUTHORING_PY if declared else AUTHORING))
    top = parse_keys(lines[1:starts[0]], file, TOP_KINDS, lang)
    unknown = set(top) - set(TOP_KINDS)
    if unknown:
        raise GenError(file, "syntax", "unknown keys: " + ", ".join(sorted(unknown)))
    if "note" not in top:
        raise GenError(file, "G7", "missing note: heading")
    if "checks" not in top:
        raise GenError(file, "G10", "missing checks: (edge cases + oracle are required, D-08)")
    bad = lang == "kotlin" and CHECKS_BANNED.search(top["checks"])
    if bad:
        raise GenError(file, "G10", "checks: uses %r (JVM-only or Native-deprecated API)" % bad.group(1))
    sc = {"id": lid, "file": file, "lang": lang, "top": top, "theory": [], "exercises": []}
    for a, b in zip(starts, starts[1:]):
        head = lines[a][3:].strip()
        body = lines[a + 1:b]
        if head.startswith("theory "):
            card = parse_keys(body, file, CARD_KINDS, lang)
            bad = set(card) - set(CARD_KEYS)
            if bad:
                raise GenError(file, "syntax", "## %s: unknown keys: %s" % (head, ", ".join(sorted(bad))))
            if not any(card.get(k) for k in CARD_KEYS):
                raise GenError(file, "G7", "## %s is empty" % head)
            card["title"] = head[len("theory "):].strip()
            sc["theory"].append(card)
        elif re.match(r"^e\d+$", head):
            sc["exercises"].append(parse_exercise(head, body, file, lang))
        else:
            raise GenError(file, "syntax", "unknown section: ## " + head)
    names = [e["name"] for e in sc["exercises"]]
    if names != ["e%d" % k for k in range(1, len(names) + 1)]:
        raise GenError(file, "G7", "exercise sections must be e1..eN in order, got %s" % names)
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
    for k in ("pitfalls", "variations"):
        if sc["lang"] != "kotlin" and k not in top:
            raise GenError(file, "G7", "%s: is required for %s (the note's is about Kotlin)" % (k, sc["lang"]))

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
        # the note code is Kotlin; a python lesson is proven by the python harness (G12) instead
        if sc["lang"] == "kotlin" and norm(code) != norm(note["code"]):
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


def check_tags(file, where, kind, h, text, why, tags):
    """G9: @H needs exactly one kind, a distractor with a why needs >= 1, ~ and a bare distractor take none."""
    who = "%s: %s`%s`" % (where, "@%d " % h if kind == "@" else "~" if kind == "~" else "", text)
    bad = [t for t in tags if t not in KINDS]
    if bad:
        raise GenError(file, "G9", "%s: unknown kind %s (use %s)" % (who, ",".join(bad), "|".join(KINDS)))
    if len(set(tags)) != len(tags):
        raise GenError(file, "G9", "%s: repeated kind in [%s]" % (who, ",".join(tags)))
    if kind == "~" and tags:
        raise GenError(file, "G9", "%s: acceptable chips take no kind tag" % who)
    if kind == "@" and len(tags) != 1:
        raise GenError(file, "G9", "%s: needs exactly one kind tag [%s], got %d" % (who, "|".join(KINDS), len(tags)))
    if kind == "-" and why and not tags:
        raise GenError(file, "G9", "%s: distractor with a why needs a kind tag" % who)
    if kind == "-" and tags and not why:
        # claim_for only turns a chip why into a claim, so this tag would never be checked
        raise GenError(file, "G9", "%s: kind tag without a why is never checked; tag the @H overrides instead" % who)


def build_exercise(lid, file, ex, segs, holes):
    where = ex["name"]
    correct = [set(ok) | set(acc) for ok, acc, _ in holes]
    any_correct = set().union(*correct)
    acc_texts = set(t for _, acc, _ in holes for t in acc)
    wrong = [dict() for _ in holes]
    notes, distractors = {}, []
    for kind, h, text, why, tags in ex["bullets"]:
        if kind == "~":
            if text not in acc_texts:
                raise GenError(file, "G6", "%s: ~`%s` is not acceptable in any hole" % (where, text))
            if not why:
                raise GenError(file, "G6", "%s: ~`%s` has a blank note" % (where, text))
            if text in notes:
                raise GenError(file, "G6", "%s: ~`%s` is noted twice" % (where, text))
            notes[text] = why
        elif kind == "@":
            if not 1 <= h <= len(holes):
                raise GenError(file, "G6", "%s: @%d out of range (1..%d)" % (where, h, len(holes)))
            if text in correct[h - 1]:
                raise GenError(file, "G6", "%s: @%d `%s` is correct for that hole" % (where, h, text))
            if not why:
                raise GenError(file, "G6", "%s: @%d `%s` has a blank why" % (where, h, text))
            if text in wrong[h - 1]:
                raise GenError(file, "G6", "%s: @%d `%s` is overridden twice" % (where, h, text))
            wrong[h - 1][text] = why
        else:
            if text in any_correct or text in [d[0] for d in distractors]:
                raise GenError(file, "G6", "%s: distractor `%s` is correct or duplicated" % (where, text))
            distractors.append((text, why))
        check_tags(file, where, kind, h, text, why, tags)  # after G6 so G6 errors stay reported first

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
    """(code, label, id) for the reference and every ok/acceptable alternative, reference first."""
    out = [(lesson["solution"]["code"], "reference", lesson["id"] + ".reference")]
    for ex in lesson["exercises"]:
        base = [h["ok"][0] for h in ex["holes"]]
        for i, h in enumerate(ex["holes"]):
            alts = [(t, "ok") for t in h["ok"][1:]] + [(a["text"], "acceptable") for a in h.get("acceptable", [])]
            for text, kind in alts:
                fills = list(base)
                fills[i] = text
                code = assemble(ex["segments"], fills)
                if code not in [c for c, _, _ in out]:
                    out.append((code, "%s hole %d = %s (%s)" % (ex["id"], i + 1, text, kind),
                                "%s.h%d:%s" % (ex["id"], i + 1, text)))
    return out


def wrong_programs(lesson, sc):
    """One entry per unique program: ok[0] everywhere except hole i = a chip not correct for it."""
    by_code = {}
    for ex, sx in zip(lesson["exercises"], sc["exercises"]):
        base = [h["ok"][0] for h in ex["holes"]]
        for i, h in enumerate(ex["holes"]):
            correct = set(h["ok"]) | set(a["text"] for a in h.get("acceptable", []))
            for chip in [c["text"] for c in ex["bank"] if c["text"] not in correct]:
                fills = list(base)
                fills[i] = chip
                code = assemble(ex["segments"], fills)
                pid = "%s.h%d:%s" % (ex["id"], i + 1, chip)
                e = by_code.setdefault(code, {"id": pid, "ids": [], "lesson": lesson["id"], "claims": [],
                                              "label": "%s hole %d = %s" % (ex["id"], i + 1, chip)})
                e["ids"].append(pid)
                claim = claim_for(ex["id"], sx["bullets"], i + 1, chip)
                if claim and claim not in e["claims"]:
                    e["claims"].append(claim)
    return [(code, e) for code, e in by_code.items()]


def claim_for(ex_id, bullets, hole, chip):
    """The bullet whose reason the app shows for (hole, chip): @H override first, else the chip why."""
    for kind, h, text, why, tags in bullets:
        if kind == "@" and h == hole and text == chip:
            return {"bullet": "%s @%d `%s`" % (ex_id, h, text), "scope": "hole", "tags": list(tags)}
    for kind, h, text, why, tags in bullets:
        if kind == "-" and text == chip and why:
            return {"bullet": "%s `%s`" % (ex_id, text), "scope": "chip", "tags": list(tags)}
    return None


# ponytail: shims only the JVM-only Map.getOrDefault used by note 05 level 1; add another shim
# here if a future note calls more JVM-only API. On JVM the real member wins over this extension.
GET_OR_DEFAULT_SHIM = """@Suppress("EXTENSION_SHADOWED_BY_MEMBER")
private fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: V): V {
    @Suppress("UNCHECKED_CAST")
    return if (containsKey(key)) get(key) as V else defaultValue
}
"""

TICK_LINE = re.compile(r"^(\s*while\s*\(.*\)\s*\{)\s*$")


def prelude():
    """Common-only test harness shared by ReferenceSolutionsTest.kt and every wrong program.
    case {} classifies one check: AssertionError -> wrong, HangError -> hang, other Throwable -> crash."""
    return GET_OR_DEFAULT_SHIM + """
private class HangError : Error()

private object Probe {
    var steps = 0
    val kinds = mutableSetOf<String>()
    fun reset() {
        steps = 0
        kinds.clear()
    }
}

// injected after every `while (...) {`; deterministic hang detection, no threads (JVM and Native)
private fun tick() {
    if (++Probe.steps > 1_000_000) throw HangError()
}

private fun case(f: () -> Unit) {
    Probe.steps = 0
    try {
        f()
    } catch (e: AssertionError) {
        Probe.kinds += "wrong"
    } catch (e: HangError) {
        Probe.kinds += "hang"
    } catch (e: Throwable) {
        Probe.kinds += "crash"
    }
}

private fun pw(b: Int, e: Int): Int {
    var r = 1
    repeat(e) { r *= b }
    return r
}
"""


def instrument(code, where):
    """Append tick() to every `while (...) {` line; G11 for loops it cannot instrument."""
    out = []
    for ln in code.split("\n"):
        m = TICK_LINE.match(ln)
        if m:
            out.append(m.group(1) + " tick()")
        elif re.search(r"\bwhile\b|\bdo\s*\{", ln):
            raise GenError(where, "G11", "loop the tick injector cannot instrument: " + ln.strip())
        else:
            out.append(ln)
    return "\n".join(out)


def render_object(name, label, code, examples, checks, with_kinds):
    """Same body for both outputs, so the JVM verdict on a wrong program matches what Native would see."""
    body = "\n".join("    " + ln if ln else ln for ln in instrument(code, label).split("\n"))
    cases = "".join("        case { %s }\n" % e for e in examples)
    chk = "".join("        %s\n" % ln if ln else "\n" for ln in checks.split("\n")) if checks else ""
    kinds = ("\n    fun kinds(): String {\n        Probe.reset()\n        run()\n"
             "        return Probe.kinds.sorted().joinToString(\",\")\n    }\n") if with_kinds else ""
    return ("%sobject %s { // %s\n%s\n\n    fun run() {\n%s        checks()\n    }\n\n"
            "    @Suppress(\"UNUSED_VARIABLE\")\n    private fun checks() {\n"
            "        val rnd = kotlin.random.Random(1234)\n%s    }\n%s}\n"
            % ("" if with_kinds else "private ", name, label, body, cases, chk, kinds))


HEADER = "// GENERATED by tools/gen_content.py from content/authoring/*.md. Do not edit; rerun the generator.\n"


def render_kt(items):
    objs, tests = [], []
    for lesson, sc in items:
        prefix = "L" + lesson["id"][1:].replace(".", "_")
        for n, (code, label, _) in enumerate(variants(lesson)):
            name = "%s_v%d" % (prefix, n)
            objs.append(render_object(name, label, code, sc["top"]["examples"], sc["top"].get("checks"), False))
            tests.append("    @Test\n    fun %s() {\n        Probe.reset()\n        %s.run()\n"
                         "        assertEquals(emptySet<String>(), Probe.kinds)\n    }\n" % (name.lower(), name))
    return (HEADER + "package com.tlenparty.algolingo.lesson\n\nimport kotlin.test.*\n\n" + prelude() + "\n"
            + "\n".join(objs) + "\nclass ReferenceSolutionsTest {\n" + "\n".join(tests) + "}\n")


def render_wrong_json(items):
    """Full program per wrong (hole, chip) pair for the JVM classification test (WrongChipsTest)."""
    def src(label, code, sc):
        return (HEADER + "import kotlin.test.*\n\n" + prelude() + "\n"
                + render_object("V", label, code, sc["top"]["examples"], sc["top"].get("checks"), True))

    out = []
    for lesson, sc in items:
        # control: the reference through the same harness must compile and run clean, so a broken
        # harness/classpath for this lesson fails loudly instead of "proving" every [compile] claim
        cid = lesson["id"] + ".control"
        out.append({"id": cid, "ids": [cid], "lesson": lesson["id"], "label": "reference (control)",
                    "claims": [], "control": True, "source": src("reference (control)", lesson["solution"]["code"], sc)})
        for code, e in wrong_programs(lesson, sc):
            out.append({"id": e["id"], "ids": e["ids"], "lesson": e["lesson"], "label": e["label"],
                        "claims": e["claims"], "source": src(e["label"], code, sc)})
    return dump(out)


# ---------- python lessons ----------

PY_TIMEOUT = 10  # seconds per program; a hang is killed and counts as a failing run

PY_PRELUDE = """
import sys as _sys
import traceback as _tb
_fails = []
_kinds = set()


class case:
    \"\"\"with case(): one check; any Exception is recorded as a failure and the module goes on.\"\"\"
    def __enter__(self):
        return self

    def __exit__(self, t, e, tb):
        if t is not None and issubclass(t, Exception):
            _fails.append("line %d: %s" % (tb.tb_lineno, "".join(_tb.format_exception_only(t, e)).strip()))
            _kinds.add(t.__name__)
            return True


def pw(b, e):
    return b ** e
"""


def py_module(code, examples, checks):
    """Variant code, then the case()/pw prelude, each example in its own case, checks at top level.
    The __future__ import lets `tuple[int, int] | None` annotations run on python3 < 3.10."""
    return ("from __future__ import annotations\n" + code + "\n\n" + PY_PRELUDE + "\n"
            + "".join("with case():\n    %s\n" % e for e in examples) + (checks or "")
            + "\n\nif _fails:\n    print(\"%d failed, first: %s\" % (len(_fails), _fails[0]))\n"
            + "    print(\"" + KINDS_LINE + "\" + \",\".join(sorted(_kinds)))\n    _sys.exit(1)\n")


KINDS_LINE = "GSD_KINDS: "  # last stdout line of a failing py_module: sorted exception type names of failed cases


def run_python(source, timeout=None):
    """(ok, detail): run source with `python3 -I` in a fresh temp dir, env = PATH only."""
    return run_python_kinds(source, timeout)[:2]


def run_python_kinds(source, timeout=None):
    """(ok, detail, exception type names): None = timeout; no KINDS_LINE on a failing run = {"?"}
    (crashed outside any case). detail = last 3 output lines without the KINDS_LINE."""
    exe = shutil.which("python3")
    if not exe:
        raise GenError("python3", "G12", "python3 not found in PATH (needed to check python lessons)")
    timeout = timeout or PY_TIMEOUT
    with tempfile.TemporaryDirectory() as d:
        path = os.path.join(d, "variant.py")
        with open(path, "w", encoding="utf-8") as f:
            f.write(source)
        try:
            r = subprocess.run([exe, "-I", path], cwd=d, stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                               stderr=subprocess.STDOUT, env={"PATH": os.environ.get("PATH", "")},
                               timeout=timeout, encoding="utf-8", errors="replace")
        except subprocess.TimeoutExpired:
            return False, "timeout after %ss" % timeout, None
    lines = r.stdout.strip().splitlines()
    names = set() if r.returncode == 0 else {"?"}
    if lines and lines[-1].startswith(KINDS_LINE):
        names = set(n for n in lines.pop()[len(KINDS_LINE):].split(",") if n)
    return r.returncode == 0, " | ".join(lines[-3:]) or "exit code %d" % r.returncode, names


def python_kinds(source, timeout=None):
    """Observed outcome kinds of a program: {compile} on SyntaxError (compiled only, never exec'd here),
    {hang} on timeout, else wrong per AssertionError and crash per any other exception; set() = runs clean.
    ponytail: a timeout hides the kinds of the cases that ran before it; per-case step limits like
    Kotlin's Probe if a lesson ever needs [hang] together with another tag on one chip."""
    try:
        compile(source, "variant", "exec")
    except SyntaxError:
        return {"compile"}
    names = run_python_kinds(source, timeout)[2]
    if names is None:
        return {"hang"}
    return set("wrong" if n == "AssertionError" else "crash" for n in names)


def claim_mismatch(observed):
    """G14 over [(wrong program entry, its kinds)], like WrongChipsTest: a hole claim's tags must all be
    seen in its own program, a chip claim's tags in the union over every program carrying that claim.
    Returns the first "bullet: заявлено X, на деле Y" or None."""
    union = {}
    for e, kinds in observed:
        for c in e["claims"]:
            if c["scope"] == "chip":
                union.setdefault(c["bullet"], set()).update(kinds)
    for e, kinds in observed:
        for c in e["claims"]:
            seen = kinds if c["scope"] == "hole" else union[c["bullet"]]
            if not set(c["tags"]) <= seen:
                return "%s: заявлено %s, на деле %s" % (c["bullet"], "/".join(c["tags"]),
                                                       "/".join(sorted(seen)) or "ok")
    return None


def python_gates(lesson, sc):
    """G12: reference + every ok/acceptable variant passes examples + checks.
    G13: every wrong (hole, chip) program fails them.
    G14: the kind tags of each claim are all observed (python_kinds, claim_mismatch)."""
    ex, chk = sc["top"]["examples"], sc["top"]["checks"]
    for code, label, _ in variants(lesson):
        ok, detail = run_python(py_module(code, ex, chk))
        if not ok:
            raise GenError(sc["file"], "G12", "%s: %s" % (label, detail))
    observed = []
    for code, e in wrong_programs(lesson, sc):
        kinds = python_kinds(py_module(code, ex, chk))
        if not kinds:
            raise GenError(sc["file"], "G13", "%s: chip does not fail any check" % e["label"])
        observed.append((e, kinds))
    bad = claim_mismatch(observed)
    if bad:
        raise GenError(sc["file"], "G14", bad)


def render_all(repo_root, allow_removed=()):
    root = Path(repo_root)
    note_lines = (root / NOTE).read_text(encoding="utf-8").splitlines()
    items = []
    for p in sorted((root / AUTHORING).glob("*.md"), key=lambda p: id_key(p.stem)):
        sc = parse_sidecar(p.read_text(encoding="utf-8"), p.stem)
        items.append((build_lesson(sc, note_lines), sc))
    ids = [lesson["id"] for lesson, _ in items]
    py_items = []
    for p in sorted((root / AUTHORING_PY).glob("*.md"), key=lambda p: id_key(p.stem)):
        sc = parse_sidecar(p.read_text(encoding="utf-8"), p.stem, "python")
        if sc["id"] not in ids:
            raise GenError(sc["file"], "G7", "no Kotlin lesson %s/%s.md to pair with" % (AUTHORING, sc["id"]))
        lesson = build_lesson(sc, note_lines)
        python_gates(lesson, sc)
        py_items.append((lesson, sc))
    py_ids = set(lesson["id"] for lesson, _ in py_items)
    old = root / INDEX
    if old.is_file():
        try:
            old_ids = [e["id"] for e in json.loads(old.read_text(encoding="utf-8"))["lessons"]]
        except (ValueError, KeyError, TypeError) as e:
            raise GenError(INDEX, "syntax", "cannot read lesson ids: %r" % e)
        for oid in old_ids:
            if oid not in ids and oid not in allow_removed:
                raise GenError(INDEX, "G4", "lesson id %s disappeared (pass --allow-removed %s)" % (oid, oid))
    entries = [{"id": l["id"], "title": l["title"]} for l, _ in items]
    for e in entries:
        if e["id"] in py_ids:
            e["langs"] = ["kotlin", "python"]
    out = {INDEX: dump({"schema": 1, "lessons": entries})}
    for lesson, _ in items:
        out["%s/%s.json" % (LESSONS, lesson["id"])] = dump(lesson)
    for lesson, _ in py_items:
        out["%s/%s.json" % (LESSONS_PY, lesson["id"])] = dump(lesson)
    out[TEST_KT] = render_kt(items)
    # WRONG_JSON sits in `out`, so --check compares it like every other output
    out[WRONG_JSON] = render_wrong_json(items)
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
    orphans = ["%s/%s" % (d, p.name) for d in (LESSONS, LESSONS_PY) for p in sorted((root / d).glob("*.json"))
               if "%s/%s" % (d, p.name) not in out]
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

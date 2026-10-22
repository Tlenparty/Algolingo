#!/usr/bin/env python3
"""Copy the interview-prep Obsidian vault into the app bundle and write kb/index.json.

Usage: python3 tools/sync_notes.py <vault_root>   (or env ALGOLINGO_VAULT)
<vault_root> is the folder that contains Алгоритмы/, Матрицы/, Картинки/ ...
"""
import hashlib
import json
import os
import re
import shutil
import subprocess
import sys
import unicodedata
from collections import Counter
from pathlib import Path


def nfc(s):
    return unicodedata.normalize("NFC", s)


# Allow-list and order in one: (folder relative to the vault root, NFC) -> section.
# Subfolders inherit their parent's section; anything not listed is skipped.
FOLDERS = [(nfc(f), s) for f, s in [
    ("Алгоритмы", "Алгоритмы"),
    ("Матрицы/Котлин, Java", "Kotlin / Java"),
    ("Матрицы/Многопоточность", "Многопоточность"),
    ("Матрицы/Coroutines", "Корутины и Flow"),
    ("Матрицы/Compose", "Compose"),
    ("Матрицы/Android", "Android"),
    ("Матрицы/Архитектура", "Архитектура"),
    ("Матрицы/Performance", "Performance"),
    ("Матрицы/KMP", "KMP"),
    ("SystemDesign", "System Design"),
    ("Матрицы/Исключения", "Исключения"),
    ("Вопросы с собесов", "Вопросы с собесов"),
    ("Матрицы/Общие вопросы", "Общие вопросы"),
]]
ROOT_NOTES = {nfc("СОДЕРЖАНИЕ"), nfc("Подготовка к собеседованию")}
ROOT_SECTION = "Обзор"
# Notes lying directly in Матрицы/ (no topic subfolder).
TITLE_SECTION = {nfc("Плотность экрана в Android"): "Android", nfc("Свой DI на коленке"): "Архитектура"}
SECTIONS = [ROOT_SECTION] + [s for _, s in FOLDERS]

EMBED = re.compile(r"(?m)^[ \t]*!\[\[([^\]|]+\.(?:png|jpe?g))(?:\|[^\]]*)?\]\][ \t]*$", re.IGNORECASE)
MAX_W = 1080


def section_of(parent, title):
    if parent == "" and title in ROOT_NOTES:
        return ROOT_SECTION
    if parent == nfc("Матрицы") and title in TITLE_SECTION:
        return TITLE_SECTION[title]
    best = None
    for f, s in FOLDERS:
        if (parent == f or parent.startswith(f + "/")) and (best is None or len(f) > len(best[0])):
            best = (f, s)
    return best[1] if best else None


def copy_image(src, dst):
    out = subprocess.run(["sips", "-g", "pixelWidth", str(src)], check=True, capture_output=True, text=True).stdout
    if int(out.split()[-1]) <= MAX_W:
        shutil.copyfile(src, dst)
    else:  # never `-Z`: it upscales small images
        subprocess.run(["sips", "--resampleWidth", str(MAX_W), str(src), "--out", str(dst)], check=True, capture_output=True)


def main():
    vault = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("ALGOLINGO_VAULT")
    if not vault:
        print(__doc__)
        return 2
    vault = Path(vault)
    if not (vault / "Алгоритмы").is_dir():
        print("pass the vault root (folder containing Алгоритмы/)")
        return 1
    out = Path(__file__).resolve().parent.parent / "shared/src/commonMain/composeResources/files/kb"

    # Read everything first: a decode error or a guard must not leave the bundle half-deleted.
    notes, skipped = [], Counter()
    for p in vault.rglob("*.md"):
        rel = Path(nfc(p.relative_to(vault).as_posix()))
        parent = "" if rel.parent == Path(".") else rel.parent.as_posix()
        title = nfc(p.stem)
        section = section_of(parent, title)
        if section is None:
            skipped[rel.parts[0] if len(rel.parts) > 1 else "/"] += 1
            continue
        notes.append((section, title, nfc(p.read_text(encoding="utf-8")).replace("\r\n", "\n")))
    if not notes:
        print(f"no notes found in {vault}")
        return 1

    seen_titles, seen_ids = {}, {}
    for _, title, _ in notes:
        key = title.casefold()
        if key in seen_titles:
            print(f"duplicate title: {title!r} / {seen_titles[key]!r}")
            return 1
        seen_titles[key] = title
        # Stable, ASCII-only id: title only, so moving a note between folders keeps it.
        nid = "n" + hashlib.sha1(title.encode("utf-8")).hexdigest()[:8]
        if nid in seen_ids:
            print(f"duplicate id {nid}: {title!r} / {seen_ids[nid]!r}")
            return 1
        seen_ids[nid] = title

    imgs = {nfc(p.name): p for p in (vault / "Картинки").glob("*") if p.is_file()} if (vault / "Картинки").is_dir() else {}

    # Build the new bundle (notes + img/) in a sibling temp dir, then swap it in.
    tmp = out.with_name(out.name + ".tmp")
    shutil.rmtree(tmp, ignore_errors=True)
    (tmp / "img").mkdir(parents=True)
    missing_imgs = set()

    def embed(m):
        name = nfc(m.group(1).strip())
        if name not in imgs:
            missing_imgs.add(name)
            return m.group(0)
        f = "i" + hashlib.sha1(name.encode("utf-8")).hexdigest()[:8] + Path(name).suffix.lower()
        if not (tmp / "img" / f).exists():
            copy_image(imgs[name], tmp / "img" / f)
        return f"![[{f}]]"

    entries = []
    for section, title, text in notes:
        nid = "n" + hashlib.sha1(title.encode("utf-8")).hexdigest()[:8]
        (tmp / f"{nid}.md").write_bytes(EMBED.sub(embed, text).encode("utf-8"))
        entries.append({"id": nid, "title": title, "section": section, "file": f"{nid}.md"})
    entries.sort(key=lambda e: (SECTIONS.index(e["section"]), e["title"].casefold()))
    (tmp / "index.json").write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    old = out.with_name(out.name + ".old")
    shutil.rmtree(old, ignore_errors=True)
    if out.exists():
        out.rename(old)
    tmp.rename(out)
    shutil.rmtree(old, ignore_errors=True)

    broken = set()
    for _, _, text in notes:
        for m in re.finditer(r"\[\[([^\]]+)\]\]", text):
            target = re.split(r"[#|]", m.group(1))[0].strip()
            if target and target.casefold() not in seen_titles and not re.search(r"\.(png|jpe?g)$", target, re.I):
                broken.add(target.lower())
    counts = Counter(e["section"] for e in entries)
    for s in SECTIONS:
        print(f"  {s}: {counts[s]}")
    print(f"notes: {len(entries)}, sections: {len(counts)}, images: {len(list((out / 'img').iterdir()))}")
    print("skipped .md per top folder:", dict(sorted(skipped.items())))
    print("unresolved wikilinks:", sorted(broken))
    print("missing images:", sorted(missing_imgs))
    return 0


if __name__ == "__main__":
    sys.exit(main())

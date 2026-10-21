#!/usr/bin/env python3
"""Copy Obsidian notes into the app bundle and write kb/index.json.

Usage: python3 tools/sync_notes.py <vault_dir>   (or env ALGOLINGO_VAULT)
"""
import hashlib
import json
import os
import re
import shutil
import sys
import unicodedata
from pathlib import Path

# Editable section table: first match wins, everything else goes to the last section.
SECTIONS = ["Навигация", "Тренажёры", "Задачи с собесов", "Темы"]
PREFIX_SECTION = {"00": 0, "01": 0, "02": 0, "03": 0, "04": 0, "05": 1, "06": 1}
TITLE_SECTION = {
    "Алгоритмы Avito": 2,
    "Алгоритмы Glovo": 2,
    "Задачи с Avito": 2,
    "Задача от Яндекс со станциями": 2,
}


def nfc(s):
    return unicodedata.normalize("NFC", s)


def section_of(title):
    if title in TITLE_SECTION:
        return TITLE_SECTION[title]
    m = re.match(r"(\d\d) ", title)
    return PREFIX_SECTION.get(m.group(1), 3) if m else 3


def main():
    vault = sys.argv[1] if len(sys.argv) > 1 else os.environ.get("ALGOLINGO_VAULT")
    if not vault:
        print(__doc__)
        return 2
    vault = Path(vault)
    if not vault.is_dir():
        print(f"vault not found: {vault}")
        return 1
    out = Path(__file__).resolve().parent.parent / "shared/src/commonMain/composeResources/files/kb"

    # Read everything first: a decode error must not leave the bundle half-deleted.
    notes = sorted(((nfc(p.stem), nfc(p.read_text(encoding="utf-8"))) for p in vault.glob("*.md")))
    if not notes:
        print(f"no notes found in {vault}")
        return 1

    # Build the new bundle in a sibling temp dir, then swap it in.
    tmp = out.with_name(out.name + ".tmp")
    shutil.rmtree(tmp, ignore_errors=True)
    tmp.mkdir(parents=True)
    entries = []
    for title, text in notes:
        # Stable, ASCII-only id: adding/renaming other notes never remaps this one.
        nid = "n" + hashlib.sha1(title.encode("utf-8")).hexdigest()[:8]
        (tmp / f"{nid}.md").write_bytes(text.replace("\r\n", "\n").encode("utf-8"))
        entries.append({"id": nid, "title": title, "section": SECTIONS[section_of(title)], "file": f"{nid}.md"})
    entries.sort(key=lambda e: SECTIONS.index(e["section"]))  # stable: title order kept
    (tmp / "index.json").write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")
    old = out.with_name(out.name + ".old")
    shutil.rmtree(old, ignore_errors=True)
    if out.exists():
        out.rename(old)
    tmp.rename(out)
    shutil.rmtree(old, ignore_errors=True)

    titles = {t.lower() for t, _ in notes}
    broken = set()
    for _, text in notes:
        for m in re.finditer(r"\[\[([^\]]+)\]\]", text):
            target = re.split(r"[#|]", m.group(1))[0].strip().lower()
            if target and target not in titles:
                broken.add(target)
    print(f"notes: {len(entries)}")
    print("unresolved wikilinks:", sorted(broken))
    return 0


if __name__ == "__main__":
    sys.exit(main())

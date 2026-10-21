#!/usr/bin/env python3
"""Copy Obsidian notes into the app bundle and write kb/index.json.

Usage: python3 tools/sync_notes.py <vault_dir>   (or env ALGOLINGO_VAULT)
"""
import json
import os
import re
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
    out.mkdir(parents=True, exist_ok=True)
    for old in list(out.glob("n*.md")) + [out / "index.json"]:
        old.unlink(missing_ok=True)

    notes = sorted(((nfc(p.stem), nfc(p.read_text(encoding="utf-8"))) for p in vault.glob("*.md")))
    entries = []
    for i, (title, text) in enumerate(notes):
        nid = f"n{i:02d}"
        (out / f"{nid}.md").write_bytes(text.replace("\r\n", "\n").encode("utf-8"))
        entries.append({"id": nid, "title": title, "section": SECTIONS[section_of(title)], "file": f"{nid}.md"})
    entries.sort(key=lambda e: SECTIONS.index(e["section"]))  # stable: title order kept
    (out / "index.json").write_text(json.dumps(entries, ensure_ascii=False, indent=1) + "\n", encoding="utf-8")

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

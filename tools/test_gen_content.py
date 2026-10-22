#!/usr/bin/env python3
"""Tests for tools/gen_content.py.

Usage: python3 -m unittest tools/test_gen_content.py
"""
import json
import shutil
import subprocess
import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import gen_content  # noqa: E402

ROOT = Path(__file__).resolve().parent.parent
NOTE_REL = "shared/src/commonMain/composeResources/files/kb/n07ed966c.md"
L04_REL = "shared/src/commonMain/composeResources/files/content/lessons/l0.4.json"
NOTE_LINES = (ROOT / NOTE_REL).read_text(encoding="utf-8").splitlines()
L04 = json.loads((ROOT / L04_REL).read_text(encoding="utf-8"))
REF = L04["solution"]["code"]

E1_CODE = REF.replace("while (lo <= hi)", "while (⟦lo <= hi⟧)")
E2_CODE = E1_CODE.replace("val mid = lo + (hi - lo) / 2", "val mid = ⟦lo + (hi - lo) / 2 | ~(lo + hi) ushr 1⟧")

BASE = """# l0.4
note: 0.4 Бинарный поиск
examples:
- assertEquals(0, binarySearch(intArrayOf(5), 5))

## e1
```kotlin
%s
```
- `lo < hi`: при `lo == hi` цикл не зайдёт: `[5]` вернёт `-1`
? hint one

## e2
```kotlin
%s
```
- ~`(lo + hi) ushr 1`: работает, но эталон читаемее
- `lo < hi`: при `lo == hi` цикл не зайдёт
- @2 `lo < hi`: `mid` станет `Boolean`
""" % (E1_CODE, E2_CODE)


def build(text, stem="l0.4"):
    return gen_content.build_lesson(gen_content.parse_sidecar(text, stem), NOTE_LINES)


class NoteParserGolden(unittest.TestCase):
    def sec(self, heading):
        return gen_content.parse_note_section(NOTE_LINES, heading)

    def test_0_1(self):
        s = self.sec("0.1 Сумма и максимум массива")
        self.assertEqual(s["statement"], "Верни сумму всех чисел массива и (отдельно) максимальный элемент.")
        self.assertEqual(s["trains"], "базовый линейный проход с аккумулятором")
        self.assertEqual(s["targetTimeSec"], 60)
        self.assertEqual((s["time"], s["space"]), ("O(n)", "O(1)"))
        self.assertEqual(s["pitfalls"], [
            "`max = 0` ломается на `[-5, -2]`",
            "Пустой массив — уточни у интервьюера, что делать (кинуть исключение / вернуть null)",
        ])
        self.assertEqual(s["variations"], [])
        self.assertEqual(len(s["hints"]), 1)
        self.assertTrue(s["code"].startswith("fun sumAndMax(nums: IntArray): Pair<Int, Int> {\n"))
        self.assertIn("        if (x > max) max = x\n", s["code"])

    def test_0_2(self):
        s = self.sec("0.2 Разворот строки на месте")
        self.assertEqual(s["statement"], "Переверни `CharArray`: `abcd` → `dcba`. Без создания новой строки.")
        self.assertEqual(s["trains"], "два указателя навстречу (скелет [[04 Маркеры + Заготовки|#3]])")
        self.assertEqual(s["targetTimeSec"], 120)
        self.assertEqual((s["time"], s["space"]), ("O(n)", "O(1)"))
        self.assertEqual(s["pitfalls"], [])
        self.assertEqual(s["variations"], [
            "проверка палиндрома — тот же скелет, но вместо swap сравниваем `s[l] != s[r] → return false`"])
        self.assertEqual(len(s["hints"]), 1)
        self.assertTrue(s["code"].startswith("fun reverse(s: CharArray) {\n"))

    def test_0_3(self):
        s = self.sec("0.3 Палиндром ли строка")
        self.assertEqual(s["statement"],
                         "Читается ли строка одинаково с обеих сторон? (считаем, что регистр и пробелы уже неважны)")
        self.assertEqual(s["trains"], "два указателя навстречу как проверка")
        self.assertEqual(s["targetTimeSec"], 120)
        self.assertEqual((s["time"], s["space"]), ("O(n)", "O(1)"))
        self.assertEqual((s["pitfalls"], s["variations"], s["hints"]), ([], [], []))
        self.assertTrue(s["code"].endswith("    return true\n}"))

    def test_0_4_matches_committed_json(self):
        s = self.sec("0.4 Бинарный поиск")
        self.assertEqual(s["title"], L04["title"])
        self.assertEqual(s["statement"], L04["statement"])
        self.assertEqual(s["trains"], L04["trains"])
        self.assertEqual(s["targetTimeSec"], L04["targetTimeSec"])
        self.assertEqual(s["code"], REF)
        self.assertEqual(s["time"], L04["solution"]["time"])
        self.assertEqual(s["space"], L04["solution"]["space"])
        self.assertEqual(s["pitfalls"], L04["solution"]["pitfalls"])
        self.assertEqual(len(s["hints"]), 1)

    def test_split_items_ignores_separators_in_inline_code(self):
        self.assertEqual(gen_content.split_items("`l++; r--` ломает всё. `a. B` тоже; конец."),
                         ["`l++; r--` ломает всё", "`a. B` тоже", "конец"])

    def test_unquote_keeps_arrows_and_comparisons(self):
        code = self.sec("0.4 Бинарный поиск")["code"]
        self.assertIn("nums[mid] == target -> return mid", code)
        self.assertIn("while (lo <= hi)", code)
        self.assertEqual(gen_content.unquote(["> a -> b", ">", "> x >= y", "after"]), ["a -> b", "", "x >= y"])


class Validation(unittest.TestCase):
    def assertRule(self, rule, fn, *args):
        with self.assertRaises(gen_content.GenError) as cm:
            fn(*args)
        self.assertEqual(cm.exception.rule, rule, str(cm.exception))

    def test_base_sidecar_is_valid(self):
        lesson = build(BASE)
        self.assertEqual([len(e["holes"]) for e in lesson["exercises"]], [1, 2])
        self.assertEqual(lesson["solution"]["code"], REF)

    def test_g1_hole_without_correct_entry_fails(self):
        self.assertRule("G1", build, BASE.replace("while (⟦lo <= hi⟧)", "while (⟦~lo <= hi⟧)", 1))

    def test_g1_empty_entry_fails(self):
        self.assertRule("G1", build, BASE.replace("⟦lo <= hi⟧", "⟦lo <= hi | ⟧", 1))

    def test_g2_distractor_without_why_fails(self):
        self.assertRule("G2", build, BASE.replace("- `lo < hi`: при `lo == hi` цикл не зайдёт: `[5]` вернёт `-1`",
                                                  "- `lo < hi`"))

    def test_g3_why_without_counterexample_fails(self):
        self.assertRule("G3", build, BASE.replace("- `lo < hi`: при `lo == hi` цикл не зайдёт\n",
                                                  "- `lo < hi`: просто неверно\n"))

    def test_g3_override_without_counterexample_fails(self):
        self.assertRule("G3", build, BASE.replace("- @2 `lo < hi`: `mid` станет `Boolean`", "- @2 `lo < hi`: нет"))

    def test_g4_removed_lesson_id_fails_unless_allowed(self):
        tmp = Path(tempfile.mkdtemp())
        try:
            note = tmp / NOTE_REL
            note.parent.mkdir(parents=True)
            shutil.copy(str(ROOT / NOTE_REL), str(note))
            (tmp / "content/authoring").mkdir(parents=True)
            (tmp / "content/authoring/l0.4.md").write_text(BASE, encoding="utf-8")
            index = tmp / "shared/src/commonMain/composeResources/files/content/index.json"
            index.parent.mkdir(parents=True)
            index.write_text('{"schema":1,"lessons":[{"id":"l0.4","title":"x"},{"id":"l0.9","title":"y"}]}',
                             encoding="utf-8")
            self.assertRule("G4", gen_content.render_all, tmp)
            out = gen_content.render_all(tmp, allow_removed=("l0.9",))
            self.assertIn("l0.4", out["shared/src/commonMain/composeResources/files/content/index.json"])
        finally:
            shutil.rmtree(str(tmp))

    def test_g5_stripped_code_differs_from_note_fails(self):
        self.assertRule("G5", build, BASE.replace("var lo = 0", "var lo = 1", 1))

    def test_g5_exercises_disagree_fails(self):
        # comments are ignored vs the note, but every exercise must yield the same code
        self.assertRule("G5", build, BASE.replace("var lo = 0", "var lo = 0 // e1", 1))

    def test_g6_acceptable_without_note_fails(self):
        self.assertRule("G6", build, BASE.replace("- ~`(lo + hi) ushr 1`: работает, но эталон читаемее\n", ""))

    def test_g6_override_hole_out_of_range_fails(self):
        self.assertRule("G6", build, BASE.replace("- @2 `lo < hi`", "- @3 `lo < hi`"))

    def test_g6_override_on_correct_text_fails(self):
        self.assertRule("G6", build, BASE + "- @1 `lo <= hi`: `x`\n")

    def test_duplicate_sections_and_bullets_fail(self):
        self.assertRule("G7", build, BASE.replace("## e2", "## e1"))
        self.assertRule("G6", build, BASE + "- @2 `lo < hi`: `mid` снова `Boolean`\n")
        self.assertRule("G6", build, BASE + "- ~`(lo + hi) ushr 1`: ещё раз `x`\n")

    def test_g7_bank_too_big_fails(self):
        extra = "".join("- `d%d`: `x`\n" % i for i in range(9))
        self.assertRule("G7", build, BASE.replace("? hint one\n", extra + "? hint one\n"))

    def test_g7_non_increasing_hole_counts_fails(self):
        text = BASE.split("## e2")[0] + "## e2\n```kotlin\n" + E1_CODE + "\n```\n"
        self.assertRule("G7", build, text)

    def test_g7_id_not_file_stem_fails(self):
        self.assertRule("G7", gen_content.parse_sidecar, BASE, "l0.3")

    def test_g7_unknown_note_heading_fails(self):
        self.assertRule("G7", build, BASE.replace("note: 0.4 Бинарный поиск", "note: 0.9 Нет такой"))

    def test_theory_card_unknown_key_fails(self):
        card = "## theory Грабли\npitfalls:\n- `x`\n\n## e1"
        self.assertEqual(build(BASE.replace("## e1", card, 1))["theory"], [{"title": "Грабли", "pitfalls": ["`x`"]}])
        self.assertRule("syntax", build, BASE.replace("## e1", card.replace("pitfalls:", "pitfal:"), 1))

    def test_key_value_shape_is_checked(self):
        one_line = BASE.replace("examples:\n- ", "examples: ", 1)
        self.assertRule("syntax", build, one_line)
        self.assertRule("syntax", build, BASE.replace("## e1", "## theory T\nbody:\n- x\n\n## e1", 1))
        self.assertRule("syntax", build, BASE.replace("## e1", "## theory T\nskeleton: x\n\n## e1", 1))

    def test_malformed_note_and_index_fail_with_generror(self):
        sec = ["## 0.9 X", "> [!success] Решение"]
        cases = [sec + ["> no fence"],  # no ```kotlin
                 sec + ["> ```kotlin", "> f()", "> ```", "> **Сложность:** O(n)"],  # one complexity
                 ["## 0.9 X", "text only"],  # no [!success]
                 sec + ["> ```kotlin", "> f()", "> ```", "> **Сложность:** O(n) / O(1)", "> ⚠️ **Грабли:**"]]
        for note in cases[:3]:
            self.assertRule("G7", gen_content.parse_note_section, note, "0.9 X")
        self.assertIsNone(gen_content.parse_note_section(cases[3], "0.9 X")["pitfalls"])
        nested = sec + ["> ```kotlin", "> f()", "> ```", "> **Сложность:** O(min(n, m)) / O(1)"]
        self.assertEqual(gen_content.parse_note_section(nested, "0.9 X")["time"], "O(min(n, m))")
        self.assertRule("G7", gen_content.parse_sidecar, BASE.replace("# l0.4", "# x4", 1), "x4")
        tmp = Path(tempfile.mkdtemp())
        try:
            shutil.copytree(str(ROOT / "content"), str(tmp / "content"))
            (tmp / NOTE_REL).parent.mkdir(parents=True)
            shutil.copy(str(ROOT / NOTE_REL), str(tmp / NOTE_REL))
            index = tmp / gen_content.INDEX
            index.parent.mkdir(parents=True)
            index.write_text("{not json", encoding="utf-8")
            self.assertRule("syntax", gen_content.render_all, tmp)
        finally:
            shutil.rmtree(str(tmp))

    def test_g8_no_examples_fails(self):
        self.assertRule("G8", build, BASE.replace("examples:\n- assertEquals(0, binarySearch(intArrayOf(5), 5))\n", ""))


class EndToEnd(unittest.TestCase):
    def test_l04_render_is_byte_identical(self):
        out = gen_content.render_all(ROOT)
        self.assertEqual(out[L04_REL], (ROOT / L04_REL).read_text(encoding="utf-8"))

    def test_check_passes_on_committed_tree(self):
        r = subprocess.run([sys.executable, "tools/gen_content.py", "--check"], cwd=str(ROOT),
                           stdout=subprocess.PIPE, stderr=subprocess.PIPE, universal_newlines=True)
        self.assertEqual(r.returncode, 0, r.stdout + r.stderr)

    def test_write_then_check_and_fail_closed(self):
        tmp = Path(tempfile.mkdtemp())
        try:
            note = tmp / NOTE_REL
            note.parent.mkdir(parents=True)
            shutil.copy(str(ROOT / NOTE_REL), str(note))
            (tmp / "content/authoring").mkdir(parents=True)
            sidecar = tmp / "content/authoring/l0.4.md"
            sidecar.write_text(BASE.replace("var lo = 0", "var lo = 1", 1), encoding="utf-8")
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 1)
            self.assertFalse((tmp / "shared/src/commonTest").exists())  # nothing written on error
            sidecar.write_text(BASE, encoding="utf-8")
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 1)  # outputs missing = stale
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 0)
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 0)
            kt = (tmp / gen_content.TEST_KT).read_text(encoding="utf-8")
            self.assertIn("GENERATED by tools/gen_content.py", kt)
            self.assertIn("ushr", kt)
            self.assertNotIn(str(tmp), kt)
            orphan = tmp / gen_content.LESSONS / "l0.9.json"
            orphan.write_text("{}", encoding="utf-8")  # lesson dropped via --allow-removed
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 1)
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 0)
            self.assertFalse(orphan.exists())
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 0)
        finally:
            shutil.rmtree(str(tmp))


if __name__ == "__main__":
    unittest.main()

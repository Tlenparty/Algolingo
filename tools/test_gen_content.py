#!/usr/bin/env python3
"""Tests for tools/gen_content.py.

Usage: python3 -m unittest tools/test_gen_content.py
"""
import contextlib
import io
import json
import shutil
import subprocess
import sys
import tempfile
import time
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
checks:
```kotlin
case { }
```

## e1
```kotlin
%s
```
- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт: `[5]` вернёт `-1`
? hint one

## e2
```kotlin
%s
```
- ~`(lo + hi) ushr 1`: работает, но эталон читаемее
- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт
- @2 `lo < hi` [compile]: `mid` станет `Boolean`
""" % (E1_CODE, E2_CODE)


PY_E1 = """def two_sum(nums: list[int], target: int) -> tuple[int, int] | None:
    seen = {}
    for i, x in enumerate(nums):
        if ⟦target - x⟧ in seen:
            return seen[target - x], i
        seen[x] = i
    return None"""
PY_E2 = PY_E1.replace("seen[x] = i", "seen[⟦x⟧] = i")

PY_BASE = """# l1.4
lang: python
note: 1.4 Two Sum (массив НЕ отсортирован)
title: Two Sum
pitfalls:
- Положить `x` в `seen` до проверки: `[3, 2, 4]`, `6` → `(0, 0)`
variations:
- `nums.index(...)` в цикле — O(n²)
examples:
- assert two_sum([2, 7, 11, 15], 9) == (0, 1)
- assert two_sum([3, 2, 4], 6) == (1, 2)
- assert two_sum([1, 2], 7) is None
checks:
```python
import itertools
for n in range(5):
    for a in itertools.product([-1, 0, 1, 2], repeat=n):
        for t in range(-2, 5):
            with case():
                ok = any(a[i] + a[j] == t for i in range(n) for j in range(i + 1, n))
                r = two_sum(list(a), t)
                assert (r is not None and r[0] < r[1] and a[r[0]] + a[r[1]] == t) if ok else r is None
with case():
    assert two_sum([], 0) is None
```

## theory Каркас
skeleton:
```python
for i, x in enumerate(nums):
    if target - x in seen:
        return seen[target - x], i
    seen[x] = i
```

## e1
```python
%s
```
- `x` [wrong]: ищем само число: `[2, 7]`, `9` → `None`
- `target` [wrong]: ищем `target` целиком: `[2, 7]`, `9` → `None`

## e2
```python
%s
```
- `target` [wrong]: ищем `target` целиком: `[2, 7]`, `9` → `None`
- @1 `x` [wrong]: ищем само число: `[2, 7]`, `9` → `None`
- @2 `target - x` [wrong]: кладём дополнение: `[2, 7]`, `9` → `None`
""" % (PY_E1, PY_E2)


def py_root(py_text=PY_BASE, kotlin=True):
    """Temp repo root with note 05, the Kotlin l1.4 sidecar (unless kotlin=False) and a py sidecar."""
    tmp = Path(tempfile.mkdtemp())
    (tmp / NOTE_REL).parent.mkdir(parents=True)
    shutil.copy(str(ROOT / NOTE_REL), str(tmp / NOTE_REL))
    (tmp / "content/authoring/py").mkdir(parents=True)
    if kotlin:
        shutil.copy(str(ROOT / "content/authoring/l1.4.md"), str(tmp / "content/authoring/l1.4.md"))
    (tmp / "content/authoring/py/l1.4.md").write_text(py_text, encoding="utf-8")
    return tmp


def py_build(text):
    return gen_content.build_lesson(gen_content.parse_sidecar(text, "l1.4", "python"), NOTE_LINES)


def build(text, stem="l0.4"):
    return gen_content.build_lesson(gen_content.parse_sidecar(text, stem), NOTE_LINES)


class NoteParserGolden(unittest.TestCase):
    def sec(self, heading):
        return gen_content.parse_note_section(NOTE_LINES, heading)

    def test_0_1(self):
        s = self.sec("0.1 Сумма и максимум массива")
        self.assertEqual(s["statement"], "Верни сумму всех чисел массива и (отдельно) максимальный элемент. Массив непустой.")
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
        self.assertRule("G2", build, BASE.replace("- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт: `[5]` вернёт `-1`",
                                                  "- `lo < hi`"))

    def test_g3_why_without_counterexample_fails(self):
        self.assertRule("G3", build, BASE.replace("- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт\n",
                                                  "- `lo < hi` [wrong]: просто неверно\n"))

    def test_g3_override_without_counterexample_fails(self):
        self.assertRule("G3", build, BASE.replace("- @2 `lo < hi` [compile]: `mid` станет `Boolean`", "- @2 `lo < hi` [compile]: нет"))

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
        extra = "".join("- `d%d` [wrong]: `x`\n" % i for i in range(9))
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


class PySidecar(unittest.TestCase):
    assertRule = Validation.assertRule

    def test_py_sidecar_parses_with_python_fences(self):
        sc = gen_content.parse_sidecar(PY_BASE, "l1.4", "python")
        self.assertEqual(sc["lang"], "python")
        self.assertEqual(sc["file"], "content/authoring/py/l1.4.md")
        self.assertIn("with case():", sc["top"]["checks"])
        self.assertEqual(gen_content.parse_sidecar(BASE, "l0.4")["lang"], "kotlin")
        lesson = py_build(PY_BASE)  # G5 vs the Kotlin note code is skipped for python
        self.assertEqual([len(e["holes"]) for e in lesson["exercises"]], [1, 2])
        self.assertIn("def two_sum", lesson["solution"]["code"])
        self.assertEqual(lesson["solution"]["time"], "O(n)")
        self.assertEqual(lesson["theory"][0]["skeleton"].split("\n")[0], "for i, x in enumerate(nums):")

    def test_lang_must_match_folder_and_be_python(self):
        self.assertRule("syntax", py_build, PY_BASE.replace("lang: python", "lang: java"))
        self.assertRule("syntax", py_build, PY_BASE.replace("lang: python\n", ""))
        self.assertRule("syntax", build, BASE.replace("note:", "lang: python\nnote:", 1))

    def test_fence_language_is_enforced(self):
        self.assertRule("syntax", py_build, PY_BASE.replace("```python\ndef", "```kotlin\ndef", 1))
        self.assertRule("syntax", py_build, PY_BASE.replace("checks:\n```python", "checks:\n```kotlin", 1))
        self.assertRule("syntax", py_build, PY_BASE.replace("skeleton:\n```python", "skeleton:\n```kotlin", 1))
        self.assertRule("syntax", build, BASE.replace("checks:\n```kotlin", "checks:\n```python", 1))

    def test_python_needs_own_pitfalls_and_variations(self):
        self.assertRule("G7", py_build, PY_BASE.replace("pitfalls:\n- Положить `x` в `seen` до проверки: `[3, 2, 4]`, `6` → `(0, 0)`\n", ""))
        self.assertRule("G7", py_build, PY_BASE.replace("variations:\n- `nums.index(...)` в цикле — O(n²)\n", ""))

    def test_python_exercises_still_must_agree(self):
        self.assertRule("G5", py_build, PY_BASE.replace("seen = {}", "seen = dict()", 1))

    def test_render_writes_lessons_py_and_langs(self):
        tmp = py_root()
        try:
            out = gen_content.render_all(tmp)
            py = json.loads(out[gen_content.LESSONS_PY + "/l1.4.json"])
            self.assertIn("def two_sum", py["solution"]["code"])
            self.assertEqual(json.loads(out[gen_content.LESSONS + "/l1.4.json"])["title"], "Two Sum")
            index = json.loads(out[gen_content.INDEX])["lessons"]
            self.assertEqual(index, [{"id": "l1.4", "title": "Two Sum", "langs": ["kotlin", "python"]}])
            self.assertNotIn("two_sum", out[gen_content.TEST_KT])
            self.assertNotIn("two_sum", out[gen_content.WRONG_JSON])
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 0)
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 0)
            orphan = tmp / gen_content.LESSONS_PY / "l0.9.json"
            orphan.write_text("{}", encoding="utf-8")
            self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 1)
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 0)
            self.assertFalse(orphan.exists())
        finally:
            shutil.rmtree(str(tmp))

    def test_python_without_kotlin_pair_fails(self):
        tmp = py_root(kotlin=False)
        try:
            self.assertRule("G7", gen_content.render_all, tmp)
        finally:
            shutil.rmtree(str(tmp))


class PyHarness(unittest.TestCase):
    assertRule = Validation.assertRule
    E1_TAIL = "- `target` [wrong]: ищем `target` целиком: `[2, 7]`, `9` → `None`\n\n## e2"

    def render(self, text):
        tmp = py_root(text)
        try:
            return gen_content.render_all(tmp)
        finally:
            shutil.rmtree(str(tmp))

    def test_run_python_isolated_tempdir(self):
        ok, detail = gen_content.run_python(
            "import os, sys\nassert sys.flags.isolated == 1\nassert 'HOME' not in os.environ\n"
            "open('junk.txt', 'w').write('x')\nprint(os.getcwd())")
        self.assertTrue(ok, detail)
        self.assertFalse(Path(detail.strip()).exists())  # tempdir removed
        self.assertFalse((ROOT / "junk.txt").exists())
        self.assertEqual(gen_content.run_python("raise SystemExit(3)")[0], False)

    def test_module_counts_failed_cases(self):
        src = gen_content.py_module("def f(x):\n    return x", ["assert f(1) == 2", "assert f(2) == 2"], "with case():\n    f(1)")
        ok, detail = gen_content.run_python(src)
        self.assertFalse(ok)
        self.assertIn("AssertionError", detail)
        self.assertTrue(gen_content.run_python(src.replace("f(1) == 2", "f(1) == 1"))[0])

    def test_hang_is_killed(self):
        t = time.time()
        ok, detail = gen_content.run_python("while True:\n    pass", timeout=1)
        self.assertFalse(ok)
        self.assertIn("timeout", detail)
        self.assertLess(time.time() - t, 5)

    def test_valid_python_lesson_passes_gates(self):
        self.assertIn(gen_content.LESSONS_PY + "/l1.4.json", self.render(PY_BASE))

    def test_g12_broken_reference_fails(self):
        with self.assertRaises(gen_content.GenError) as cm:
            self.render(PY_BASE.replace("return None", "return -1"))
        self.assertEqual(cm.exception.rule, "G12")
        self.assertTrue(cm.exception.detail.startswith("reference: "), cm.exception.detail)
        self.assertIn("AssertionError", cm.exception.detail)

    def test_g12_broken_acceptable_variant_fails(self):
        text = PY_BASE.replace("⟦target - x⟧ in seen", "⟦target - x | ~x - target⟧ in seen", 1)
        text = text.replace(self.E1_TAIL, "- ~`x - target`: зеркально\n" + self.E1_TAIL, 1)
        with self.assertRaises(gen_content.GenError) as cm:
            self.render(text)
        self.assertEqual(cm.exception.rule, "G12")
        self.assertIn("l1.4.e1 hole 1 = x - target (acceptable)", cm.exception.detail)

    def test_g13_wrong_chip_that_passes_fails(self):
        text = PY_BASE.replace(self.E1_TAIL, "- `(target - x)` [wrong]: то же самое: `x`\n" + self.E1_TAIL, 1)
        with self.assertRaises(gen_content.GenError) as cm:
            self.render(text)
        self.assertEqual(cm.exception.rule, "G13")
        self.assertIn("l1.4.e1 hole 1 = (target - x)", cm.exception.detail)

    def test_hanging_wrong_chip_is_killed_and_counts_as_failing(self):
        text = PY_BASE.replace(self.E1_TAIL, "- `any(iter(int, 1))` [hang]: вечный цикл: `x`\n" + self.E1_TAIL, 1)
        old = gen_content.PY_TIMEOUT
        gen_content.PY_TIMEOUT = 2
        try:
            t = time.time()
            self.assertIn(gen_content.LESSONS_PY + "/l1.4.json", self.render(text))
            self.assertLess(time.time() - t, 30)
        finally:
            gen_content.PY_TIMEOUT = old

    def g14(self, text):
        """Render must stop with G14 (claimed kind tags not observed); returns the detail."""
        with self.assertRaises(gen_content.GenError) as cm:
            self.render(text)
        self.assertEqual(cm.exception.rule, "G14", cm.exception.detail)
        return cm.exception.detail

    def chip(self, bullet):
        return PY_BASE.replace(self.E1_TAIL, bullet + "\n" + self.E1_TAIL, 1)

    def test_g14_wrong_tag_but_crash(self):
        detail = self.g14(self.chip("- `y` [wrong]: нет такого имени: `[2, 7]`, `9` → `None`"))
        self.assertIn("l1.4.e1 `y`: заявлено wrong, на деле crash", detail)

    def test_g14_compile_tag_but_wrong(self):
        detail = self.g14(PY_BASE.replace("- `x` [wrong]: ищем само", "- `x` [compile]: ищем само", 1))
        self.assertIn("заявлено compile, на деле crash/wrong", detail)  # seen[target - x] -> KeyError too

    def test_g14_hang_tag_but_wrong(self):
        detail = self.g14(PY_BASE.replace("- `x` [wrong]: ищем само", "- `x` [hang]: ищем само", 1))
        self.assertIn("заявлено hang, на деле crash/wrong", detail)

    def test_g14_hole_scope_tag_checked(self):
        detail = self.g14(PY_BASE.replace("- @1 `x` [wrong]", "- @1 `x` [hang]", 1))
        self.assertIn("l1.4.e2 @1 `x`: заявлено hang, на деле crash/wrong", detail)

    def test_g14_over_claim_needs_every_tag(self):
        detail = self.g14(PY_BASE.replace("- `x` [wrong]: ищем само", "- `x` [compile,wrong]: ищем само", 1))
        self.assertIn("заявлено compile/wrong, на деле crash/wrong", detail)

    def test_g14_true_tags_pass(self):
        """G14 accepts tags that match: real SyntaxError [compile], real NameError [crash]."""
        text = self.chip("- `x +` [compile]: не выражение: `x +`")
        text = text.replace(self.E1_TAIL, "- `y` [crash]: нет такого имени: `NameError`\n" + self.E1_TAIL, 1)
        self.assertIn(gen_content.LESSONS_PY + "/l1.4.json", self.render(text))

    def test_outcome_kinds(self):
        k = gen_content.python_kinds
        self.assertEqual(k("x = (\n"), {"compile"})
        self.assertEqual(k("import sys\nsys.exit(0)"), set())
        self.assertEqual(k("raise KeyError(1)"), {"crash"})  # top level, no GSD_KINDS line
        mixed = gen_content.py_module("def f(x):\n    return x", ["assert f(1) == 2"],
                                      "".join("with case():\n    assert f(%d) == 0\n" % i for i in range(1, 6))
                                      + "with case():\n    f(1) + 'a'\n")
        self.assertEqual(k(mixed), {"wrong", "crash"})  # TypeError after 6 assert failures is not hidden
        self.assertEqual(k("while True:\n    pass", timeout=1), {"hang"})

    def test_g14_skips_untagged_and_unclaimed(self):
        """G14 ignores empty tags and programs without a claim; chip tags check the union."""
        e = {"claims": [{"bullet": "b", "scope": "chip", "tags": []}]}
        self.assertIsNone(gen_content.claim_mismatch([(e, {"crash"}), ({"claims": []}, {"wrong"})]))
        e2 = {"claims": [{"bullet": "c", "scope": "chip", "tags": ["wrong"]}]}
        self.assertIsNone(gen_content.claim_mismatch([(e2, {"crash"}), (e2.copy(), {"wrong"})]))  # union
        self.assertEqual(gen_content.claim_mismatch([(e2, {"crash"})]), "c: заявлено wrong, на деле crash")


class Tags(unittest.TestCase):
    assertRule = Validation.assertRule

    def bullets(self, text):
        return gen_content.parse_sidecar(text, "l0.4")["exercises"][1]["bullets"]

    def test_tag_is_parsed_and_kept_out_of_why(self):
        b = self.bullets(BASE.replace("- @2 `lo < hi` [compile]:", "- @2 `lo < hi` [hang]:"))
        self.assertIn(("@", 2, "lo < hi", "`mid` станет `Boolean`", ("hang",)), b)
        self.assertIn(("~", None, "(lo + hi) ushr 1", "работает, но эталон читаемее", ()), b)

    def test_g9_override_without_tag_fails(self):
        self.assertRule("G9", build, BASE.replace("- @2 `lo < hi` [compile]:", "- @2 `lo < hi`:"))

    def test_g9_override_with_two_kinds_fails(self):
        self.assertRule("G9", build, BASE.replace("[compile]", "[compile,wrong]"))

    def test_g9_unknown_kind_fails(self):
        self.assertRule("G9", build, BASE.replace("[compile]", "[boom]"))
        self.assertRule("G9", build, BASE.replace("- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт\n",
                                                  "- `lo < hi` [wrong,boom]: при `lo == hi` цикл не зайдёт\n"))

    def test_g9_distractor_with_why_without_tag_fails(self):
        self.assertRule("G9", build, BASE.replace("- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт\n",
                                                  "- `lo < hi`: при `lo == hi` цикл не зайдёт\n"))

    def test_distractor_tag_set_is_accepted(self):
        build(BASE.replace("- `lo < hi` [wrong]: при `lo == hi` цикл не зайдёт\n",
                           "- `lo < hi` [compile,wrong]: при `lo == hi` цикл не зайдёт\n"))

    def test_distractor_without_why_needs_no_tag(self):
        lesson = build(BASE.replace("? hint one\n", "- `lo != hi`\n- @1 `lo != hi` [wrong]: `[5]` → `-1`\n? hint one\n"))
        self.assertEqual(lesson["exercises"][0]["holes"][0]["wrongWhy"], {"lo != hi": "`[5]` → `-1`"})

    def test_g9_tag_on_distractor_without_why_fails(self):
        # the tag would never become a claim (claim_for needs a why), so it must not pass as proven
        self.assertRule("G9", build, BASE.replace("? hint one\n", "- `lo != hi` [hang]\n- @1 `lo != hi` [wrong]: `[5]` → `-1`\n? hint one\n"))

    def test_g9_tag_on_acceptable_fails(self):
        self.assertRule("G9", build, BASE.replace("- ~`(lo + hi) ushr 1`:", "- ~`(lo + hi) ushr 1` [wrong]:"))

    def test_tags_never_reach_lesson_json(self):
        other = BASE.replace("[wrong]", "[crash]").replace("[compile]", "[hang]")
        self.assertEqual(build(BASE), build(other))
        self.assertNotIn("[wrong]", gen_content.dump(build(BASE)))


class Checks(unittest.TestCase):
    assertRule = Validation.assertRule
    FENCE = "checks:\n```kotlin\ncase { }\n```\n"

    def with_checks(self, body):
        return BASE.replace(self.FENCE, "checks:\n```kotlin\n%s\n```\n" % body, 1)

    def test_checks_fence_is_parsed_and_never_in_json(self):
        sc = gen_content.parse_sidecar(self.with_checks("case { assertEquals(-1, binarySearch(intArrayOf(), 1)) }"), "l0.4")
        self.assertEqual(sc["top"]["checks"], "case { assertEquals(-1, binarySearch(intArrayOf(), 1)) }")
        self.assertEqual(build(self.with_checks("case { assertTrue(true) }")), build(BASE))

    def test_g10_missing_checks_fails(self):
        self.assertRule("G10", build, BASE.replace(self.FENCE, "", 1))

    def test_g10_jvm_only_api_in_checks_fails(self):
        for bad in ("val p = Math.pow(2.0, 3.0)", "val l = java.util.ArrayList<Int>()", "val s = String(charArrayOf('a'))"):
            self.assertRule("G10", build, self.with_checks(bad))

    def test_g10_allows_concat_to_string(self):
        build(self.with_checks("case { assertEquals(\"a\", charArrayOf('a').concatToString()) }"))

    def test_checks_as_one_line_fails(self):
        self.assertRule("syntax", build, BASE.replace("\n## e1", "\nchecks: x\n\n## e1", 1))


class NoPending(unittest.TestCase):
    def test_no_pending_file(self):
        # every claim is a hard gate (phase 07.1): no list may park a failing variant again
        self.assertFalse((ROOT / "content/authoring/pending.txt").exists())


class RenderKt(unittest.TestCase):
    CODE = "data class E(val a: Int)\nfun g(xs: List<E>): Int = xs.sumOf { it.a }"

    def render(self, checks=None):
        lesson = {"id": "l1.9", "solution": {"code": self.CODE}, "exercises": []}
        top = {"examples": ["assertEquals(1, g(listOf(E(1))))"]}
        if checks:
            top["checks"] = checks
        return gen_content.render_kt([(lesson, {"top": top, "exercises": []})])

    def test_examples_run_inside_variant_object(self):
        kt = self.render()
        self.assertNotIn("with(", kt)
        self.assertEqual(kt.count("fun run()"), 1)
        self.assertIn("        case { assertEquals(1, g(listOf(E(1)))) }\n        checks()\n", kt)
        self.assertIn("Probe.reset()\n        L1_9_v0.run()\n        assertEquals(emptySet<String>(), Probe.kinds)", kt)
        pos = [kt.index(s) for s in ("private object L1_9_v0", "data class E", "fun run()", "E(1)")]
        self.assertEqual(pos, sorted(pos))
        self.assertLess(kt.index("E(1)"), kt.index("class ReferenceSolutionsTest"))
        self.assertNotIn("fun kinds()", kt)

    def test_prelude_emitted_once(self):
        kt = self.render()
        for decl in ("fun <K, V> Map<K, V>.getOrDefault(key: K, defaultValue: V): V", "private class HangError",
                     "private object Probe", "private fun tick()", "private fun case(", "private fun pw("):
            self.assertEqual(kt.count(decl), 1, decl)
        self.assertEqual(kt.count('@Suppress("EXTENSION_SHADOWED_BY_MEMBER")'), 1)
        self.assertIn("1_000_000", kt)

    def test_checks_fence_lands_in_checks_function(self):
        kt = self.render(checks="for (n in 0..2) case { assertEquals(n, g(List(n) { E(1) })) }")
        body = kt[kt.index("private fun checks()"):]
        self.assertTrue(body.startswith("private fun checks() {\n        val rnd = kotlin.random.Random(1234)\n"
                                        "        for (n in 0..2) case {"))

    def test_no_variant_is_ignored(self):
        self.assertNotIn("@Ignore", self.render())
        self.assertNotIn("@Ignore", (ROOT / gen_content.TEST_KT).read_text(encoding="utf-8"))

    def test_every_committed_variant_runs(self):
        kt = (ROOT / gen_content.TEST_KT).read_text(encoding="utf-8")
        self.assertEqual(kt.count("private object "), kt.count("fun run()") + 1)  # + private object Probe
        self.assertEqual(kt.count("@Test"), kt.count(".run()"))


class Tick(unittest.TestCase):
    def test_while_with_brace_gets_tick(self):
        code = "fun f() {\n    while (lo <= hi) {\n        lo++\n    }\n}"
        self.assertEqual(gen_content.instrument(code, "x").split("\n")[1], "    while (lo <= hi) { tick()")

    def test_code_without_while_is_unchanged(self):
        code = "fun f(xs: IntArray) {\n    for (x in xs) {\n    }\n}"
        self.assertEqual(gen_content.instrument(code, "x"), code)

    def test_uninstrumentable_loops_fail(self):
        for code in ("    while (i < n) i++", "    while (i < n) { i++ }", "    do {\n        i++\n    } while (i < n)"):
            with self.assertRaises(gen_content.GenError) as cm:
                gen_content.instrument(code, "x")
            self.assertEqual(cm.exception.rule, "G11")


class WrongPrograms(unittest.TestCase):
    def items(self):
        sc = gen_content.parse_sidecar(BASE, "l0.4")
        return [(gen_content.build_lesson(sc, NOTE_LINES), sc)]

    def entries(self):
        return [x for x in json.loads(gen_content.render_wrong_json(self.items())) if not x.get("control")]

    def test_one_control_per_lesson_is_the_reference(self):
        controls = [x for x in json.loads(gen_content.render_wrong_json(self.items())) if x.get("control")]
        self.assertEqual([(c["id"], c["claims"]) for c in controls], [("l0.4.control", [])])
        lesson = self.items()[0][0]
        self.assertIn(gen_content.instrument(lesson["solution"]["code"], "x").split("\n")[0], controls[0]["source"])
        self.assertIn("fun kinds(): String", controls[0]["source"])

    def test_one_program_per_unique_pair(self):
        e = self.entries()
        self.assertEqual([x["id"] for x in e], ["l0.4.e1.h1:lo < hi", "l0.4.e2.h1:lo + (hi - lo) / 2",
                                                "l0.4.e2.h1:(lo + hi) ushr 1", "l0.4.e2.h2:lo <= hi",
                                                "l0.4.e2.h2:lo < hi"])
        self.assertEqual(e[0]["ids"], ["l0.4.e1.h1:lo < hi", "l0.4.e2.h1:lo < hi"])  # identical program merged
        self.assertEqual(len(set(x["source"] for x in e)), len(e))
        self.assertEqual(set(e[0]), {"id", "ids", "lesson", "label", "claims", "source"})
        self.assertEqual(e[0]["lesson"], "l0.4")

    def test_claims_come_from_override_or_distractor(self):
        e = {x["id"]: x for x in self.entries()}
        self.assertEqual(e["l0.4.e1.h1:lo < hi"]["claims"], [
            {"bullet": "l0.4.e1 `lo < hi`", "scope": "chip", "tags": ["wrong"]},
            {"bullet": "l0.4.e2 `lo < hi`", "scope": "chip", "tags": ["wrong"]}])
        self.assertEqual(e["l0.4.e2.h2:lo < hi"]["claims"],
                         [{"bullet": "l0.4.e2 @2 `lo < hi`", "scope": "hole", "tags": ["compile"]}])
        self.assertEqual(e["l0.4.e2.h2:lo <= hi"]["claims"], [])

    def test_source_is_a_complete_program(self):
        src = self.entries()[0]["source"]
        self.assertIn("import kotlin.test.*", src)
        self.assertIn("\nobject V { // l0.4.e1 hole 1 = lo < hi\n", src)
        self.assertIn("while (lo < hi) { tick()", src)
        self.assertIn("fun kinds(): String", src)
        self.assertIn("private class HangError", src)
        self.assertNotIn("package ", src)

    def test_wrong_json_is_a_checked_output(self):
        tmp = Path(tempfile.mkdtemp())
        try:
            (tmp / NOTE_REL).parent.mkdir(parents=True)
            shutil.copy(str(ROOT / NOTE_REL), str(tmp / NOTE_REL))
            (tmp / "content/authoring").mkdir(parents=True)
            (tmp / "content/authoring/l0.4.md").write_text(BASE, encoding="utf-8")
            self.assertIn(gen_content.WRONG_JSON, gen_content.render_all(tmp))
            self.assertEqual(gen_content.main(["--root", str(tmp)]), 0)
            (tmp / gen_content.WRONG_JSON).write_text("[]\n", encoding="utf-8")
            buf = io.StringIO()
            with contextlib.redirect_stdout(buf):
                self.assertEqual(gen_content.main(["--root", str(tmp), "--check"]), 1)
            self.assertEqual(buf.getvalue(), "stale: %s\n" % gen_content.WRONG_JSON)
        finally:
            shutil.rmtree(str(tmp))


class EndToEnd(unittest.TestCase):
    def test_l04_render_is_byte_identical(self):
        out = gen_content.render_all(ROOT)
        self.assertEqual(out[L04_REL], (ROOT / L04_REL).read_text(encoding="utf-8"))
        committed = sorted((ROOT / gen_content.LESSONS).glob("*.json"))
        self.assertGreaterEqual(len(committed), 9)
        for p in committed + [ROOT / gen_content.INDEX]:
            rel = str(p.relative_to(ROOT))
            self.assertEqual(out[rel], p.read_text(encoding="utf-8"), rel)

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

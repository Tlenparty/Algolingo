# Feature Research

**Domain:** Mobile, Duolingo-style trainer for algorithm interviews. Kotlin solutions are assembled from tappable token chips placed into holes in a template, and checked locally and offline.
**Researched:** 2026-10-21
**Confidence:** MEDIUM. Competitor UX comes from third-party reviews and UX case studies, not official specs. The claims about Parsons/distractor learning are backed by peer-reviewed CS-education papers (HIGH). The mapping onto the user's notes was read directly from the files (HIGH).

---

## 0. The user's content and how it maps onto features (read first)

The source notes are in `/Users/user/Yandex.Disk.localized/Матрицы/Алгоритмы/`. They are already close to a lesson spec. A task card in `05 Тренажёр решений` has this structure:

| Note element | Example (level 0) | App feature it feeds |
|---|---|---|
| Heading `## 0.N Title` + statement | "Сумма и максимум массива" | Exercise title and statement |
| `🎯 Тренируем:` | "базовый линейный проход с аккумулятором" | Exercise subtitle and theory-card link (which skeleton) |
| `⏱ Цель:` | 1 / 2 / 2 / 3 min | Shown as info only. These targets are for writing from scratch, not tapping chips (see anti-features) |
| `> [!question]- 💡 Подсказка` (0..N, may be numbered with titles) | 0.1: 1, 0.2: 1, **0.3: 0**, 0.4: 1; `06` step 4: **3 titled hints** | Progressive hints with a penalty. The hint button must handle **zero hints** |
| `> [!success]- ✅ Решение` + kotlin block | Reference code | Template source. Holes are cut out of this code by manual markup |
| `**Сложность:**` | O(n) / O(1) | Post-solve screen |
| `⚠️ Грабли:` | 0.1 `max = 0` breaks on `[-5,-2]`; 0.4 `lo < hi`, `(lo+hi)/2` | **Paired distractors plus wrong-answer explanation text.** This is the best asset in the notes |
| `🔁 Вариации:` | 0.2 → "палиндром — тот же скелет" | Post-solve screen. Also the link to the next exercise |
| "Прогон руками" trace tables (`06`) | i / x / cur / best table | Post-solve "trace" card. Future exercise type: "predict the value" |
| "Контрольные тесты" (`06`) | `[]`, `[0]`, `[1,0,0]`… | Future "predict output" exercise type. Not MVP |
| `> [!warning] Условие тренировки` (`06` step 3) | "решение счётчиком не засчитывается" | Pre-exercise warning card. It also explains why a token-equivalent solution may be **deliberately rejected** |
| Progress table ✅ / 💡 / ❌ | "✅ сам · 💡 с подсказкой · ❌ подсмотрел" | **The three result grades.** The app should use the same three-way grade for XP and for SRS |
| "Три ✅ с интервалами день/3 дня/неделя = освоено" | — | SRS rule: Leitner boxes 1→3→7, "mastered" after the third clean solve |

**Where theory cards come from.** `05` level 0 has **no theory section**, only the single line "Цель уровня: рука сама пишет базовый цикл, индексы и два указателя". Theory has to be put together from other notes:
- `04 Маркеры + Заготовки` §3 "Два указателя навстречу" and §7 "Бинарный поиск". Each has the same layout: `🔑 Маркеры` (signal words) → skeleton code → `⚠️` trap → `🗣` one-line phrase to say aloud. **That is a ready-made 3–4 card theory deck per technique.**
- `03 Тренажёр задач` level 1 (1.1, 1.2, 1.3, 1.5) gives the "signal → method" answer for the same four tasks, which can become a "why this method" card.
- `00 Компас алгоритмов` §2, the "сигнал → метод" table, and §4, the complexity cheat-sheet, are general reference. They suit a "Справочник" screen better than a lesson.
- `06` "Главная идея" (the three hole types, the skeleton with 3 branches) is a model theory deck for a later level.

**Inconsistencies in the content (the parser and markup need to handle these):**
1. 0.2 and 0.3 have **no `⚠️ Грабли`**. 0.2 only has `🔁 Вариации`, and 0.3 also has no hint. Distractors for them have to be written by hand, or taken from `04` §3 ("`l < r`, не `<=`").
2. **The notes contradict each other on `l <= r`.** `04` §3 says `<=` is wrong ("возьмёшь элемент дважды"). The `05` 0.2 hint says `<=` is "не страшно, но лишний шаг". So is `l <= r` in reverse a distractor or an accepted alternative? **The customer has to decide.** It is the textbook case for an "accepted, with a note" state (see the feature table).
3. 0.1 has several correct forms: `max = nums[0]` or `Int.MIN_VALUE`, `if (x > max) max = x` or `max = maxOf(max, x)`, `sum to max` or `Pair(sum, max)`. With chip-per-hole design (below) most of these collapse into "the hole accepts chip A or chip B".
4. `05` defines mastery as 1 / 3 / 7 days. `06`'s progress table only has +1 / +3 columns. Standardise on 1/3/7.
5. Hint formats differ: `💡 Подсказка` vs `💡 Подсказка 1` vs `💡 Подсказка 1 — почему шаг 3 напрямую не подходит`. The parser should read the callout type, not the title.

---

## Feature Landscape

### Table Stakes (Users Expect These)

Duolingo, Mimo and Grasshopper have set what a "Duolingo-like" app is expected to do. Leaving any of these out makes the app feel broken.

| Feature | Why Expected | Complexity | Notes |
|---|---|---|---|
| **Lesson = linear session with a progress bar** | Duolingo/Mimo core loop. The bar shows how much is left | LOW | Bar counts exercises remaining, including ones re-queued after mistakes |
| **Theory cards before exercises** | Mimo opens every lesson with an explainer. Grasshopper had 1–2 "teach" puzzles before "solve" | LOW | Swipe or "Далее" through 2–4 cards: markers → skeleton (monospace code) → trap → spoken phrase. Static text plus code, no interaction |
| **Fill-in-the-holes template with a chip bank** | The product's core mechanic | MEDIUM | Kotlin template rendered as monospace lines with inline hole slots. Bank of chips below |
| **Tap chip → goes into the active hole. Tap a placed chip → returns to the bank** | Duolingo word bank works exactly this way and case studies list it as the "user control" heuristic | LOW | The chip leaves a **grey ghost** in the bank so the layout doesn't reflow. Tap the ghost or the placed chip to return it |
| **Active-hole cursor (default = first empty hole; tapping a hole selects it)** | Duolingo word bank is linear (append only). A code template has holes in fixed places, so the user must be able to fix hole 2 without clearing hole 3 | LOW | PROJECT.md says "ближайшая пустая дырка". Add "tap a hole to target it" too. It's cheap, and without it a single mistake forces undoing everything |
| **"Проверить" disabled until all holes are filled** | Duolingo pattern. Stops accidental partial submissions | LOW | — |
| **Per-hole feedback after Check** | Duolingo shows the correct answer. For code, "which hole was wrong" is the minimum useful feedback | LOW | Green/red on each hole. Bottom sheet shows the correct chip for each wrong hole |
| **Explanation of *why* the wrong chip is wrong** | Duolingo's feedback is "specific and actionable". This is the core value in PROJECT.md: "сразу понять, где грабли" | LOW (code) / MEDIUM (content) | Each distractor chip carries an `explain` string, taken from `⚠️ Грабли`. Generic fallback: "Верно: X" |
| **Wrong exercises re-queued at the end of the lesson** | Duolingo does this. Already chosen in PROJECT.md (no hearts) | LOW | Re-queue the same exercise, with chips re-shuffled. The lesson ends when the queue is empty. Cap re-queues (e.g. twice) so a stuck user can still finish |
| **Lesson-complete screen** | Every gamified learning app has one | LOW | XP earned, clean / with-hint / failed counts, streak +1 animation |
| **XP for completing exercises** | Duolingo, Mimo, SoloLearn all have it | LOW | Simple fixed numbers. Base XP is reduced by hints used and by re-queues |
| **Daily streak** | Duolingo's main retention mechanic. Mimo copies it | LOW | Local date of last completed lesson. Use the local calendar day, not 24 h windows |
| **Locally persisted progress** | Losing progress = uninstall | LOW | Lessons completed, XP, streak, per-exercise SRS state |
| **Lesson / level map (path)** | Duolingo path, Mimo course tree | LOW | MVP: a vertical list of 4 lessons for level 0 with done / locked state is enough. No fancy winding path |
| **Show the reference solution after solving** | AlgoExpert/NeetCode/LeetCode always show the solution plus complexity | LOW | Full reference code with the holes highlighted, plus `Сложность`, `Грабли`, `Вариации` |
| **Hints, revealed one at a time** | LeetCode/AlgoExpert have tiered hints. The notes say "раскрывай по одной" | LOW | Button "Подсказка (n/N)". Hidden when N = 0 (task 0.3) |
| **Exit lesson mid-way with confirmation** | Duolingo pattern | LOW | MVP: progress inside a lesson is not saved. Restart the lesson |
| **Haptic + colour feedback on Check** | Duolingo: soft sound plus firm-but-calm red | LOW | Haptics are enough. Sound is optional |

### Differentiators (Competitive Advantage)

These are aligned with the core value: interview-grade algorithms, quickly on a phone, with the traps shown.

| Feature | Value Proposition | Complexity | Notes |
|---|---|---|---|
| **Distractors = real interview traps, each with its own explanation** | Mimo/SoloLearn distractors are mostly syntax noise. Here every wrong chip is a known bug (`max = 0`, `lo < hi`, `(lo+hi)/2`). Research (Smith et al., ICER 2024) shows distractors built from common errors improve post-test scores by about 12% in practice settings | MEDIUM (content) | **The product's moat.** Put distractors only on the holes that carry the concept, not on every hole |
| **Holes on the decisions, not on boilerplate** | Grasshopper's weakness was that "it was hard to make a mistake". Holes at `while (lo <= hi)`, `mid = …`, `lo = mid + 1` force the actual decision | LOW (code) / MEDIUM (authoring) | A markup rule for the content author, not code |
| **Three-way result grade ✅ / 💡 / ❌, matching the user's own paper progress table** | The user already tracks this way. The app becomes their progress table | LOW | ✅ = first try, no hints. 💡 = hints used or re-queued once. ❌ = revealed / failed. This drives XP **and** SRS |
| **SRS 1 / 3 / 7 days with "mastered" status** | NeetCode and LeetCode have no built-in spaced review. Duolingo's Practice Hub does. The notes define mastery this way | MEDIUM | Leitner boxes `[1, 3, 7]` days. ✅ moves the item up a box. 💡 keeps it in the same box. ❌ resets it to box 1. A ✅ from box 3 = mastered. A "На повтор сегодня (N)" entry on the home screen builds a review session from due exercises |
| **Post-solve "trace" card (прогон руками)** | AlgoExpert has video walkthroughs. A static i/var table is the offline, zero-cost version | LOW | Render a monospace block straight from the note. `06` has these and level 0 doesn't |
| **"Ladder" lessons: next exercise = previous code plus one change, highlighted** | `06` is built around this ("каждый шаг меняет ровно одну деталь") and no competitor does it | MEDIUM | v1.x: diff highlighting between consecutive templates. MVP only needs to keep the order |
| **"Accepted, but…" state** | Interview-relevant nuance (`l <= r` works but adds a step). Duolingo has "you have a typo" as a soft-accept | LOW | A third check outcome: green with an amber note. The chip is in `accepted` with an attached note |
| **Edge-case chips** | The notes say "три четверти ошибок ловятся крайними тестами" | — | Future exercise type ("what does `f([1,0,0])` return?"). Not MVP |
| **Signal → method recognition exercise** | `03 Тренажёр задач` and `00 Компас` hold the whole dataset | MEDIUM | v2 (already listed in PROJECT.md as a future type) |

### Anti-Features (Commonly Requested, Often Problematic)

| Feature | Why Requested | Why Problematic | Alternative |
|---|---|---|---|
| **Atomic tokens (one chip = `lo`, another = `<=`, another = `hi`)** | Feels more like "writing code" | Exponentially many valid orderings (`hi >= lo`), a huge bank on a phone screen, and "multiple accepted answers" becomes intractable. Research: jumbled distractors add extraneous cognitive load | **One hole = one chip, and a chip can be a whole expression** (`lo <= hi`, `lo + (hi - lo) / 2`). Revisit atomic tokens for the v2 "assemble the whole body" type |
| **Comparing whole token sequences against 1–3 full reference variants** (as currently written in PROJECT.md) | Simple to describe | Variants multiply: 0.1 alone has 3 independent choices, so 8 full variants. Hard to author and review | **Per-hole accepted set**: each hole has `correct: [chipIds]` and optionally `acceptedWithNote`. Use whole-solution variants only when holes are coupled (e.g. `for (x in nums)` vs `for (i in nums.indices)` changes the body). Write this up as a decision for requirements |
| **Jumbled distractors on every hole** | "More challenge" | 26% lower success and +14% time with no transfer gain for novices (Harms et al.). Pure noise on a small screen | At most about 1 distractor per concept hole. Bank size ≤ ~10 chips for level 0. Optional later: show only the active hole's candidates (= "paired distractors", the format research recommends) |
| **Hearts / lives** | Duolingo has them | Already rejected in PROJECT.md. Punishes learning on hard content | Re-queue at end of lesson |
| **Timer enforcing `⏱ Цель`** | The notes have target times | The targets are for **writing from scratch**. Tapping 4 chips takes 20 s, so a timer measures tapping speed, not skill. It also creates pressure | Show `⏱ Цель` as info: "на собесе — за 3 мин с нуля". Optionally record time without judging it |
| **Unforgiving streak + push-notification nagging** | Duolingo retention | Single user, self-motivated. Duolingo's own data shows that forgiving streaks (freeze) improve retention, and nag notifications are a widely criticised dark pattern. Notifications also need platform permission work on both OSs | Plain streak counter. Optional 1 free freeze per week later. **No notifications in MVP** |
| **Leagues / leaderboards / social** | Duolingo/Mimo | No backend, single user. Pointless | — |
| **Coins / shop / gems** | Duolingo/Mimo monetisation | No value for a single user. Engagement theatre | XP only |
| **FSRS / SM-2 adaptive scheduling** | "Modern SRS" | Needs per-review grades and parameters. Only 4 items in MVP. The user explicitly chose 1/3/7 | Fixed Leitner 1/3/7. Add a `ponytail:` note that FSRS is the upgrade path once there are 30+ items |
| **Drag-and-drop chips** | Feels tactile | Harder to build reliably in Compose Multiplatform (drag across a scrolling template, iOS gestures), and slower than tapping. Duolingo itself uses tap | Tap only |
| **Keyboard / free code input, compilation, LLM checking** | "Real coding" | Already out of scope in PROJECT.md | — |
| **In-lesson progress persistence (resume mid-lesson)** | Convenience | Lessons are 2–5 min. State sync bugs | Restart the lesson on exit |

---

## Feature Dependencies

```
Content JSON schema (exercise, holes, chips{correct|distractor|acceptedWithNote, explain}, hints[], theory[], post{complexity, traps, variations, trace})
    └──required by──> Theory cards
    └──required by──> Hole exercise (template render + chip bank + active-hole cursor)
                          └──required by──> Check (per-hole correctness)
                                                └──required by──> Feedback sheet (+ distractor explain)
                                                └──required by──> Re-queue at end of lesson
                                                └──required by──> Result grade ✅/💡/❌
                                                                      ├──required by──> XP
                                                                      └──required by──> SRS 1/3/7 ──> "На повтор сегодня" session
Hints ──modifies──> Result grade (any hint ⇒ at best 💡)
Lesson complete ──required by──> Streak
Local persistence ──required by──> XP, Streak, SRS, Lesson map (done/locked)
Post-solve screen (reference + complexity + traps) ──enhanced by──> Trace card

Atomic tokens ──conflicts──> per-hole accepted sets / small bank
Timer on ⏱ ──conflicts──> tap-based skill measurement
```

### Dependency Notes

- **The content schema comes first.** Every UI feature reads it. The `05` card structure, plus a manual markup layer (which code spans are holes, which chips are distractors, explanation text), defines it. Lock the schema before building UI, and test-parse all 4 level-0 tasks plus `06` step 4 (3 titled hints, a warning callout, a trace table) so the schema doesn't break at level 1+.
- **Check semantics decide the content format.** Per-hole accepted sets vs whole-sequence variants has to be settled before markup is written. Changing it later means re-marking all content.
- **Result grade is the single input for XP and SRS.** Define it once (✅ / 💡 / ❌). Don't let XP and SRS compute "success" separately.
- **Review sessions reuse the lesson player.** A "due today" session is just a lesson whose exercise list comes from SRS. No new UI.
- **Streak only depends on lesson completion and persistence.** It can ship in the same phase as XP.

---

## MVP Definition

### Launch With (v1)

- [ ] Content JSON for level 0 (0.1–0.4) with per-hole accepted sets, distractors with explanations, hints (0..N), and post-solve data. **Ships with a validator** (every hole has ≥1 correct chip, every distractor has an `explain`)
- [ ] Theory cards per lesson (from `04` §3/§7 and `03` level 1). They have to be written, since level 0 in `05` has none
- [ ] Hole exercise: template render, chip bank with ghosts, active-hole cursor, tap to place and tap to return
- [ ] Check → per-hole green/red, plus a feedback sheet with the trap explanation and the correct chip
- [ ] Re-queue wrong exercises at the end of the lesson (capped)
- [ ] Progressive hints with a penalty. The button is hidden when there are no hints
- [ ] Post-solve screen: reference code, complexity, traps, variations
- [ ] Result grade ✅/💡/❌ → XP, streak, SRS boxes 1/3/7
- [ ] Home: level-0 lesson list (done/locked) + "Повтор сегодня (N)" + XP / streak counters
- [ ] Local persistence

### Add After Validation (v1.x)

- [ ] "Accepted with note" amber state. Add it if the customer decides `l <= r` and similar cases should be soft-accepted
- [ ] Trace card on the post-solve screen. Add when `06` content is imported
- [ ] Ladder diff highlighting. Add when the `06` lesson is built
- [ ] Per-active-hole candidate filtering (paired distractors). Add if the shared bank feels noisy on levels 1–2
- [ ] Streak freeze (1 per week). Add if a broken streak actually demotivates the user
- [ ] "Справочник" screen from `00 Компас` (signal → method table, complexity cheat-sheet)

### Future Consideration (v2+)

- [ ] Parsons (order lines), whole-body assembly from atomic tokens, signal → method recognition, predict output / edge-case tests. All already listed in PROJECT.md
- [ ] FSRS scheduling. Once there are 30+ items and review history to fit
- [ ] Notifications/reminders. Only if a store release needs retention
- [ ] Compile-and-run on a separate VPS

---

## Feature Prioritization Matrix

| Feature | User Value | Implementation Cost | Priority |
|---|---|---|---|
| Content schema + level-0 JSON + validator | HIGH | MEDIUM | P1 |
| Hole exercise (render, bank, cursor, tap/return) | HIGH | MEDIUM | P1 |
| Check + per-hole feedback + trap explanation | HIGH | LOW | P1 |
| Re-queue at end | HIGH | LOW | P1 |
| Theory cards | MEDIUM | LOW (code) / MEDIUM (writing) | P1 |
| Hints with penalty | MEDIUM | LOW | P1 |
| Post-solve screen | HIGH | LOW | P1 |
| Result grade → XP + streak | MEDIUM | LOW | P1 |
| SRS 1/3/7 + review session | HIGH (matches the user's method) | MEDIUM | P1 |
| Lesson map (list) | MEDIUM | LOW | P1 |
| Accepted-with-note state | MEDIUM | LOW | P2 |
| Trace card | MEDIUM | LOW | P2 |
| Ladder diff highlight | MEDIUM | MEDIUM | P2 |
| Paired (per-hole) candidates | MEDIUM | MEDIUM | P2 |
| Streak freeze | LOW | LOW | P3 |
| Reference screen (Компас) | LOW | LOW | P3 |
| Leagues, coins, hearts, notifications, timer | LOW / negative | MEDIUM | Don't build |

---

## Competitor Feature Analysis

| Feature | Duolingo | Mimo | Grasshopper (shut down 2023) | NeetCode / AlgoExpert | Our Approach |
|---|---|---|---|---|---|
| Lesson structure | Short session, harder in the middle, **ends easy** | Explainer → interactive Q&A | 1–2 teach puzzles → 1–2 solve puzzles per unit | Video + problem list by pattern | Theory cards → 4 hole exercises, easy → hard (level 0 is already ordered) |
| Answer input | Word bank, tap to place/return | Tap-to-fill, multiple choice, later typing | Custom tap keyboard of code tokens | Typed code | Chip per hole, tap to place/return, active-hole cursor |
| Mistakes | Correct answer shown, item repeated at end, Practice Hub "Mistakes" | Hearts | Real-time feedback | Run tests | Per-hole red plus trap explanation, re-queue at end, SRS reset |
| Hints | Tips per unit | — | Feedback | Tiered hints (AlgoExpert), video | 0..N hints from notes, each lowers the grade |
| Gamification | XP, streak + freeze, hearts, gems, leagues | XP, streak, hearts, coins, leaderboard | Achievements | Progress checklist | XP + streak only |
| Spaced review | Practice Hub (personalised, paid) | — | — | None built in | Leitner 1/3/7 from the user's own method |
| Known weakness | Gamification nagging | "Too shallow; tap-to-fill doesn't build writing skill" | "Hard to make a mistake", weak transfer to typing | Requires keyboard/laptop | Mitigate shallowness: holes on decisions, distractors = real traps, `⏱` targets point the user to writing from scratch |

---

## Sources

- User's notes (read directly, HIGH): `/Users/user/Yandex.Disk.localized/Матрицы/Алгоритмы/` — `05 Тренажёр решений.md` (level 0, progress table, method), `06 Движок «маркеры и дырки».md`, `00 Компас алгоритмов.md`, `04 Маркеры + Заготовки.md` §3/§7, `03 Тренажёр задач.md` level 1
- [Distractors Make You Pay Attention (ICER 2024)](https://dl.acm.org/doi/fullHtml/10.1145/3632620.3671114) — HIGH
- [Distractors in Parsons Problems Decrease Learning Efficiency (Harms et al.)](https://www.researchgate.net/publication/310819145_Distractors_in_Parsons_Problems_Decrease_Learning_Efficiency_for_Young_Novice_Programmers) — HIGH
- [Parsons Problems and Beyond: systematic review (Ericson et al. 2022)](https://juholeinonen.com/assets/pdf/ericson2022parsons.pdf) — HIGH (paired distractors recommended)
- [Discovering, Autogenerating, and Evaluating Distractors (Smith & Zilles, SIGCSE 2023)](https://zilles.cs.illinois.edu/papers/smith_parsons_sigcse23.pdf) — HIGH
- [ITiCSE 2023 Working Group: Parsons problems](https://repository.falmouth.ac.uk/5216/1/ITiCSE_2023_WorkingGroup_Parsons_Report.pdf) — HIGH
- [Duolingo heuristics case study](https://medium.com/@anamariaerascu/nielsen-and-molichs-10-user-interface-design-heuristics-a-duolingo-study-case-27828d4848c9) — MEDIUM (tap to return, mistakes at end)
- [Duolingo: Gamification as Design Language](https://blakecrosley.com/guides/design/duolingo) — LOW/MEDIUM (feedback tone, end on easy)
- [Duolingo blog: how the streak builds habit](https://blog.duolingo.com/how-duolingo-streak-builds-habit) — MEDIUM (streak freeze rationale)
- [Yu-kai Chou: streak design](https://yukaichou.com/gamification-study/master-the-art-of-streak-design-for-short-term-engagement-and-long-term-success/) — MEDIUM
- [Mimo review (Coddy, competitor-written)](https://coddy.tech/vs/mimo), [Career Karma Mimo review](https://careerkarma.com/blog/mimo-coding-app-review/) — LOW/MEDIUM
- [Grasshopper review (Career Karma)](https://careerkarma.com/blog/grasshopper-app-review/), [Grasshopper shutdown (9to5Google)](https://9to5google.com/2023/03/13/grasshopper-shut-down/) — MEDIUM
- [SM-2 vs FSRS vs Leitner](https://smartrecallai.com/blog/sm2-vs-fsrs-vs-leitner-vs-anki-2026) — LOW/MEDIUM
- NeetCode / AlgoExpert / LeetCode hint and solution patterns: training knowledge, LOW (not re-verified this session)

---
*Feature research for: Duolingo-style algorithm trainer (Algolingo)*
*Researched: 2026-10-21*

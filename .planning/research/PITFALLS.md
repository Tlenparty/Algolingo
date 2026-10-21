# Pitfalls Research

**Domain:** Offline KMP + Compose Multiplatform (Android + iOS) trainer where you build Kotlin algorithm solutions from token chips, with content generated from Obsidian notes
**Researched:** 2026-10-21
**Confidence:** MEDIUM-HIGH. Content and file-system pitfalls were checked directly against the real notes. CMP and datetime facts come from official docs and release notes. Room-on-iOS details come from community reports (MEDIUM).

Phase names below are by topic, because the roadmap does not exist yet:
**P1 Scaffold** (KMP/CMP skeleton, Android + iOS sim build) · **P2 Content pipeline** (md → JSON) · **P3 Exercise engine** (holes, chips, checker) · **P4 Lesson flow** (theory → exercises → retry queue, hints, reveal) · **P5 Progress** (persistence, XP, streak, 1/3/7 repeats) · **Later: Store release**.

---

## Critical Pitfalls

### Pitfall 1: Some distractors are also correct answers (taken from the real notes)

**What goes wrong:**
The checker marks a correct solution red. The notes contain cases like this:
- **0.2 reverse:** the hint itself says `<=` instead of `<` is "не страшно, но лишний шаг". So `l <= r` is a correct answer, yet it is the obvious distractor for that hole.
- **0.3 palindrome:** `l <= r` is also correct there.
- **0.1 sum/max:** the note's grabli say `max = 0` is wrong, so `0` is a good distractor. But `Int.MIN_VALUE` is a valid init too. If `Int.MIN_VALUE` goes into the bank as a "wrong" chip, the app teaches something false.
- **0.4 binary search:** `(lo + hi) / 2` is a fair distractor because the note explicitly warns about overflow. `(lo + hi) ushr 1` is correct, so it must not be a distractor.

**Why it happens:**
Distractors get mined from "⚠️ Грабли" text, and grabli mix "wrong" with "suboptimal" and "style" issues.

**How to avoid:**
- In the content schema, each distractor has a `verdict`: `wrong` (red, with an explanation) or `acceptable` (counts as a pass, with an optional "works, but…" note). Never use a binary right/wrong flag.
- Rule for the content reviewer: a distractor is allowed only if you can name an input where it produces wrong output. Put that counter-example in the JSON (`"breaksOn": "[-5,-2]"`). If no counter-example exists, the chip either can't be a distractor or must be `acceptable`.
- Prefer distractors that are definitely wrong for a known reason: `0` vs `nums[0]`, `lo < hi` vs `lo <= hi` in binary search, `hi = mid` vs `hi = mid - 1` in the closed-interval form.

**Warning signs:**
The customer reviews a level and says "but this works too". There is a distractor with no `breaksOn` value.

**Phase to address:** P2 (schema plus review checklist), enforced in P3 (checker reads `verdict`).

---

### Pitfall 2: Checking an exact token sequence against 1–3 references rejects equivalent answers

**What goes wrong:**
Kotlin allows many equivalent forms: `l++` / `l += 1` / `l = l + 1`, `s[l] != s[r]` / `s[r] != s[l]`, `sum to max` / `Pair(sum, max)`, `if (x > max) max = x` / `max = maxOf(max, x)`, `for (x in nums)` / `nums.forEach`. With free-form token assembly, 1–3 references can't cover the combinations. With 4 holes and 2 variants each there are 16 sequences.

**Why it happens:**
The "1–3 references" decision was made with whole-solution assembly in mind, but the MVP format is holes.

**How to avoid:**
- **Check each hole, not the whole sequence.** Each hole has its own set of accepted values, e.g. hole `cond` = {`l < r`, `l <= r`}. The answer is correct if every hole matches its accepted set. This is simpler than sequence comparison, and per-hole red/green feedback falls out of it, which the requirement "объяснение грабли для неверного облачка" needs anyway.
- Make each hole one chip (one chip = one lexical unit or a ready-made fragment like `nums[0]`). Holes that take 2+ chips multiply the variants and the ordering bugs.
- Keep cross-hole references (e.g. `<` in hole A requires `-1` in hole B) only as an explicit `alternatives: [[...],[...]]` at the exercise level. Don't add this to the MVP: no level-0 exercise needs it.

**Warning signs:**
The JSON starts accumulating 4+ "references" per exercise. The checker uses `joinToString` and string comparison.

**Phase to address:** P3 (checker design), with the JSON schema fixed in P2.

---

### Pitfall 3: Duplicate tokens in the bank break placement and checking

**What goes wrong:**
Binary search uses `mid` 4–5 times, `1` twice (`mid + 1`, `mid - 1`) and `-1` in the return. If chips are identified by text:
- tapping a placed `1` returns "some" `1` to the bank, and the wrong chip ends up back in the bank (animation and state desync),
- `remember(key = text)` / `LazyRow(key = { it.text })` crashes Compose with a duplicate key exception,
- the "explain the wrong chip" feedback attaches to the wrong instance.

**How to avoid:**
- A chip is `data class Chip(val id: Int, val text: String)`, and the id is unique within the exercise. Keys and state use `id`. The checker compares `text`.
- `Hole(id, acceptedTexts)`. State is `Map<HoleId, ChipId?>`. That's the whole thing.
- "Insert into the nearest empty hole" means the first `null` in document order, not in the order holes were filled.
- Shuffle the bank deterministically with a seed (exercise id plus attempt), so a "retry at end of lesson" doesn't look like a different exercise and UI tests stay reproducible.

**Warning signs:**
Any `key = chip.text`. Any `indexOf(text)` in the state code.

**Phase to address:** P3.

---

### Pitfall 4: Reference solutions in the notes are themselves wrong (confirmed)

**What goes wrong:**
The app is a "player" for the notes, so it faithfully teaches mistakes. Confirmed example from `05 Тренажёр решений.md`, **2.4 Волновая сортировка**: the statement asks for `o[0] ≤ o[1] ≥ o[2]…` and the hint says the even position is a "впадина". But the code at even `i` swaps when `arr[i] < arr[i±1]`, which makes the even element a **peak**. On `[1,2,3,4]` it gives `[2,1,4,3]`, so `o[0]=2 > o[1]=1` and the condition is violated. This is level 2, not in the MVP, but the same class of bug can exist anywhere.

**How to avoid:**
- Add a **verification step to the pipeline**: put each reference solution into a JVM test (a `content-verify` module or a plain `kotlinc -script`) and run it against the examples from the statement and grabli (`[-5,-2]`, `[1,2,3,0,4,5,6,7] -> (3,7)`). Every accepted hole variant gets substituted into the template and run through the same tests. This also catches Pitfall 1: a distractor that **passes** the tests is not a distractor.
- This is a dev/CI tool only. Compiling inside the app is out of scope and stays that way.

**Warning signs:**
The pipeline exports code without executing it even once.

**Phase to address:** P2 (verification is part of the generator). For level 0 it can be 4 hand-written tests.

---

### Pitfall 5: Parsing Obsidian callouts with code inside `> ` (real format variants)

**What goes wrong (checked against `05`):**
- Headers vary: `> [!question]- 💡 Подсказка`, `💡 Подсказка 1`/`2`, and `> [!success]- ✅ Решение — жадное O(n)` (a suffix in the title). `06` also has `> [!warning] Условие тренировки` without `-`, which must not become a hint.
- Inside `[!success]` there is **more than code**: 1.3 has **two** kotlin fences (the main solution plus the "идиоматичный однострочник"), followed by `**Сложность:**`, `⚠️ **Грабли:**`, `🔁 **Вариации:**`. Take the first fence and you get the right solution, but lose the alternative that belongs in the accepted list. Take the last one and you get the one-liner instead of the reference.
- Empty lines inside the code are `>` with no trailing space (`>\n`). A naive `removePrefix("> ")` leaves `>` in the code.
- Kotlin code contains `->` and `>=`. A line like `>             nums[mid] < target  -> lo = mid + 1` must lose only the first `> `. A regex like `^>+\s?` would eat `>` characters belonging to the code if a line started with `>>`.
- A callout ends at the first line without `>`. Some blockquotes are not callouts (`> 🪜 Не выходит с наскока…` in 2.8, `> 🔑 Главное правило…`).
- **0.3 has no hint at all**, while 1.5/2.2/2.4 have two. The hint UI and scoring must handle 0..N hints.
- `🎯 **Тренируем:**` and `⏱ **Цель:**` sit **outside** the callout as list items. `**Сложность:**` and `⚠️ **Грабли:**` sit **inside** `[!success]`.
- Wikilinks with aliases: `[[04 Маркеры + Заготовки|#3]]`. Shown as-is in the UI they look like garbage.
- Emoji with and without VS16: the file contains 26 U+FE0F. `⚠️` is `U+26A0 U+FE0F`. A regex on `⚠ **Грабли` without `️?` silently misses everything. Grabli also comes in two forms: `⚠️ **Грабли:**` (22) and `⚠️ **Грабли (все три — с реального собеса):**` (1).
- Some single lines hold several statements (`val t = s[l]; s[l] = s[r]; s[r] = t`, `l++; r--`). Anyone who generates holes "by line" needs to know that.

**How to avoid:**
- Don't try to make the generator smart. The script only extracts raw blocks (statement, hints[], solution fences[], complexity, grabli, variations) into an intermediate JSON. **Holes and distractors are marked up by hand** in a separate overlay file (`0.4.holes.json`) that references the reference solution's lines. The notes don't contain hole markup at all (checked: there are no `___`/`⬜` markers), so automatic "hole picking" is a fantasy.
- Unprefix rule: `line == ">"` → `""`, `line.startsWith("> ")` → `line.drop(2)`, anything else ends the block.
- Golden test: parse `05` (a copy committed into the repo as a fixture) and assert counts: 32 `[!question]`, 29 `[!success]`, 4 tasks in level 0, 0 hints in 0.3. If the customer edits the note and the counts change, the test makes that visible.
- Use an existing parser rather than writing your own markdown parser. For a JVM/KTS script, `org.jetbrains:markdown` or a line-based state machine (~100 lines) is enough. Callouts are a line convention, not CommonMark.

**Warning signs:**
The generated JSON has code with `>` at the start of lines, `0.3` crashes because hints is empty, or the grabli list is empty.

**Phase to address:** P2.

---

### Pitfall 6: Cyrillic file names on Yandex.Disk come in mixed Unicode normalization (confirmed)

**What goes wrong:**
In the same folder, `05 Тренажёр решений.md` is stored as **NFD** (`е` + U+0308, `и` + U+0306), while `03 Тренажёр задач.md` is **NFC**. Also NFD: `Проверка является ли слово анаграммой.md`; `Бинарный поиск.md` is not NFD. On macOS APFS, opening by an NFC name works (checked), so locally "everything is fine". But:
- a string comparison `fileName == "05 Тренажёр решений.md"` or a `Map<String, …>` keyed by name **won't match** (different bytes),
- on Linux CI (GitHub Actions ubuntu) the file system is byte-exact, so a fixture under an NFD name won't open by an NFC path,
- wikilinks inside the notes are NFC (`[[03 Тренажёр задач]]`), so resolving links to files without normalization breaks on exactly the `ё`/`й` files,
- git with `core.precomposeunicode=true` (as in the repo) stores NFC, and the file copied from Yandex.Disk ends up under a different name than on the disk.

**How to avoid:**
- In the generator, `Normalizer.normalize(name, NFC)` for every name and every wikilink before comparing.
- **Never use file names as content IDs.** The ID is the task number from the header (`0.1`, `0.4`) → `lvl0.binsearch`. These are stable ASCII identifiers.
- Commit the source `.md` files into the repo (`content-src/`) under ASCII names (`05-trainer.md`), and run the generator only on those. Syncing from Yandex.Disk is a separate manual `cp` step. CI then never touches Cyrillic paths.
- Don't put the KMP project inside `Yandex.Disk.localized`. Syncing `build/`, `.gradle/`, and DerivedData makes Gradle/Xcode builds unstable, and a path with Cyrillic, spaces, and `«»` is a risk for Xcode shell phases. The project is already in `$HOME/work/Algolingo`, so keep it there.

**Warning signs:**
"File not found" only in CI. A wikilink resolves for `03` but not for `05`.

**Phase to address:** P2.

---

### Pitfall 7: The streak breaks at the day boundary and when the time zone changes

**What goes wrong:**
- The day is computed as `epochMillis / 86_400_000`, which is a UTC day. For the user in Moscow (UTC+3), a lesson at 01:30 local time counts as the "previous day", and the streak doubles or breaks.
- `Instant` of last activity is stored, and on read it is turned into a date with the **current** zone. After a flight or a zone change, the day history shifts.
- Repeats "after 1/3/7 days" are computed as `+ 24h * n` in `Instant`. Then "come back tomorrow" means "no earlier than this time tomorrow" instead of "tomorrow at any time", and the exercise is not due in the morning.
- The user moves the clock back and the streak goes negative, or a "future" date gets recorded.

**How to avoid:**
- Store a **`LocalDate` (ISO string) of the user's day**, computed at the moment of activity: `Clock.System.todayIn(TimeZone.currentSystemDefault())`. Streak = consecutive dates in the set of active days. Due date for a repeat = `LocalDate.plus(n, DateTimeUnit.DAY)`; the exercise is due if `today >= due`.
- Put `Clock`/`today()` behind a parameter (`fun interface Today { fun invoke(): LocalDate }`) so tests can pass in a fake date. That is the whole abstraction, nothing more.
- Tests: the 23:59 → 00:01 transition; skipping a day; two lessons in one day (streak +1, not +2); `today < lastActive` (clock moved back) → don't break and don't increment.
- **kotlinx-datetime ≥ 0.7:** `Instant` and `Clock` moved to `kotlin.time` (Kotlin ≥ 2.1.20, `@OptIn(ExperimentalTime::class)`). Old tutorials have `import kotlinx.datetime.Clock`. In 0.7.1 that is a typealias, but mixing versions through transitive dependencies causes `IrLinkageError` on iOS at runtime only. Pin one version in the version catalog and use `kotlin.time.Clock` imports.

**Warning signs:**
`86400` anywhere in the code. `Instant` in the progress table schema. Streak tests without a fake clock.

**Phase to address:** P5.

---

### Pitfall 8: Progress is tied to content that will change

**What goes wrong:**
Progress is keyed by lesson/exercise index or by a hash of the text. The customer fixes a typo in a note, regenerates the JSON, and the progress is "lost" or attached to a different exercise. Content will change all the time, since the notes are the single source of truth.

**How to avoid:**
- Every lesson and exercise has an explicit stable `id` in the overlay markup (`lvl0.reverse.cond`). The generator **fails the build** if an id disappears relative to the previous JSON (a list of removed ids in the diff), unless an explicit `removedIds` list is given.
- Progress stores only `exerciseId` and dates/counters. Never store content (text, answers) in the DB.
- If an exercise in the JSON has no matching progress, treat it as new. If progress has no matching exercise, ignore it silently (don't crash).

**Warning signs:**
A `List` index in the persistence schema. The JSON has no `id` field.

**Phase to address:** P2 (ids in the schema), P5 (keys in storage).

---

### Pitfall 9: Persistence migrations: destructive fallback wipes progress

**What goes wrong:**
With Room KMP, `fallbackToDestructiveMigration()` is added "for development" and ships. A schema change wipes XP and streak. On iOS this happens silently.

**How to avoid (and the ladder: you might not need a DB at all):**
- The MVP progress state is tiny: a set of active days, XP, and per exercise `{lastResult, step(0..3), dueDate}`. For dozens of exercises, **one JSON file** (`kotlinx-serialization` + `okio`/`FileSystem`, or `multiplatform-settings` with one string key) is enough. It has a `schemaVersion: Int` field and a `when(version)` migration in code. Note that adding an optional field with a default value needs no migration in kotlinx-serialization (`ignoreUnknownKeys = true`, defaults on fields).
- If you do take Room KMP: `exportSchema = true`, commit `schemas/`, use `BundledSQLiteDriver` (community reports say `NativeSQLiteDriver` on iOS can hit `SQLITE_MISUSE` and requires an absolute path), and never use destructive fallback. Expect KSP per target (`kspIosSimulatorArm64` etc.) and longer K/N links.
- Atomic writes: write to `progress.json.tmp`, then rename. Otherwise killing the app mid-write gives an empty file and zero progress.
- The progress file must not get into Android backup on top of an incompatible version (low priority in the MVP).

**Warning signs:**
`fallbackToDestructive*` anywhere in the code. A progress file without a version. A write with no tmp/rename.

**Phase to address:** P5.

---

### Pitfall 10: The iOS simulator build doesn't come up on a fresh Mac / Xcode

**What goes wrong:**
- **User Script Sandboxing** (enabled by default in new Xcode projects) breaks `embedAndSignAppleFrameworkForXcode`. Even after turning it off, the old sandboxed Gradle daemon keeps failing: you need `./gradlew --stop`. Newer KGP fails early with a clear message from `checkSandboxAndWriteProtection`.
- **CMP 1.11+ removed `iosX64`** and the minimum iOS version is 14. Templates and tutorials that declare `iosX64()` won't build. Only `iosArm64` + `iosSimulatorArm64` (Apple Silicon).
- The Xcode version must be compatible with the Kotlin version (K/N has a list of supported Xcode versions). Updating Xcode first and Kotlin afterwards is a classic way to break things.
- Running Gradle from Xcode doesn't see `JAVA_HOME` (Xcode doesn't read `.zshrc`), so a "JDK not found" or wrong JDK error appears only when building from Xcode.
- The first K/N build takes minutes and `linkDebugFrameworkIosSimulatorArm64` eats RAM. This is normal. Don't "fix" it by touching the configuration.

**How to avoid:**
- Generate the project with the official KMP wizard / IDE template (it has the correct Xcode project, sandbox off, `iosArm64` + `iosSimulatorArm64`). Don't build by hand from an old article.
- Phase P1 is considered done only when **both** builds come up from the CLI: `./gradlew :composeApp:assembleDebug` and `xcodebuild -scheme iosApp -destination 'platform=iOS Simulator,name=iPhone 16' build` (or a run from Android Studio with the KMP plugin). Record the versions (Kotlin, CMP, Xcode, JDK) in the README.
- Pin all versions in `libs.versions.toml`, with no `+`.

**Warning signs:**
"It works on Android, we'll check iOS later." Every week without an iOS build makes it more expensive to fix.

**Phase to address:** P1. Then an iOS build on every phase (manual checklist or a CI macOS runner).

---

## Moderate Pitfalls

### Pitfall 11: Monospace font and code layout on iOS

**What goes wrong:**
`FontFamily.Monospace` resolves to different system fonts on Android and iOS: different glyph widths, different line breaks, and the code "jumps" between platforms. The code template with holes wraps lines on a narrow screen, and the holes end up in random places. Bundled monospace fonts often lack Cyrillic, but code comments are in Russian (`// не 0! массив может быть весь отрицательный`).

**How to avoid:**
- Bundle **JetBrains Mono** (OFL, has Cyrillic) in `commonMain/composeResources/font/` with lowercase snake_case file names (`jetbrains_mono_regular.ttf`). The `Res` class is generated by the build. Reference it via `Res.font.jetbrains_mono_regular`.
- Draw code as **lines → a Row of segments (text | hole)**, not as one `Text` with `AnnotatedString` placeholders. That gives control over wrapping: no soft wrap, horizontal scroll for the whole code block. The level-0 templates are narrow (≤ 45 chars), but level 2+ have long lines (the 2.2 `else -> gasStations[right] - carPos < …`).
- Strip comments from the code in the exercise, or show them separately (`// не 0!` gives the answer away for 0.1).

**Phase to address:** P3.

### Pitfall 12: The template gives the answer away, or the chip matches by length/position

**What goes wrong:**
The hole width equals the answer length (`[___]` sized for `nums[0]` vs `[_]` for `0`), so the answer can be guessed from the shape. Comments in the code give the answer away (Pitfall 11). Distractors are obviously absurd (`banana`) and don't train anything.

**How to avoid:**
Use a fixed-width hole (or the width of the longest chip in the bank). Distractors come only from grabli and plausible neighbors (`<`/`<=`, `0`/`nums[0]`, `mid`/`mid + 1`).

**Phase to address:** P3 (UI), P2 (reviewing distractors).

### Pitfall 13: Retry at the end of the lesson as an infinite loop

**What goes wrong:**
The user keeps getting the same exercise wrong, it keeps going to the end of the queue, and the lesson never ends. Or the retry gives the same bank in the same order and it gets solved from memory.

**How to avoid:**
Limit to N repeats (e.g. 2). After that, show the reference answer and mark it "в повтор на завтра" (interval 1 day). Reshuffle the bank on the retry.

**Phase to address:** P4.

### Pitfall 14: Hints, scoring, and showing the reference answer conflict

**What goes wrong:**
After the reference answer is shown, the exercise is marked "solved" and counts toward "умею" by the 1/3/7 rule, although the methodology in the notes says "подсмотрел не подумав — тренировка не засчитана".

**How to avoid:**
Use explicit outcomes: `CLEAN` (no hints), `WITH_HINTS(k)`, `REVEALED`. Only `CLEAN`/`WITH_HINTS` move the 1→3→7 step forward. `REVEALED`/wrong resets the interval to 1. Points = max − k·penalty, floor 0.

**Phase to address:** P4/P5.

### Pitfall 15: CMP iOS performance and touch feel

**What goes wrong:**
Chip animations (`animateContentSize`, shared elements) jank on the simulator, which is slower than a device for Skia rendering. People spend days on optimization that isn't needed. The opposite case is real jank from recomposing the whole code block on every tap.

**How to avoid:**
Judge performance only on a real device (or not at all in the MVP). Give state as an immutable `Map`/`List` with `@Immutable`/stable classes, and use keyed `items`. The MVP has no text input, so CMP's iOS TextField problems don't apply here. Don't add text fields.

**Phase to address:** P3 (keep state stable), measure on a device later.

---

## Minor Pitfalls

- **Resources in multi-module.** `composeResources` reliably works from the module the `Res` class is generated in. Put content JSON in `composeApp/src/commonMain/composeResources/files/` and read it with `Res.readBytes("files/lessons.json")`. Don't create a separate `:content` module with resources in the MVP (community reports of `MissingResourceException` on iOS from library modules).
- **The generator lives outside the app.** The `md → JSON` script is not part of the app build (otherwise a Gradle task depends on Yandex.Disk). It is a separate JVM module / `kts` script whose output gets committed.
- **Kotlin code with Cyrillic comments in JSON.** Use `kotlinx-serialization` without escaping problems (UTF-8), and remember that `\` in code (`'\n'`) must be escaped in JSON. Generate the JSON with a serializer, not by gluing strings together.
- **Russian plurals in the UI.** "1 день / 2 дня / 5 дней" for the streak. `pluralStringResource` in compose resources supports plural forms.
- **Emoji in headers** (`🔥Avito`, `🟢`). Strip them from the lesson titles, or test rendering on iOS. Skia color emoji are supported, but the line height changes.

---

## Technical Debt Patterns

| Shortcut | Immediate Benefit | Long-term Cost | When Acceptable |
|----------|-------------------|----------------|-----------------|
| Progress in one JSON file instead of Room | No KSP/SQLite, simple migrations | Doesn't scale to thousands of records/queries | MVP and levels 0–5 (dozens of exercises). Revisit if history/statistics appear |
| Holes and distractors marked up by hand in an overlay | Accuracy, review by the customer | Manual work per exercise | Always: auto-generating holes is not worth it |
| Checking by hole instead of AST/compilation | Offline, trivial | Doesn't accept answers outside the accepted list | MVP and all of v1. Compiling on a VPS is v2 |
| Copy of the notes in the repo instead of reading Yandex.Disk | Reproducible builds, CI | Manual sync | Always |
| `fallbackToDestructiveMigration` | Fewer migrations | Wiped progress | Never |
| Progress keyed by index | Fast | Lost on any content edit | Never |

## Integration Gotchas

| Integration | Common Mistake | Correct Approach |
|-------------|----------------|------------------|
| Obsidian → JSON | Regex on all markdown, auto-holes | Line state machine for callouts + manual overlay of holes + golden counts test |
| Yandex.Disk (file system) | Comparing NFD/NFC names, a project inside the synced folder | NFC normalization, ASCII ids, a copy in the repo, project outside Yandex.Disk |
| Xcode ↔ Gradle | Sandboxing on, old daemon, `iosX64` | Official template, `./gradlew --stop`, only Arm64 targets |
| kotlinx-datetime | `kotlinx.datetime.Clock`, mixed 0.6/0.7 | `kotlin.time.Clock`, one pinned version, `LocalDate` in storage |
| Compose resources | Fonts in `resources/` instead of `composeResources/font`, hyphens in names | `composeResources/font`, snake_case, rebuild for `Res` |

## Performance Traps

| Trap | Symptoms | Prevention | When It Breaks |
|------|----------|------------|----------------|
| Parsing all the lesson JSON on the main thread at startup | Startup freeze | `Res.readBytes` in a coroutine, parse once, cache | Hundreds of exercises (levels 1–5). Not critical for level 0 |
| Recomposing the whole code block on every tap | Lag on long templates | Stable/immutable state, keys by chip id | Templates of 20+ lines |
| Measuring performance on the iOS simulator | False alarm | Only on a device | Always |

## Security Mistakes

| Mistake | Risk | Prevention |
|---------|------|------------|
| Compiling/running code on a shared personal VPS (v2) | RAM pressure on other services, untrusted code next to them | Already in Out of Scope. A separate VPS and sandbox if needed |
| Publishing interview tasks verbatim (Avito/Glovo/Yandex) | Possible NDA claim from the company/candidate | Rephrase, remove the company name from the task text in the store build (the 🔥Avito tags) |

## UX Pitfalls

| Pitfall | User Impact | Better Approach |
|---------|-------------|-----------------|
| "Correct" answer marked red (Pitfall 1/2) | Loss of trust in the trainer, the most painful bug for the product | Per-hole accepted sets + `acceptable` verdict + tests that run all variants |
| Showing only "wrong" without the grabli | No learning | Each wrong chip has an `explain` from the grabli |
| Code with soft wrap on a narrow screen | Holes jump around | No-wrap + horizontal scroll |
| Spoilers in code comments | The exercise solves itself | Strip/move comments |
| An endless retry queue | Frustration | Retry limit + "на завтра" |

## "Looks Done But Isn't" Checklist

- [ ] **iOS build:** verify that `xcodebuild … iOS Simulator` passes from a clean clone after `./gradlew --stop`, not just from the IDE
- [ ] **Checker:** verify that `l <= r` in 0.2/0.3 and `Int.MIN_VALUE`/`nums.first()` in 0.1 (if those chips exist) count as a pass
- [ ] **Duplicate chips:** verify that in 0.4, tapping the second placed `mid` returns that exact chip, and nothing crashes on duplicate keys
- [ ] **Parser:** verify that 0.3 (no hints) opens, that 1.3 gives 2 fences, and that the code has no `>` at line starts
- [ ] **Reference solutions:** verify that each one runs against the examples from the statement (2.4 will fail; fix it in the note)
- [ ] **Streak:** verify with a fake date: 23:59→00:01, two lessons a day, a skipped day, clock moved back
- [ ] **Progress:** verify that after regenerating JSON with a changed typo, the progress stays
- [ ] **Progress write:** verify that killing the app during a save doesn't zero the file
- [ ] **Font:** verify that Cyrillic in code/comments renders in the same font on Android and iOS

## Recovery Strategies

| Pitfall | Recovery Cost | Recovery Steps |
|---------|---------------|----------------|
| Exact sequence comparison already written | MEDIUM | Convert references into per-hole accepted sets (an automatic diff of the references by hole) |
| Progress keyed by index | HIGH after release, LOW before | Add ids; one-off index→id migration using the current JSON |
| Name normalization broke CI | LOW | NFC everywhere + ASCII copies of the files |
| Destructive migration already wiped data | HIGH (unrecoverable) | Only prevention |
| Wrong reference answer in the content | LOW | Fix the note → regenerate → verify-test |

## Pitfall-to-Phase Mapping

| Pitfall | Prevention Phase | Verification |
|---------|------------------|--------------|
| 10 iOS build / sandbox / iosX64 | P1 | CLI builds for both platforms from a clean clone |
| 6 NFD/NFC, Cyrillic paths | P2 | Generator test on Linux/CI with ASCII copies; ids not derived from names |
| 5 Callout parsing | P2 | Golden test: 32/29 callouts, 0 hints in 0.3, 2 fences in 1.3 |
| 4 Wrong reference answers | P2 | Verify-runner runs the references and all accepted variants on examples |
| 8 Stable ids | P2 → P5 | Build fails when an id disappears; progress survives regeneration |
| 1 Distractors that are also correct | P2 + P3 | Each distractor has `breaksOn`, and the verify-runner confirms it fails |
| 2 Equivalent answers | P3 | Per-hole accepted sets; unit tests for the checker |
| 3 Duplicate tokens | P3 | Test on 0.4 with repeated `mid`/`1` |
| 11/12 Font, wrapping, spoilers | P3 | Screenshots on Android and iOS |
| 13/14 Retry loop, hints vs "умею" | P4 | Lesson flow unit tests |
| 7 Streak/time zone | P5 | Tests with a fake `today` |
| 9 Migrations / atomic write | P5 | Test: v1 file → read by v2 code; kill during write |
| LeetCode/trademarks | Store release | Content audit before publishing (see below) |

## Store Release: copyright and trademarks (later, but affects content now)

- **LeetCode ToS** explicitly states that problem statements ("questions") and solutions are LeetCode's property and protected by copyright. Copying and scraping are prohibited. Repos with verbatim copied statements have received DMCA takedowns. Practice: **your own wording + an optional link**. This is already in the PROJECT.md decision. The **algorithm** and the idea of the problem are not protected. The text and examples are. (MEDIUM: from the ToS text; not legal advice.)
- **Check the existing notes**: they contain titles that match LeetCode (`Two Sum`, `Climb Stairs`, `LRU Cache`, `Top K…`, `Longest substring without repeating`). Classic problem names are generic, but **examples and wording must be your own**. Don't keep LeetCode numbers or the "LeetCode" brand in the UI or the store description.
- **Interview tasks** (Avito/Glovo/Yandex, `🔥` tags): remove company names and recognizable wording from the store build.
- **Trademarks:** "Duolingo для алгоритмов" in the store description is a risk of using someone else's trademark in a competing category (mobile education). "Algolingo" is close to the "-lingo" family. Duolingo actively protects its brand. (LOW confidence: no specific disputes were checked.) Check the name before release and don't mention Duolingo in the listing.
- **Fonts:** JetBrains Mono is under OFL, so it can be bundled. Keep the license file in the app.
- To make this cheap later: start a `source`/`license` field in the content JSON now (`"origin": "own" | "interview-rephrased"`).

## Sources

- Real notes: `$HOME/Yandex.Disk.localized/Матрицы/Алгоритмы/05 Тренажёр решений.md`, `06 Движок «маркеры и дырки».md` (callouts, fences, VS16, and Unicode normalization of file names were checked with python `unicodedata` and `xxd`; the 2.4 bug was traced by hand on `[1,2,3,4]`) (HIGH)
- [What's new in Compose Multiplatform 1.12.2](https://kotlinlang.org/docs/multiplatform/whats-new-compose-112.html), [1.11.1](https://kotlinlang.org/docs/multiplatform/whats-new-compose-111.html) (iosX64 removed, iOS 14 minimum, native text input experimental) (HIGH)
- [CMP 1.8.0: iOS Stable](https://blog.jetbrains.com/kotlin/2025/05/compose-multiplatform-1-8-0-released-compose-multiplatform-for-ios-is-stable-and-production-ready/) (HIGH)
- [Gradle issue #27068: Xcode User Script Sandboxing](https://github.com/gradle/gradle/issues/27068), [Kotlin Slack thread on sandbox](https://slack-chats.kotlinlang.org/t/26854439/is-there-a-way-to-opt-out-of-user-script-sandbox-checking-wh) (MEDIUM-HIGH)
- [kotlinx-datetime 0.7.0 announcement](https://slack-chats.kotlinlang.org/t/29159489/mega-kotlinx-datetime-0-7-0-is-here-instant-and-clock-were-m), [0.7.1](https://slack-chats.kotlinlang.org/t/29233447/mega-kotlinx-datetime-0-7-1-is-here-providing-a-more-conveni), [iOS IrLinkageError example](https://github.com/jkcb-dev/booqi/pull/48) (HIGH/MEDIUM)
- [Room KMP migration codelab](https://developer.android.com/codelabs/kmp-migrate-room), [Room migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions), community PRs on NativeSQLiteDriver/SQLITE_MISUSE on iOS (MEDIUM)
- [CMP issue #4111: MissingResourceException fonts iOS](https://github.com/JetBrains/compose-multiplatform/issues/4111) (MEDIUM)
- [LeetCode Terms of Service](https://leetcode.com/terms/), [BleepingComputer on DMCA for copied HackerRank/LeetCode content](https://www.bleepingcomputer.com/news/security/github-restores-popular-python-repo-hit-by-bogus-dmca-takedown/) (MEDIUM)

---
*Pitfalls research for: offline KMP/CMP token-chip algorithm trainer*
*Researched: 2026-10-21*

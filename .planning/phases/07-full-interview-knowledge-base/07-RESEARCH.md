# Phase 7: Full Interview Knowledge Base - Research

**Researched:** 2026-10-22
**Domain:** Obsidian vault → bundled offline KB (Python sync script + KMP/Compose reader)
**Confidence:** HIGH. Every number below was measured on the real vault and the real code. A prototype sync was run and the existing Kotlin tests were executed against the full 217-note bundle in a repo copy under /tmp.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Vault: `$HOME/Yandex.Disk.localized/Матрицы` (local path — never commit it; pass via arg/env like `sync_notes.py` does now).
- Include: `Алгоритмы/`, `Вопросы с собесов/` (incl. subfolders Коллекции, Многопоточка, Примитивы), `Матрицы/` (all subfolders: Android, Compose, Coroutines, KMP, Performance, Архитектура, Исключения, Котлин Java, Многопоточность, Общие вопросы), `SystemDesign/`, root notes «Подготовка к собеседованию», «СОДЕРЖАНИЕ».
- Exclude: `Работа за рубежом/`, `Work/`, `Для работы/`, `Excalidraw/`, `.obsidian/`, `.claude/`, `*.base`.
- Publishing all included notes to the public GitHub repo is approved.
- Images: SHOW them — copy only referenced png into resources and render inline in the note (7 notes, 27 png in `Картинки/`).
- Layout: keep the current KB screen (collapsible section list + search); just more sections by topic. No two-level navigation, no new design.
- Existing 23 note ids must stay stable (gen_content.py reads note 05 as `kb/n07ed966c.md`; [[links]] and progress may reference ids).

### Claude's Discretion
- Section names/order (derive from folders; СОДЕРЖАНИЕ can guide order), id scheme for new notes, search implementation details, image format/size handling.

### Deferred Ideas (OUT OF SCOPE)
- «Прочитано» marks / reading progress (AIDV2-05), link from lesson to KB note (AIDV2-03).
</user_constraints>

## Project Constraints (from CLAUDE.md)
- KMP + Compose Multiplatform. The UI lives in `shared`, and design tokens come from `AlgoTheme.*`. Do not hard-code dp, sp or `Color(0x…)` in `shared`. The one existing exception is the table-width heuristic in BlockView.
- Offline only, no network libraries. Content ships through `composeResources/files/` and is read with `Res.readBytes`. There is no directory listing, so an index file is required.
- The UI is Russian-only, with string literals in composables.
- Do not commit `local.properties`, secrets or local paths. The vault path must never appear in committed files.
- Pinned stack: Kotlin 2.4.21, CMP 1.12.2, no new dependencies. Everything this phase needs is already in `libs.compose.components.resources`.
- Work goes through GSD. Commits are authored by tlenparty (see memory).
- Device runs: the agent drives Android with adb/uiautomator and iOS with idb itself, and does not ask the user for manual checks.

## Summary

The vault fits the existing design almost perfectly. The allow-list gives exactly 217 notes, 2.12 MB total: Алгоритмы 23, Вопросы с собесов 28, Матрицы 153, SystemDesign 11, root 2. There are no title collisions, no case-insensitive collisions and no 8-hex id collisions. The current id is `"n" + sha1(NFC title)[:8]` and is based on the title only. The folder plays no part, so all 23 existing ids, including `n07ed966c`, stay unchanged. The 23 `Алгоритмы/` notes are byte-identical to the bundled copies (verified with `cmp`).

I prototyped a folder-recursive sync (`/tmp/p7/sync2.py`) and ran the unmodified `KbContentTest` against the full 217-note bundle. **All notes parse.** Every cross-folder `[[link]]` resolves except 5. Every `#anchor` resolves, including the emoji-heading slugs in СОДЕРЖАНИЕ, such as `#-kotlin--java`. The test fails on only three things: (a) 5 dangling links from СОДЕРЖАНИЕ to excluded or nonexistent notes, (b) 25 image embeds that the parser currently treats as links, and (c) the pinned 4-section list. So the parser needs one new block, `Image`. Everything else works today.

The real work is in four places. (1) `sync_notes.py`: recurse folders, apply a folder→section map, and copy and downscale the referenced images to ASCII names. (2) An `Image` block plus a renderer using `Res.readBytes` + `decodeToImageBitmap`, which I compile-verified on Android and iOS with no opt-in. (3) Section headers that can be collapsed. The current list is **not** collapsible, although CONTEXT says it is. With 217 rows it needs to be. (4) Search: cache the lowercased text and stop re-running the search on every recomposition. Measured cost today is 7 ms per query on desktop JVM, which is probably 30–100 ms on a phone or K/N.

**Primary recommendation:** Extend `sync_notes.py` in place. Use an ordered `FOLDERS` dict with folder→section as the allow-list. Rewrite `![[X.png]]` → `![[i<sha1>.png]]` and copy the image to `kb/img/`, downscaled to 1080 px wide with `sips`, never upscaled. Add `Image(file)` to the parser and BlockView. Make the section headers collapsible. Precompute lowercase text for search. Update `KbContentTest`: new section list, new known-missing set, 23-id stability, and image files exist.

## Architectural Responsibility Map

| Capability | Primary Tier | Secondary Tier | Rationale |
|------------|-------------|----------------|-----------|
| Note selection, section assignment, ids, image copy/resize | Build-time tool (`tools/sync_notes.py`, runs on the Mac with the vault) | — | The vault is local-only. The app sees only the generated bundle. |
| Note/image storage | Bundled resources (`composeResources/files/kb/`, `kb/img/`) | — | Offline, no backend. |
| Index load, search, link resolve | `shared` commonMain (`KbRepository`) | — | Pure Kotlin, testable in commonTest. |
| Markdown → blocks (incl. `Image`) | `shared` commonMain (`MarkdownParser.kt`) | — | Pure, no Compose imports. |
| Rendering (sections, images, placeholders) | `shared` commonMain UI (`kb/ui/*`) using `design` tokens | `design` | Follows the existing pattern. |

## Inventory (measured)

| Folder (NFC) | Notes | Proposed section |
|---|---|---|
| `/` root: «СОДЕРЖАНИЕ», «Подготовка к собеседованию» | 2 | Обзор |
| `Алгоритмы` | 23 | Алгоритмы |
| `Матрицы/Котлин, Java` (the folder name contains a comma) | 33 | Kotlin / Java |
| `Матрицы/Многопоточность` | 11 | Многопоточность |
| `Матрицы/Coroutines` | 6 | Корутины и Flow |
| `Матрицы/Compose` | 15 | Compose |
| `Матрицы/Android` + root `Матрицы/Плотность экрана в Android` | 37+1 | Android |
| `Матрицы/Архитектура` + root `Матрицы/Свой DI на коленке` | 11+1 | Архитектура |
| `Матрицы/Performance` | 5 | Performance |
| `Матрицы/KMP` | 8 | KMP |
| `SystemDesign` | 11 | System Design |
| `Матрицы/Исключения` | 2 | Исключения |
| `Вопросы с собесов` (+Коллекции 11, Многопоточка 8, Примитивы 3) | 28 | Вопросы с собесов |
| `Матрицы/Общие вопросы` | 23 | Общие вопросы |
| **Total** | **217** | 14 sections |

- The order follows СОДЕРЖАНИЕ's `##` headings. The two root `Матрицы/*.md` notes are placed where СОДЕРЖАНИЕ lists them: Android (line 174) and Архитектура/DI (line 194). [VERIFIED: vault grep]
- СОДЕРЖАНИЕ files `Общие вопросы` under «Вопросы с собесов». Keeping it as its own section mirrors the folders and keeps «Вопросы с собесов» at 28 rows instead of 51. This is a discretion call; merging is a one-line map change.
- Excluded folders that contain `.md` files: `Работа за рубежом` (4), `Work` (1), `Для работы` (1), `Excalidraw` (1). `Personal`, `x5` and `Картинки` contain no `.md` files. There are three `*.base` files at the root, and `rglob("*.md")` already skips them. [VERIFIED]
- **NFD paths exist.** 12 vault paths are not NFC, including `Алгоритмы/05 Тренажёр решений.md` and `Матрицы/Свой DI на коленке.md`. The folder→section lookup must NFC-normalize the *relative path* as well as the stem, or those notes silently fall out of their sections. [VERIFIED]
- Filename oddities include commas, `?` (`Кто поддерживает деструктуризацию в Kotlin ?`), a trailing `**` (`…мобильного системного дизайна**`) and double spaces (`Activity, Service,  Broadcast Receiver,  Content Provider`). None of these matter, because the id is a hash and the file on disk is `n<hash>.md`. Wikilinks to these titles resolve, as shown by the prototype test run. Titles display as-is, including the literal `**`.
- One title contains profanity («Пиздец от Яндекса», in Общие вопросы). Publishing is approved; I mention it only as a store-review note for later. [VERIFIED]
- A privacy scan of the prototype bundle found no `/Users/` paths, no Yandex.Disk mentions and no API-key or token patterns. The only emails are examples such as `user@example.com`. [VERIFIED]

## Id Stability (SC1)

- `nid = "n" + sha1(nfc(title).encode()).hexdigest()[:8]` is title-only. With the prototype I confirmed that all 23 old `(id, title)` pairs are present in the new index, and all 23 files are byte-identical, including `n07ed966c.md`. [VERIFIED: /tmp prototype + cmp]
- `python3 tools/test_gen_content.py` (33 tests OK) and `gen_content.py --check` (rc=0) read only `kb/n07ed966c.md`, so they stay green as long as note 05 is unchanged in the vault. [VERIFIED]
- No lesson JSON or progress state references KB ids. Only `NoteRoute(id)` in the nav saved state does. [VERIFIED: grep]
- New notes use the same scheme, so there is no reason to change it. Add two fail-loud guards to the sync: duplicate NFC title across folders, and duplicate id. Neither occurs today, and each costs one line.

## Markdown Features vs the Current Parser

Counts are over the 217 included notes. The decisive evidence is the prototype `KbContentTest` run: every note parses to non-empty blocks, there are more than 100 callouts, and all links and anchors resolve except the ones listed.

| Feature | Count | Current behaviour | Action |
|---|---|---|---|
| `![[X.png]]` embeds | 25 in 7 notes, always alone on a line (one has a leading space) | Parsed as `!` + a Link, which renders as the literal text «!Снимок экрана…png» | **Required:** sync rewrites to `![[i<hash>.png]]`, and the parser adds an `Image(file)` block |
| Size suffix `\|300`, markdown `![](…)`, note embeds `![[note]]`, Excalidraw embeds | 0 / 0 / 0 / 0 | — | Ignore. The sync regex tolerates `\|…` anyway |
| Tables | 75 notes | Supported | none |
| Callouts | 477 in 158 notes | Supported | none |
| Nested callouts `> >` | **0**. My first regex reported 70, but that was a false match across lines; a POSIX grep confirms 0 | — | none |
| `==highlight==` | 152 in 42 notes, often wrapping `**bold**` | Renders the literal `==` | Recommended (small). See Pattern 3 |
| Headings h4–h6 | 13 in 4 notes | Supported (titleMedium) | none |
| Code fence languages | kotlin 963, none 1118, java 49, text 48, xml 23, http 6, groovy 5, swift 3, others ≤2 | Kotlin is highlighted; the rest are monospace | none |
| Indented fences (inside lists) | 19 | Handled (trimmed) | none |
| `[label](https://…)` / bare URLs | 24 / 30 | Label or text only (offline) | none |
| `$` | 52 | All Kotlin string templates, no math | none |
| HTML tags | 2 in 1 note | Literal text | ignore |
| `_italic_`, tab-indented lists, `+ ` lists, footnotes, `#tags`, checkboxes | 12 / 9 / 3 / 3 / 3 / 2 | Literal or plain | ignore (cosmetic) |
| Frontmatter, mermaid, `%%`, `~~`, setext headings | 0 | — | none |

**Dangling links.** The parser-level test found exactly 5, all in СОДЕРЖАНИЕ: `KPI (2)` (exists nowhere), `Extension для List` and `Spec` (in the excluded `Для работы/`), `00 Remote worldwide — чеклист` and `01 LinkedIn — итоговый профиль` (in the excluded `Работа за рубежом/`). They render as plain text, which is the existing behaviour. The 6 old `knownMissing` entries (coil, hashmap vs hashset, heap & stack, performance. память и утечки, как работает hashmap, структуры данных) **now resolve**, so replace the set rather than extend it. [VERIFIED: prototype test output]

## Images

- 25 distinct PNGs are referenced by 7 notes: Compose. Оптимизация 13, Compose. Recomposition 7, and one each in Compose, Чистая архитектура, Метод main в Android, Activity/Service…, ViewModel/SaveStateHandle…. `Картинки/` holds 27; the 2 unreferenced ones are `Pasted image 20250211124658.png` and `Снимок экрана … 12.28.28.png`. All filenames are NFC-clean but contain non-ASCII text and spaces. [VERIFIED]
- Raw size is about 10.4 MB for the 25 files. The largest is 2214×1188. Downscaled to 1080 px wide as PNG (only images wider than 1080) the total is **7.1 MB**. JPEG at q80 would be about 2.8 MB, but these are screenshots of code and IDE windows, so keep lossless PNG. For reference, the current debug APK is 14.5 MB. [VERIFIED: sips on copies in /tmp]
- **Pitfall:** `sips -Z 1080` *upscales* small images. 389×507 became 828×1080. Read `sips -g pixelWidth` first and copy unchanged when the width is ≤ 1080; otherwise run `sips --resampleWidth 1080`. [VERIFIED]
- `sips` output is byte-deterministic: two runs gave the same sha1. Re-sync therefore causes no git churn. `sips` ships with macOS, and the vault only exists on the Mac. [VERIFIED]
- Naming: `kb/img/i<sha1(NFC filename)[:8]>.png`. This is ASCII and mirrors the note-id scheme, which sidesteps any question of non-ASCII names in Android assets or the iOS bundle. Rewriting the embed in the copied note also stops search from matching «Снимок экрана». Only those 7 notes change; note 05 does not.
- Loading: `Res.readBytes("files/kb/img/$f").decodeToImageBitmap()`, imported from `org.jetbrains.compose.resources.decodeToImageBitmap` and declared in the commonMain of components-resources 1.12.2. A probe composable using exactly this code compiled for `compileAndroidMain` and `compileKotlinIosSimulatorArm64` with no opt-in warning. [VERIFIED: compiled in /tmp repo copy; CITED: kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html]
- Memory: a decoded 1080×650 ARGB bitmap is about 2.8 MB. LazyColumn disposes off-screen items, so only the visible bitmaps stay alive. No cache is needed.
- Use `files/`, not `drawable/`. Drawables need static `Res.drawable.x` accessors generated at build time, which does not fit names that come from data.

## Search & Load Performance

- Today, `KbRepository.load()` eagerly reads the index plus every note. `searchNotes` calls `raw.lowercase()` on every note for every query. `KbListScreen` calls `search(query)` on **every recomposition** because the call is not wrapped in `remember`.
- I measured on the desktop JVM (M-series, full 217-note bundle, 1.47 M chars): loading all files took 41 ms; one search took 4–7.6 ms; `parseBlocks` on the largest note (121 KB, 729 blocks) took 1.5 ms; parsing all notes took 22 ms. [VERIFIED: BenchTest in /tmp repo]
- Extrapolation: a phone running ART, or Kotlin/Native on iOS, is probably 3–10× slower. That puts a search keystroke at roughly 20–80 ms, which is borderline jank. [ASSUMED]
- Recommended fix, about 3 lines, no index structure. Precompute the lowercase text once with `val lower by lazy { raw.lowercase() }` (and the same for the title) in `Note`. The existing tests construct `Note(id,title,section,raw)`, and that keeps working. Wrap the call as `val hits = remember(query) { search(query) }`. Search then becomes `indexOf` over about 1.5 M chars, which takes ≤1 ms on desktop. An inverted index is unnecessary at this size.
- Keep eager loading: search needs every body anyway, and 217 small `readBytes` calls are cheap. Check on the device that «…» on the KB tab disappears quickly. If the iOS cold load turns out to be slow, the upgrade path is to concatenate notes into one bundle file. Do not do that pre-emptively.
- A pre-existing, harmless quirk: the snippet indices come from the lowercased string but are applied to `raw`. Lowercasing Cyrillic and Latin preserves length, so this is fine.

## Sections & List Screen

- `KbListScreen` groups with `notes.groupBy { it.section }`, which keeps index order. The section order is therefore the order of entries in index.json, and the sync must sort by section index, then by title.
- Sort within a section by `title.casefold()`. Today the sort is by raw code point, which puts `let, apply…` after every capitalized Latin title. The 00–06 algorithm notes still come first because digits sort first.
- **The current list is NOT collapsible.** Headers are plain `Text` (verified in code, git history and 01.1 docs). CONTEXT assumes it is collapsible. At 217 rows the list is about 15 screens long, so make the headers tappable, matching the existing callout pattern: a `▸`/`▾` chevron, state in `rememberSaveable` (for example a `Set<String>` of open sections, saved as a list), with every section collapsed by default so the 14 headers fit on about 1.5 screens. Search results stay flat and ignore collapse state. This is about 10 lines and introduces no new design language, because the chevron and toggle already exist in CalloutView. Flag it in the plan as the implementation of the "collapsible" decision.
- Proposed `sync_notes.py` shape: replace `SECTIONS`, `PREFIX_SECTION` and `TITLE_SECTION` with one ordered `FOLDERS` dict (folder → section) that is both the allow-list and the order, plus a 2-entry `TITLE_SECTION` override for the root `Матрицы/` notes and a `ROOT_NOTES` set. Resolve a folder by its longest matching prefix, so subfolders of `Вопросы с собесов` inherit the section. Print the count of skipped `.md` files per top folder so a new vault folder is visible rather than silently dropped.
- **KB-02 text becomes stale.** REQUIREMENTS KB-02 says «разделами: Навигация · Тренажёры · Темы · Задачи с собесов». After this phase there is a single «Алгоритмы» section. The plan must update the KB-01 and KB-02 wording, or add a Phase 7 requirement row, so verification is not judged against the old text.

## Standard Stack

No new dependencies. This phase installs no packages, so no Package Legitimacy Audit is needed.

| Piece | Version | Use |
|---|---|---|
| `components-resources` (CMP) | 1.12.2 (already in `shared`) | `Res.readBytes`, `decodeToImageBitmap` |
| `androidx.compose.foundation.Image` | via CMP 1.12.2 | render the bitmap |
| Python 3 stdlib (`hashlib`, `unicodedata`, `pathlib`, `subprocess`) | /usr/bin/python3 | sync |
| `sips` (macOS) | sips-316 | image downscale |

Do not use PIL. It happens to be installed on this machine (11.3.0), but it is not stdlib and adds an install step for the sync.

## Architecture Patterns

```
vault (Mac, read-only)
  └─ sync_notes.py: rglob *.md → NFC rel path → FOLDERS/TITLE_SECTION → section | skip
        ├─ text: CRLF→LF, ![[X.png]] → ![[i<h>.png]] (copy+sips to kb/img/)
        ├─ kb/n<sha1(title)>.md
        └─ kb/index.json  (sorted: section order, then title.casefold())
           (built in kb.tmp, swapped in — existing atomic pattern)
app start → KbRepository.load(): index.json → Res.readBytes each note → notes (+lazy lower)
KB tab → KbListScreen: search field (remember(query)) | collapsible sections (groupBy section)
open note → NoteScreen: parseBlocks → LazyColumn(BlockView)
        ├─ Link → repo.resolve(title) → NoteRoute(id, anchor) (cross-folder works: title-based)
        └─ Image(file) → produceState { Res.readBytes("files/kb/img/f").decodeToImageBitmap() } → Image | placeholder
```

### Pattern 1: Sync embed rewrite (prototype, verified to run)
```python
EMBED = re.compile(r"(?m)^[ \t]*!\[\[([^\]|]+\.(?:png|jpe?g))(?:\|[^\]]*)?\]\][ \t]*$")
def emb(m):
    name = nfc(m.group(1).strip())
    if name not in imgs: return m.group(0)          # leave as-is -> parser shows placeholder
    f = "i" + hashlib.sha1(name.encode()).hexdigest()[:8] + ".png"
    dst = tmp / "img" / f
    if not dst.exists():
        w = int(subprocess.run(["sips", "-g", "pixelWidth", str(imgs[name])], check=True,
                               capture_output=True, text=True).stdout.split()[-1])
        if w <= 1080: shutil.copyfile(imgs[name], dst)
        else: subprocess.run(["sips", "--resampleWidth", "1080", str(imgs[name]), "--out", str(dst)],
                             check=True, capture_output=True)
    return f"![[{f}]]"
```
The full prototype is at `/tmp/p7/sync2.py`. It is not committed and will not survive a reboot.

### Pattern 2: Image block
```kotlin
// MarkdownParser.kt
data class Image(val file: String) : Block
private val imageRe = Regex("^!\\[\\[([^\\]|]+\\.(?:png|jpe?g))(?:\\|[^\\]]*)?]]$")
// in parseBlocks `when`, before the paragraph fallback; also add `imageRe.matches(t)` to startsBlock()
imageRe.matches(t) -> { out.add(Image(imageRe.find(t)!!.groupValues[1])); i++ }

// BlockView.kt  (imports: org.jetbrains.compose.resources.decodeToImageBitmap, androidx.compose.foundation.Image)
is Image -> {
    val bmp by produceState<ImageBitmap?>(null, block.file) {
        value = runCatching { Res.readBytes("files/kb/img/${block.file}").decodeToImageBitmap() }.getOrNull()
    }
    bmp?.let { Image(it, null, pad.fillMaxWidth(), contentScale = ContentScale.FillWidth) }
        ?: Text("🖼 картинка", pad, color = MaterialTheme.colorScheme.onSurfaceVariant)
}
```
`Block` is a sealed interface. Adding `Image` makes the `when` in `KbContentTest.inlines()` and in BlockView non-exhaustive, so the compiler points to both places that need updating. On iOS `Res` is already imported in `shared`; the `Res` import path is `algolingo.shared.generated.resources.Res`. Note the name clash: the new block `Image` collides with `androidx.compose.foundation.Image`. Name the block `ImageBlock`, or alias one of the imports, the same way `kb.Text` is aliased as `PlainText`.

### Pattern 3: `==mark==` (optional, recommended)
Add `Mark(v: String)` to `parseInline`. Open only when `==` is followed by a non-space character, and close only on a `==` preceded by a non-space character. Prose such as «сумма == target» has spaces around `==` and must stay literal; there are 153 such lines, mostly in code, but some in prose. Render it by recursively calling `inlineText(parseInline(v))` inside `withStyle(SpanStyle(background = AlgoTheme.colors.<tint>))`, because 152 marks often wrap `**bold**`.

### Anti-patterns
- Enumerating `kb/img/` at runtime: Compose Resources has no directory listing. The file name comes from the note text.
- Keying sections on `PREFIX_SECTION` digit prefixes: those only exist in `Алгоритмы`.
- Matching folder names without NFC: 12 paths are NFD.
- `Image` without a placeholder: if a file is missing or fails to decode, the screen must still render (SC3).

## Don't Hand-Roll

| Problem | Don't Build | Use Instead |
|---|---|---|
| PNG decode | anything platform-specific (`BitmapFactory`/`UIImage` expect/actual) | `ByteArray.decodeToImageBitmap()` (common) |
| Image resize | Python PNG code, PIL dependency | `sips` (macOS built-in, deterministic) |
| Search index | inverted index / trie | lowercase cache + `indexOf` (1.5 M chars) |
| Markdown library | swapping in a markdown library | the existing parser, which already handles 217/217 notes; only `Image` (and optionally `Mark`) is new |

## Common Pitfalls

1. **sips upscales.** `-Z` grows small images. Check the width first. Warning sign: a 389-px image becomes 828×1080.
2. **NFD folder paths.** Normalize the relative path with NFC before section lookup. Warning sign: per-section counts don't add up to 217, or `05 Тренажёр решений` drops out of Алгоритмы.
3. **Stale `knownMissing`.** Replace it with the 5 new entries, lowercase as the test compares: `"kpi (2)", "extension для list", "spec", "00 remote worldwide — чеклист", "01 linkedin — итоговый профиль"`.
4. **Search on every recomposition.** `val hits = search(query)` sits in the composable body. Wrap it in `remember(query)`.
5. **Image height jump.** The image loads asynchronously, so its LazyColumn item grows after composition, and an anchor scroll into a note with images can land slightly off. None of the 11 anchored links target the 7 image notes, so accept this. Add a `ponytail:` comment and store the aspect ratio in the index if it ever bites.
6. **APK/IPA grows by about 9 MB** (7.1 MB images + 2 MB notes). This is acceptable and expected; note it in the summary.
7. **Bundle swap.** `img/` must be written inside `kb.tmp` so the existing atomic rename covers it. Writing straight to `kb/img` would leave orphans after a re-sync.
8. **Name clash** between the block `Image` and Compose's `Image` (see Pattern 2).

## Runtime State Inventory
Mostly additive, but ids and bundle layout are migration-sensitive:
- Stored data: no KB ids in progress.json or lesson JSON (grep). Nothing to migrate.
- Live service config: none (offline app).
- OS-registered state: none.
- Secrets/env vars: `ALGOLINGO_VAULT`, whose name is unchanged.
- Build artifacts: the generated `Res` accessors pick up the new files on the next build. The stale `kb.tmp`/`kb.old` cleanup is already handled by the script.

## Environment Availability

| Dependency | Required By | Available | Version | Fallback |
|---|---|---|---|---|
| Vault at `$HOME/Yandex.Disk.localized/Матрицы` | sync | ✓ | — | — |
| python3 | sync, gen tests | ✓ | /usr/bin/python3 | — |
| sips | image resize | ✓ | sips-316 | plain copy (+3 MB) |
| adb + emulator-5554 | Android walkthrough | ✓ (device attached) | — | — |
| xcrun simctl, iPhone 17 sim (4461DE0A…) booted | iOS walkthrough | ✓ | iOS 26.2 | — |
| idb | iOS UI driving | not on PATH (Phase 6 used it from /tmp drivers) | — | reinstall as in Phase 6, or `simctl io screenshot` plus coordinates |

## Validation Architecture (nyquist disabled in config; mapping kept for the verifier)

| SC | Automated proof | Command |
|---|---|---|
| 1 recursive sync, stable ids | `KbContentTest`: assert the 23 old ids (literal list below) are in the index with Алгоритмы titles; `entries.size == 217`; generator tests unchanged | `./gradlew :shared:testAndroidHostTest`; `python3 tools/test_gen_content.py`; `python3 tools/gen_content.py --check` |
| 2 sections, global search | `sectionsInOrder` → the new 14-section list; commonTest `searchNotes` still green; a test that a body hit from another folder is found (e.g. search «корутин» returns notes from ≥2 sections) | same |
| 3 cross-folder links, images | `allNotesParseAndLinksResolve` with the new knownMissing; add: every `ImageBlock.file` exists in `kb/img/`; parser unit tests for `![[x.png]]` → ImageBlock and for a leading space | same |
| 4 bundle test + devices | the above, plus `./gradlew :shared:allTests :androidApp:assembleDebug` and `xcodebuild … iphonesimulator CODE_SIGNING_ALLOWED=NO build` | see Phase 6 summary for exact commands |

The 23 ids to pin: `n054ccc1e n056e6b1c n07ed966c n0875a2bf n08afb307 n17006052 n20407d85 n3aa07ae9 n3cf4f903 n3f4be5ed n3f836927 n4e5e7ef4 n6f3f2844 n87785994 n87d0fd88 n9e62f72d n9e814012 nb2db8dca nd0ce2057 nde479513 ne8e12a31 ned5215b7 nef21c1e1`.

**Device walkthrough (Android + iOS, agent-driven):**
1. KB tab: 14 section headers, collapsed. Expand Kotlin / Java to see 33 titles; collapse it again.
2. Open СОДЕРЖАНИЕ, tap the «Быстрый переход» `[Compose](#-jetpack-compose)` link, and check that it scrolls. Tap `[[Coroutines]]` (or any wikilink into Матрицы), then Back.
3. A cross-folder hop: Подготовка к собеседованию → `[[ООП]]` (Kotlin / Java) → a link inside → Back twice.
4. «Compose. Оптимизация»: 13 images render full-width while scrolling, with no crash or OOM. «ViewModel, SaveStateHandle…» shows its image as the first block.
5. Search «HashMap» returns hits from Вопросы с собесов and Kotlin / Java. Typing feels instant. Search «(» does not crash.
6. Open note 05 (Тренажёр решений) and confirm it is unchanged. Lessons still open (no regression).
7. Dark theme spot check on an image note and a table note.

## Assumptions Log

| # | Claim | Section | Risk if Wrong |
|---|---|---|---|
| A1 | Phone/K-N search is 3–10× slower than desktop JVM (20–80 ms per keystroke without the cache) | Search | Low. The cache fix is cheap either way |
| A2 | Eager load of 218 files is fast enough on the iOS simulator | Search & Load | Low. Visible as «…»; the fallback is a single concatenated file |
| A3 | Customer wants sections collapsed by default | Sections | Low. Default is one boolean |
| A4 | Separate «Общие вопросы» section (not merged into Вопросы с собесов) | Inventory | Cosmetic; one map entry |

## Open Questions (RESOLVED)
1. **Collapsible list.** CONTEXT says the screen is collapsible, but it is not. Recommendation: implement collapsible headers (Pattern above) and treat this as fulfilling the locked decision. Do not escalate.
   RESOLVED: collapsible section headers → 07-02.
2. **`==mark==` support.** It is not required by the success criteria, but 42 notes show a literal `==`. Recommendation: include it as a small task.
   RESOLVED: included → 07-01 Task 1.
3. **REQUIREMENTS KB-01/KB-02 wording** is outdated, as described above. Update it in the plan's docs task.
   RESOLVED: KB-01/KB-02 reworded, KB-07 added in REQUIREMENTS.md (planner).

## Sources
- Vault and repo inspection with scripts at `/tmp/p7/inv.py`, `/tmp/p7/md.py` and `/tmp/p7/sync2.py`. The prototype bundle and the KbContentTest/BenchTest runs were done in the repo copy `/tmp/p7/repo`. [VERIFIED]
- components-resources 1.12.2 metadata jar in the Gradle cache (`decodeToImageBitmap` in commonMain), plus a compile probe on Android and iOS. [VERIFIED]
- https://kotlinlang.org/docs/multiplatform/compose-multiplatform-resources-usage.html: `Res.readBytes`, `decodeToImageBitmap`, no directory listing. [CITED]

## Metadata
- Stack: HIGH (no new dependencies; image API compile-verified)
- Architecture: HIGH (prototype ran end to end against real data)
- Pitfalls: HIGH (each was reproduced: upscaling, NFD paths, knownMissing, section pin)
- Research date: 2026-10-22. Valid until the vault changes; re-run `/tmp/p7/inv.py`-style counts if notes are added.

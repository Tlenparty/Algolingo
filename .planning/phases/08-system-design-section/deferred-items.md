# Deferred items (Phase 08)

- `./gradlew :shared:assemble` падает на `compileCommonMainKotlinMetadata`: `ProgressRepository.kt:38,53` — `FileSystem.SYSTEM` / `writeUtf8` не резолвятся в common-метаданных okio. Файл в 08-01 не трогали, ошибка была до фазы. Android (`:androidApp:assembleDebug`), iOS (`:shared:compileKotlinIosSimulatorArm64`) и host-тесты собираются. (08-01)
- Банк облачков (`TokenChip` в design) по-прежнему подсвечивает числа и ключевые слова Kotlin и в design-уроках (например, `1200`, `120`). План требовал отключить подсветку в шаблоне, дырках, теории, эталоне и фидбеке; банк в этот список не входил. Решить на ревью 08-02/08-03. (08-01)

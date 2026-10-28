# Deferred items (Phase 08)

- `./gradlew :shared:assemble` падает на `compileCommonMainKotlinMetadata`: `ProgressRepository.kt:38,53` — `FileSystem.SYSTEM` / `writeUtf8` не резолвятся в common-метаданных okio. Файл в 08-01 не трогали, ошибка была до фазы. Android (`:androidApp:assembleDebug`), iOS (`:shared:compileKotlinIosSimulatorArm64`) и host-тесты собираются. (08-01)

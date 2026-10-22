# FragmentTransaction

`FragmentTransaction` — набор операций над фрагментами (`add`/`replace`/`remove`/`show`/`hide`), применяемых атомарно. Создаётся через `fragmentManager.beginTransaction()`, завершается одним из методов фиксации.

## Способы фиксации

| Метод                              | Когда выполняется         | Back stack | Допускает state loss |
| ---------------------------------- | ------------------------- | ---------- | -------------------- |
| `commit()`                         | Асинхронно (в очередь)    | ✅ можно    | ❌ кинет исключение    |
| `commitAllowingStateLoss()`        | Асинхронно                | ✅ можно    | ✅ (данные могут потеряться) |
| `commitNow()`                      | **Синхронно**, сразу      | ❌ нельзя   | ❌                    |
| `commitNowAllowingStateLoss()`     | Синхронно                 | ❌ нельзя   | ✅                    |

> [!important] Почему commitNow несовместим с back stack
> `commitNow()` / `commitNowAllowingStateLoss()` **не позволяют** добавлять транзакцию в back stack (`addToBackStack(...)`). Причина: синхронная транзакция выполняется немедленно и вне очереди, а корректный порядок back stack гарантируется только для отложенных (асинхронных) транзакций, обрабатываемых `FragmentManager` по очереди. Поэтому одновременное `commitNow()` + `addToBackStack()` бросит `IllegalStateException`.

## Проблема «state loss»

`commit()` бросает `IllegalStateException: Can not perform this action after onSaveInstanceState`, если вызвать его **после** `onSaveInstanceState()` (например, в `onPause`/`onStop` или в колбэке, пришедшем после ухода экрана в фон).

- **Причина:** после сохранения состояния система уже не сможет восстановить эту транзакцию при пересоздании → данные о ней потеряются.
- **`commitAllowingStateLoss()`** подавляет исключение, но транзакция действительно может «потеряться» при восстановлении. Это **обходной путь, а не решение** — применять осознанно (например, для некритичного UI в async-колбэке).
- **Правильно:** не выполнять транзакции после `onSaveInstanceState`; использовать `lifecycleScope`/`repeatOnLifecycle`, чтобы код шёл только в активном состоянии.

> [!tip] На современном Android
> Ручные `FragmentTransaction` всё реже — навигацией управляет **Navigation Component** (или Compose Navigation), который сам корректно работает с back stack и жизненным циклом. `commit`/`commitNow` важно понимать для legacy-кода и собеса.

Связано: [[Activity и Fragment]], [[Передача данных между фрагментами]], [[Сохранение состояние в android и Bundle]]

---
quick_id: 261102-lc4
status: complete
---
# Итог: четыре урока с LeetCode

- Новые уроки l2.8, l3.2, l4.2, l5.1 (Kotlin + Python), по 3 упражнения, причина на каждое неверное облачко.
- Эталоны в заметке 05 переписаны (350ddb3); уровни 3–5 работают без изменений в коде.
- test_gen_content.py: golden-тесты 0.1/0.2 обновлены под строки «Случай N» (сломаны в 261102-cas).
- Проверка: gen_content.py и --check OK, test_gen_content.py OK, :shared:testAndroidHostTest зелёный; на S10e уроки видны в конце списка (закрыты до прохождения предыдущих).
- Debug-сборка: все уроки открыты (unlockAll из FLAG_DEBUGGABLE в MainActivity → AppGraph → BuildHomeModelUseCase); release — квест по порядку; тест CatalogTest.unlockAllOpensLockedKeepsDoneAndSoon.

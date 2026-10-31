---
quick_id: 261031-arc
status: complete
---
# Слои data/domain/presentation и MVI

Выбранный заказчиком вариант после ревью архитектуры. Поведение приложения и форматы файлов не меняются.

1. CodeLang из design в core.domain, в design нейтральный SyntaxLang.
2. Разложить lesson, progress, kb, home по data, domain, presentation; общий markdown и inline text в core.
3. Отделить DTO от доменных моделей.
4. Интерфейсы репозиториев, контейнер AppGraph/AppContainer, UseCase только для загрузки урока, записи результата и расчёта главной.
5. HomeViewModel, KbListViewModel, NoteViewModel с UiState/Action/Effect; App.kt только навигация; чтение картинок из UI убрать.
6. Проверка: host-тесты, сборка Android, компиляция iOS и device-тестов, e2e на устройстве, затем слияние в main.

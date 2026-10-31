---
quick_id: 261031-rm1
status: complete
---
# Итог: урок SD0.1 убран

- Каталог System Design: 14 уроков, первый sd0.3 (f196801).
- Запись sd0.1 в прогрессе безвредна: статусы и повторения строятся только по каталогу.
- Проверка: :shared:testAndroidHostTest и :androidApp:assembleDebug exit 0, test_gen_content.py 82 OK, gen_content.py --check exit 0. На устройстве и e2e не гонялись, iOS не собирался.
- Инкрементальная сборка оставляет sd0.1.json в build/ и в APK; урок нигде не показывается, clean-сборка убирает копию.
- gsd-sdk на этой машине не установлен, задача оформлена вручную.

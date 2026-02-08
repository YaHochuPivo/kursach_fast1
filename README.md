# RealEstate — запуск и руководство

## Требования
- Java 17+
- Maven 3.8+
- PostgreSQL 13+ (продакшн/дев)

## Переменные окружения
- MAIL_HOST (default: smtp.gmail.com)
- MAIL_PORT (default: 465)
- MAIL_USERNAME, MAIL_PASSWORD, MAIL_FROM
- DB_URL (default: jdbc:postgresql://localhost:5432/realestate)
- DB_USERNAME (default: postgres)
- DB_PASSWORD (default: postgres)

## Сборка и запуск
```
mvn spring-boot:run
```
Или с профилем тестов:
```
mvn -Dspring.profiles.active=test test
```

## Миграции БД (Flyway)
- V1__db_enhancements.sql — статус archived, аудит properties, view v_property_summary
- V2__procedures_triggers_views.sql — процедуры архивации/снятия продвижения, универсальный аудит, v_deal_summary (если есть deals)
- V3__user_settings.sql — таблица user_settings

## Основные функции
- Архивация объявлений (владелец или админ)
  - POST /v1/api/properties/{id}/archive
  - POST /v1/api/properties/{id}/unarchive
- CSV экспорт/импорт объявлений пользователя
  - GET /v1/api/properties/csv/export (text/csv)
  - POST /v1/api/properties/csv/import (multipart form field: file)
- Отчеты (веб-страница)
  - GET /reports — графики по городам и типам, CSV экспорт
- Логи админа
  - Кнопка «Логи» в профиле администратора
- Пользовательские настройки
  - GET/POST /v1/api/user-settings (theme, locale, numberFormat, pageSize, savedFilters)
  - UI: Профиль → «Настройки пользователя»
- Горячие клавиши (подключено на главной и Мои объявления)
  - g h — Главная
  - g p — Недвижимость
  - g m — Мои объявления
  - g r — Отчеты
  - / — Фокус на поиск (на главной)
  - n — Новое объявление (если доступно)
  - s — Фокус на настройках профиля
  - ? — Справка по хоткеям

## Резервное копирование/восстановление
PowerShell скрипты:
- scripts/backup-db.ps1
- scripts/restore-db.ps1 -DumpFile <путь к .dump>

Требуются переменные окружения DB_HOST, DB_PORT, DB_NAME, DB_USERNAME, DB_PASSWORD.

## Тесты
Интеграционные тесты (H2, профиль test):
- PropertyApiArchiveTests — архивирование/разархивирование
- PropertyCsvTests — экспорт/импорт CSV

Запуск:
```
mvn -Dspring.profiles.active=test test
```

## Безопасность и роли
- User, Realtor, Admin (role-based UI и API)
- Владелец/Админ могут архивировать объявление

## Известные заметки
- В dev-логах возможны предупреждения статических ресурсов — не критично.

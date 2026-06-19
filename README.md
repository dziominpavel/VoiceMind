# VoiceMind

Android-**напоминалка с голосового ввода**: говорите «завтра в 9:00 позвонить соседу» — приложение распознаёт речь, извлекает **когда** и **о чём** напомнить, и в нужный момент срабатывает выбранным способом (уведомление, будильник, вибрация и т.д.).

> Статус: **MVP реализован** — фазы 0-3 готовы: голосовой ввод, парсинг, подтверждение, Room, AlarmManager, уведомления, списки, настройки. Фазы 4+ — в плане.

## Как это работает (кратко)

1. Пользователь записывает фразу голосом (или вводит текст).
2. Получаем текст (Speech-to-Text) → **парсер** выделяет дату/время и текст напоминания.
3. Экран **подтверждения**: можно поправить время и текст.
4. Напоминание сохраняется в Room → планируется через `AlarmManager`.
5. В срок — доставка по режиму из **настроек** (пуш / будильник / только вибро / без звука и др.).

## Возможности (целевые)

| Область | Описание |
|---------|----------|
| Голос | Запись фразы, STT на устройстве (MVP), опционально уточнение через API |
| Парсинг | Русские формулировки: «завтра», «через час», «в понедельник в 9», «21:30» |
| Подтверждение | Превью «когда + текст» до постановки в очередь |
| Список | Предстоящие и прошедшие напоминания |
| Срабатывание | Точные alarm, переживание перезагрузки, Doze |
| Настройки | Режим оповещения по умолчанию, тихие часы (позже) |

## Стек

- **Kotlin**, модуль `:app`
- **Jetpack Compose** + Material 3
- **Room**, **DataStore**
- **SpeechRecognizer** (или аналог) + свой **парсер даты/времени** (offline)
- **AlarmManager** + **NotificationManager** + опционально full-screen alarm
- Архитектура кода — как в **GymProgress** (один ViewModel, `safeDb`, overlay-навигация); **UI и продукт — свои**

## Документация

| Файл | Содержание |
|------|------------|
| [docs/PROJECT_OVERVIEW.md](docs/PROJECT_OVERVIEW.md) | Продукт, сценарии, модель данных, идеи |
| [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) | Парсинг, планировщик, уведомления |
| [docs/FEATURE_PLAN.md](docs/FEATURE_PLAN.md) | **План реализации по фазам** |
| [docs/FOLDER_STRUCTURE.md](docs/FOLDER_STRUCTURE.md) | Дерево каталогов |
| [openspec/specs/reminder-parsing/spec.md](openspec/specs/reminder-parsing/spec.md) | Правила парсинга и примеры фраз (BDD) |
| [openspec/specs/notification-delivery/spec.md](openspec/specs/notification-delivery/spec.md) | Режимы оповещения и доставка (BDD) |
| [openspec/specs/ui-screens/spec.md](openspec/specs/ui-screens/spec.md) | Требования к экранам и навигации (BDD) |

Для агента: [AGENTS.md](AGENTS.md).

## Референсы

- **Стиль кода и документации:** GymProgress (`AGENTS.md`, `docs/`, Room/DataStore/Compose-паттерны).
- **UI VoiceMind:** свой (тема CLEAR BELL/NeoWave в `app/.../ui/theme`, требования — в `openspec/specs/ui-screens/spec.md`), не копировать «IRON CORE».

## Быстрый старт

1. Открыть в Android Studio, `local.properties` с `sdk.dir` (или скопировать из другого Android-проекта).
2. Сборка: `gradlew.bat assembleDebug` (Windows) / `./gradlew assembleDebug`.
3. Установка на телефон: `gradlew.bat installDebug` или Run в IDE.
4. APK: `app/build/outputs/apk/debug/app-debug.apk`.

## Лицензия

Уточняется владельцем репозитория.

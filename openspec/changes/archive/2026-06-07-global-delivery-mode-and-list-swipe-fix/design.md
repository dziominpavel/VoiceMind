## Context

**Текущее состояние delivery mode:**
- `SettingsRepository.defaultDeliveryMode` — глобальный дефолт для новых напоминаний.
- `Reminder.deliveryMode` (Room) — снимок при создании; при срабатывании `ReminderNotifier` и `ReminderAlarmReceiver` читают поле записи.
- `ConfirmReminderScreen` показывает `DeliveryModeGrid`, но выбор не сохраняется (баг).
- `ManualReminderScreen` позволяет выбрать режим per-reminder.

**Текущее состояние swipe-to-reveal:**
- `SwipeToRevealBox` использует `matchParentSize()` + `errorContainer` на всю ширину за скруглённой `Card`.
- Иконка режима доставки — у правого края карточки; при offset −80dp наезжает на иконку удаления.
- Change `fix-vibration-swipe-reveal-and-ui-layout` внедрил паттерн, но не устранил clip/overlap.

**Ограничения:**
- `ReminderScheduler` — единственная точка планирования alarm (не меняем).
- Destructive migration в release запрещена.
- Колонку `deliveryMode` в Room можно оставить (deprecated field) для обратной совместимости схемы.

## Goals / Non-Goals

**Goals:**
- Единственный источник правды для режима оповещения — `settings.defaultDeliveryMode` (+ `useVibration`).
- Миграция всех записей (любой статус) на текущий default при обновлении приложения.
- Немедленная синхронизация всех записей при смене режима в настройках.
- Убрать per-reminder UI выбора режима с Confirm/Manual.
- Исправить визуальные дефекты swipe-to-reveal: красные углы, наложение иконок.

**Non-Goals:**
- Удаление колонки `deliveryMode` из Room (можно в будущем).
- Изменение логики `AlarmSoundPlayer`, каналов, `ReminderScheduler`.
- Новые режимы доставки.
- Изменение widget API.

## Decisions

### 1. Runtime: читать режим из Settings, не из Reminder

**Решение:** `ReminderNotifier.show()` и `ReminderAlarmReceiver.onReceive()` получают `deliveryMode` через `SettingsRepository.getDefaultDeliveryMode()` вместо `reminder.deliveryMode`.

**Почему:** Прямое соответствие продуктовой модели «один профиль для всех». Не требует перепланирования alarm при смене настроек (alarm не зависит от delivery mode).

**Альтернатива:** Оставить чтение из Room, но синхронизировать при каждой смене настроек. **Отклонено:** дублирование источников правды; риск рассинхрона если синхронизация пропущена.

### 2. Синхронизация Room при смене настроек и при миграции

**Решение:**
- Room migration `5 → 6`: `UPDATE reminders SET deliveryMode = '<default>'` — default читается в `Migration` через raw SQL с фиксированным fallback `ALARM` (миграция не имеет доступа к DataStore). Дополнительно при первом запуске после миграции `VoiceMindViewModel` или `Application.onCreate` вызывает `ReminderRepository.syncAllDeliveryModes(settings.getDefaultDeliveryMode())`.
- При `setDefaultDeliveryMode()` в ViewModel: `repository.updateAllDeliveryModes(mode)` для всех записей без фильтра по статусу.

**Почему:** Пользователь явно потребовал миграцию в текущие настройки и применение ко всем статусам. Двойной шаг (migration + startup sync) гарантирует корректность даже если DataStore default отличается от SQL fallback.

**Альтернатива:** Только runtime из Settings, Room поле игнорировать полностью. **Отклонено частично:** поле остаётся для widget/отладки, но синхронизируется для консистентности UI.

### 3. UI списка: иконка режима из Settings Flow

**Решение:** `ReminderListScreen` и `HomeScreen` принимают `currentDeliveryMode: DeliveryMode` из `settings.defaultDeliveryMode` Flow; не читают `reminder.deliveryMode`.

**Почему:** Все карточки показывают одинаковую иконку, соответствующую текущим настройкам.

### 4. Убрать DeliveryModeGrid с Confirm и Manual

**Решение:** Удалить `DeliveryModeGrid`, `selectedDeliveryMode`, параметры `deliveryMode` из save-путей. `DeliveryModeGrid` composable можно оставить в коде (unused) или удалить — предпочтительно удалить если не используется в Settings.

**Почему:** Нет per-reminder выбора — сетка вводит в заблуждение (особенно на Confirm, где выбор и так не сохранялся).

### 5. Исправление SwipeToRevealBox

**Решение (три изменения):**

```
ДО (баг):
┌──────────────────────────────────────── red errorContainer (full width)
│  [ Card rounded corners → red peeks through ]
│                              [delete icon]

ПОСЛЕ:
┌────────────────────────────────────────────────────────┐
│  [ Card clipped to shape.medium, opaque background ]   │
│                                    │ red 80dp strip │  │
│                                    │   [delete]     │  │
└────────────────────────────────────────────────────────┘
```

1. **Фон действия** — `Box` шириной `maxReveal` (80dp), `align(Alignment.CenterEnd)`, фон `errorContainer`. Убрать `matchParentSize()` красный фон.
2. **Clip foreground** — `Modifier.clip(MaterialTheme.shapes.medium)` на foreground `Box`; `Card` с непрозрачным `containerColor`.
3. **Скрытие иконки режима при reveal** — `SwipeToRevealBox` принимает `isRevealed`; `UpcomingReminderCard` скрывает delivery icon когда `isRevealed == true`. Альтернатива: передавать `isRevealed` в card через callback/state.

**Альтернатива:** Убрать иконку режима из карточки совсем (раз она одинаковая). **Отклонено:** иконка информативна; скрытие при reveal достаточно.

### 6. ReminderDao: bulk update

**Решение:** Добавить `@Query("UPDATE reminders SET deliveryMode = :mode") suspend fun updateAllDeliveryModes(mode: String)`.

## Risks / Trade-offs

| Риск | Компенсация |
|------|-------------|
| Migration не знает DataStore default | Startup sync после миграции перезаписывает из реальных настроек |
| Смена ALARM→SILENT не отменяет уже играющий alarm | Out of scope; dismiss через notification actions как сейчас |
| Удаление DeliveryModeGrid ломает тесты/превью | Обновить composable previews и instrumented tests |
| Clip + offset может дать артефакты на старых API | Тестировать API 26–36; использовать `graphicsLayer` если clip недостаточен |

## Migration Plan

1. Bump Room `version` 5 → 6.
2. `MIGRATION_5_6`: `UPDATE reminders SET deliveryMode = 'ALARM'` (fallback; startup sync исправит).
3. В `VoiceMindApplication` или `MainActivity.onCreate`: one-shot `syncAllDeliveryModes()` при первом запуске после upgrade (флаг в DataStore `delivery_mode_synced_v6`).
4. `setDefaultDeliveryMode()` → `updateAllDeliveryModes()` + emit settings flow.
5. Откат: revert runtime к `reminder.deliveryMode`; данные в Room уже синхронизированы — безопасно.

## Open Questions

- Нет — пользователь подтвердил: мигрировать в текущие настройки, применять ко всем статусам.

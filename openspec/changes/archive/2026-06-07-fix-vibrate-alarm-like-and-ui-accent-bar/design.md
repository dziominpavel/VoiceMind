## Context

Текущая реализация `DeliveryMode.VIBRATE` использует `PARTIAL_WAKE_LOCK` и не запускает `AlarmActivity`. Пользователь ожидает, что «Вибрация» — это будильник без звука: телефон должен включать экран, показывать полноэкранный интерфейс поверх блокировки, и вибрировать длинным паттерном (так же, как ALARM с вибрацией, только без ringtone).

Также в `ReminderListScreen` левая цветная полоса (accent bar) реализована через `Box` с цепочкой модификаторов, где `fillMaxWidth()` ошибочно перекрывает `width(barWidth)`, растягивая полосу на всю ширину карточки. Это создаёт визуальное наслоение — фон карточки полностью заливается accent-цветом.

## Goals / Non-Goals

**Goals:**
- Режим VIBRATE ведёт себя идентично ALARM за исключением отсутствия звукового сигнала
- `AlarmActivity` срабатывает для VIBRATE с тем же UI, что и для ALARM
- Accent bar в `ReminderListScreen` имеет фиксированную ширину и высоту карточки, не перекрывая контент

**Non-Goals:**
- Изменение поведения ALARM, NOTIFICATION, SILENT
- Изменение дизайна `AlarmActivity`
- Редизайн карточки списка (только фикс бага раскладки)

## Decisions

### VIBRATE = ALARM без звука
- **Decision**: `ReminderAlarmReceiver` для VIBRATE берёт `FULL_WAKE_LOCK | ACQUIRE_CAUSES_WAKEUP | ON_AFTER_RELEASE` (как ALARM), запускает `AlarmSoundPlayer.playVibrationOnly()`, и показывает `AlarmActivity` через full-screen intent в `ReminderNotifier`.
- **Rationale**: Пользовательский интерфейс настроек позиционирует «Вибрация» как громкий режим (между ALARM и NOTIFICATION). Ожидание — включение экрана + полноэкранный UI.
- **Alternative considered**: Оставить `PARTIAL_WAKE_LOCK` и добавить отдельный режим «Тихая вибрация» — отклонено, т.к. SILENT уже покрывает «тихое» поведение, а 5 режимов достаточно.

### Общий код ALARM и VIBRATE в ReminderAlarmReceiver
- **Decision**: Объединить ветвление ALARM и VIBRATE в одну ветку wake lock, а разделить только на уровне вызова `AlarmSoundPlayer`.
- **Rationale**: DRY. Оба режима требуют одного и того же wake lock и одного и того же full-screen intent.

### Accent bar: fillMaxHeight() вместо fillMaxWidth()
- **Decision**: Заменить `fillMaxWidth()` на `fillMaxHeight()` в `Box` accent bar.
- **Rationale**: `Box` должен занимать фиксированную ширину (`barWidth`, обычно 4dp) и всю высоту карточки. `fillMaxWidth()` игнорирует `width()` и растягивает Box по ширине родителя.

## Risks / Trade-offs

- **[Risk]** VIBRATE теперь потребляет столько же батареи, сколько ALARM (яркий экран + wake lock) → **Mitigation**: Ожидаемое поведение для громкого режима; пользователь сам выбирает режим.
- **[Risk]** На API 34+ `USE_FULL_SCREEN_INTENT` требует разрешения; для VIBRATE это ранее не проверялось → **Mitigation**: Уже запрошено в манифесте и проверяется в `ReminderScheduler`.

## Migration Plan

Нет миграции данных. Изменение поведения runtime-only.

## Open Questions

*(none)*

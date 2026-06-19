## Context

В MVP VoiceMind все напоминания планируются через `AlarmManager.setExactAndAllowWhileIdle(RTC_WAKEUP, fireAt, pi)` в `ReminderScheduler`. Этот API специально предназначен для одиночных срабатываний в Doze и уже обходит оптимизацию батареи для одного alarm. Recurring-напоминания перепланируются внутри `ReminderAlarmReceiver.onReceive()` через `goAsync()` + 10-секундный `WakeLock`, после чего следующий alarm снова ставится через тот же `setExactAndAllowWhileIdle`. Между срабатываниями приложение не ведёт фоновой работы: нет сервиса, нет polling, нет sync.

Текущая подсистема `reliability-onboarding` запрашивает у пользователя исключение из оптимизации батареи через `PowerManager.isIgnoringBatteryOptimizations()` и `Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)`. Permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` является flagged в Google Play и требует обоснования при публикации. Эмпирически без этого exemption напоминания срабатывали стабильно.

## Goals / Non-Goals

**Goals:**
- Убрать из приложения запрос исключения из оптимизации батареи как обязательный шаг надёжности.
- Удалить flagged permission `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` из манифеста.
- Упростить wizard надёжности и экран настроек (минус один шаг/блок).
- Обновить spec `reliability-onboarding` так, чтобы он больше не требовал проверки battery optimization.

**Non-Goals:**
- Не менять механизм планирования через `AlarmManager` / `ReminderScheduler`.
- Не добавлять замену battery exemption (например, инструкции для OEM-киллеров Xiaomi/Huawei) — это отдельная фича на будущее.
- Не чинить баг race condition при первом запуске wizard (отдельная change).
- Не трогать `BootReceiver`, `WakeLock` в receiver, `setExactAndAllowWhileIdle` fallback на `setAndAllowWhileIdle`.

## Decisions

### Decision 1: Полное удаление вместо «спрятать в расширенные настройки»

**Решение:** Удалить battery exemption целиком — код, permission, UI-блоки, spec-requirement.

**Альтернативы:**
- *Перенести в Settings как опциональную «расширенную настройку».* Отклонено: фича не даёт измеримой пользы для текущего MVP, а её присутствие в UI провоцирует пользователя выдавать flagged permission без необходимости. Если когда-то понадобится — вернём как отдельную capability с обоснованием.
- *Оставить только проверку без запроса (read-only индикатор).* Отклонено: индикатор без возможности исправить бесполезен и раздражает; кроме того, он всё равно заставляет тащить `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` для запроса, либо оставляет пользователя без действия.

### Decision 2: Удалить `ReliabilityIssue.BATTERY_OPTIMIZATION_NOT_IGNORED` из enum

**Решение:** Убрать значение из enum целиком, а не просто перестать его вычислять. Это гарантирует, что нигде не останется мёртвых `when`-веток и баннер надёжности не будет показываться из-за battery.

**Альтернативы:**
- *Оставить значение, но не добавлять в `_reliabilityIssues`.* Отклонено: оставляет мёртвый код и ветки в `when`, которые невозможно достичь.

### Decision 3: Не вводить миграцию настроек

**Решение:** Не трогать DataStore. Флаг `onboarding_completed` остаётся как есть; никаких ключей, связанных с battery exemption, в DataStore не хранилось (состояние проверялось через `PowerManager` в рантайме).

**Альтернативы:**
- *Сбросить `onboarding_completed`, чтобы пользователи прошли wizard заново без шага battery.* Отклонено: несправедливо заставлять пользователя повторять onboarding ради косметического изменения.

### Decision 4: Spec-операция — REMOVED Requirement

**Решение:** В delta spec для `reliability-onboarding` использовать `## REMOVED Requirements` для requirement «Запрос исключения из оптимизации батареи» с Reason и Migration. Остальные requirements capability (мастер при первом запуске, тестовое напоминание, индикатор на главном экране, повторный вызов из настроек) остаются без изменений — они продолжают работать, просто wizard становится короче.

**Альтернативы:**
- *MODIFIED на requirement «Индикатор надёжности на главном экране».* Рассматривался, но индикатор уже скрывается, когда `reliabilityIssues` пуст; после удаления battery-проверки условие просто становится строже, поведение не меняется на уровне spec. Достаточно REMOVED одного requirement.

## Risks / Trade-offs

- **[Risk] На отдельных OEM-сборках (Xiaomi MIUI, Huawei EMUI) система убивает приложение и alarm не срабатывает.** → Mitigation: battery exemption всё равно не решает эту проблему надёжно (у OEM свои настройки «Автозапуска»/«Запуска в фоне», не подчиняющиеся стандартному Android exemption). Если проблема станет массовой — заведём отдельную change с OEM-специфичными инструкциями, не возвращая flagged permission.
- **[Risk] Пользователь, привыкший видеть шаг battery в wizard, удивится его исчезновению.** → Mitigation: шаг был обязательным только при первом запуске; для уже прошедших onboarding wizard автоматически не показывается. Влияние минимально.
- **[Trade-off] Теряем «правильный с точки зрения Android-доков» запрос battery exemption.** → Принято: корректность для нашего сценария (одиночные alarms через `setExactAndAllowWhileIdle`) не требует этого permission; доки рекомендуют exemption для persistent background work, которого у нас нет.
- **[Risk] В коде останутся ссылки на удалённые символы (compile errors).** → Mitigation: tasks.md включает обязательный шаг `./gradlew assembleDebug` после правок; компилятор поймает все dangling references.

## Migration Plan

1. Удалить permission из `AndroidManifest.xml`.
2. Удалить методы из `ReminderPermissions.kt`.
3. Удалить значение enum и проверку из `VoiceMindViewModel.kt`.
4. Удалить шаг/блоки из UI-экранов и wiring в `MainActivity.kt`.
5. Удалить строковые ресурсы.
6. Обновить spec (REMOVED Requirement).
7. `./gradlew assembleDebug` + `./gradlew testDebugUnitTest` — убедиться, что ничего не сломалось.
8. Ручной smoke: запустить приложение, пройти wizard (должен быть без шага battery), проверить, что баннер надёжности не показывается при наличии notifications + exact alarm.

**Rollback:** `git revert` коммита change. Спека и код восстановятся целиком. DataStore не модифицировался, миграция данных не нужна.

## Open Questions

Нет. Решение полностью определено в рамках proposal.

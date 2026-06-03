## ADDED Requirements

### Requirement: Контракт ParseResult
Парсер ДОЛЖЕН возвращать ParseResult с полями: fireAt (Instant?), body (String), confidence (Float), warnings (List<ParseWarning>), matchedTimeSpan (IntRange?), rawPhrase (String).

#### Scenario: Успешный парсинг
- **WHEN** пользователь говорит "завтра в 9:00 позвонить"
- **THEN** ParseResult.fireAt = завтра 09:00
- **AND** ParseResult.body = "позвонить"
- **AND** ParseResult.confidence >= 0.7
- **AND** ParseResult.warnings пустой

### Requirement: ParseWarning TIME_AMBIGUOUS
Если время найдено без явного указания am/pm или части дня, парсер ДОЛЖЕН выдавать warning TIME_AMBIGUOUS.

#### Scenario: Неоднозначное время
- **WHEN** пользователь говорит "в девять позвонить"
- **THEN** fireAt = сегодня 09:00 (или 21:00 — по эвристике)
- **AND** ParseResult.warnings содержит TIME_AMBIGUOUS
- **AND** confidence <= 0.5

### Requirement: ParseWarning NO_TIME_FOUND
Если время не найдено, парсер ДОЛЖЕН выдавать warning NO_TIME_FOUND.

#### Scenario: Нет времени
- **WHEN** пользователь говорит "купить молоко" без времени
- **THEN** ParseResult.fireAt = null
- **AND** ParseResult.warnings содержит NO_TIME_FOUND
- **AND** confidence = 0

### Requirement: ParseWarning BODY_EMPTY
Если после удаления time spans остаётся пустая строка, парсер ДОЛЖЕН выдавать warning BODY_EMPTY.

#### Scenario: Пустой body
- **WHEN** пользователь говорит "завтра в 9:00"
- **THEN** fireAt = завтра 09:00
- **AND** ParseResult.body = ""
- **AND** ParseResult.warnings содержит BODY_EMPTY

### Requirement: Candidate-based engine
Парсер ДОЛЖЕН использовать candidate-based engine: находить ВСЕ матчи regex, создавать DateCandidate/TimeCandidate со score, выбирать лучших по max score.

#### Scenario: Конфликт кандидатов
- **WHEN** пользователь говорит "утром в 9:00 позвонить"
- **THEN** выбирается кандидат HH:mm (score 100), а не "утром" (score 40)
- **AND** fireAt = 09:00
- **AND** TIME_AMBIGUOUS warning НЕ выдаётся

### Requirement: Прошедшее время
Если распознанный fireAt < now и в фразе было "сегодня", парсер ДОЛЖЕН выдавать warning PAST_TIME_ADJUSTED.

#### Scenario: Утро уже прошло
- **WHEN** пользователь говорит "сегодня в 8 утра позвонить" в 14:00
- **THEN** fireAt = завтра 08:00
- **AND** ParseResult.warnings содержит PAST_TIME_ADJUSTED

### Requirement: Часовой пояс локальный
Все времена парсера ДОЛЖНЫ быть в локальном часовом поясе устройства.

#### Scenario: Локальный timezone
- **WHEN** парсер вычисляет fireAt
- **THEN** используется ZoneId.systemDefault()
- **AND** epoch millis корректны для текущего TZ

## ADDED Requirements

### Requirement: Parse "через пару часов"
The parser SHALL recognize "через пару часов" as a relative time offset of 2 hours from now.

#### Scenario: Couple of hours
- **WHEN** user says "через пару часов позвонить"
- **THEN** fireAt is now + 2 hours
- **AND** body is "позвонить"

### Requirement: Parse "через несколько минут"
The parser SHALL recognize "через несколько минут" as a relative time offset of 5 minutes from now.

#### Scenario: A few minutes
- **WHEN** user says "через несколько минут проверить"
- **THEN** fireAt is now + 5 minutes
- **AND** body is "проверить"

### Requirement: Parse "через неделю"
The parser SHALL recognize "через неделю" as a relative date offset of 7 days, defaulting to morning time if no explicit time is given.

#### Scenario: In a week
- **WHEN** user says "через неделю сдать отчёт"
- **THEN** fireAt is today + 7 days at 09:00
- **AND** body is "сдать отчёт"

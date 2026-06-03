## ADDED Requirements

### Requirement: Parse "следующий" weekday
The parser SHALL recognize phrases containing "следующий" or "следующую" followed by a weekday name and resolve them to the same weekday of the following week.

#### Scenario: Next Monday
- **WHEN** user says "в следующий понедельник в 10:00 встреча"
- **THEN** fireAt is the Monday of next week at 10:00
- **AND** body is "встреча"

#### Scenario: Next Wednesday with feminine form
- **WHEN** user says "в следующую среду позвонить"
- **THEN** fireAt is the Wednesday of next week at 09:00 (default morning)
- **AND** body is "позвонить"

#### Scenario: Next Friday with accusative form
- **WHEN** user says "в следующую пятницу в 18:00 тренировка"
- **THEN** fireAt is the Friday of next week at 18:00
- **AND** body is "тренировка"

## ADDED Requirements

### Requirement: Parse digit + "числа"
The parser SHALL recognize "<digit> числа" as referring to that day of the current month, rolling to the next month if the day has already passed.

#### Scenario: 9th of the month
- **WHEN** user says "9 числа заполнить анкету" on May 17
- **THEN** fireAt is June 9 at 09:00 (default morning)
- **AND** body is "заполнить анкету"

#### Scenario: 5th of the month in current month
- **WHEN** user says "5 числа оплатить" on May 3
- **THEN** fireAt is May 5 at 09:00
- **AND** body is "оплатить"

#### Scenario: 31st with explicit time
- **WHEN** user says "31 числа в 14:00 совещание"
- **THEN** fireAt is the 31st of current or next month at 14:00
- **AND** body is "совещание"

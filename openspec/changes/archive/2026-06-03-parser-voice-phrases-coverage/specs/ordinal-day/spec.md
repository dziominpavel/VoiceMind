## ADDED Requirements

### Requirement: Parse ordinal word + "числа"
The parser SHALL recognize Russian ordinal numerals from "первого" to "тридцать первого" followed by "числа" and resolve them to the corresponding day of the month, rolling to the next month if the day has already passed.

#### Scenario: Ninth of the month
- **WHEN** user says "девятого числа заполнить анкету" on May 17
- **THEN** fireAt is June 9 at 09:00 (default morning)
- **AND** body is "заполнить анкету"

#### Scenario: First of the month
- **WHEN** user says "первого числа оплатить счёт" on May 17
- **THEN** fireAt is June 1 at 09:00
- **AND** body is "оплатить счёт"

#### Scenario: Thirty-first with explicit time
- **WHEN** user says "тридцать первого числа в 10:00 отчёт"
- **THEN** fireAt is the 31st of current or next month at 10:00
- **AND** body is "отчёт"

#### Scenario: Composite form (twenty-first)
- **WHEN** user says "двадцать первого числа в 18:00 тренировка" on May 17
- **THEN** fireAt is May 21 at 18:00 (still in current month)
- **AND** body is "тренировка"

#### Scenario: Alternative spelling with ё
- **WHEN** user says "четвёртого числа дело"
- **THEN** fireAt is the 4th of current or next month at 09:00
- **AND** body is "дело"

## MODIFIED Requirements

### Requirement: Extend ordinal-day to support digit form
The existing ordinal-day capability SHALL be extended to also match "<digit> числа" using the same month-rollover logic.

#### Scenario: Digit ordinal 5th
- **WHEN** user says "5 числа позвонить"
- **THEN** fireAt is the 5th of current or next month at 09:00
- **AND** body is "позвонить"

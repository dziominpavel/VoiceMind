## ADDED Requirements

### Requirement: Parse part-of-day before time
The parser SHALL recognize when a part-of-day word (утром, днём, вечером, ночью) precedes a time expression and resolve the time correctly.

#### Scenario: Morning before time
- **WHEN** user says "утром в 9 позвонить"
- **THEN** fireAt is today at 09:00 (morning context removes ambiguity)
- **AND** body is "позвонить"
- **AND** TIME_AMBIGUOUS warning is NOT present

#### Scenario: Evening before time
- **WHEN** user says "вечером в 8 фильм"
- **THEN** fireAt is today at 20:00
- **AND** body is "фильм"

#### Scenario: Part-of-day before short hour
- **WHEN** user says "днём в 3 обед"
- **THEN** fireAt is today at 15:00
- **AND** body is "обед"

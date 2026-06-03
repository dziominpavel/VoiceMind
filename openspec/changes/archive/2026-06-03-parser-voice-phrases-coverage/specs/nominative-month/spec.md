## ADDED Requirements

### Requirement: Parse nominative month names
The parser SHALL recognize month names in nominative case (as STT may output them) alongside the existing genitive forms.

#### Scenario: Nominative May
- **WHEN** user says "9 май заполни анкету"
- **THEN** fireAt is May 9 at 09:00 (or next year if past)
- **AND** body is "заполнить анкету"

#### Scenario: Nominative June
- **WHEN** user says "25 июнь в 10:00 встреча"
- **THEN** fireAt is June 25 at 10:00
- **AND** body is "встреча"

#### Scenario: Nominative December
- **WHEN** user says "20 декабрь 2026 в 10:00 праздник"
- **THEN** fireAt is December 20, 2026 at 10:00
- **AND** body is "праздник"

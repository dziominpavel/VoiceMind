## ADDED Requirements

### Requirement: Parse "на выходных"
The parser SHALL recognize "на выходных" and resolve it to the nearest upcoming Saturday at the default morning time.

#### Scenario: Weekend reminder
- **WHEN** user says "на выходных сходить в магазин"
- **THEN** fireAt is the nearest upcoming Saturday at 09:00
- **AND** body is "сходить в магазин"

#### Scenario: Weekend with explicit time
- **WHEN** user says "на выходных в 11:00 бранч"
- **THEN** fireAt is the nearest upcoming Saturday at 11:00
- **AND** body is "бранч"

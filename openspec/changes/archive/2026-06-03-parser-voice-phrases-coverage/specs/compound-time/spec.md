## ADDED Requirements

### Requirement: Parse "половина N+1"
The parser SHALL recognize "половина [часа]" as meaning N:30 where N is the hour named minus one (Russian convention: "половина девятого" = 8:30).

#### Scenario: Half past eight
- **WHEN** user says "в половине девятого позвонить"
- **THEN** fireAt is today at 08:30 (or tomorrow if past)
- **AND** body is "позвонить"

### Requirement: Parse "без M [минут] N"
The parser SHALL recognize "без M [минут] [часа] N" as meaning (N-1)h:(60-M)min (Russian convention: "без пятнадцати девять" = 8:45).

#### Scenario: Quarter to nine
- **WHEN** user says "без пятнадцати девять встреча"
- **THEN** fireAt is today at 08:45 (or tomorrow if past)
- **AND** body is "встреча"

#### Scenario: Without minutes word
- **WHEN** user says "без четверти восемь"
- **THEN** fireAt is today at 07:45
- **AND** body is empty or triggers BODY_EMPTY warning

### Requirement: Parse "четверть N+1"
The parser SHALL recognize "четверть [часа] N" as meaning (N-1):15 (Russian convention: "четверть девятого" = 8:15).

#### Scenario: Quarter past eight
- **WHEN** user says "в четверть девятого обед"
- **THEN** fireAt is today at 08:15
- **AND** body is "обед"

### Requirement: Parse "N с половиной"
The parser SHALL recognize "[в] N с половиной" as meaning N:30.

#### Scenario: Nine thirty colloquial
- **WHEN** user says "в девять с половиной звонок"
- **THEN** fireAt is today at 09:30
- **AND** body is "звонок"

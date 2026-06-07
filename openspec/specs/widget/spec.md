## Purpose

Домашний виджет VoiceMind с ближайшим напоминанием и быстрым доступом к микрофону.

## Requirements

### Requirement: Цвета виджета
Виджет MUST использовать прозрачный фон и цвета NeoWave Voice.

#### Scenario: Отображение
- **WHEN** виджет отображается
- **THEN** фон прозрачный
- **AND** текст body TextPrimary #F6F8FA, 12sp
- **AND** текст time TextSecondary #B0BAC8, 10sp
- **AND** фон микрофона AccentPrimary #2EC4B6
- **AND** радиус карточки виджета 18dp (shapeMedium)
- **AND** цвета соответствуют обновлённой палитре NeoWave Voice

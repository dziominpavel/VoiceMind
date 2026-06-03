## ADDED Requirements

### Requirement: On-device STT
Приложение ДОЛЖНО использовать SpeechRecognizer с on-device движком, locale ru-RU.

#### Scenario: Голосовой ввод
- **WHEN** пользователь нажимает кнопку микрофона
- **THEN** запускается SpeechRecognizer
- **AND** результат возвращается как rawPhrase строка

### Requirement: Fallback на системный диалог
Если on-device STT недоступен или вернул ошибку, приложение ДОЛЖНО fallback на RecognizerIntent.

#### Scenario: On-device недоступен
- **WHEN** on-device SpeechRecognizer возвращает ERROR_RECOGNIZER_BUSY или ERROR_INSUFFICIENT_PERMISSIONS
- **THEN** открывается системный диалог распознавания речи
- **AND** результат возвращается в приложение

### Requirement: Timeout 10 секунд
Если пользователь молчит более 10 секунд, STT ДОЛЖЕН автоматически остановиться.

#### Scenario: Тишина
- **WHEN** пользователь активировал микрофон и молчит 10 секунд
- **THEN** SpeechInputController останавливает прослушивание
- **AND** показывается состояние Idle

### Requirement: Не хранить аудио в MVP
Приложение НЕ ДОЛЖНО записывать аудиозаписи на диск в MVP.

#### Scenario: Создание напоминания
- **WHEN** пользователь создаёт голосовое напоминание
- **THEN** сохраняется только rawPhrase текстом
- **AND** аудиофайл не создаётся

### Requirement: Обработка ошибок STT
Приложение ДОЛЖНО обрабатывать ошибки STT без крашей.

#### Scenario: Нет сети
- **WHEN** on-device движок требует сети и она недоступна
- **THEN** показывается Snackbar с предложением fallback или ручного ввода
- **AND** приложение не падает

#### Scenario: Не распознано
- **WHEN** STT вернул пустой результат
- **THEN** показывается Snackbar "Не распознано, попробуйте ещё раз"
- **AND** состояние возвращается в Idle

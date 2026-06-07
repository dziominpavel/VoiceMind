# Язык openspec (Devin)

> Зеркало `.cursor/rules/openspec-language.mdc` — держать в sync при изменениях.

## Текст — русский

`proposal.md`, `design.md`, `tasks.md`, `specs/**/*.md` — **русский**.
Код в snippets — **английский**.

## Normative keywords — только английский

`openspec validate` **не принимает** «ДОЛЖЕН», «ДОЛЖНА», «ДОЛЖНО», «НЕ ДОЛЖНО».

В каждом `### Requirement:` используй **MUST**, **SHALL** или **MUST NOT**:

```
✅ Система MUST включать экран при deliveryMode ALARM.
❌ Система ДОЛЖНА включать экран...
```

Сценарии: `#### Scenario:` + `**WHEN**` / `**THEN**` / `**AND**`.

## Структура

| Файл | Заголовки |
|------|-----------|
| `openspec/specs/<capability>/spec.md` | `## Purpose` + `## Requirements` |
| `openspec/changes/<change>/specs/.../spec.md` | `## ADDED/MODIFIED/REMOVED Requirements` |

## Проверка

```bash
openspec validate --all
```

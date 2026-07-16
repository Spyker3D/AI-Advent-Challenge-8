# Local MCP Server

## Git branch tool (Day 31)

`get_current_git_branch` без аргументов выполняет только `git branch --show-current` в каталоге `MCP_PROJECT_ROOT` (по умолчанию — корень репозитория):

```powershell
$env:MCP_PROJECT_ROOT = "C:\Projects\AI-Advent-Challenge-8"
node server.js
```

Произвольные shell-команды tool не принимает.

Локальный MCP-сервер для задания Day 17.

## Что реализовано

Tool:

```text
get_task_status

Описание:

Returns status and short details for a task by taskId

Input schema:

{
  "type": "object",
  "properties": {
    "taskId": {
      "type": "string",
      "description": "Task identifier, for example AI-17"
    }
  },
  "required": ["taskId"]
}
Запуск
npm start

Сервер будет доступен:

http://localhost:3000/mcp

Для Android Emulator использовать:

http://10.0.2.2:3000/mcp
Пример tools/list
{
  "jsonrpc": "2.0",
  "id": 1,
  "method": "tools/list",
  "params": {}
}
Пример tools/call
{
  "jsonrpc": "2.0",
  "id": 2,
  "method": "tools/call",
  "params": {
    "name": "get_task_status",
    "arguments": {
      "taskId": "AI-17"
    }
  }
}

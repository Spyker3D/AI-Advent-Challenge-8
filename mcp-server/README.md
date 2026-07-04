# Local MCP Server

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
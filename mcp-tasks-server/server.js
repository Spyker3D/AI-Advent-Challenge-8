const fs = require("fs");
const http = require("http");
const path = require("path");

const PORT = 3002;
const SERVER_ID = "tasks";
const DATA_DIR = path.join(__dirname, "data");
const TASKS_FILE = path.join(DATA_DIR, "tasks.json");

function ensureStorage() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  if (!fs.existsSync(TASKS_FILE)) {
    fs.writeFileSync(TASKS_FILE, "[]", "utf8");
  }
}

function readTasks() {
  ensureStorage();
  try {
    const parsed = JSON.parse(fs.readFileSync(TASKS_FILE, "utf8"));
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Failed to read tasks:", error);
    return [];
  }
}

function writeTasks(tasks) {
  ensureStorage();
  fs.writeFileSync(TASKS_FILE, JSON.stringify(tasks, null, 2), "utf8");
}

function normalizeLimit(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) return 10;
  return Math.min(Math.floor(parsed), 100);
}

function createJsonRpcResponse(id, result) {
  return { jsonrpc: "2.0", id, result };
}

function createJsonRpcError(id, code, message) {
  return { jsonrpc: "2.0", id, error: { code, message } };
}

function textResult(text) {
  return { content: [{ type: "text", text }] };
}

function safeLogArguments(args) {
  const copy = { ...args };
  if (typeof copy.description === "string") {
    copy.descriptionLength = copy.description.length;
    delete copy.description;
  }
  return JSON.stringify(copy);
}

function createTask(title, description, due) {
  const tasks = readTasks();
  const task = {
    id: `task-${Date.now()}`,
    title: String(title || "").trim(),
    description: String(description || ""),
    due: due ? String(due) : "",
    status: "todo",
    createdAt: new Date().toISOString(),
  };
  tasks.push(task);
  writeTasks(tasks);
  return {
    message: "Task created successfully.",
    id: task.id,
    title: task.title,
    status: task.status,
    due: task.due,
  };
}

function listTasks(limit) {
  return readTasks().slice(-normalizeLimit(limit));
}

function handleToolsList(id) {
  return createJsonRpcResponse(id, {
    tools: [
      {
        name: "create_task",
        description: "Creates a todo task.",
        inputSchema: {
          type: "object",
          properties: {
            title: { type: "string", description: "Task title" },
            description: { type: "string", description: "Task description" },
            due: { type: "string", description: "Due date or human-readable due text" },
          },
          required: ["title"],
        },
      },
      {
        name: "list_tasks",
        description: "Returns latest tasks.",
        inputSchema: {
          type: "object",
          properties: {
            limit: {
              type: "number",
              description: "How many latest tasks to return. Default is 10.",
            },
          },
        },
      },
    ],
  });
}

function handleToolsCall(id, params) {
  const toolName = params && params.name;
  const args = params && params.arguments ? params.arguments : {};
  console.log(`[MCP tools/call] server=${SERVER_ID} start tool=${toolName} arguments=${safeLogArguments(args)}`);

  try {
    if (toolName === "create_task") {
      if (!args.title) {
        return createJsonRpcError(id, -32602, "Missing required parameter: title");
      }
      const result = createTask(args.title, args.description, args.due);
      console.log(`[MCP tools/call] server=${SERVER_ID} success tool=${toolName}`);
      return createJsonRpcResponse(id, textResult(JSON.stringify(result, null, 2)));
    }

    if (toolName === "list_tasks") {
      const result = listTasks(args.limit);
      console.log(`[MCP tools/call] server=${SERVER_ID} success tool=${toolName}`);
      return createJsonRpcResponse(id, textResult(JSON.stringify(result, null, 2)));
    }

    return createJsonRpcError(id, -32601, `Unknown tool: ${toolName}`);
  } catch (error) {
    console.error(`[MCP tools/call] server=${SERVER_ID} error tool=${toolName}:`, error);
    return createJsonRpcError(id, -32000, error.message);
  }
}

async function handleMcpRequest(body) {
  let request;
  try {
    request = JSON.parse(body);
  } catch (error) {
    return createJsonRpcError(null, -32700, "Parse error");
  }

  const { id, method, params } = request;
  if (method === "tools/list") return handleToolsList(id);
  if (method === "tools/call") return handleToolsCall(id, params);
  return createJsonRpcError(id, -32601, `Method not found: ${method}`);
}

const server = http.createServer((req, res) => {
  if (req.method !== "POST" || req.url !== "/mcp") {
    res.writeHead(404, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ error: "Use POST /mcp" }));
    return;
  }

  let body = "";
  req.on("data", (chunk) => {
    body += chunk;
  });
  req.on("end", async () => {
    const response = await handleMcpRequest(body);
    res.writeHead(200, {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    });
    res.end(JSON.stringify(response));
  });
});

ensureStorage();
server.listen(PORT, () => {
  console.log(`Tasks MCP server is running on http://localhost:${PORT}/mcp`);
});

const fs = require("fs");
const http = require("http");
const path = require("path");

const PORT = 3001;
const SERVER_ID = "notes";
const DATA_DIR = path.join(__dirname, "data");
const NOTES_FILE = path.join(DATA_DIR, "notes.json");

function ensureStorage() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  if (!fs.existsSync(NOTES_FILE)) {
    fs.writeFileSync(NOTES_FILE, "[]", "utf8");
  }
}

function readNotes() {
  ensureStorage();
  try {
    const parsed = JSON.parse(fs.readFileSync(NOTES_FILE, "utf8"));
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Failed to read notes:", error);
    return [];
  }
}

function writeNotes(notes) {
  ensureStorage();
  fs.writeFileSync(NOTES_FILE, JSON.stringify(notes, null, 2), "utf8");
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
  if (typeof copy.content === "string") {
    copy.contentLength = copy.content.length;
    delete copy.content;
  }
  return JSON.stringify(copy);
}

function saveNote(title, content) {
  const notes = readNotes();
  const createdAt = new Date().toISOString();
  const note = {
    id: `note-${Date.now()}`,
    title: String(title || "").trim(),
    content: String(content || ""),
    createdAt,
  };
  notes.push(note);
  writeNotes(notes);
  return {
    message: "Note saved successfully.",
    id: note.id,
    title: note.title,
    createdAt: note.createdAt,
  };
}

function listNotes(limit) {
  return readNotes().slice(-normalizeLimit(limit));
}

function handleToolsList(id) {
  return createJsonRpcResponse(id, {
    tools: [
      {
        name: "save_note",
        description: "Saves a note with title and content.",
        inputSchema: {
          type: "object",
          properties: {
            title: { type: "string", description: "Note title" },
            content: { type: "string", description: "Note content" },
          },
          required: ["title", "content"],
        },
      },
      {
        name: "list_notes",
        description: "Returns latest saved notes.",
        inputSchema: {
          type: "object",
          properties: {
            limit: {
              type: "number",
              description: "How many latest notes to return. Default is 10.",
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
    if (toolName === "save_note") {
      if (!args.title || !args.content) {
        return createJsonRpcError(id, -32602, "Missing required parameters: title, content");
      }
      const result = saveNote(args.title, args.content);
      console.log(`[MCP tools/call] server=${SERVER_ID} success tool=${toolName}`);
      return createJsonRpcResponse(id, textResult(JSON.stringify(result, null, 2)));
    }

    if (toolName === "list_notes") {
      const result = listNotes(args.limit);
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
  console.log(`Notes MCP server is running on http://localhost:${PORT}/mcp`);
});

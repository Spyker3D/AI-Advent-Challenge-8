const fs = require("fs");
const http = require("http");
const path = require("path");

const PORT = 3000;
const WEATHER_INTERVAL_MS = 10_000;
const WEATHER_API_URL =
  "https://api.open-meteo.com/v1/forecast?latitude=55.7558&longitude=37.6173&current=temperature_2m,wind_speed_10m,precipitation";
const DATA_DIR = path.join(__dirname, "data");
const WEATHER_HISTORY_FILE = path.join(DATA_DIR, "weather-history.json");

const tasks = {
  "AI-17": { status: "In Progress", title: "Implement first custom MCP tool" },
  "AI-18": { status: "In Progress", title: "Periodic MCP weather tool" },
  "BUG-1": { status: "Done", title: "Fix Android cleartext network config" },
};

function createJsonRpcResponse(id, result) {
  return { jsonrpc: "2.0", id, result };
}

function createJsonRpcError(id, code, message) {
  return { jsonrpc: "2.0", id, error: { code, message } };
}

function ensureDataStorage() {
  if (!fs.existsSync(DATA_DIR)) {
    fs.mkdirSync(DATA_DIR, { recursive: true });
  }
  if (!fs.existsSync(WEATHER_HISTORY_FILE)) {
    fs.writeFileSync(WEATHER_HISTORY_FILE, "[]", "utf8");
  }
}

function readWeatherHistory() {
  ensureDataStorage();
  try {
    const raw = fs.readFileSync(WEATHER_HISTORY_FILE, "utf8");
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch (error) {
    console.error("Failed to read weather history:", error);
    return [];
  }
}

function writeWeatherHistory(history) {
  ensureDataStorage();
  fs.writeFileSync(WEATHER_HISTORY_FILE, JSON.stringify(history, null, 2), "utf8");
}

function normalizeLimit(value) {
  const parsed = Number(value);
  if (!Number.isFinite(parsed) || parsed <= 0) {
    return 10;
  }
  return Math.min(Math.floor(parsed), 100);
}

async function collectWeatherNow() {
  const response = await fetch(WEATHER_API_URL);
  if (!response.ok) {
    throw new Error(`Open-Meteo HTTP ${response.status} ${response.statusText}`);
  }

  const payload = await response.json();
  const current = payload.current || {};
  const record = {
    timestamp: new Date().toISOString(),
    temperature: Number(current.temperature_2m),
    windSpeed: Number(current.wind_speed_10m),
    precipitation: Number(current.precipitation),
    source: "Open-Meteo",
  };

  if (
    !Number.isFinite(record.temperature) ||
    !Number.isFinite(record.windSpeed) ||
    !Number.isFinite(record.precipitation)
  ) {
    throw new Error(`Open-Meteo returned invalid weather data: ${JSON.stringify(current)}`);
  }

  const history = readWeatherHistory();
  history.push(record);
  writeWeatherHistory(history);

  console.log(
    `Weather collected: ${record.temperature}°C, wind=${record.windSpeed} km/h, precipitation=${record.precipitation} mm`
  );
  return record;
}

async function collectWeatherSafely() {
  try {
    await collectWeatherNow();
  } catch (error) {
    console.error("Weather scheduler error:", error);
  }
}

function getLatestWeatherRecords(limit) {
  return readWeatherHistory().slice(-normalizeLimit(limit));
}

function formatNumber(value) {
  return Number.isFinite(value) ? value.toFixed(1) : "n/a";
}

function buildWeatherSummary(limit) {
  const records = getLatestWeatherRecords(limit);
  if (records.length === 0) {
    return "Weather summary for Moscow is not available yet. No collected records.";
  }

  const temperatures = records.map((record) => Number(record.temperature));
  const windSpeeds = records.map((record) => Number(record.windSpeed));
  const precipitations = records.map((record) => Number(record.precipitation));
  const average = (values) => values.reduce((sum, value) => sum + value, 0) / values.length;
  const totalPrecipitation = precipitations.reduce((sum, value) => sum + value, 0);
  const lastRecord = records[records.length - 1];

  return [
    `Weather summary for Moscow based on ${records.length} records.`,
    `Average temperature: ${formatNumber(average(temperatures))}°C.`,
    `Min temperature: ${formatNumber(Math.min(...temperatures))}°C.`,
    `Max temperature: ${formatNumber(Math.max(...temperatures))}°C.`,
    `Average wind speed: ${formatNumber(average(windSpeeds))} km/h.`,
    `Total precipitation: ${formatNumber(totalPrecipitation)} mm.`,
    `Last update: ${lastRecord.timestamp}.`,
  ].join("\n");
}

function buildWeatherHistory(limit) {
  const records = getLatestWeatherRecords(limit);
  if (records.length === 0) {
    return "Weather history for Moscow is empty.";
  }

  return records
    .map(
      (record, index) =>
        `${index + 1}. ${record.timestamp}: temperature=${record.temperature}°C, wind=${record.windSpeed} km/h, precipitation=${record.precipitation} mm, source=${record.source}`
    )
    .join("\n");
}

function textResult(text) {
  return { content: [{ type: "text", text }] };
}

function handleToolsList(id) {
  return createJsonRpcResponse(id, {
    tools: [
      {
        name: "get_task_status",
        description: "Returns status and short details for a task by taskId",
        inputSchema: {
          type: "object",
          properties: {
            taskId: {
              type: "string",
              description: "Task identifier, for example AI-17",
            },
          },
          required: ["taskId"],
        },
      },
      {
        name: "get_weather_summary",
        description: "Returns aggregated weather summary collected by background scheduler.",
        inputSchema: {
          type: "object",
          properties: {
            limit: {
              type: "number",
              description: "How many latest records to aggregate. Default is 10.",
            },
          },
        },
      },
      {
        name: "get_weather_history",
        description: "Returns latest collected weather records.",
        inputSchema: {
          type: "object",
          properties: {
            limit: {
              type: "number",
              description: "How many latest records to return. Default is 10.",
            },
          },
        },
      },
      {
        name: "collect_weather_now",
        description: "Manually triggers weather collection and stores result in JSON.",
        inputSchema: {
          type: "object",
          properties: {},
        },
      },
    ],
  });
}

async function handleToolsCall(id, params) {
  const toolName = params && params.name;
  const args = params && params.arguments ? params.arguments : {};

  if (toolName === "get_task_status") {
    const taskId = args.taskId;
    if (!taskId) {
      return createJsonRpcError(id, -32602, "Missing required parameter: taskId");
    }
    const task = tasks[taskId];
    const text = task
      ? `Task ${taskId}: ${task.status}. ${task.title}.`
      : `Task ${taskId} was not found.`;
    return createJsonRpcResponse(id, textResult(text));
  }

  if (toolName === "get_weather_summary") {
    return createJsonRpcResponse(id, textResult(buildWeatherSummary(args.limit)));
  }

  if (toolName === "get_weather_history") {
    return createJsonRpcResponse(id, textResult(buildWeatherHistory(args.limit)));
  }

  if (toolName === "collect_weather_now") {
    try {
      const record = await collectWeatherNow();
      return createJsonRpcResponse(id, textResult(JSON.stringify(record, null, 2)));
    } catch (error) {
      console.error("Manual weather collection failed:", error);
      return createJsonRpcError(id, -32000, `Weather collection failed: ${error.message}`);
    }
  }

  return createJsonRpcError(id, -32601, `Unknown tool: ${toolName}`);
}

async function handleMcpRequest(body) {
  let request;
  try {
    request = JSON.parse(body);
  } catch (error) {
    return createJsonRpcError(null, -32700, "Parse error");
  }

  const { id, method, params } = request;
  if (method === "tools/list") {
    return handleToolsList(id);
  }
  if (method === "tools/call") {
    return handleToolsCall(id, params);
  }
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
    console.log("Incoming MCP request:");
    console.log(body);
    const response = await handleMcpRequest(body);
    console.log("MCP response:");
    console.log(JSON.stringify(response, null, 2));
    res.writeHead(200, {
      "Content-Type": "application/json",
      "Access-Control-Allow-Origin": "*",
    });
    res.end(JSON.stringify(response));
  });
});

ensureDataStorage();
collectWeatherSafely();
setInterval(collectWeatherSafely, WEATHER_INTERVAL_MS);

server.listen(PORT, () => {
  console.log(`MCP server is running on http://localhost:${PORT}/mcp`);
  console.log(`Weather scheduler interval: ${WEATHER_INTERVAL_MS / 1000} seconds`);
});

const fs = require("fs");
const http = require("http");
const path = require("path");
const { execFile } = require("child_process");
const { promisify } = require("util");

const PORT = Number(process.env.MCP_PORT || 3000);
const SERVER_ID = "weather";
const PUBLIC_BASE_URL = "http://31.129.110.10:3000";
const WEATHER_INTERVAL_MS = 60_000;
const CURRENT_FIELDS = "temperature_2m,wind_speed_10m,precipitation";
const DATA_DIR = path.join(__dirname, "data");
const REPORTS_DIR = path.join(DATA_DIR, "reports");
const WEATHER_HISTORY_FILE = path.join(DATA_DIR, "weather-history.json");
const PROJECT_ROOT = path.resolve(process.env.MCP_PROJECT_ROOT || path.join(__dirname, ".."));
const execFileAsync = promisify(execFile);

const tasks = {
  "AI-17": { status: "In Progress", title: "Implement first custom MCP tool" },
  "AI-18": { status: "In Progress", title: "Periodic MCP weather tool" },
  "BUG-1": { status: "Done", title: "Fix Android cleartext network config" },
};

const supportedCities = {
  moscow: { name: "Moscow", latitude: 55.7558, longitude: 37.6173 },
  "москва": { name: "Moscow", latitude: 55.7558, longitude: 37.6173 },
  "saint petersburg": { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  "st petersburg": { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  spb: { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  "санкт-петербург": { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  "санкт петербург": { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  "питер": { name: "Saint Petersburg", latitude: 59.9311, longitude: 30.3609 },
  kazan: { name: "Kazan", latitude: 55.7961, longitude: 49.1064 },
  "казань": { name: "Kazan", latitude: 55.7961, longitude: 49.1064 },
  sochi: { name: "Sochi", latitude: 43.5853, longitude: 39.7203 },
  "сочи": { name: "Sochi", latitude: 43.5853, longitude: 39.7203 },
};

function createJsonRpcResponse(id, result) {
  return { jsonrpc: "2.0", id, result };
}

function createJsonRpcError(id, code, message) {
  return { jsonrpc: "2.0", id, error: { code, message } };
}

function textResult(text) {
  return { content: [{ type: "text", text }] };
}

function validateGitRef(value, fieldName) {
  if (typeof value !== "string" || value.length === 0 || value.length > 200) {
    throw new Error(`${fieldName} must be a non-empty Git ref (max 200 characters).`);
  }
  if (value.startsWith("-") || value.includes("..") || !/^[A-Za-z0-9._/-]+$/.test(value)) {
    throw new Error(`${fieldName} contains unsafe characters.`);
  }
  return value;
}

async function runGitDiff(projectRoot, baseRef, headRef, changedFilesOnly = false) {
  const base = validateGitRef(baseRef, "baseRef");
  const head = validateGitRef(headRef, "headRef");
  const args = changedFilesOnly
    ? ["diff", "--name-status", base, head]
    : ["diff", "--unified=3", "--no-ext-diff", base, head];
  const { stdout } = await execFileAsync("git", args, {
    cwd: projectRoot,
    windowsHide: true,
    timeout: 30_000,
    maxBuffer: 20 * 1024 * 1024,
  });
  return stdout;
}

function safeLogArguments(args) {
  const copy = { ...args };
  if (typeof copy.content === "string") {
    copy.contentLength = copy.content.length;
    delete copy.content;
  }
  if (typeof copy.weatherJson === "string") {
    copy.weatherJsonLength = copy.weatherJson.length;
    delete copy.weatherJson;
  }
  return JSON.stringify(copy);
}

function ensureDataStorage() {
  fs.mkdirSync(DATA_DIR, { recursive: true });
  fs.mkdirSync(REPORTS_DIR, { recursive: true });
  if (!fs.existsSync(WEATHER_HISTORY_FILE)) {
    fs.writeFileSync(WEATHER_HISTORY_FILE, "[]", "utf8");
  }
}

function readWeatherHistory() {
  ensureDataStorage();
  try {
    const parsed = JSON.parse(fs.readFileSync(WEATHER_HISTORY_FILE, "utf8"));
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
  if (!Number.isFinite(parsed) || parsed <= 0) return 10;
  return Math.min(Math.floor(parsed), 100);
}

function normalizeCity(city) {
  return String(city || "").trim().toLowerCase();
}

function resolveCity(city) {
  const normalized = normalizeCity(city);
  const direct = supportedCities[normalized];
  if (direct) return direct;
  if (normalized.includes("санкт-петербург") || normalized.includes("санкт петербург") || normalized.includes("питер")) {
    return supportedCities["saint petersburg"];
  }
  if (normalized.includes("москв")) return supportedCities.moscow;
  if (normalized.includes("казан")) return supportedCities.kazan;
  if (normalized.includes("соч")) return supportedCities.sochi;
  return undefined;
}

async function fetchWeatherForCity(cityName, latitude, longitude) {
  const url =
    `https://api.open-meteo.com/v1/forecast?latitude=${latitude}` +
    `&longitude=${longitude}&current=${CURRENT_FIELDS}`;
  const response = await fetch(url);
  if (!response.ok) {
    throw new Error(`Open-Meteo HTTP ${response.status} ${response.statusText}`);
  }

  const payload = await response.json();
  const current = payload.current || {};
  const record = {
    city: cityName,
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

  return record;
}

async function collectWeatherNow() {
  const city = resolveCity("Moscow");
  const record = await fetchWeatherForCity(city.name, city.latitude, city.longitude);
  const history = readWeatherHistory();
  history.push({
    timestamp: record.timestamp,
    temperature: record.temperature,
    windSpeed: record.windSpeed,
    precipitation: record.precipitation,
    source: record.source,
  });
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
  if (records.length === 0) return "Weather history for Moscow is empty.";

  return records
    .map(
      (record, index) =>
        `${index + 1}. ${record.timestamp}: temperature=${record.temperature}°C, wind=${record.windSpeed} km/h, precipitation=${record.precipitation} mm, source=${record.source}`
    )
    .join("\n");
}

function createWeatherReport(weatherJson) {
  let weather;
  try {
    weather = JSON.parse(weatherJson);
  } catch (error) {
    throw new Error("Invalid weatherJson. Expected JSON string from get_weather_by_city.");
  }

  return [
    "Weather Report",
    "",
    `City: ${weather.city}`,
    `Temperature: ${weather.temperature}°C`,
    `Wind speed: ${weather.windSpeed} km/h`,
    `Precipitation: ${weather.precipitation} mm`,
    `Source: ${weather.source}`,
    `Updated at: ${weather.timestamp}`,
    "",
    "Summary:",
    `Current weather in ${weather.city} is ${weather.temperature}°C with wind speed ${weather.windSpeed} km/h and precipitation ${weather.precipitation} mm.`,
  ].join("\n");
}

function sanitizeReportFileName(fileName) {
  const baseName = path.basename(String(fileName || "weather-report.txt"));
  const safe = baseName
    .replace(/[^a-zA-Z0-9._-]/g, "-")
    .replace(/-+/g, "-")
    .replace(/^\.+/, "")
    .replace(/^-+/, "");
  const fallback = safe || "weather-report.txt";
  return fallback.toLowerCase().endsWith(".txt") ? fallback : `${fallback}.txt`;
}

function saveReportToFile(fileName, content) {
  ensureDataStorage();
  const safeFileName = sanitizeReportFileName(fileName);
  const reportPath = path.join(REPORTS_DIR, safeFileName);
  fs.writeFileSync(reportPath, String(content || ""), "utf8");

  return {
    message: "Report saved successfully.",
    path: `data/reports/${safeFileName}`,
    url: `${PUBLIC_BASE_URL}/reports/${safeFileName}`,
  };
}

function handleReportsGet(req, res) {
  const encodedName = req.url.slice("/reports/".length);
  const decodedName = decodeURIComponent(encodedName);
  const safeFileName = sanitizeReportFileName(decodedName);

  if (safeFileName !== decodedName) {
    res.writeHead(400, { "Content-Type": "text/plain; charset=utf-8" });
    res.end("Invalid report file name");
    return;
  }

  const reportPath = path.join(REPORTS_DIR, safeFileName);
  if (!reportPath.startsWith(REPORTS_DIR) || !fs.existsSync(reportPath)) {
    res.writeHead(404, { "Content-Type": "text/plain; charset=utf-8" });
    res.end("Report not found");
    return;
  }

  res.writeHead(200, { "Content-Type": "text/plain; charset=utf-8" });
  res.end(fs.readFileSync(reportPath, "utf8"));
}

function handleToolsList(id) {
  return createJsonRpcResponse(id, {
    tools: [
      {
        name: "get_current_git_branch",
        description: "Returns the current Git branch of the configured local project repository.",
        inputSchema: { type: "object", properties: {}, additionalProperties: false },
      },
      {
        name: "get_changed_files",
        description: "Returns git name-status entries changed between two refs.",
        inputSchema: {
          type: "object",
          properties: { baseRef: { type: "string" }, headRef: { type: "string" } },
          required: ["baseRef", "headRef"],
          additionalProperties: false,
        },
      },
      {
        name: "get_git_diff",
        description: "Returns a unified Git diff between two refs.",
        inputSchema: {
          type: "object",
          properties: { baseRef: { type: "string" }, headRef: { type: "string" } },
          required: ["baseRef", "headRef"],
          additionalProperties: false,
        },
      },
      {
        name: "get_weather_by_city",
        description: "Returns current weather by city. Supported: Moscow, Saint Petersburg, Kazan, Sochi.",
        inputSchema: {
          type: "object",
          properties: {
            city: {
              type: "string",
              description: "City name. Supported: Moscow, Saint Petersburg, Kazan, Sochi.",
            },
          },
          required: ["city"],
        },
      },
      {
        name: "create_weather_report",
        description: "Creates a human-readable weather report from weather JSON.",
        inputSchema: {
          type: "object",
          properties: {
            weatherJson: {
              type: "string",
              description: "Weather JSON returned by get_weather_by_city",
            },
          },
          required: ["weatherJson"],
        },
      },
      {
        name: "save_report_to_file",
        description: "Saves report content to a text file and returns public URL.",
        inputSchema: {
          type: "object",
          properties: {
            fileName: { type: "string", description: "Report file name" },
            content: { type: "string", description: "Report content to save" },
          },
          required: ["fileName", "content"],
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
        inputSchema: { type: "object", properties: {} },
      },
    ],
  });
}

async function handleToolsCall(id, params) {
  const toolName = params && params.name;
  const args = params && params.arguments ? params.arguments : {};
  console.log(`[MCP tools/call] server=${SERVER_ID} start tool=${toolName} arguments=${safeLogArguments(args)}`);

  const success = (response) => {
    console.log(`[MCP tools/call] server=${SERVER_ID} success tool=${toolName}`);
    return response;
  };

  if (toolName === "get_current_git_branch") {
    try {
      const { stdout } = await execFileAsync("git", ["branch", "--show-current"], {
        cwd: PROJECT_ROOT,
        windowsHide: true,
        timeout: 5000,
      });
      const branch = stdout.trim();
      if (!branch) return createJsonRpcError(id, -32000, "Git repository is in detached HEAD state or has no current branch.");
      return success(createJsonRpcResponse(id, textResult(branch)));
    } catch (error) {
      return createJsonRpcError(id, -32000, `Cannot read Git branch for configured project: ${error.message}`);
    }
  }

  if (toolName === "get_changed_files" || toolName === "get_git_diff") {
    try {
      const output = await runGitDiff(PROJECT_ROOT, args.baseRef, args.headRef, toolName === "get_changed_files");
      return success(createJsonRpcResponse(id, textResult(output)));
    } catch (error) {
      const code = error.message && error.message.includes("unsafe") ? -32602 : -32000;
      return createJsonRpcError(id, code, `Cannot get Git diff: ${error.message}`);
    }
  }

  if (toolName === "get_task_status") {
    const taskId = args.taskId;
    if (!taskId) return createJsonRpcError(id, -32602, "Missing required parameter: taskId");
    const task = tasks[taskId];
    const text = task ? `Task ${taskId}: ${task.status}. ${task.title}.` : `Task ${taskId} was not found.`;
    return success(createJsonRpcResponse(id, textResult(text)));
  }

  if (toolName === "get_weather_by_city") {
    const city = resolveCity(args.city);
    if (!city) return createJsonRpcError(id, -32602, `Unsupported city: ${args.city}`);
    try {
      const weather = await fetchWeatherForCity(city.name, city.latitude, city.longitude);
      return success(createJsonRpcResponse(id, textResult(JSON.stringify(weather, null, 2))));
    } catch (error) {
      return createJsonRpcError(id, -32000, `Weather request failed: ${error.message}`);
    }
  }

  if (toolName === "create_weather_report") {
    try {
      return success(createJsonRpcResponse(id, textResult(createWeatherReport(args.weatherJson))));
    } catch (error) {
      return createJsonRpcError(id, -32602, error.message);
    }
  }

  if (toolName === "save_report_to_file") {
    if (!args.fileName || !args.content) {
      return createJsonRpcError(id, -32602, "Missing required parameters: fileName, content");
    }
    return success(createJsonRpcResponse(id, textResult(JSON.stringify(saveReportToFile(args.fileName, args.content), null, 2))));
  }

  if (toolName === "get_weather_summary") {
    return success(createJsonRpcResponse(id, textResult(buildWeatherSummary(args.limit))));
  }

  if (toolName === "get_weather_history") {
    return success(createJsonRpcResponse(id, textResult(buildWeatherHistory(args.limit))));
  }

  if (toolName === "collect_weather_now") {
    try {
      const record = await collectWeatherNow();
      return success(createJsonRpcResponse(id, textResult(JSON.stringify(record, null, 2))));
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
  if (method === "tools/list") return handleToolsList(id);
  if (method === "tools/call") return handleToolsCall(id, params);
  return createJsonRpcError(id, -32601, `Method not found: ${method}`);
}

const server = http.createServer((req, res) => {
  if (req.method === "GET" && req.url.startsWith("/reports/")) {
    handleReportsGet(req, res);
    return;
  }

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

if (require.main === module) {
  ensureDataStorage();
  const weatherEnabled = process.env.MCP_ENABLE_WEATHER === "true";
  if (weatherEnabled) {
    collectWeatherSafely();
    setInterval(collectWeatherSafely, WEATHER_INTERVAL_MS);
  }
  server.listen(PORT, () => {
    console.log(`MCP server is running on http://localhost:${PORT}/mcp`);
    console.log(`Project root: ${PROJECT_ROOT}`);
    console.log(`Weather collection: ${weatherEnabled ? "enabled" : "disabled"}`);
  });
}

module.exports = { handleMcpRequest, handleToolsCall, runGitDiff, validateGitRef };

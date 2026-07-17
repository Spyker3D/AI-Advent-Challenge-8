const assert = require("node:assert/strict");
const test = require("node:test");
const { handleMcpRequest, isWeatherDisabled } = require("../server");

test("MCP_DISABLE_WEATHER hides weather tools", async () => {
  const previous = process.env.MCP_DISABLE_WEATHER;
  process.env.MCP_DISABLE_WEATHER = "true";
  try {
    assert.equal(isWeatherDisabled(), true);
    const response = await handleMcpRequest(JSON.stringify({ jsonrpc: "2.0", id: 1, method: "tools/list" }));
    const names = response.result.tools.map((tool) => tool.name);
    assert.ok(names.includes("get_changed_files"));
    assert.ok(names.includes("get_git_diff"));
    assert.ok(!names.some((name) => name.includes("weather")));
  } finally {
    if (previous === undefined) delete process.env.MCP_DISABLE_WEATHER;
    else process.env.MCP_DISABLE_WEATHER = previous;
  }
});

test("disabled weather tool cannot be called", async () => {
  const previous = process.env.MCP_DISABLE_WEATHER;
  process.env.MCP_DISABLE_WEATHER = "true";
  try {
    const response = await handleMcpRequest(JSON.stringify({
      jsonrpc: "2.0", id: 2, method: "tools/call",
      params: { name: "collect_weather_now", arguments: {} },
    }));
    assert.equal(response.error.code, -32601);
  } finally {
    if (previous === undefined) delete process.env.MCP_DISABLE_WEATHER;
    else process.env.MCP_DISABLE_WEATHER = previous;
  }
});

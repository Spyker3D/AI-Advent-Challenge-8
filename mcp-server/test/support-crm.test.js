const test = require("node:test");
const assert = require("node:assert/strict");
const fs = require("node:fs"); const path = require("node:path");
const { handleToolsCall, readSupportArray } = require("../server");
async function call(name, args = {}) { const r = await handleToolsCall(1, { name, arguments: args }); return JSON.parse(r.result.content[0].text); }
test("gets ticket with related user", async () => { const r = await call("get_ticket", { ticketId: "ticket-101" }); assert.equal(r.ticket.diagnostics.lastErrorCode, "OPENAI_RATE_LIMIT"); assert.equal(r.supportUser.id, "support-user-001"); });
test("unknown ticket is structured", async () => { assert.equal((await call("get_ticket", { ticketId: "ticket-999" })).error.code, "TICKET_NOT_FOUND"); });
test("lists and filters tickets", async () => { const r = await call("list_tickets", { category: "history" }); assert.deepEqual(r.tickets.map(x => x.id), ["ticket-104"]); });
test("unknown user and invalid args", async () => { assert.equal((await call("get_support_user", { supportUserId: "missing" })).error.code, "USER_NOT_FOUND"); assert.equal((await call("get_ticket", {})).error.code, "INVALID_ARGUMENT"); });
test("missing and corrupt files are structured", () => {
  const missing = path.join(__dirname, "..", "data", "missing.json");
  assert.equal(readSupportArray(missing).error.code, "DATA_FILE_NOT_FOUND");
  const corrupt = path.join(__dirname, "..", "data", "corrupt-test.json");
  fs.writeFileSync(corrupt, "{"); try { assert.equal(readSupportArray(corrupt).error.code, "INVALID_DATA_FILE"); } finally { fs.unlinkSync(corrupt); }
});

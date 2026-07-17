const assert = require("node:assert/strict");
const test = require("node:test");
const { MARKER, findReviewComment, upsertReviewComment } = require("../../scripts/upsert-ai-review");

test("marker is detected", () => {
  assert.equal(findReviewComment([{ id: 7, body: `before\n${MARKER}\nafter` }]).id, 7);
  assert.equal(findReviewComment([{ id: 1, body: "ordinary comment" }]), undefined);
});

test("creates a comment when marker is absent", async () => {
  const calls = [];
  const result = await upsertReviewComment({
    create: async (body) => { calls.push(["create", body]); return { id: 9 }; },
    update: async () => { throw new Error("must not update"); },
  }, [], `${MARKER}\nreview`);
  assert.deepEqual(result, { action: "created", id: 9 });
  assert.equal(calls.length, 1);
});

test("updates existing marker comment without duplicate", async () => {
  const calls = [];
  const result = await upsertReviewComment({
    create: async () => { throw new Error("must not create"); },
    update: async (id, body) => calls.push([id, body]),
  }, [{ id: 42, body: MARKER }, { id: 43, body: "human" }], `${MARKER}\nnew review`);
  assert.deepEqual(result, { action: "updated", id: 42 });
  assert.deepEqual(calls, [[42, `${MARKER}\nnew review`]]);
});

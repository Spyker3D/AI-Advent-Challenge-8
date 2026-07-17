const assert = require("node:assert/strict");
const fs = require("node:fs");
const os = require("node:os");
const path = require("node:path");
const { execFileSync } = require("node:child_process");
const test = require("node:test");
const { runGitDiff, validateGitRef } = require("../server");

function git(root, ...args) {
  return execFileSync("git", args, { cwd: root, encoding: "utf8" }).trim();
}

function repository() {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "mcp-git-test-"));
  git(root, "init");
  git(root, "config", "user.email", "test@example.com");
  git(root, "config", "user.name", "Test User");
  fs.writeFileSync(path.join(root, "renamed.txt"), "before\n", "utf8");
  fs.writeFileSync(path.join(root, "deleted.txt"), "delete me\n", "utf8");
  git(root, "add", "."); git(root, "commit", "-m", "base");
  const base = git(root, "rev-parse", "HEAD");
  fs.renameSync(path.join(root, "renamed.txt"), path.join(root, "new-name.txt"));
  fs.rmSync(path.join(root, "deleted.txt"));
  fs.appendFileSync(path.join(root, "new-name.txt"), "after\n", "utf8");
  git(root, "add", "-A"); git(root, "commit", "-m", "head");
  return { root, base, head: git(root, "rev-parse", "HEAD") };
}

test("changed files reports rename and deletion", async (t) => {
  const repo = repository(); t.after(() => fs.rmSync(repo.root, { recursive: true, force: true }));
  const result = await runGitDiff(repo.root, repo.base, repo.head, true);
  assert.match(result, /R\d*\s+renamed\.txt\s+new-name\.txt/);
  assert.match(result, /D\s+deleted\.txt/);
});

test("unified diff contains changed content", async (t) => {
  const repo = repository(); t.after(() => fs.rmSync(repo.root, { recursive: true, force: true }));
  assert.match(await runGitDiff(repo.root, repo.base, repo.head), /\+after/);
});

test("invalid ref and shell injection are rejected", async () => {
  assert.throws(() => validateGitRef("HEAD;echo owned", "headRef"), /unsafe/);
  await assert.rejects(runGitDiff(process.cwd(), "HEAD", "missing-ref", true));
});

test("non-Git directory fails", async (t) => {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), "mcp-not-git-"));
  t.after(() => fs.rmSync(root, { recursive: true, force: true }));
  await assert.rejects(runGitDiff(root, "HEAD", "HEAD", true));
});

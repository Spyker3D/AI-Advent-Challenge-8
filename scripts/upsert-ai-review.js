const fs = require("node:fs");

const MARKER = "<!-- ai-developer-review -->";

function findReviewComment(comments) {
  return comments.find((comment) => typeof comment.body === "string" && comment.body.includes(MARKER));
}

async function upsertReviewComment(api, comments, body) {
  const existing = findReviewComment(comments);
  if (existing) {
    await api.update(existing.id, body);
    return { action: "updated", id: existing.id };
  }
  const created = await api.create(body);
  return { action: "created", id: created.id };
}

async function githubRequest(path, token, options = {}) {
  const response = await fetch(`https://api.github.com${path}`, {
    ...options,
    headers: {
      Accept: "application/vnd.github+json",
      Authorization: `Bearer ${token}`,
      "X-GitHub-Api-Version": "2022-11-28",
      "Content-Type": "application/json",
      "User-Agent": "ai-developer-review",
      ...options.headers,
    },
  });
  if (!response.ok) throw new Error(`GitHub API ${response.status}: ${await response.text()}`);
  return response.status === 204 ? {} : response.json();
}

async function listAllComments(owner, repo, pr, token) {
  const comments = [];
  for (let page = 1; ; page += 1) {
    const batch = await githubRequest(
      `/repos/${owner}/${repo}/issues/${pr}/comments?per_page=100&page=${page}`,
      token
    );
    comments.push(...batch);
    if (batch.length < 100) return comments;
  }
}

async function main(env = process.env) {
  const [owner, repo] = (env.GITHUB_REPOSITORY || "").split("/");
  const pr = env.PR_NUMBER;
  const token = env.GITHUB_TOKEN;
  const reviewFile = env.REVIEW_FILE || "build/ai-review.md";
  if (!owner || !repo || !pr || !token) throw new Error("GITHUB_REPOSITORY, PR_NUMBER and GITHUB_TOKEN are required.");
  const body = fs.readFileSync(reviewFile, "utf8");
  if (!body.includes(MARKER)) throw new Error(`Review file does not contain required marker: ${MARKER}`);
  const comments = await listAllComments(owner, repo, pr, token);
  const api = {
    update: (id, value) => githubRequest(`/repos/${owner}/${repo}/issues/comments/${id}`, token,
      { method: "PATCH", body: JSON.stringify({ body: value }) }),
    create: (value) => githubRequest(`/repos/${owner}/${repo}/issues/${pr}/comments`, token,
      { method: "POST", body: JSON.stringify({ body: value }) }),
  };
  const result = await upsertReviewComment(api, comments, body);
  console.log(`AI review comment ${result.action} (id=${result.id}).`);
}

if (require.main === module) main().catch((error) => { console.error(error.message); process.exitCode = 1; });

module.exports = { MARKER, findReviewComment, upsertReviewComment };

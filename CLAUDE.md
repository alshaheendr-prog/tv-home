# Notes for Claude

## Every pull request gets eight reviews

The owner wants every aspect of each PR reviewed by Claude review threads, each an expert in its field. The lanes, their briefs and the process are in [.github/review-lanes/README.md](.github/review-lanes/README.md).

When you open a PR on this repo:
- Ask the project coordinator to start the eight review threads (one per lane) for it. If that isn't possible, run the eight lanes yourself as separate subagents, one per brief.
- Every reviewer must read its knowledge file (`.github/review-lanes/knowledge/`) and research online before reviewing, as its brief says.
- Commit each reviewer's knowledge-file additions (past findings, lessons, sources, open questions) in the same PR, so every lane gets more expert with each review.
- Fix all Must fix and Should fix findings before telling the owner the PR is ready. Pass lane 8's on-TV checklist to the owner.

## Scope

The home screen is feature-complete. Work on visual and UI polish only, and don't pitch new features.

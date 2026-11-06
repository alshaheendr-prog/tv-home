# Notes for Claude

## Every pull request gets expert reviews

The owner wants every aspect of each PR reviewed by Claude review threads, each an expert in its field. There are fifteen lanes; the briefs, knowledge files and process are in [.github/review-lanes/README.md](.github/review-lanes/README.md).

When you open a PR on this repo:
- Pick the lanes with "Which lanes review a PR" in that README: always 1, 2, 3, 7 and 8, plus the owners of every area the PR changes; all fifteen only when the owner asks or the PR touches most areas; one quick check for paperwork-only PRs. List the chosen lanes in the PR description.
- Ask the project coordinator to start one review thread per chosen lane. If that isn't possible, run them yourself as separate subagents, one per brief.
- Every reviewer reads its core knowledge file (`.github/review-lanes/knowledge/`), searches its archive only for relevant history, and researches online whatever its knowledge doesn't already cover, as its brief says.
- Commit each reviewer's additions in the same PR: findings, sources and open questions to `knowledge/archive/`, lessons to the core file.
- Fix all Must fix and Should fix findings. Re-checks: only the lanes that raised them confirm the fixes. Then tell the owner the PR is ready, and pass lane 8's on-TV checklist to the owner.
- Quality watch: if a skipped lane's area breaks later, or a reviewer had to guess, tell the owner and put that lane back on (see the README).

## Scope

The home screen is feature-complete. Work on visual and UI polish only, and don't pitch new features.

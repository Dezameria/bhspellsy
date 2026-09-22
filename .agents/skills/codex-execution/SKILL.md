---
name: codex-execution
description: >-
  Execute a main-authorized OpenAI Codex CLI request safely when routed by
  codex-orchestration. Own observable prompt transport, bounded watchdogs,
  process-tree cleanup, output validation, and failure reporting; do not use
  without a reserved orchestration call.
---

# Codex Execution

## Skill Contract

**Purpose:** Execute one main-authorized Codex request through the sole safe process gateway.

**When to use:** Only when `codex-orchestration` supplies a prepared prompt, expected artifact, timeout, phase, and reserved invocation count.

**Inputs:**
- Prepared prompt
- Expected output/artifact contract
- Planning or review phase identifier
- Startup, stall, and total timeout constraints
- Main-authorized invocation reservation

**Outputs:**
- Validated Codex output and exit status; or
- An explicit failure result with process state and dependent phase identified

**May invoke Codex:** YES. This is the only orchestration Skill allowed to execute Codex CLI.

**May ask user:** NO.

**Failure behavior:** Diagnose and terminate the failed invocation when necessary, return the failure to the main orchestrator, and never fabricate the missing result.

## Authorization Boundary

Do not execute Codex unless the main orchestrator has decided the call is justified and reserved it against the global call limits. Do not decide planning/review eligibility or start retries independently.

## Observable PowerShell Invocation

All automated planning and review calls MUST use
[`scripts/invoke_codex.ps1`](scripts/invoke_codex.ps1). Do not invoke `codex exec`
directly from a background shell.

The runner sends the prompt through redirected stdin and closes it, invokes
`codex exec --json --output-last-message`, captures stderr separately, monitors
observable activity, and terminates only the invocation's process tree on failure.
This avoids treating a silent foreground shell as evidence of progress.

Write the prepared prompt to a temporary file outside the repository, then call:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .agents/skills/codex-execution/scripts/invoke_codex.ps1 -PromptPath <absolute-prompt-path> -WorkingDirectory <absolute-repository-path> -Phase planning
```

Use `-Phase review` for review. The runner defaults to a 45-second startup
deadline, 180-second inactivity deadline, and 900-second total deadline. A user
request to allow a long run may increase the total deadline, but MUST NOT disable
the startup or inactivity watchdogs. "No time limit" never means unobservable or
unbounded execution.

## Execution Protocol

For every authorized call:

1. Receive the prepared prompt and expected artifact contract.
2. Save the exact prompt to a temporary file; do not interpolate it into a shell command.
3. Run `scripts/invoke_codex.ps1` with explicit phase and repository paths.
4. Treat the runner's JSON result and exit code as the process result.
5. On success, read `LastMessagePath` and validate it against the expected artifact contract.
6. Preserve `TracePath` and `ErrorPath` until the phase is parsed or the failure is reported.
7. Return the validated output to the main orchestrator for routing to the requesting planning or review Skill.

Do not reinterpret or replace invalid output with assumptions.

## Failure Handling

A call fails when it:
- Produces no JSONL event before the startup deadline (`STARTUP_STALLED`)
- Produces no stdout/stderr growth before the inactivity deadline (`STALLED`)
- Times out
- Exits with a non-zero exit code
- Is canceled
- Returns incomplete or invalid output
- Fails to produce the expected artifact

On failure:

1. Do not silently continue.
2. Do not invent a Codex result.
3. Do not replace failed architecture or review with Antigravity assumptions.
4. Use the runner result to determine the invocation's actual process state.
5. Confirm `RootProcessAlive` is false and `RemainingDescendantProcessIds` is empty; if not, terminate only that verified invocation tree.
6. Return a concise failure result with trace/error artifact paths to the main orchestrator.

The main orchestrator decides whether independent work may continue and reports the failure to the user.

## Timeout and Hung Processes

Never infer progress from a process merely remaining alive. Progress means the
JSONL trace or stderr changed within the configured window.

- `STARTUP_STALLED`: no JSONL event appeared before the startup deadline.
- `STALLED`: at least one event appeared, then neither trace nor stderr changed before the inactivity deadline.
- `TIMED_OUT`: total deadline elapsed despite intermittent activity.

On any watchdog failure, let the runner terminate and verify the exact process
tree. Do not poll forever, extend a deadline after it expires, or spawn a duplicate
call merely because the current call is slow.

Do not retry automatically. A retry requires a fresh authorization from the main orchestrator under the global call policy.

## Result Shape

```text
Phase:
Status: SUCCESS | STARTUP_STALLED | STALLED | FAILED | TIMED_OUT | CANCELED | INVALID_OUTPUT
Exit Code:
Validated Output:
Process State:
Trace Path:
Error Path:
Last Event Type:
Failure Summary:
```

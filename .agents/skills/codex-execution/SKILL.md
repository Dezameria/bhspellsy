---
name: codex-execution
description: >-
  Execute a main-authorized OpenAI Codex CLI request safely when routed by
  codex-orchestration. Own prompt transport, stdin closure, output validation,
  timeout handling, and process failure reporting; do not use without a reserved
  orchestration call.
---

# Codex Execution

## Skill Contract

**Purpose:** Execute one main-authorized Codex request through the sole safe process gateway.

**When to use:** Only when `codex-orchestration` supplies a prepared prompt, expected artifact, timeout, phase, and reserved invocation count.

**Inputs:**
- Prepared prompt
- Expected output/artifact contract
- Planning or review phase identifier
- Timeout/process constraints
- Main-authorized invocation reservation

**Outputs:**
- Validated Codex output and exit status; or
- An explicit failure result with process state and dependent phase identified

**May invoke Codex:** YES. This is the only orchestration Skill allowed to execute Codex CLI.

**May ask user:** NO.

**Failure behavior:** Diagnose and terminate the failed invocation when necessary, return the failure to the main orchestrator, and never fabricate the missing result.

## Authorization Boundary

Do not execute Codex unless the main orchestrator has decided the call is justified and reserved it against the global call limits. Do not decide planning/review eligibility or start retries independently.

## Safe PowerShell Invocation

When invoking Codex CLI from an Antigravity background shell, never run `codex exec "..."` directly. An open stdin pipe can cause Codex to wait indefinitely for more input.

Construct non-trivial prompts separately with a PowerShell here-string or another safe mechanism. Avoid fragile command-line quoting and escaping.

```powershell
$prompt = @'
<Codex instructions here>
'@

$null | codex exec $prompt
```

All automated Codex invocations MUST use exactly:

```powershell
$null | codex exec $prompt
```

This explicitly closes stdin and is a verified invariant of the Antigravity environment.

## Execution Protocol

For every authorized call:

1. Receive the prepared prompt and expected artifact contract.
2. Invoke Codex with stdin explicitly closed.
3. Capture stdout, stderr, exit code, and completion state.
4. Validate the process exit code.
5. Validate that the expected non-empty output or artifact was produced.
6. Return the validated result to the main orchestrator for routing to the requesting planning or review Skill.

Do not reinterpret or replace invalid output with assumptions.

## Failure Handling

A call fails when it:
- Times out
- Exits with a non-zero exit code
- Is canceled
- Returns incomplete or invalid output
- Fails to produce the expected artifact

On failure:

1. Do not silently continue.
2. Do not invent a Codex result.
3. Do not replace failed architecture or review with Antigravity assumptions.
4. Determine the invocation's actual process state.
5. Terminate the failed invocation if necessary.
6. Return a concise failure result to the main orchestrator.

The main orchestrator decides whether independent work may continue and reports the failure to the user.

## Timeout and Hung Processes

Never allow a Codex process to wait indefinitely.

If it appears stuck:

1. Check whether it is waiting for stdin or still making progress.
2. Inspect the existing process before starting anything else.
3. Terminate the failed invocation if necessary.
4. Do not spawn a duplicate call merely because the current call is slow.

Do not retry automatically. A retry requires a fresh authorization from the main orchestrator under the global call policy.

## Result Shape

```text
Phase:
Status: SUCCESS | FAILED | TIMED_OUT | CANCELED | INVALID_OUTPUT
Exit Code:
Validated Output:
Process State:
Failure Summary:
```

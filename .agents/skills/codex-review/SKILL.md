---
name: codex-review
description: >-
  Prepare and parse a targeted diff-first Codex review when routed by
  codex-orchestration after its review-risk decision. Own review context,
  compact finding categories, and review-fix triage; never decide review
  eligibility or execute Codex directly.
---

# Codex Review

## Skill Contract

**Purpose:** Prepare and consume a concise, diff-first architecture and correctness review.

**When to use:** Only after the main orchestrator's risk assessment requires Codex review, or architecture-sensitive build evidence is routed for review.

**Inputs:**
- Original requirement or concise requirement summary
- Relevant architecture decisions and plan tasks
- Current git diff
- Relevant build/test results
- Specific affected files/components

**Outputs:**
- Prepared review prompt and expected schema; then
- Parsed BLOCKING, IMPORTANT, and OPTIONAL findings with fix disposition

**May invoke Codex:** NO. Return the prepared request to the main orchestrator, which authorizes `codex-execution`.

**May ask user:** NO. Return requirement conflicts or blocking completion issues to the main orchestrator.

**Failure behavior:** Block dependent completion when required review cannot be validated. Never fabricate approval or findings.

## Targeted Review Request

Codex MUST begin from the supplied diff. Provide only the requirement summary, relevant architecture decisions/tasks, diff, build/test evidence, and affected components.

Do not ask Codex to rediscover or rescan the repository. Additional inspection is allowed only to verify a specific concern, finding, or architectural relationship.

Focus review on:
- Architecture compliance
- Correctness
- Scope creep
- Duplicate systems
- Dependency boundaries
- Error handling and edge cases
- Performance risks
- Maintainability
- Missing verification

Avoid repeating discovery completed during planning.

## Compact Review Output

### BLOCKING

Issues that must be fixed before completion.

### IMPORTANT

Real issues that should be addressed but are not necessarily blocking.

### OPTIONAL

Non-required improvements that remain within scope.

Each finding must be concise:

```text
File/Component:
Issue:
Why it matters:
Required action:
```

If there are no meaningful findings, Codex should say so directly rather than generate filler.

Do not include OPTIONAL suggestions that significantly expand the original scope.

## Finding Triage and Fixes

Return BLOCKING and valid IMPORTANT findings for correction. Optional suggestions never authorize scope expansion and must not be implemented automatically.

When a finding conflicts with the original requirement, return the conflict to the main orchestrator instead of choosing silently.

Fixes may use the main orchestrator or appropriately owned workers. File/component ownership and shared-file integration remain governed by `implementation-orchestration`.

After fixes, return the changed scope and required verification to the main orchestrator for `build-verification`.

## Result Shape

```text
Status: FINDINGS | NO_MEANINGFUL_FINDINGS | INVALID_REVIEW
Blocking:
Important:
Optional:
Requirement Conflicts:
Fixes Applied:
Verification Required:
```

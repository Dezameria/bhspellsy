---
name: implementation-orchestration
description: >-
  Schedule and integrate approved implementation work when routed by
  codex-orchestration. Own useful-only parallelization, task dependencies,
  file/component ownership, worker briefs, and shared-file integration; do not
  perform architecture planning or Codex calls.
---

# Implementation Orchestration

## Skill Contract

**Purpose:** Convert an approved Fast Path or Codex plan into conflict-safe implementation work and integration.

**When to use:** When work needs decomposition, worker dispatch, file ownership, dependency scheduling, or shared integration coordination. Very small LOW tasks may remain with the main orchestrator.

**Inputs:**
- Locked requirement, clarifications, and assumptions
- Approved architecture or established Fast Path pattern
- Tasks, dependencies, constraints, and acceptance criteria

**Outputs:**
- Scheduled task set
- File/component ownership map
- Minimal worker briefs
- Integrated implementation status or unresolved dependency/conflict

**May invoke Codex:** NO.

**May ask user:** NO.

**Failure behavior:** Serialize conflicting work or return the unresolved dependency to the main orchestrator. Never allow workers to redesign architecture independently.

## Task Scheduling

Do not schedule work dependent on an unresolved blocking requirement gap.

Consume the compact plan directly. Do not rewrite it in full. Extract only:
- Architecture decisions relevant to implementation
- Tasks and dependencies
- Parallel-safe status
- Constraints and acceptance criteria
- File/component ownership needs

Respect hard dependencies. Tasks with contract dependencies may proceed once the required contract is established.

## Useful-Only Parallelization

Do not spawn workers merely because parallel execution is technically possible. Use workers only when:
- Tasks are meaningfully independent
- Each task has enough work to justify startup/context overhead
- Parallel execution materially reduces latency
- File ownership remains clean

Prefer the main Antigravity orchestrator for small tasks and small shared integration changes. Do not fragment one registry line, one localization entry, and one constant into separate workers.

## File and Component Ownership

Before parallel work, assign explicit file/component ownership for every task.

Two parallel workers MUST NOT modify the same file unless the main orchestrator explicitly coordinates them. When overlap is unavoidable, serialize the work or assign a single owner.

Shared integration files normally remain owned by the main orchestrator, including:
- Registries
- Build configuration
- Networking bootstrap
- Shared configuration
- Common interfaces
- Central initialization
- Shared integration hooks

Workers implement isolated components first. The main orchestrator performs small shared-file integration after workers complete.

## Worker Brief

Each worker receives only:
- Relevant original requirement, clarifications, and assumptions
- Relevant Codex plan or Fast Path task section
- Assigned files/components and explicit ownership boundary
- Dependencies and established contracts
- Constraints and acceptance criteria

Workers MUST:
- Follow the approved architecture
- Stay within assigned boundaries
- Reuse existing project architecture
- Avoid unrelated refactors

Workers MUST NOT expand scope, modify unassigned shared files, or redesign the architecture.

## Integration

After workers complete:

1. Verify outputs against their acceptance criteria.
2. Check for ownership violations and overlapping edits.
3. Integrate shared files centrally.
4. Preserve unrelated and pre-existing worktree changes.
5. Return the changed scope and verification needs to the main orchestrator.

## Result Shape

```text
Tasks:
Dependencies:
Ownership:
Workers Used:
Integration Changes:
Acceptance Status:
Unresolved Conflicts:
```

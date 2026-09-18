---
name: codex-orchestration
description: >-
  Coordinate Antigravity implementation work that uses OpenAI Codex CLI as a
  non-interactive architecture, planning, or review authority. Use for
  Codex-orchestrated development, Codex CLI execution, architecture planning,
  implementation routing, or risk-based Codex review; route detailed procedures
  to the specialized workspace Skills only when their phase is active.
---

# Codex Orchestration

This is the main orchestrator and router. It owns when and why phases run, global policy, state transitions, and completion. Specialized Skills own how each phase executes.

## Global Roles and Invariants

- **Antigravity:** Main orchestrator, implementation lead, integration coordinator, verification coordinator, failure coordinator, and the only agent that communicates with the user.
- **Codex:** Non-interactive architecture, planning, and review authority. Codex is not the primary implementation agent unless the user explicitly requests Codex implementation.
- **Antigravity workers:** Implementation agents that follow the approved architecture, assigned ownership, scope, constraints, and acceptance criteria. They must not redesign architecture independently.

Antigravity owns all phase transitions. Routing always returns control to this Skill:

```text
Main Orchestrator → Specialized Skill → Result → Main Orchestrator
```

Do not create hidden specialized-Skill chains. A specialized Skill never selects the next phase.

## Shared Orchestration State

Maintain a small conceptual state in agent context; do not introduce a runtime framework:

```text
requirement
resolvedClarifications
assumptions
requirementLocked
complexity
phase
repositoryContext
architectureDecisions
tasks
dependencies
acceptanceCriteria
fileOwnership
verificationResults
reviewRisk
reviewFindings
codexInvocationCounts
scope
```

Pass only the relevant fields to each routed Skill. Preserve results between phases so no Skill repeats discovery or rewrites the full plan.

## Single Policy Owners

Each policy has exactly one detailed owner:

| Policy | Owner |
|---|---|
| Pipeline, phase transitions, complexity, Fast Path, Codex eligibility, global call limits, review-risk decision, scope/Git safety, completion | `codex-orchestration` |
| Requirement blocking, defaults, locking, and Codex-discovered gap handling | [`requirement-analysis`](../requirement-analysis/SKILL.md) |
| Prompt transport, safe Codex process invocation, output validation, timeout/hung handling, and process failure diagnosis | [`codex-execution`](../codex-execution/SKILL.md) |
| Targeted discovery, context budget, planning prompt/schema, and plan parsing | [`codex-planning`](../codex-planning/SKILL.md) |
| Task scheduling, useful parallelism, file/component ownership, worker briefs, and integration | [`implementation-orchestration`](../implementation-orchestration/SKILL.md) |
| Build/test/runtime evidence, routine build fixes, architecture-sensitive escalation, and final build | [`build-verification`](../build-verification/SKILL.md) |
| Diff-first review request/schema, finding triage, and review fixes | [`codex-review`](../codex-review/SKILL.md) |

Reference an owner's policy; do not reproduce competing versions elsewhere.

## Routing Table

Read a specialized Skill fully only when its activation condition is met:

| Condition | Route |
|---|---|
| Initial requirement or a planning-discovered gap | `requirement-analysis` |
| MEDIUM/HIGH task, uncertain LOW task, or allowed architecture-sensitive reassessment | `codex-planning` |
| Main-authorized prepared Codex request with a reserved call | `codex-execution` |
| Task decomposition, workers, ownership, or shared integration is needed | `implementation-orchestration` |
| Implementation verification or final build is due | `build-verification` |
| Review-risk assessment requires Codex review | `codex-review` |

Very small LOW implementation may remain with the main Antigravity agent. It still routes through requirement analysis and build verification, but does not load planning, Codex execution, implementation orchestration, or review unless their activation condition arises.

## Pipeline

```text
User Requirement
        ↓
Requirement Analysis / Lock
        ↓
Complexity Classification
        ↓
   ┌────┴───────────────┐
   │                   │
  LOW              MEDIUM / HIGH
   │                   │
Fast Path       Targeted Codex Planning
   │                   │
   └─────────┬─────────┘
               ↓
Implementation / Useful Parallelization
               ↓
          Build / Test
               ↓
       Review-Risk Decision
          /             \
       Skip       Targeted Codex Review
          \             /
               ↓
      Review Fixes if Required
               ↓
           Final Build
               ↓
       Completion Report
```

### 1. Capture and Lock Requirements

Capture the original requirement without expanding scope. Route it to `requirement-analysis` before complexity classification.

If clarification is required, ask the user the returned minimal question. Pause only dependent work. After an answer, update state and route the answer back for requirement locking.

Codex never communicates with the user.

### 2. Classify Complexity

Classification is an orchestration decision, not a user-facing score.

**LOW** examples:
- Explicit parameter, localization, or resource-reference changes
- Small isolated rendering or mechanical fixes
- Straightforward work following an established pattern
- Minor work inside one existing component

LOW normally uses Fast Path. Skip Codex planning unless architectural uncertainty remains.

**MEDIUM** examples:
- A feature involving several related files or integration points
- A new component using established architecture
- Work benefiting from task decomposition or meaningful parallelism

MEDIUM normally uses targeted Codex planning.

**HIGH** examples:
- New or cross-module architecture
- Networking or client/server boundaries
- Concurrency, persistence, or complex lifecycle behavior
- Dependency/compatibility integration or public API changes
- Security-sensitive behavior or significant architectural uncertainty

HIGH uses targeted Codex planning and normally requires Codex review.

### 3. Select Fast Path or Planning

Fast Path is allowed only after requirements are sufficiently locked and an existing pattern is clear:

```text
Targeted inspection → established pattern?
  YES → implement directly
  NO  → targeted Codex planning
```

Fast Path never bypasses project rules, scope control, dependency/client-server boundaries, file ownership, build/test verification, or required manual verification.

When used, the completion report says:

```text
Codex Planning: Skipped — low-risk established pattern
```

For planning, route first to `codex-planning`, reserve an allowed planning invocation, route the prepared request to `codex-execution`, then return validated output to `codex-planning` for parsing. Route meaningful requirement gaps through `requirement-analysis`.

### 4. Plan and Implement

Use plan results directly without a long-form rewrite. For small direct work, the main Antigravity agent implements. When decomposition, workers, ownership, or shared integration is material, route to `implementation-orchestration`.

Do not start work dependent on an unresolved blocking requirement. Parallelize only when the latency benefit exceeds worker startup and integration overhead.

### 5. Verify

Route the changed scope and required checks to `build-verification`. Do not proceed as successful without required build/test evidence.

Routine in-plan compilation and integration fixes stay with Antigravity. Architecture-sensitive failures return here for an escalation decision; they do not silently rewrite the architecture.

### 6. Decide Review Risk

After successful implementation verification, decide whether Codex review is justified.

Codex review SHOULD occur for:
- New architecture or shared systems
- Networking, client/server boundaries, concurrency, or persistence
- Complex lifecycle behavior
- Dependency or optional compatibility integration
- Public API or security-sensitive changes
- Significant cross-module changes
- Deviations from the approved plan
- Build/test evidence showing unresolved architectural uncertainty

Codex review MAY be skipped when the implementation is isolated, follows the approved plan, changes no architecture-sensitive boundary, passes build/tests, and has verifiable acceptance criteria.

- LOW-risk localized work normally skips review.
- MEDIUM uses actual architecture impact to decide.
- HIGH normally requires review.

When skipped, the completion report says:

```text
Codex Review: Skipped — low-risk implementation
```

When required, route to `codex-review`, reserve a review invocation, route its prepared diff-first request to `codex-execution`, then return validated output to `codex-review` for parsing and fix triage.

### 7. Fix, Final Build, and Complete

Apply valid review findings through the main agent or ownership-safe workers. Do not implement optional scope-expanding suggestions. Route requirement conflicts back to the user through the main orchestrator.

After review fixes, route to `build-verification` for the final build. Do not report success unless required final verification succeeds.

## Global Codex Usage Policy

Codex should be invoked only when its architecture, reasoning, or review value justifies the usage. Prefer one high-quality targeted planning call.

Do NOT invoke Codex for:
- Straightforward compilation errors, imports, formatting, or naming
- Minor resource or localization changes
- Mechanical fixes inside approved architecture
- Questions answerable from already inspected files

Do not call Codex repeatedly for the same question unless new information creates genuine architectural uncertainty. A normal compilation failure never justifies another planning call.

All Codex execution must route through `codex-execution`; no other Skill may execute Codex directly.

## Global Loop and Call Limits

Default maximum:
- 0 or 1 targeted Codex planning invocation
- 1 implementation phase
- 0 or 1 Codex review
- 1 review-fix phase when applicable
- 1 final build

Planning may be skipped through Fast Path. Review may be skipped through risk assessment.

A second planning invocation is allowed only when a user's clarification materially changes an architecture-sensitive decision that Codex must reassess. A behavioral parameter or option already covered by the plan does not qualify.

A second Codex review is allowed only when:
- The first review found architecture-critical issues
- Fixes materially changed architecture-sensitive code
- The user explicitly requests another review

Invocation counts are global across all specialized Skills. The main orchestrator authorizes and tracks every started call, including failures or authorized retries.

## Failure Coordination

`codex-execution` owns process diagnosis and returns explicit failure state. On Codex failure:

- Stop the dependent phase immediately.
- Report the failure to the user.
- Do not invent or replace the missing Codex result.
- Continue only work that does not depend on the failed result.

No Skill may independently retry, bypass global limits, or start a duplicate process.

## Progress and Completion

For significant tasks, provide short operational updates such as `[Discovery]`, `[Codex]`, `[Implementation]`, `[Build]`, or `[Review]`. Progress reporting must not delay work.

The concise completion report includes:
- Material requirement assumptions or clarifications
- Complexity classification
- Codex planning used or skipped
- What was implemented and important files/components changed
- Workers used, if any
- Build/test result
- Codex review used or skipped
- Blocking/important findings addressed
- Remaining material risks
- Manual verification required

Do not reproduce the entire Codex plan.

Do not automatically commit, push, merge, or create a pull request unless the user explicitly requests it.

## Adding a New Orchestration Skill

Choose a cohesive procedure with a distinct activation boundary; do not extract trivial rules.

Every new Skill contract declares:

1. Purpose
2. When to use / activation condition
3. Required inputs
4. Produced outputs
5. Whether it may invoke Codex
6. Whether it may block progression
7. Whether it may ask the user
8. Failure behavior and return phase

Global policy remains here or with its existing single owner. Pass relevant shared-state fields through context, return control directly to this main Skill, and do not chain specialized Skills.

New Skills normally may not execute Codex. If they need Codex reasoning, they prepare a request; the main orchestrator decides eligibility and reserves a global call; `codex-execution` performs it.

Add one narrow routing condition and description-based activation. Do not load the new Skill for unrelated workflows.

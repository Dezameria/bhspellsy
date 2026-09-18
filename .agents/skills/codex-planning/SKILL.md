---
name: codex-planning
description: >-
  Prepare and parse targeted Codex architecture planning when routed by
  codex-orchestration for MEDIUM/HIGH work or an architecture-uncertain LOW
  task. Own targeted discovery, the compact planning schema, and planning gap
  output; never execute Codex directly.
---

# Codex Planning

## Skill Contract

**Purpose:** Prepare and consume concise, targeted Codex architecture planning.

**When to use:** For MEDIUM/HIGH tasks, architecture-uncertain LOW tasks, or the narrowly authorized architecture-sensitive reassessment after clarification.

**Inputs:**
- Locked user requirement
- Resolved clarifications and assumptions
- Relevant project rules and constraints
- Targeted repository context
- Known dependency boundaries

**Outputs:**
- Prepared planning prompt and expected schema; then
- Parsed architecture decisions, tasks, dependencies, acceptance criteria, risks, verification requirements, and meaningful requirement gaps

**May invoke Codex:** NO. Return the prepared request to the main orchestrator, which authorizes `codex-execution`.

**May ask user:** NO. Return blocking requirement gaps to the main orchestrator through `requirement-analysis`.

**Failure behavior:** Return a blocking planning failure or unresolved gap without inventing architecture.

## Targeted Repository Discovery

Follow this principle:

```text
Antigravity performs repository discovery.
Codex performs high-value reasoning.
```

Antigravity performs targeted discovery before preparing the prompt. Begin with approximately 5–12 highly relevant files or equivalent context as a soft initial budget.

Prioritize:

1. Project rules/instructions
2. Closest existing implementation
3. Relevant interfaces/base classes
4. Registration/integration points
5. Required utilities/managers/helpers
6. Dependency/compatibility boundary files

If fewer than 5 files are sufficient, do not inspect more artificially. If more than 12 are genuinely required, correctness takes priority.

Do not recursively inspect broad directories when targeted files are sufficient. Exclude unrelated packages, generated files, build outputs, caches, unrelated assets, and the dependency source tree unless a specific architectural uncertainty requires them.

## Planning Prompt

Provide Codex with:
- Original requirement and resolved clarifications
- Recorded assumptions
- Relevant repository findings and files/classes
- Existing implementation patterns to reuse
- Project constraints
- Known dependency and compatibility boundaries

Instruct Codex to analyze the supplied context, inspect additional files only for a specific unresolved question, identify reusable architecture and affected files, and produce the compact plan below.

Codex MUST NOT implement, perform an unrestricted repository scan, restate all supplied context, or produce a tutorial or architectural essay.

## Compact Planning Output

### REQUIREMENT_GAPS

Include only when a meaningful gap exists. For each gap:

```text
Question:
Why it matters:
Options:
Recommended default:
Blocking: YES/NO
```

Do not generate speculative questions, filler, or questions about details established by project architecture.

### ARCHITECTURE

Include only:
- Relevant existing patterns to reuse
- Important architecture decisions
- Important dependency/integration boundaries

### TASKS

For each task:

```text
Task ID:
Objective:
Files/Components:
Dependencies:
Parallel-Safe:
Constraints:
Acceptance:
```

Dependencies may be:
- **Hard Dependency:** The task cannot begin until its prerequisite finishes.
- **Contract Dependency:** The task may begin after an interface, identifier, or architecture contract is agreed.

Use the distinction only when useful.

### RISKS

Include only material correctness, architecture, integration, runtime, or verification risks. Omit generic development risks.

### VERIFY

Include only required build commands, automated checks, runtime checks, and necessary manual verification.

## Compact Response Rules

Codex SHOULD NOT:
- Restate the full requirement or supplied context
- Explain obvious code behavior or unchanged architecture
- Produce redundant file lists outside task definitions
- Repeat dependency information in multiple sections
- Add generic best-practice commentary or unrelated suggestions

The schema requires information, not verbosity.

## Parsing and Return

Consume the structured result directly. Extract only architecture decisions, task scheduling, dependencies, parallel-safe status, constraints, acceptance criteria, material risks, verification, and requirement gaps.

Do not rewrite the entire plan. Return relevant task sections directly to the main orchestrator for later worker briefs.

Resolved architecture decisions become authoritative. Any task dependent on a blocking requirement gap remains unapproved. Preserve valid decisions when a gap returns for clarification.

## Additional Inspection

Codex may inspect an additional file only when necessary to resolve a specific architectural uncertainty. Keep inspection narrowly scoped and do not repeat discovery already supplied by Antigravity.

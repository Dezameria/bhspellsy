---
name: codex-orchestration
description: >-
  Orchestrate and invoke OpenAI Codex CLI as an external architecture, review,
  and analysis authority from Antigravity. Use whenever executing Codex commands,
  performing architectural reviews with Codex, or delegating tasks to Codex CLI.
---

# Codex Orchestration Skill

This skill governs the execution and orchestration of OpenAI Codex CLI (`codex`) when invoked by Antigravity as an external architecture, review, or code analysis authority.

---

## Codex CLI Invocation

> [!IMPORTANT]
> When invoking Codex CLI from an Antigravity background shell, **NEVER** execute:
> ```powershell
> codex exec "..."
> ```
> directly.

Antigravity background processes may keep `stdin` open as a pipe, causing Codex CLI to wait indefinitely for additional input (`Reading additional input from stdin...`).

**Always close stdin explicitly:**
```powershell
$null | codex exec "..."
```

All automated Codex invocations **MUST** use this pattern.

### Expected Execution Flow

```text
Antigravity Orchestrator
        │
        │ Background Shell
        ▼
$null | codex exec "..."
        │
        │ stdin = EOF
        ▼
      Codex
        │
        ├─ Inspect
        ├─ Reason
        └─ Output
        │
        ▼
Process exits normally
        │
        ▼
Antigravity receives output
```

---

## Prompt Construction

Do **NOT** construct large multi-line Codex prompts directly inside the command-line invocation.

For non-trivial prompts:
- Construct the prompt separately
- Use a PowerShell here-string or another safe mechanism
- Avoid fragile quoting/escaping
- Then pass the resulting prompt to `codex exec`

### Example

```powershell
$prompt = @'
<Codex instructions here>
'@

$null | codex exec $prompt
```

---

## Codex Execution Protocol

For every Codex phase, adhere to the following sequence:

```text
Antigravity
    │
    ├─ Construct Codex prompt
    │
    ├─ Invoke Codex with stdin explicitly closed
    │
    │   $null | codex exec $prompt
    │
    ├─ Capture Codex output
    │
    ├─ Validate process exit code
    │
    ├─ Validate that expected output was actually produced
    │
    ├─ Parse/use the result
    │
    ▼
Continue orchestration
```

---

## Failure Handling

Codex is an architecture and review authority in this workflow.

If a Codex phase:
- Times out
- Exits with a non-zero exit code
- Is canceled
- Returns incomplete or invalid output
- Fails to produce the expected artifact

**Strict rules on failure:**
1. **DO NOT** silently continue.
2. **DO NOT** invent the missing Codex result.
3. **DO NOT** replace the failed architecture/review with Antigravity's own assumptions.

**Action:** Stop the dependent phase immediately and report the Codex failure to the user. Implementation may continue only for tasks that do not depend on the failed Codex result.

---

## Timeout / Hung Process Handling

A Codex process must not be allowed to wait indefinitely.

If Codex appears stuck:
1. Check whether the process is waiting for stdin.
2. Terminate the failed invocation if necessary.
3. Report the failure.
4. Do not repeatedly spawn duplicate Codex processes.

Never start another Codex invocation merely because the previous one appears slow without first determining the state of the existing process.

---

## Codex Usage Efficiency

Follow this principle:

```text
Antigravity performs repository discovery.
Codex performs high-value reasoning.
```

Codex MUST NOT perform unrestricted full-repository scans by default.

Before invoking Codex for feature planning, Antigravity should perform targeted repository discovery using its own repository and search capabilities. Antigravity should identify:
- Relevant project rules
- Existing implementations similar to the requested feature
- Relevant classes/files
- Registries and integration points
- Existing utilities/managers/helpers
- Dependency or compatibility boundaries
- Relevant documentation

Antigravity should then provide Codex with this targeted context.

Codex may inspect additional repository files only when necessary to resolve a specific architectural uncertainty.

Avoid asking Codex to inspect:
- unrelated packages
- generated files
- build outputs
- caches
- unrelated assets
- the entire dependency source tree

unless specifically required by the feature.

### Repository Discovery Budget

For normal feature planning, Antigravity SHOULD begin with approximately 5–12 highly relevant files or equivalent repository context. This is a soft initial budget, not a hard correctness limit.

Prioritize:

1. Project rules/instructions
2. Closest existing implementation
3. Relevant interfaces/base classes
4. Registration/integration points
5. Required utilities/managers/helpers
6. Dependency/compatibility boundary files

Do NOT recursively inspect broad directories when these files are already sufficient. Do NOT inspect unrelated packages merely to provide Codex with additional context.

Expand discovery only when an unresolved architectural question requires more context. If fewer than 5 files are sufficient, do not artificially inspect more. If more than 12 files are genuinely required, correctness takes priority over the budget.

### Avoid Redundant Codex Calls

Codex should be invoked only when its architecture, reasoning, or review value justifies the additional usage.

Do NOT invoke Codex for:
- Straightforward compilation errors
- Formatting
- Imports
- Simple naming corrections
- Minor resource changes
- Trivial localization changes
- Mechanical implementation fixes that remain inside the approved plan
- Questions Antigravity can answer directly from already inspected files

Antigravity should handle these directly.

Do not invoke Codex repeatedly for the same architectural question unless new information materially changes the problem.

Prefer one high-quality targeted Codex planning call over multiple small planning calls.

A second planning call MUST NOT occur merely because implementation encountered a normal compilation error. Escalate back to Codex only when new information introduces a genuine architectural uncertainty.

---

## Latency and Fast-Path Rules

The orchestration workflow must optimize for both:
- Codex usage efficiency
- Time from user request to useful implementation work

Avoid unnecessary repository discovery, Codex reasoning, long Codex outputs, repeated summarization, and unnecessary agent orchestration.

The goal is not to minimize context at the expense of correctness. Use the smallest sufficient amount of work and context.

### Task Complexity Classification

After the Requirement Clarification Gate is satisfied, Antigravity MUST classify the requested task before Codex planning. Task complexity classification is an orchestration decision, not a user-facing score.

#### LOW

Examples:
- Simple parameter changes
- Localization changes
- Resource references
- Small isolated rendering adjustments
- Mechanical fixes
- Straightforward implementation following an already established pattern
- Minor changes contained inside one existing component

LOW tasks normally use the Fast Path. Codex planning SHOULD be skipped unless Antigravity discovers architectural uncertainty.

#### MEDIUM

Examples:
- A feature involving several related files
- A new component using established project architecture
- A feature that benefits from task decomposition
- Changes involving several integration points
- Features where parallel implementation may be useful

MEDIUM tasks should normally use targeted Codex planning.

#### HIGH

Examples:
- New architecture
- Networking
- Client/server boundaries
- Concurrency
- Persistence
- Complex lifecycle behavior
- Dependency or compatibility integration
- Public API changes
- Cross-module architecture
- Security-sensitive behavior
- Significant architectural uncertainty

HIGH tasks should use targeted Codex planning and normally require Codex review after implementation.

### Fast Path

For LOW-complexity tasks, Antigravity MAY skip Codex planning.

Fast Path begins only after the Requirement Clarification Gate confirms that no blocking requirement gap remains.

```text
User Requirement
    ↓
Antigravity targeted inspection
    ↓
Existing pattern clearly identified?
    │
    ├─ YES → Implement directly → Build/Test → Verify → Complete
    │
    └─ NO  → Escalate to targeted Codex planning
```

Fast Path MUST NOT be used when architecture is uncertain.

Fast Path MUST NOT bypass:
- Build/test verification
- Existing project rules
- File ownership rules
- Scope control
- Dependency boundaries
- Client/server boundaries
- Required manual verification

When Fast Path is used, the Completion Report should state:

```text
Codex Planning: Skipped — low-risk established pattern
```

Briefly explain why.

### Process Codex Output Directly

After Codex returns, Antigravity SHOULD consume the structured result directly. Do NOT produce another long-form rewrite or summary of the complete Codex response unless the user explicitly asks for it.

Extract only what is needed for orchestration:
- Architecture decisions
- Task scheduling
- Dependencies
- Parallel-safe status
- File/component ownership
- Constraints
- Acceptance criteria
- Material risks
- Verification requirements

Prefer:

```text
Codex compact plan
    ↓
Antigravity parses tasks
    ↓
Relevant task section sent directly to worker
```

Avoid rewriting the complete plan between Codex and implementation workers.

### Progress Reporting

For orchestration tasks that take significant time, Antigravity should surface concise operational progress when supported by the environment, for example:

```text
[Discovery] Finding relevant existing implementation
[Codex] Running targeted architecture planning
[Implementation] Running 3 independent tasks
[Build] Running project verification
[Review] Codex review skipped — low risk
```

Do not produce long progress explanations. Progress reporting must not itself delay execution significantly.

---

## Agent Responsibilities

### Codex

Codex acts as:
- Repository analyst
- Architecture planner
- Implementation planner
- Architecture reviewer
- Final code reviewer

Codex is NOT the primary implementation agent unless the user explicitly requests Codex implementation.

Codex is non-interactive in this workflow. It reports requirement gaps and other findings to Antigravity and MUST NOT communicate with the user directly.

### Antigravity

Antigravity acts as:
- Main orchestrator
- Implementation lead
- Integration coordinator
- Build/test executor
- Failure coordinator

Antigravity is the only agent that communicates with the user, including for requirement clarification.

Antigravity may use sub-agents for independent implementation tasks.

### Antigravity Sub-agents

Sub-agents are implementation workers.

They MUST:
- Follow the approved architecture and implementation plan, whether produced by targeted Codex planning or derived by Antigravity through Fast Path
- Respect assigned task boundaries
- Reuse existing project architecture
- Avoid unrelated refactors

They MUST NOT redesign the approved architecture independently.

---

## Development Pipeline

When the user requests Codex-orchestrated implementation, execute the following phases:

### Pipeline Overview

```text
User Requirement
        ↓
Antigravity captures requirement
        ↓
Requirement Clarification Gate
        │
        ├─ Complete → Continue
        │
        ├─ Non-blocking gap → Established default / explicit assumption → Continue
        │
        └─ Blocking gap → Ask User
        ↓
Requirement Locked
        ↓
Antigravity Complexity Classification
        ↓
   ┌────┴───────────────┐
   │                   │
  LOW              MEDIUM/HIGH
   │                   │
Fast Path      Targeted Discovery
   │                   │
   │              Small Context
   │                   ↓
   │                 Codex
   │            Compact Planning
   │                   │
   └─────────┴─────────┘
               ↓
     Implementation Planning
               ↓
     Only useful parallelization
               ↓
     Implementation / Integration
               ↓
           Build/Test
               ↓
       Review-Risk Assessment
          /             \
        LOW            HIGH
         │               │
         │      Targeted Codex Review
         │         using diff first
         │               │
         └───────┬───────┘
                 ↓
            Final Build
                 ↓
          Completion Report
```

### Phase 1 — Requirements

Capture the user's original requirements.

Preserve them as the source requirements for all later phases.

Do not silently expand the requested scope.

### Requirement Clarification Gate

Immediately after capturing the original requirement, Antigravity MUST perform the initial requirement completeness check before complexity classification, Fast Path, implementation planning, or Codex architecture planning.

Determine whether implementation-critical requirements are missing:

- If no meaningful gap exists, continue immediately.
- For a non-blocking gap, use a clearly established project convention or default, record the assumption briefly, and continue without interrupting the user.
- For a blocking gap, ask the user the minimum necessary concise clarification, provide relevant options when useful, and do not begin dependent implementation until the user answers.

After blocking gaps are resolved, lock the requirement sufficiently for planning, then classify the task as LOW, MEDIUM, or HIGH and choose Fast Path or targeted Codex planning.

Antigravity MUST perform this first check. Do NOT invoke Codex merely to determine whether a simple requirement is complete. Keep the Gate lightweight for LOW tasks so Fast Path remains fast.

A requirement gap is blocking when different answers would materially change:
- User-visible behavior
- Architecture
- Public API
- Data model
- Client/server responsibility
- Networking
- Persistence
- Dependency integration
- Compatibility behavior
- Security behavior
- Core gameplay behavior
- Major lifecycle behavior
- Acceptance criteria

Examples of blocking questions include whether releasing a charge early fires or cancels, whether a projectile targets an entity or follows aim direction, whether data persists after restart, whether an integration is required or optional, and whether an action is client-side or server-authoritative.

Do NOT silently choose between materially different behaviors unless the repository already establishes an unambiguous convention.

Do not normally interrupt the user for details safely derived from:
- Existing project conventions
- Existing implementation patterns
- Explicit project rules
- Existing naming conventions
- Mechanical implementation details
- Internal class structure
- Imports
- File placement
- Registration mechanics
- Straightforward resource paths
- Formatting
- Build fixes

The goal is to ask only when the user's decision materially affects the intended feature, not to require the user to specify technical implementation details.

### Phase 2 — Targeted Codex Analysis and Planning

Skip this phase when the LOW-complexity Fast Path applies.

Before invoking Codex:

1. Antigravity performs targeted repository discovery using the soft initial context budget.
2. Antigravity identifies the smallest sufficient set of relevant files, existing implementations, project rules, and integration points.
3. Antigravity constructs the Codex planning prompt using:
   - Original requirements
   - Resolved clarifications and recorded assumptions
   - Relevant repository findings
   - Relevant files/classes
   - Existing implementation patterns
   - Project constraints
   - Known dependency boundaries

Then invoke Codex using the existing safe Codex invocation protocol.

Codex should:
1. Analyze the supplied targeted repository context.
2. Inspect additional files only when necessary.
3. Identify architecture that should be reused.
4. Identify files likely to be created or modified.
5. Produce the implementation plan.
6. Define task dependencies.
7. Identify parallel-safe tasks.
8. Define acceptance criteria.
9. Identify architectural risks.
10. Define required verification.

Codex MUST NOT implement during this phase.

Codex MUST NOT perform another unrestricted repository scan unless the targeted context is insufficient to make a safe architectural decision.

If additional repository inspection is required, Codex should keep the inspection narrowly scoped to the unresolved question.

Resolved architecture decisions in the plan become the architecture source of truth for implementation. Tasks dependent on a blocking requirement gap remain unapproved until that gap is clarified.

## Codex Compact Planning Output

Codex should return the following compact, implementation-oriented structure.

### REQUIREMENT_GAPS

For MEDIUM/HIGH tasks where targeted Codex planning is already justified, Codex may identify meaningful requirement gaps discovered during architecture analysis. For each gap, use:

```text
Question:
Why it matters:
Options:
Recommended default:
Blocking: YES/NO
```

Include this section only when a meaningful requirement gap exists. Do NOT generate speculative questions or filler, and do NOT ask about implementation details that existing project architecture can resolve. If no meaningful gap exists, omit this section entirely.

### ARCHITECTURE

Include only:
- Relevant existing patterns to reuse
- Important architecture decisions
- Important dependency/integration boundaries

Keep this concise.

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
- **Hard Dependency:** The dependent task cannot begin until the prerequisite is completed.
- **Contract Dependency:** The task only requires an agreed interface, class contract, identifier, or architecture decision and may execute in parallel once that contract is established.

Use the distinction only when useful.

### RISKS

Include only material risks that could affect:
- Correctness
- Architecture
- Integration
- Runtime behavior
- Build/test verification

Do not list generic software-development risks.

### VERIFY

Include only required verification:
- Build command
- Automated tests/checks
- Runtime checks
- Manual verification when necessary

### Compact Codex Response Rules

Codex planning responses MUST be concise and implementation-oriented.

Codex SHOULD NOT:
- Restate the full user requirements
- Repeat repository context already supplied by Antigravity
- Explain obvious code behavior
- Produce tutorials
- Produce long architectural essays
- Describe unchanged architecture
- Produce redundant file lists outside task definitions
- Repeat dependency information in multiple sections
- Produce generic best-practice commentary

The planning schema defines required information, not required verbosity. Prefer compact structured output over explanatory prose.

The output should contain enough information for Antigravity to safely schedule and implement tasks, but no more than necessary.

### Handling Codex-Discovered Requirement Gaps

If Codex reports `Blocking: NO`, Antigravity may use the recommended default when it is consistent with project conventions and the user's original requirement. Record the assumption briefly and do NOT invoke Codex again.

If Codex reports `Blocking: YES`, Antigravity must:

1. Stop only the implementation tasks dependent on that decision.
2. Ask the user the minimum necessary clarification, presenting concise options when useful.
3. Preserve all already-valid architecture decisions.
4. Resume after the user answers.

Do NOT automatically restart the entire planning process, repeat full repository discovery, or make a second Codex planning call.

A second Codex planning invocation is allowed only when the user's clarification materially changes an architecture-sensitive decision that Codex must reassess. If the clarification only fills a behavioral parameter or selects between options already covered by the existing plan, Antigravity updates the requirement and plan directly and continues implementation without invoking Codex again.

### Suggestions and Alternatives

Codex may suggest concise alternatives only when they represent meaningful design choices. Clearly distinguish required decisions from optional suggestions.

Do not generate unrelated optional ideas, expand project scope, or let optional suggestions block implementation. Antigravity must not automatically implement optional suggestions without user approval.

### Phase 3 — Implementation Planning

When Codex planning was used, Antigravity consumes the compact plan directly and extracts only the information required for orchestration. When Fast Path was used, Antigravity derives the minimal implementation tasks directly from the requirement and established pattern.

Do not schedule implementation tasks that depend on an unresolved blocking requirement gap.

Split the plan into implementation tasks.

Only parallelize tasks that Codex identified as independent or that are clearly non-conflicting.

Do not spawn sub-agents merely because parallel execution is possible. Consider whether parallelization provides meaningful latency benefit after accounting for startup, context transfer, waiting, and integration overhead.

Avoid assigning multiple sub-agents to modify the same central file simultaneously.

Examples of central/shared files include:
- registries
- build configuration
- networking bootstrap
- common managers
- shared interfaces

## Parallel Task Safety

Before spawning implementation sub-agents, Antigravity MUST assign
file/component ownership for each task.

Two parallel sub-agents MUST NOT modify the same file unless
explicitly coordinated by the orchestrator.

Shared integration files should normally remain owned by the
main Antigravity orchestrator.

Examples:
- registries
- build configuration
- networking bootstrap
- shared configuration
- common interfaces
- central initialization
- shared integration hooks

Sub-agents should implement isolated components first.

The main orchestrator performs small shared-file integration after
parallel tasks complete.

### Phase 4 — Parallel Implementation

Delegate safe independent tasks to Antigravity sub-agents only when:
- Tasks are meaningfully independent
- Each task contains enough work to justify agent startup overhead
- Parallel execution materially reduces implementation time
- File ownership can remain clean

For very small tasks, direct implementation may be faster. Prefer the main Antigravity agent for small integration tasks and avoid excessive agent fragmentation.

Do not split changes such as one registry line, one localization line, and one constant into separate agents.

Each sub-agent receives:
- Relevant original requirements, resolved clarifications, and assumptions
- Relevant Codex plan or Fast Path task section
- Assigned files/components
- Constraints
- Acceptance criteria

Sub-agents MUST NOT expand scope or redesign architecture.

Antigravity integrates the resulting changes.

### Phase 5 — Build and Verification

Run the project's appropriate build/test commands.

For Gradle projects, use the repository wrapper when available.

Example:
```bash
./gradlew build
```
or on Windows:
```powershell
.\gradlew.bat build
```

Antigravity may fix straightforward implementation and compilation errors that remain within the approved architecture.

If fixing an error requires changing the architecture, stop and escalate to Codex instead.

### Phase 6 — Review-Risk Assessment and Targeted Codex Review

After implementation and successful build/test verification, Antigravity performs a review-risk assessment.

For LOW-risk localized implementation, Antigravity verification and successful build/tests may complete the review path without Codex.

For MEDIUM-risk implementation, Antigravity decides whether Codex review is justified by the actual architecture impact.

For HIGH-risk implementation, Codex review should normally occur. Architecture-sensitive or high-risk areas include:
- New architecture or shared systems
- Networking
- Client/server boundaries
- Concurrency
- Persistence
- Complex lifecycle behavior
- Dependency integration
- Optional compatibility layers
- Public APIs
- Security-sensitive behavior
- Significant cross-module changes
- Changes that deviated from the approved Codex plan
- Build/test results that indicate unresolved architectural uncertainty

Codex review MAY be skipped for low-risk, localized changes when:
- The implementation follows the approved plan
- The change is isolated
- No architecture-sensitive boundary changed
- Build/tests succeed
- Antigravity can verify the acceptance criteria

When Codex review is skipped, the Completion Report MUST explicitly say:

```text
Codex Review: Skipped — low-risk implementation
```

Briefly explain the reason for skipping review.

When Codex review is required, invoke Codex using the safe invocation protocol and provide it primarily with:
- Original requirement or a concise requirement summary
- Relevant architecture decisions
- Relevant plan tasks
- Current git diff
- Relevant build/test results
- Specific files or components affected

Codex MUST begin review from the supplied diff.

Do NOT ask Codex to rediscover the repository. Codex may inspect additional repository files only when necessary to verify a specific concern, review finding, or architectural relationship.

Review should focus on:
- Architecture compliance
- Correctness
- Scope creep
- Duplicate systems
- Dependency boundaries
- Error handling
- Edge cases
- Performance risks
- Maintainability
- Missing verification

Avoid repeating repository discovery already completed during planning.

### Codex Compact Review Output

Codex review should use:

#### BLOCKING

Issues that must be fixed before completion.

#### IMPORTANT

Real issues that should be addressed but are not necessarily blocking.

#### OPTIONAL

Non-required improvements.

Keep findings concise. Each finding should contain:

```text
File/Component:
Issue:
Why it matters:
Required action:
```

Do not include OPTIONAL suggestions that significantly expand the original scope. If there are no meaningful findings, Codex should say so directly instead of generating filler commentary.

### Phase 7 — Review Fixes

When a Codex review occurs, Antigravity evaluates the review findings.

Apply valid findings using Antigravity or appropriate sub-agents.

Do not implement optional suggestions that expand the original scope.

If a Codex recommendation conflicts with the original requirements, report the conflict instead of silently choosing one.

### Phase 8 — Final Build

Run the complete build/test process again.

Do not report success unless the required build/verification actually succeeds.

### Phase 9 — Completion Report

Keep the completion report concise. Report:
- Material requirement assumptions or clarifications, if any
- Task complexity classification
- Codex planning: used or skipped
- What was implemented
- Important files/components changed
- Sub-agents used, if any
- Build/test result
- Codex review: used or skipped
- Blocking/important findings addressed
- Remaining material risks
- Manual verification required

Do not reproduce the entire Codex plan in the completion report.

Do not automatically commit, push, merge, or create a pull request unless explicitly requested by the user.

---

## Loop Limits

Avoid infinite Codex ↔ Antigravity review loops.

Default maximum:
- 0 or 1 targeted Codex planning invocation
- 1 implementation phase
- 0 or 1 Codex review
- 1 review-fix phase when applicable
- 1 final build

Codex planning may be skipped through Fast Path.

Codex review may be skipped through risk assessment.

A second Codex planning invocation is an exception to the default limit and may occur only when a user's clarification materially changes an architecture-sensitive decision that Codex must reassess. Behavioral parameters or choices already covered by the plan do not justify another planning call.

A second Codex review may be performed only when:
- Architecture-critical findings were discovered
- Architecture-sensitive fixes materially changed the implementation
- Or the user explicitly requests another review

Do not use additional Codex calls for minor compilation or integration fixes.

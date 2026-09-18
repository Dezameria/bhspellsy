---
name: requirement-analysis
description: >-
  Analyze and lock implementation requirements when routed by
  codex-orchestration. Use for the initial clarification gate or for meaningful
  requirement gaps returned by Codex planning; do not use for architecture
  planning or implementation details.
---

# Requirement Analysis

## Skill Contract

**Purpose:** Lock requirements sufficiently for complexity classification and implementation.

**When to use:** When `codex-orchestration` routes an initial user requirement or a requirement gap discovered during planning.

**Inputs:**
- Original user requirement
- Relevant project conventions or explicit project rules already known
- Existing assumptions and resolved clarifications
- Codex-discovered gaps, when applicable

**Outputs:**
- Locked requirement, resolved clarifications, and recorded assumptions; or
- A minimal clarification request with affected dependent work identified

**May invoke Codex:** NO.

**May ask user:** NO. Return a clarification request to the main orchestrator, which is the only user-facing agent.

**Failure behavior:** Mark only the work dependent on an unresolved blocking decision as blocked and return control to the main orchestrator.

## Initial Requirement Clarification Gate

Run this gate immediately after the main orchestrator captures the requirement and before complexity classification, Fast Path, implementation planning, or Codex planning.

1. Determine whether an implementation-critical requirement is missing.
2. If no meaningful gap exists, return the requirement as sufficiently locked.
3. If a gap is non-blocking, use a clearly established project convention or default, record the assumption briefly, and continue.
4. If a gap is blocking, return the minimum necessary concise question and relevant options when useful. Do not authorize dependent implementation.

Keep the gate lightweight for simple LOW tasks. A request such as changing one explicit value is normally already complete.

## Blocking Requirement Gaps

A gap is blocking when different answers would materially change:
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

Do not silently choose between materially different behaviors unless the repository establishes an unambiguous convention.

## Non-Blocking Details

Do not request clarification for details safely derived from:
- Existing project conventions or implementation patterns
- Explicit project rules and naming conventions
- Mechanical implementation details or internal class structure
- Imports, file placement, registration mechanics, or straightforward resource paths
- Formatting or routine build fixes

The user should not need to specify every technical detail.

## Codex-Discovered Gaps

For each planning gap, consume:

```text
Question:
Why it matters:
Options:
Recommended default:
Blocking: YES/NO
```

If `Blocking: NO`, accept the recommended default only when consistent with project conventions and the original requirement. Record it and return without requesting another Codex call.

If `Blocking: YES`:

1. Identify only the tasks dependent on the decision.
2. Return one minimal clarification request to the main orchestrator.
3. Preserve all already-valid architecture decisions.
4. Resume dependent work after the main orchestrator supplies the answer.

Do not restart planning, repeat repository discovery, or request another Codex planning call. The main orchestrator alone decides whether a clarification materially changes an architecture-sensitive decision enough to permit the exceptional second planning call.

## Suggestions and Alternatives

Keep required decisions distinct from optional suggestions. Optional suggestions never block implementation, must remain within the requested scope, and must not be implemented without user approval.

## Result Shape

Return only the fields needed by the main orchestrator:

```text
Status: LOCKED | ASSUMPTION_RECORDED | CLARIFICATION_REQUIRED
Requirement:
Resolved Clarifications:
Assumptions:
Question:
Options:
Dependent Work:
```

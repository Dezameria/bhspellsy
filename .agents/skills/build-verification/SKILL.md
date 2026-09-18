---
name: build-verification
description: >-
  Run implementation and final build/test verification when routed by
  codex-orchestration. Own project-appropriate commands, evidence collection,
  routine in-plan fixes, manual-check tracking, and architecture-sensitive
  failure escalation; never invoke Codex directly.
---

# Build Verification

## Skill Contract

**Purpose:** Establish reliable build, test, runtime, and manual-verification evidence.

**When to use:** After implementation/integration and again after review fixes when a final build is required.

**Inputs:**
- Changed files/components
- Plan or Fast Path verification requirements
- Project build/test conventions and available wrappers
- Review fixes, when applicable

**Outputs:**
- Commands executed and results
- Automated/runtime/manual verification status
- Routine fixes applied
- Architecture-sensitive failure or uncertainty requiring main-orchestrator escalation

**May invoke Codex:** NO.

**May ask user:** NO directly. Return required manual verification or blockers to the main orchestrator.

**Failure behavior:** Fix straightforward in-scope failures; return architecture-sensitive failures without silently changing the approved architecture.

## Verification Procedure

1. Select the project's appropriate build and test commands from repository conventions.
2. Prefer repository-provided wrappers when available.
3. Run the checks required by the plan, affected components, and acceptance criteria.
4. Capture command, exit status, test result, and any unverified runtime/manual behavior.
5. Classify failures before changing implementation.

Do not report success unless all required automated verification succeeds.

## Failure Classification

Antigravity may directly fix straightforward issues within the approved architecture, such as compilation errors, imports, formatting, simple naming, mechanical integration mistakes, or minor resources/localization.

Return control to the main orchestrator when a fix would require:
- Changing an architecture decision
- Crossing an unapproved dependency boundary
- Revising client/server or networking responsibility
- Expanding scope
- Replanning lifecycle, persistence, public API, compatibility, or security behavior

Do not invoke Codex for ordinary compilation or integration fixes.

## Final Build

After required review fixes, run the complete required build/test process again. The final build is mandatory when the routed workflow reaches this phase.

Do not infer runtime success from compilation alone. Record manual verification still required.

## Result Shape

```text
Phase: IMPLEMENTATION_VERIFY | FINAL_BUILD
Commands:
Build Result:
Tests/Checks:
Routine Fixes:
Architecture-Sensitive Issues:
Manual Verification:
```

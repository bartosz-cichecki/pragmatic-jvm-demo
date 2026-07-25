# Instructions for coding agents

## 1. Purpose

This document defines how coding agents work in this repository. Pragmatic JVM Engineering Demo is a pragmatic JVM architecture demonstration developed through small, reviewable iterations.

`docs/architecture.en.md` is the primary source of truth for implementation and architecture. `docs/architecture.pl.md` is the parallel Polish version of the same architecture contract. The architecture defines how new system elements must be implemented when they are introduced; it is not a backlog, roadmap, implementation-status report, or promise that planned features will be delivered.

## 2. Sources of truth

Use this hierarchy:

1. `docs/architecture.en.md`
   - Defines the architecture, layer responsibilities, dependency direction, ownership boundaries, and the required implementation pattern for new elements.
   - Wins whenever sources conflict about architecture.
2. `README.md`
   - Defines the project's purpose, current status, documented setup and run instructions, presented or planned technology stack, and basic repository guidance.
   - Do not interpret planned technology or business scope as already implemented.
3. Existing code, tests, Gradle configuration, CI configuration, and `Makefile`, when present
   - Establish local conventions, actual module and package structure, available commands, and executable verification.
   - The repository currently contains no application code, tests, Gradle build, Gradle Wrapper, CI configuration, or `Makefile`; do not invent commands or infer that planned tooling is available.
4. A backlog or other development plans, when present
   - May describe future work.
   - Cannot override the architecture contract or be treated as a description of the existing system.

`docs/architecture.pl.md` is the Polish version of the same contract. Future architecture changes must keep it semantically aligned with `docs/architecture.en.md`; this intentional language duplication must not be removed or consolidated.

## 3. Workflow

1. Understand the task's objective.
2. Define a measurable DONE criterion.
3. Identify the affected area, modules, and layers.
4. Read the relevant sections of `docs/architecture.en.md`.
5. Find analogous solutions already present in the repository.
6. Establish the minimum necessary change set.
7. Implement the smallest complete solution.
8. Add or update tests when system behaviour changes.
9. Run the quality gates appropriate to the scope.
10. Perform a final compliance check against the architecture.

## 4. Pre-flight

Before the first file edit, briefly state:

- the task interpretation;
- the affected area, modules, and layers;
- the relevant rules from `docs/architecture.en.md`;
- the DONE criterion;
- the verification plan.

Keep the pre-flight proportional to the task. Do not introduce a large form or checklist for concerns that the project does not currently have. If the objective, DONE criterion, or architectural fit cannot be determined without guessing, name the concrete blocker. Do not change the architecture contract on your own to remove the ambiguity.

## 5. Implementation rules

- Search for an existing analogy before designing a solution.
- Adapt an existing approach rather than introduce a new abstraction where possible.
- Apply KISS and keep the diff minimal.
- Do not design extensibility for hypothetical future cases.
- Do not add libraries, frameworks, modules, or infrastructure without a concrete need in the task.
- Do not implement technology mentioned only as a possible or planned extension.
- Do not perform opportunistic refactoring or cosmetic cleanup.
- Do not change public contracts outside the task scope.
- Every changed line must map to the task objective.
- Preserve the layer responsibilities and dependency direction defined by the architecture.
- Do not bypass a layer merely to make an implementation shorter.
- Add an appropriate test for a change in system behaviour.
- Report out-of-scope problems, but do not fix them without an explicit need.

## 6. Architecture guardrails

- Treat `docs/architecture.en.md` as an input contract before implementation, not as a document checked only after the work is complete.
- Green tests alone do not prove architectural compliance.
- A normal feature task does not authorize changing architecture rules.
- Changing those rules requires an explicit scope that includes architecture documentation and the corresponding automated controls.
- A decision that conflicts with the current architecture contract, and any technical exception for which the contract requires it, needs a separate ADR describing context, alternatives, and consequences; follow `docs/architecture.en.md` §15 and the applicable architecture section.
- An architecture change must keep `docs/architecture.en.md` and `docs/architecture.pl.md` semantically aligned.
- Do not write backlog items or planned features into the architecture as if they already existed.
- Do not add technology or patterns merely to make the project appear more elaborate.
- An empty or not-yet-implemented repository does not invalidate the architecture. Add the first applicable elements according to the documented pattern.

## 7. Quality gates

Before choosing commands, inspect the actual Gradle build, Gradle Wrapper, `Makefile`, and CI configuration. Use only commands that exist in the checked-out repository.

At present, the repository has no Gradle build, Gradle Wrapper, `Makefile`, CI configuration, application code, or automated tests. The only defined gate applicable to documentation-only changes is:

```text
git diff --check
```

When executable gates are introduced, discover their exact commands from the repository instead of guessing task names. Follow these rules:

- Select gates that match the change scope and run them from cheapest to most expensive.
- Run gates sequentially, waiting for the complete result and exit code before starting the next.
- Never claim that a command ran when it did not.
- On failure, stop the sequence, fix the cause, and rerun the affected gates.
- For documentation-only changes, run only verification that is meaningful for documentation.
- Use the Gradle Wrapper rather than assuming a locally installed Gradle version when the wrapper exists.

Do not describe planned gates as currently available.

## 8. Completion report

Finish work with these sections:

- `Summary`
- `Files Created / Files Modified`
- `Pre-flight outcome`
- `Architecture compliance`
- `Verification`
- `Risks / Notes`

In `Architecture compliance`:

- identify the specific sections, headings, or rules in `docs/architecture.en.md` relevant to the change;
- briefly explain how the implementation respects each one;
- report ambiguity or an intentional deviation as a blocker or a decision requiring approval;
- do not use only a generic statement such as “compliant with architecture.”

In `Verification`, list only commands actually run and their results.

## 9. Commit messages

Use Conventional Commits. Keep the message short and in English, and choose a scope that reflects the actual change. Do not create a commit unless the task explicitly requires one.

Examples appropriate to the current repository:

- `docs(agents): add shared coding instructions`
- `docs(architecture): clarify dependency direction`
- `docs(readme): clarify project status`

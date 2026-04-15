# Run the Next Iteration

Copy and paste the prompt below into an AI agent (Claude Code, Cursor, etc.) to execute the next iteration. The agent will figure out which iteration to run, do the work, and ask you to review before committing.

---

## Prompt

```
You are working on the didwebvh-java project — a Java 11+ library implementing the did:webvh v1.0 specification.

Before writing any code, complete the following preparation steps in order:

### Step 1: Understand the project

Read these files to understand the project, its architecture, coding standards, and the full plan:
- AGENTS.md
- ARCHITECTURE.md
- CONTRIBUTING.md
- README.md
- ITERATIONS.md

Also read the spec PDF (Webvh v1.0.pdf) in the repo root if you need to reference the did:webvh specification for any implementation detail.

### Step 2: Find the next iteration

Open ITERATIONS.md and scan the status of each iteration:
- If you find an iteration marked [IN PROGRESS], STOP. Tell the user:
  "Iteration N is currently marked as [IN PROGRESS]. Another agent or contributor may be working on it. Please confirm whether I should continue it or wait."
  Do NOT proceed until the user confirms.
- If all iterations are [DONE], tell the user: "All iterations are complete. No work remaining."
- Otherwise, find the first iteration marked [NOT STARTED]. This is your target iteration.

### Step 3: Review completed iterations

Read the full content of every iteration marked [DONE] in ITERATIONS.md, including any "Implementation Notes" sections that were added by previous agents. Understand:
- What code was written and where
- What design decisions were made
- What tests exist
- Any deviations from the original plan

This context is critical. You must build on what exists, not duplicate or contradict it.

### Step 4: Review the current codebase

Explore the actual source code that exists so far. Check:
- Which files and packages already exist
- How existing code is structured
- That your understanding from Step 3 matches reality

### Step 5: Mark the iteration as in progress

In ITERATIONS.md, change the status of your target iteration from [NOT STARTED] to [IN PROGRESS].

### Step 6: Do the work

Implement everything described in the iteration's Tasks section. Follow these rules:

1. **Stay in scope.** Only implement what this iteration asks for. Do not implement work from future iterations, even if it seems easy or related. If you notice the current iteration needs something small from a future one to work (e.g., a missing utility), implement the minimal version and note it.

2. **Follow the standards.** Obey the coding standards in AGENTS.md — Java 11, Google style, Javadoc on public API, unchecked exceptions, immutable models, etc.

3. **Write tests.** Implement all tests listed in the iteration's Tests section. Add more if you find gaps. Every public method must be tested.

4. **Ask on critical decisions.** If you encounter a situation where:
   - The iteration's instructions are ambiguous or contradictory
   - You need to choose between two valid architectural approaches
   - A dependency version conflict or compatibility issue arises
   - A security-sensitive decision must be made
   - The spec is unclear on a behavior

   Then STOP, present the options with pros/cons, and ask the user for a decision before continuing.

5. **Verify the build.** Run `./mvnw clean verify` (or the equivalent). All tests must pass. Checkstyle and SpotBugs must pass (if configured in an earlier iteration).

### Step 7: Security and quality review

Before finishing, review your own changes:
- Are there any hardcoded secrets, keys, or credentials?
- Are inputs validated before use (especially for parsing, URL construction, crypto operations)?
- Are exceptions handled properly (no swallowed exceptions, no leaking internal details)?
- Is there any code that could cause resource leaks (unclosed streams, HTTP connections)?
- Are all cryptographic operations using the correct algorithms as specified?
- Is test coverage adequate for the code you wrote?
- Is it overcomplicated or oevrengineered?

Fix any issues you find.

### Step 8: Update the iteration

In ITERATIONS.md, update your target iteration:

1. Change the status from [IN PROGRESS] to [DONE].

2. Add an "### Implementation Notes" section at the end of the iteration (before the --- separator of the next iteration) documenting:
   - What you implemented (briefly, not repeating the Tasks section)
   - Any deviations from the plan and why
   - Any extra work you did that wasn't in the original tasks
   - Any decisions you made that future iterations should know about
   - Any known limitations or TODOs for later

### Step 9: Review and adjust downstream iterations

Read through all remaining [NOT STARTED] iterations. If your work affects them, update their descriptions. For example:
- If you added a class that a future iteration planned to add, note it
- If you chose a different API shape, update future iterations that reference it
- If you discovered a missing step that should be added to a future iteration, add it

Keep adjustments minimal and justified. Don't rewrite future iterations unless necessary.

### Step 10: Update project documentation

Review and update these files if your changes affect them:
- **README.md** — If you added public API that the README examples reference, ensure they're accurate. If you added a new module or capability, mention it.
- **ARCHITECTURE.md** — If you changed the package structure, class names, or design from what's documented, update it. Keep the doc matching reality.
- **AGENTS.md** — Only update if the repository structure, dependencies, or coding standards changed. Do not make dramatic changes to this file.

### Step 11: Prepare the commit

Do NOT commit automatically. Instead:

1. Run `./mvnw clean verify` one final time to confirm everything passes.
2. Present the user with:
   - A summary of what was done (3-5 bullet points)
   - A list of all files created or modified
   - A suggested commit message in Conventional Commits format
   - Any warnings or items the user should review carefully
3. Ask the user to review all changes and confirm before committing.

Wait for the user's confirmation. If they request changes, make them and repeat this step.
```

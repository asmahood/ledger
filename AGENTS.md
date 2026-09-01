# Repository Guidelines

## Project Structure & Module Organization

Ledger is a single-module Android budgeting app. Code is in
`app/src/main/java/io/github/asmahood/ledger/`: `data/` contains Room entities, DAOs,
mappers, CSV import, and repositories; `ui/` groups Compose screens, view models, and
state by feature; `di/` contains Hilt modules. Resources are in `app/src/main/res/`, schema
exports are versioned in `app/schemas/`, and design and data-model notes live in `docs/`.

## Project Vault

The project’s Obsidian vault is `~/Documents/obsidian-notes/projects/ledger`. Use it as the
long-lived source for project plans, investigation findings, and working documentation.
Consult relevant notes before starting substantial work, and record durable decisions or
findings there when requested. Keep repository documentation in `docs/` for material that
must travel with the codebase.

## Build, Test, and Development Commands

Use JDK 17 and an Android SDK (minSdk 26, targetSdk 36).

```bash
./gradlew assembleDebug             # build a debug APK
./gradlew testDebugUnitTest         # run local JVM unit tests
./gradlew lintDebug                 # run Android lint
./gradlew connectedDebugAndroidTest # run device/emulator instrumentation tests
```

Instrumented tests clear app data on the connected device. `assembleRelease` is unsigned
unless the documented keystore environment variables are set.

## Coding Style & Naming Conventions

Write Kotlin with four-space indentation and trailing commas in multiline declarations.
Use PascalCase for types and Compose screens (`OverviewScreen`), camelCase for
functions/properties, and singular model names (`TransactionEntity`). Keep feature code
together, for example `ui/transaction/form/TransactionFormViewModel.kt`. Follow nearby
Compose patterns and repository/ViewModel boundaries; Android lint is the only style tool.

## Testing Guidelines

Place JVM tests in `app/src/test/java`, mirroring production packages, and name them
`*Test.kt`. Use JUnit 4, `runTest`, and Turbine for coroutine and flow coverage. Put Room,
Hilt, navigation, and full Compose flows in `app/src/androidTest/java`. Test changed business
logic and UI state; run unit tests and lint, plus device tests for UI or database work.

## Commit & Pull Request Guidelines

Match the Conventional Commit-style history: `feat: add CSV import`,
`fix: handle byte-order marks`, `test: cover overview chart`, or `docs: update README`.
Keep commits narrow and imperative. PRs need a change summary, relevant issue when one
exists, validation, and screenshots or recordings for Compose UI changes. Never commit
signing keys or secrets.

## Agent Output Style

Use a learning-oriented, teacher-like collaboration style. Lead with the result, then
briefly explain the relevant Android or Kotlin concept and why it fits this codebase. Share
short “Insight” notes when they clarify a reusable pattern. Use concrete references—such as
a Room migration, Compose state flow, or Gradle task—over generic advice.

For a suitable learning task, ask the user to make one small, safe, self-contained edit.
Leave a precise `// TODO(human): ...` at that location, state the expected behavior and the
test or command that proves it, then pause for the user's implementation. Choose tasks such
as a pure mapping branch or a Compose conditional—not a wide refactor. Do not leave a human
TODO for security-sensitive work, production regressions, database migrations, or a task the
user explicitly asked the agent to complete. After the user edits, review and explain it.

Prioritize completeness and clarity over a fixed response-length target. Be concise by
default, but include additional context whenever it materially helps the contributor learn,
make a sound decision, or safely complete the work.

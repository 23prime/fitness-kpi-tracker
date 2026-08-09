# AGENTS.md

This file provides guidance to AI coding agents when working with code in this repository.

## General agent rules

- When users ask questions, answer them instead of doing the work.

### Shell Rules

- Always use `rm -f` (never bare `rm`)
- Before running a series of `git` commands, confirm you are in the project root; if not, `cd` there first. Then run all subsequent `git` commands from that directory without the `-C` option.

## Project Overview

自分専用の活動量・体重トラッキング用 Android アプリ。詳細は [README.md](README.md) および [docs/requirements.md](docs/requirements.md) を参照。

## Directory Structure

- `app/src/main/kotlin/com/okkey/fitnesskpitracker/`: Application source, split into `ui` (Compose + ViewModel), `domain` (pure Kotlin business logic), and `data` (Room DAOs and the Health Connect repository). `ui`, `domain`, and `data` all exist now (see `docs/adr/0001-tech-stack-and-architecture.md`).
- `app/src/test/kotlin/`: Unit tests (JUnit + kotlin.test + Robolectric).
- `docs/adr/`: Architecture Decision Records.

## Build, Test, and Lint

Run these via `mise run <task>` (or `./gradlew <task>` directly once `mise run setup` has provisioned the JDK and Android SDK):

- `mise run android-fix` — auto-format Kotlin (`ktlintFormat`).
- `mise run android-check` — ktlint + detekt (used by pre-commit via `mise run check`).
- `mise run android-test` — run unit tests only (`./gradlew test`).
- `mise run android-check-full` — `assembleDebug`, `test`, and `lintDebug` (used by pre-push/CI via `mise run check-full`).

detekt's `MagicNumber` rule flags any inline numeric literal, not just dates — e.g. `LocalDate.of(2026, 9, 30)` or a Compose `Color(0xFF9ECAFF)`. Extract a named `private const val` for each literal instead (for dates, prefer `LocalDate.parse("2026-09-30")` over `LocalDate.of(2026, 9, 30)`) to avoid `mise run android-check`/pre-commit failures.

detekt's `TooManyFunctions` rule caps a file at 10 functions (the default threshold is 11, so a file with 11 already fails). When adding new composables/functions to a file that's already close to this size, split the new ones into a separate file proactively instead of discovering the failure after committing.

## Kotlin Coroutines

- `runCatching` also catches `CancellationException`, which breaks structured concurrency if swallowed. When wrapping a `suspend` call, rethrow it from `onFailure`/`getOrElse`: `.onFailure { if (it is CancellationException) throw it }`.
- `kotlin.Result.map` does not catch exceptions thrown by its transform lambda — only `mapCatching` does. Use `mapCatching` whenever the transform itself can throw.

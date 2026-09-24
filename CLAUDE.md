# CLAUDE.md — Photo Placeholders

JetBrains IDE plugin (Kotlin) that inserts placeholder photos from https://picsum.photos.
Owner: James Mosquera (jamesmosq). Built on the official IntelliJ Platform Plugin Template.

## Status — read first
- The code was written in an environment WITHOUT access to Gradle or JetBrains repositories.
  **It has never been compiled, tested or run.** Treat everything as unverified.
- APIs were checked against the intellij-community sources (HttpRequests/RequestBuilder,
  BaseState, WriteCommandAction, CaretModel, AppExecutorUtil, DynamicBundle, action group ids
  `GenerateGroup` / `EditorPopupMenu`), but not compiled.
- The picsum `/v2/list` JSON shape could not be fetched live; the parser accepts `id` as string or number.

## Working rules
- Before changing any platform/Gradle code, read the official docs
  (https://plugins.jetbrains.com/docs/intellij/) or the intellij-community source. Do not guess APIs.
- Fix root causes; do not silence errors with `@Suppress` or by deleting features.
- **Never change the plugin id** `com.jamesmosquera.photoplaceholders` (it is permanent once published).
- Never publish, create releases, or push tags without the owner's explicit OK.
- All UI strings go through `PhotoPlaceholdersBundle`: English in `PhotoPlaceholdersBundle.properties`,
  Spanish in `PhotoPlaceholdersBundle_es.properties`. Keep both in sync.
- No network calls on the EDT. Dialog callbacks use `ModalityState.any()` (the dialog is modal).
- Plugin name must not contain third-party trademarks (no "Picsum"/"Lorem Picsum" in the name),
  "Plugin", "IntelliJ" or "JetBrains" (Marketplace approval guidelines).
- Every user-visible change gets a line under `## [Unreleased]` in CHANGELOG.md.
- Commit messages in English, conventional style (`fix:`, `feat:`, `docs:`).

## Commands
```bash
./gradlew build          # compile + tests
./gradlew check          # unit tests only
./gradlew verifyPlugin   # JetBrains Plugin Verifier (must pass before any release)
./gradlew runIde         # sandbox IDE for manual testing
./gradlew buildPlugin    # build/distributions/*.zip
```

## Layout
- `PicsumOptions.kt` — URL + snippet building (pure Kotlin, unit tested)
- `PicsumApi.kt` — `/v2/list`, `/id/{id}/info`, image download, thumbnail LRU cache
- `PicsumSettings.kt` — persisted last-used options (app-level light service)
- `ui/PicsumDialog.kt` — generator, live preview, gallery (bounded 4-thread executor)
- `actions/` — editor actions, multi-caret insertion
- `resources/liveTemplates/PhotoPlaceholders.xml`, `META-INF/plugin.xml`, `pluginIcon*.svg`

## Known risks to check first
1. Kotlin plugin 2.1.20 (from template) vs target platform 2025.2.6.2 — confirm compatibility.
2. Action text/description resolved from the bundle via `action.<id>.text` keys (no `text=` in plugin.xml).
3. Live template context ids (`HTML`, `CSS`, `JAVA_SCRIPT`, `PHP`, `MARKDOWN`, `OTHER`) in IDEs lacking those plugins.
4. `DialogWrapper.dispose()` override + `executor.shutdownNow()` while downloads are in flight.
5. Unit tests must not touch the bundle (enum `toString()` uses it); keep tests on `buildUrl`/`snippet`/parsing.

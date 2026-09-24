# Contributing

Thanks for helping improve Photo Placeholders! Contributions in English or Spanish are welcome.

## Getting started

1. Fork and clone the repository, then open it in IntelliJ IDEA (JDK 21).
2. Run the **Run Plugin** configuration (or `./gradlew runIde`) to try your changes in a sandbox IDE.
3. Before opening a pull request, run:

   ```bash
   ./gradlew check verifyPlugin
   ```

## Guidelines

- Keep UI strings in `src/main/resources/messages/PhotoPlaceholdersBundle.properties`
  and add the Spanish translation to `PhotoPlaceholdersBundle_es.properties`.
- Never do network calls on the EDT; use the dialog's background executor.
- Add or update tests in `src/test/kotlin` when you touch URL building or parsing.
- Add a line under `## [Unreleased]` in `CHANGELOG.md` describing your change.

## Reporting bugs

Open an issue with your IDE version (<kbd>Help</kbd> > <kbd>About</kbd>), the plugin version,
steps to reproduce and, if possible, the relevant part of `idea.log`.

<p align="center">
  <img src="src/main/resources/META-INF/pluginIcon.svg" width="80" alt="Photo Placeholders logo">
</p>

<h1 align="center">Photo Placeholders</h1>

<p align="center">
  Real placeholder photos in any JetBrains IDE, powered by <a href="https://picsum.photos">picsum.photos</a>.
</p>

<p align="center">
  <a href="https://github.com/jamesmosq/photo-placeholders/actions/workflows/build.yml"><img src="https://github.com/jamesmosq/photo-placeholders/actions/workflows/build.yml/badge.svg" alt="Build"></a>
  <!-- After the first Marketplace release, replace MARKETPLACE_ID and uncomment:
  <a href="https://plugins.jetbrains.com/plugin/MARKETPLACE_ID"><img src="https://img.shields.io/jetbrains/plugin/v/MARKETPLACE_ID.svg" alt="Version"></a>
  <a href="https://plugins.jetbrains.com/plugin/MARKETPLACE_ID"><img src="https://img.shields.io/jetbrains/plugin/d/MARKETPLACE_ID.svg" alt="Downloads"></a>
  -->
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-MIT-blue.svg" alt="MIT License"></a>
</p>

<!-- TODO: add a GIF or screenshot here (min. 1200×760, default IDE theme) -->

## Features

| Option | Generated URL |
|---|---|
| Random | `https://picsum.photos/600/400` |
| Square | `https://picsum.photos/450` |
| Specific image | `https://picsum.photos/id/237/600/400` |
| Stable random (seed) | `https://picsum.photos/seed/picsum/600/400` |
| Grayscale | `…?grayscale` |
| Blur 1–10 | `…?blur=2` |
| Extension | `…/600/400.jpg` · `…/600/400.webp` |
| Avoid caching | `…?random=1` (different on every caret) |

- **Gallery**: browse picsum.photos with thumbnails and authors; click to use that photo by id.
- **Live preview** while you tweak options.
- **Output formats**: URL, HTML `<img>`, CSS `background-image`, Markdown, JSX, `src="…"` attribute.
- **Multi-caret** insertion that replaces the current selection.
- **Remembers** your last settings, with a one-step *Last Settings* action.
- **Live templates**: `picsum`, `picsumimg`, `picsumbg`, `picsummd` + <kbd>Tab</kbd>.
- **English UI** with Spanish translation.

## Usage

<kbd>Code</kbd> > <kbd>Generate…</kbd> (<kbd>Alt+Insert</kbd> / <kbd>⌘N</kbd>) > **Placeholder Photo…**, or right-click in the editor.
Assign a shortcut under <kbd>Settings</kbd> > <kbd>Keymap</kbd> (search "Placeholder Photo").

## Installation

- **Marketplace** (after the first release): <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > search "Photo Placeholders" > <kbd>Install</kbd>.
- **Manually**: download the [latest release](https://github.com/jamesmosq/photo-placeholders/releases/latest) and use <kbd>Settings</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install Plugin from Disk…</kbd>.

## Development

Requires JDK 21. Gradle is provided by the wrapper.

```bash
./gradlew runIde        # sandbox IDE with the plugin loaded
./gradlew check         # unit tests
./gradlew verifyPlugin  # JetBrains Plugin Verifier
./gradlew buildPlugin   # build/distributions/*.zip
```

See [CONTRIBUTING.md](CONTRIBUTING.md).

## Privacy

The plugin collects no data. It only downloads images and image metadata from picsum.photos when you use it.

## Credits

Photos by [Unsplash](https://unsplash.com) contributors, served by [Lorem Picsum](https://picsum.photos) ([source](https://github.com/DMarby/picsum-photos)).
This is an unofficial plugin, not affiliated with picsum.photos.
Built on the [IntelliJ Platform Plugin Template](https://github.com/JetBrains/intellij-platform-plugin-template).

---

Made by [James Mosquera](https://www.jamesmosquera.com) · Medellín, Colombia.
Other open source work: [laravel-fiscal-colombia](https://github.com/jamesmosq/laravel-fiscal-colombia), [pasarelas-pago-simulador](https://github.com/jamesmosq/pasarelas-pago-simulador) and [more](https://github.com/jamesmosq).

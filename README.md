# Java Code Aligner

![Build](https://github.com/bash-spbu/intellij-java-code-aligner/workflows/Build/badge.svg)
[![Version](https://img.shields.io/jetbrains/plugin/v/17939-java-code-aligner.svg)](https://plugins.jetbrains.com/plugin/17939-java-code-aligner)
[![Downloads](https://img.shields.io/jetbrains/plugin/d/17939-java-code-aligner.svg)](https://plugins.jetbrains.com/plugin/17939-java-code-aligner)

<!-- Plugin description -->
Provides displaying the Java code as aligned horizontally in columns without actual formatting of the code.

It mainly serves for _increasing code readability_ and _reducing tension_
when reading dozens of tightly placed code lines with different elements.

More formally the plugin provides:

- alignment of consecutive fields, local variables and params in columns of annotations, modifiers, types, names,
  initializers;
- alignment of consecutive assignments by the equal sign;
- pretty rich set of alignment settings in `File | Settings | Editor | Inlay Hints | Java | Java Code Aligner`.

Please note, that fields alignment is performed only if its modifiers are placed
in [the canonical order](https://docs.oracle.com/javase/specs/jls/se7/html/jls-8.html#jls-8.3.1).

From the technical side, plugin works by adding invisible inlay hints (like the ones shown as parameter names for method
calls), which can be made visible via `Enable debug mode` in settings above.

Additionally, the plugin provides **folding of explicit types** to `var` and `final var` for local variable
declarations, which can be enabled in `File | Settings | Editor | General | Code Folding | Java Code Aligner`.
<!-- Plugin description end -->

## Screenshots

### Fields alignment:

<img src="docs/fields_demo.png" alt="Fields alignment demo" title="fields alignment demo"/>

### Params alignment:

<img alt="Params alignment" src="docs/params_demo.png" title="Params alignment demo"/>

### Local variables declaration alignment:

<img alt="Local variables declaration alignment demo" src="docs/locals_demo.png" title="Local variables declaration alignment demo"/>

### Local variables declaration with folded types alignment:

<img alt="Local variables declaration with folded types demo" src="docs/local_vars_with_folded_types_demo.png" title="Local variables declaration with folded types demo"/>

### Alignment settings:

<img alt="Alignment settings demo" src="docs/inlay_settings_demo.png" title="Alignment settings demo"/>

### Local variables types folding settings:

<img alt="Local variables types folding settings demo" src="docs/folding_settings_demo.png" title="Local variables types folding settings demo"/>

## Installation

- Using IDE built-in plugin system:

  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "
  intellij-java-code-aligner"</kbd> >
  <kbd>Install Plugin</kbd>

- Manually:

  Download the [latest release](https://github.com/bash-spbu/intellij-java-code-aligner/releases/latest) and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>


---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template

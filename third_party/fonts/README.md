# Fonts (SIL OFL 1.1)

Bundled in `design/src/commonMain/composeResources/font/`:

- `jetbrains_mono_regular.ttf`, `jetbrains_mono_bold.ttf` - JetBrains Mono, from
  https://github.com/JetBrains/JetBrainsMono (`fonts/ttf/`). License: `OFL-JetBrainsMono.txt`.
- `nunito_regular.ttf` (wght=400), `nunito_bold.ttf` (wght=700) - static instances of Nunito
  (https://github.com/google/fonts/tree/main/ofl/nunito). License: `OFL-Nunito.txt`.

Regenerate the Nunito instances (fonttools is a one-off dev tool, not a project dependency):

    curl -fsSL "https://raw.githubusercontent.com/google/fonts/main/ofl/nunito/Nunito%5Bwght%5D.ttf" -o /tmp/Nunito-var.ttf
    python3 -m venv /tmp/ftvenv && /tmp/ftvenv/bin/pip install fonttools
    /tmp/ftvenv/bin/fonttools varLib.instancer /tmp/Nunito-var.ttf wght=400 -o nunito_regular.ttf
    /tmp/ftvenv/bin/fonttools varLib.instancer /tmp/Nunito-var.ttf wght=700 -o nunito_bold.ttf

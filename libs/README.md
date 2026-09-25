# Third-party jars go here

This folder must contain the two Iron's Spells jars before the project compiles.
They are **not** committed, because they are other people's work and must not be
redistributed from this repository.

| File | Where to get it |
| --- | --- |
| `irons_spellbooks-1.20.1-3.16.3.jar` | [Iron's Spells 'n Spellbooks](https://www.curseforge.com/minecraft/mc-mods/irons-spells-n-spellbooks) on CurseForge / Modrinth |
| `irons_lib-1.20.1-2.1.0.jar` | dependency of the same mod, shipped in the same pack |

Rename them to exactly the names above and drop them in this folder, or edit the
two `compileOnly` lines in `build.gradle` if you use different versions.

The mod still builds and loads without them, but the `truenins:curse` school and
the five curse spells are only registered when `irons_spellbooks` is present.

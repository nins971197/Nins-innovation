# Nins innovation · Nins创想

A Minecraft **1.20.1 / Forge** mod built around a **dual-channel perk system**:
every effect exists twice, once as an item NBT tag (applied with a command) and
once as an enchantment (applied on an anvil or by other mods).
Same effect, two independent channels, independent state.

On top of that it ships a small **Iron's Spells 'n Spellbooks** addon:
one school, five spells, hand-written cast animations and hand-built 3D effects.

---

## Contents

| | Count |
| --- | --- |
| NBT tags | 20 |
| Enchantments | 15 |
| Curse spells (needs Iron's Spells) | 5 |
| Items | 1 (guide book) |

### Tags (20)

| Tag | Name | Effect |
| --- | --- | --- |
| `true_me` | True Name | melee damage is judged as true damage |
| `deity` | Divinity | nothing can shake you |
| `fly` | Skyward | ride the void wind |
| `immunity` | Purity | immune to negative effects |
| `thorns` | Reflect | attackers take damage |
| `adaptation` | Adaptation | you gradually adapt to repeated hits |
| `mitigation` | Mitigation | per-tick damage cap scaled to health |
| `revive` | Revival | the dead shall return |
| `muryokusho` | muryokusho | stuns the hit target, per-target cooldown |
| `antiheal` | Antiheal | enemies cannot restore health |
| `justice` | Sanction | cuts the enemy max health |
| `scaling` | Scaling | higher enemy health, higher damage |
| `malice` | Malice | the more you hit, the harder you hit |
| `resolve` | Last Stand | invincible near death, then pay for it |
| `siphon` | Siphon | heal yourself by the damage you deal |
| `rage` | Blood Rage | lower health, higher damage |
| `unclear` | Unclear | keeps this item from being cleared by `/clear` |
| `colorfast` | Colorfast | immune to `/kick` and `/ban` |
| `infinity` | Infinity | melee nullified, projectiles slowed, fall braking |
| `blackflash` | Black Flash | a chance to deal ten times the damage and launch the target |

### Enchantments (15)

`true_name` `stasis` `antiheal` `sanction` `scaling` `malice` `blood_rage`
`divinity` `skyward` `purity` `adaptation` `mitigation` `revival`
`last_stand` `infinity`

### Curse spells — optional, requires Iron's Spells 'n Spellbooks

| Spell | Name | Mana | What it does |
| --- | --- | --- | --- |
| `truenins:infinity` | 无下限 | 999 | 30 s of an impassable wall, then a health drain |
| `truenins:made_in_heaven` | 天堂制造 | 200/s | accelerates the clock and everything nearby |
| `truenins:curse_zone` | 咒术zone | 250 | three guaranteed Black Flashes; re-casting refills them |
| `truenins:ao` | 苍 | 200 | a blue vortex sphere orbits you, eats the terrain, drags the debris and drops it where it dies |
| `truenins:he` | 赫 | 200 | a red-black burst sphere flies dead straight, tears a tunnel and ends in an inside-out blast |

`truenins:ao` and `truenins:he` come with hand-authored player animations
(`assets/truenins/player_animation/casting_animations.json`) — the right hand rises
in front of the face, then flings the sphere out.

---

## Commands

```
/truenins tag apply <tag> [percent]
/truenins tag remove
/truenins check
```

`percent` only applies to the `blackflash` tag.

## Configuration

Everything is tunable in `config/truenins-common.toml`, grouped by feature:

* one section per tag / enchantment (`[thorns]`, `[adaptation]`, `[rage]`, …)
* `[tag_blackflash]` — damage multiplier, launch speed, burst effects
* `[spells]` — mana, cooldown, duration, damage and radius for all five spells

The spell numbers in `[spells]` are authoritative: the spells read this file,
not Iron's own spell config, so editing here takes effect without touching
`irons_spellbooks`.

---

## Building

Requirements:

* JDK 17
* the two jars listed in [`libs/README.md`](libs/README.md) dropped into `libs/`

```bash
./gradlew build
```

The jar lands in `build/libs/truenins-<version>.jar`.

If Gradle does not pick up your JDK 17 automatically, either set the `JAVA_HOME`
environment variable or add a line to `gradle.properties`:

```properties
org.gradle.java.home=/absolute/path/to/your/jdk17
```

## Notes

* **Sound effects** in `assets/truenins/sounds/` are synthesised from scratch with
  ffmpeg (`tools/make_sfx.ps1` is the recipe, kept outside this repo).
* **Particles** are all custom; vanilla particles are not reused for custom visuals.
* Only one `@Mod` class exists (`com.truenins.TrueNinsMod`); everything else is a
  plain handler class registered on the Forge event bus.

## License

MIT — see [LICENSE](LICENSE).

Iron's Spells 'n Spellbooks and irons_lib are the property of their authors and
are **not** distributed with this repository.

# Forge & Fury

Forge & Fury is a Fabric content-expansion mod for Minecraft that adds weapons and tools with their own active abilities and on-hit effects, instead of just bigger stat numbers.

## Items

- **Duskrend** – Lifesteal blade: every hit heals you for 25% of the damage dealt.
- **Fire Axe** – Sneak-click to cycle three right-click abilities: a piercing fireball, a five-wave meteor barrage, and a fiery AoE swing that ignites the ground.
- **Frozen Fang** – Sword that slows enemies on hit and chips through ice blocks with extra flair.
- **Gravity Inverter** – Right-click to levitate yourself, or sneak-right-click to launch a target skyward instead.
- **Healing Staff** – Right-click heals whatever ally you're aiming at (or yourself), curing fire and debuffs too.
- **Infernal Bow** – Flaming arrows; a full draw detonates on impact into a fire ring, meteor shower, and fire tornado, scaling with the Power enchantment.
- **Ruiner** – Fast pickaxe that vein-mines: one swing shatters every soft block next to it.
- **Storm Caller Bow** – Arrows call down lightning on impact, with a chance to home in mid-flight and an electrified shockwave that damages and staggers everything nearby.
- **Storm Fang** – Sword with a chance per hit to slow enemies or strike them with lightning; sneak-right-click unleashes a 3-target lightning burst and buffs you with Speed and Resistance.
- **Storm Shaper Staff** – Right-click calls down a cluster of lightning strikes that chain between enemies; enough casts in quick succession summon a real thunderstorm.
- **Titan's Hammer** – Every hit sends out a knockback shockwave; right-click slams the ground for an AoE earthquake.

## Requirements

| Component | Version |
|---|---|
| Minecraft | 26.2 |
| Fabric Loader | 0.19.3+ |
| Fabric API | 0.158.0+26.2 |
| Java | 25+ |

## Building from source

Requires a JDK 25 install on your machine (the Gradle daemon itself must run on JDK 25 — point `org.gradle.java.home` in `gradle.properties` at it if it isn't your system default).

```
./gradlew build
```

The first run downloads and decompiles Minecraft 26.2 plus Fabric Loom/API, so it will take a few minutes. Output jars land in `build/libs/`:

- `forgeandfury-1.0.0.jar` — the mod, drop this into a `mods` folder
- `forgeandfury-1.0.0-sources.jar` — matching sources

## Testing the mod

**Fastest path — Loom's dev run:**

```
./gradlew runClient
```

This launches a vanilla-account dev client with the mod already loaded. Open the "Forge & Fury" creative tab (or `/give @s forgeandfury:fire_axe`) and try each item — the `use()` abilities need a right-click (sneak-right-click for a few, noted per item above), melee ones just need a hit.

`./gradlew runServer` similarly boots a dedicated server with the mod loaded, useful for checking the server-only logic (most of the ability code runs server-side and is gated on `!level.isClientSide()`).

**Installed-instance path:** copy `build/libs/forgeandfury-1.0.0.jar` into a Minecraft 26.2 instance's `mods` folder that already has Fabric Loader 0.19.3+ and Fabric API 0.158.0+26.2 installed, then launch normally.

**What to check per item** (beyond "does it crash"):
- Duskrend / Frozen Fang / Storm Fang — melee hit procs (lifesteal heal, Slowness, spark particles/lightning chance)
- Fire Axe — sneak-click cycles the 3 abilities (chat message), each fires on plain right-click once its cooldown clears
- Titan's Hammer — on-hit shockwave, right-click earthquake slam
- Infernal Bow / Storm Caller Bow — draw and release; try enchanted arrows/enchanted bow (Power, Flame, Multishot, Infinity) to confirm those paths still work
- Storm Shaper Staff — right-click to strike, 5 casts within 60s should also trigger a forced thunderstorm
- Healing Staff / Gravity Inverter — aim at another entity vs. empty air to hit both branches (target vs. self)
- Ruiner — mine a block next to other soft blocks and confirm the vein-mine AoE break

## License

MIT

<p align="center">
  <img src="docs/icon-512.png" alt="Pirouette icon" width="160">
</p>

# Pirouette

**Smooth, fast, fully configurable full-turn spins for Minecraft (Fabric 1.21.4).**

Press a key and your camera whips around exactly 360 degrees, as if you swung your mouse a full circle. It lands on the exact angle every time, at any mouse sensitivity or DPI.

Pirouette is client-side only. Install it on your own game; no server mod is needed.

## Features

- **Spin key** - one clean full turn (360 degrees by default).
- **Quick turn key** - an instant "look behind you" (180 degrees by default).
- **Every spin is configurable** - angle (15 to 3600 degrees), duration (50 to 2000 ms), motion curve and direction.
- **Six motion curves** - Linear, Gentle, Smooth, Snappy, Flick and Overshoot.
- **Live preview** in the settings screen: a speed-curve plot and a dial that play your spin while you tweak it.
- **Test button** - runs the spin in your world straight from the settings screen.
- **Repeat handling** - choose what the key does when pressed mid-spin: ignore, restart or cancel.
- **Optional safety nets** - a cooldown between spins, and cancel the spin when you move the mouse.
- **Mod Menu support** plus a plain "Open settings" key. No extra config library needed.
- Settings are saved to `config/pirouette.json`. Invalid values are repaired automatically, and a file that cannot be read is kept as `pirouette.json.broken` instead of being lost.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.4.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and the Pirouette jar into your `mods` folder.
3. Optional: add [Mod Menu](https://modrinth.com/mod/modmenu) to get a "Configure" button.

Requires Java 21, which Minecraft 1.21.4 already uses.

## Controls

| Action        | Default key  | What it does                          |
|---------------|--------------|---------------------------------------|
| Spin          | `R`          | Full turn (360 degrees by default)    |
| Quick turn    | `G`          | Half turn (180 degrees by default)    |
| Open settings | `Right Shift`| Opens the Pirouette settings screen   |

Rebind them in Options > Controls > Key Binds > Pirouette.

## Settings

The settings screen has three tabs.

**Spin** and **Turn** (one profile per key)

| Setting   | Range / options                                          | Default (Spin / Turn) |
|-----------|----------------------------------------------------------|-----------------------|
| Angle     | 15 to 3600 degrees, in steps of 15                       | 360 / 180             |
| Duration  | 50 to 2000 ms, in steps of 10                            | 300 / 160             |
| Curve     | Linear, Gentle, Smooth, Snappy, Flick, Overshoot         | Smooth / Smooth       |
| Direction | Right, Left, Alternate (flips every time)                | Right / Right         |

**General**

| Setting               | Options                                  | Default |
|-----------------------|------------------------------------------|---------|
| Pirouette             | On / Off for every key                   | On      |
| Key during a spin     | Ignore, Restart, Cancel                  | Ignore  |
| Cancel on mouse move  | Stop the spin when you move the mouse    | Off     |
| Cooldown              | 0 to 2000 ms between spins               | 0       |

Hand-editing `config/pirouette.json` allows wider ranges (angle up to 7200, duration up to 10000 ms, cooldown up to 10000 ms).

## Why it is smooth and exact

Minecraft only ticks 20 times per second, which makes scripted turns look choppy. Pirouette runs once per rendered frame, reads the real clock and asks: "where should the camera be right now?" Then it rotates by the difference, through the same call the mouse itself uses.

- The motion looks the same at 30 FPS and at 360 FPS.
- Every curve starts at exactly 0 and ends at exactly 1, so a 360 degree spin ends on exactly 360 degrees.
- Because it uses real degrees, it does not depend on your mouse DPI or in-game sensitivity.
- Opening any menu, dying, or leaving the world cancels the spin.

## Fair play

Pirouette automates a precise camera turn on a key press. Many servers treat that kind of input automation as a macro or a cheat, especially competitive servers and servers with anti-cheat. Check the server rules before you use it there. How you use it is your responsibility.

## Building from source

You need Java 21.

```
bash build.sh
```

or, if you already have the Gradle wrapper:

```
./gradlew build
```

The jar appears in `build/libs/`. The first build downloads Minecraft and Fabric, so it takes a while.

If Gradle cannot resolve a version, take the current numbers for Minecraft 1.21.4 from https://fabricmc.net/develop and update `gradle.properties`.

### Project layout

```
src/main/java/io/github/ikiuox/pirouette/
  PirouetteClient.java    entry point, keys and the per-frame hook
  SpinDriver.java         starts, runs and cancels spins
  core/                   pure Java: motion curves and the spin animation
  config/                 settings and the JSON file
  gui/                    settings screen and Mod Menu hook
```

## License

Apache License 2.0. You may use, modify and redistribute Pirouette, including commercially, as long as you keep the `LICENSE` and `NOTICE` files (which credit Kiuox) with it and mark any changes you make. See [LICENSE](LICENSE) and [NOTICE](NOTICE).

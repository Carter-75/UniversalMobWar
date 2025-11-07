# Universal Mob War (Fabric 1.21.1)

![Version](https://img.shields.io/badge/version-1.0.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow)

## 🎯 What This Mod Does

**Transform Minecraft into total chaos!** Every mob attacks the nearest DIFFERENT species, including players!

- Zombies attack skeletons, creepers, cows, sheep, **YOU**, etc.
- Creepers attack zombies, endermen, chickens, **YOU**, etc.
- Animals attack each other AND hostile mobs
- Everyone attacks **YOU** if you're in Survival mode!

This mod makes every living mob automatically attempt to attack the nearest living mob of a different mob type, including all modded/custom mobs, without modifying individual mob AI files.

**Works on Fabric 1.21.1, singleplayer or multiplayer.**

---

## 📋 Key Features

✅ **Works with ALL mobs** (vanilla, modded, custom)  
✅ **Chat message on join** explains commands  
✅ **Icon included** for CurseForge/Modrinth  
✅ **Prevents same-species retaliation** (default)  
✅ **Fully toggleable** with gamerules  
✅ **Performance optimized**  
✅ **No vanilla mob behavior broken** — only overrides target selection logic  

---

## 🎮 Installation

**✅ Build successful!** The mod JAR is ready at: `build/libs/universal-mob-war-1.0.0.jar`

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (version 0.106.1 or higher)
3. Download or build the mod JAR
4. Place the `.jar` file in your `mods` folder
5. Launch Minecraft!

**When you join a world, you'll see a colorful welcome message in chat explaining all available commands.**

---

## ⚙️ Game Rules (In-Game Commands)

### 1. universalMobWarEnabled

**Default**: `true`

Turn the entire mod ON or OFF. When disabled, it's like the mod isn't installed.

```bash
# Enable the mod (default)
/gamerule universalMobWarEnabled true

# Disable the mod completely
/gamerule universalMobWarEnabled false
```

### 2. universalMobWarIgnoreSame

**Default**: `true`

Controls same-species targeting behavior.

| Value | Behavior |
|-------|----------|
| `true` (default) | Only attack different mob types. Same-type mobs are always ignored. |
| `false` | **TOTAL CHAOS!** Mobs attack ANY living mob, including same-species. |

```bash
# Default: Mobs ignore their own species
/gamerule universalMobWarIgnoreSame true

# Chaos mode: Allow same-species combat
/gamerule universalMobWarIgnoreSame false
```

**The toggle can be changed at any time without reloading.**

---

## 🎲 Target Rules

A mob will only target another entity if:

- The entity is a **living mob** (has health)
- **AND** it is not the same mob type (default mode)
- Also will attack **players** if they're in Survival mode

Because of this, the mod automatically ignores all non-living entities, including:

- Armor stands
- Boats
- Minecarts
- Item frames / paintings
- Projectiles (arrows, tridents, etc.)
- Dropped items
- Anything without health

**No special exclusions required.**

---

## 🔄 Default Behavior (universalMobWarIgnoreSame = true)

Each mob constantly looks for the nearest living mob of a different type.

- **If found** → the mob attacks that target
- **If no valid different-type mob is nearby** → the mob returns to normal vanilla behavior (wandering, idling, etc.)

### Same-Species Interaction (Default)

While `universalMobWarIgnoreSame = true` (default):

- Mobs **do not** target or retaliate against mobs of the same mob type
- If damaged by a same-species mob, they:
  - Take damage normally
  - But **do NOT** switch targets or become hostile toward that mob

This rule applies:
- During combat
- During idle/normal behavior
- Always, unless the toggle is changed

### Behavior Summary Table

| Situation | Result (Default Mode) |
|-----------|----------------------|
| Different-type mob in range | Mob attacks it |
| Only same-type mobs nearby | Mob ignores them |
| Same-type mob hits it | Mob does **not** retaliate, still takes damage |
| No valid targets around | Mob behaves normally but still won't attack same-type mobs |

---

## 🔧 Compatibility

Works with:

- ✅ Vanilla mobs
- ✅ Fabric mod mobs
- ✅ Datapack/custom mobs

Works on **servers or singleplayer worlds**.

**Does not remove base AI** — only overrides target selection logic, so mobs still have their unique behaviors (boss mechanics, etc.).

---

## 🔨 Building from Source

```bash
# Windows
gradlew clean build

# Linux/macOS
./gradlew clean build
```

**Output JAR**: `build/libs/universal-mob-war-1.0.0.jar`

Put the JAR in your `mods/` folder with Fabric Loader and Fabric API for 1.21.1.

---

## 📁 Project Structure

```
universal-mob-war/
├─ build.gradle
├─ gradle.properties
├─ settings.gradle
├─ src/
│  └─ main/
│     ├─ java/
│     │  └─ mod/
│     │     └─ universalmobwar/
│     │        ├─ UniversalMobWarMod.java
│     │        ├─ goal/
│     │        │  └─ UniversalTargetGoal.java
│     │        ├─ util/
│     │        │  └─ TargetingUtil.java
│     │        └─ mixin/
│     │           ├─ MobRevengeBlockerMixin.java
│     │           ├─ MobEntityAccessor.java
│     │           └─ GameRulesAccessor.java
│     └─ resources/
│        ├─ fabric.mod.json
│        ├─ universalmobwar.mixins.json
│        └─ icon.png
```

---

## 🔍 Technical Details

- **Namespace**: `mod.universalmobwar`
- **Minecraft Version**: 1.21.1
- **Fabric Loader**: 0.16.7+
- **Fabric API**: 0.106.1+1.21.1
- **Java**: 21

### Fixed Bugs

✓ Fixed Mixin parameter type error (was PlayerEntity, now Entity)  
✓ All mobs now target ALL living entities, not just players  
✓ Same-species retaliation properly blocked  
✓ Accessor mixins properly configured  
✓ GameRules registration working correctly  

---

## 📜 License

MIT License - See project for details.

---

## 🤝 Credits

Created by **Carter**

---

**Enjoy the chaos!** 🗡️⚔️🛡️


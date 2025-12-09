# Universal Mob War v3.1

![Version](https://img.shields.io/badge/version-3.1.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow)

**Transform Minecraft into an epic battlefield where mobs evolve, fight each other, form alliances, and challenge legendary bosses!**

---

## 🎯 What is Universal Mob War?

Universal Mob War completely overhauls mob behavior in Minecraft, creating a dynamic, ever-evolving battlefield where:

- **Mobs fight each other** in epic battles across the world
- **Mobs evolve over time**, gaining equipment, enchantments, and special abilities
- **Alliances form naturally** as mobs team up against common enemies
- **Legendary bosses** spawn during raids and can be summoned
- **Progressive skill system** with 80+ individual mob configurations
- **Works with ALL mobs** - vanilla and modded!

Watch as a simple Zombie transforms from bare-fisted into a fully armored Netherite warrior with devastating special abilities!

---

## ⚔️ Core Features

### 🔥 Mob Evolution System
Mobs aren't static anymore - they **grow stronger** based on world age and combat experience:

- **Point Accumulation**: Mobs earn points from world days (0.1 → 5.0 pts/day) and player kills (1 pt/kill)
- **Progressive Upgrades**: 80% chance to buy upgrades, 20% chance to save for better ones
- **Equipment Progression**: Wood → Stone → Iron → Diamond → Netherite
- **47 Total Skills**: Health, Strength, Speed, Healing, Resistance, and mob-specific abilities
- **Visual Evolution**: Watch mobs spawn with equipment and upgrade over time

**Example**: A Zombie on Day 50 with 120 kills has ~178 skill points:
- Diamond Sword with Sharpness IV, Fire Aspect II, Looting II
- Full Diamond Armor with Protection III, Thorns II
- Special Skills: Horde Summon (30% chance), Infectious Bite (66%)
- Health: 42 HP (21 hearts), Strength III, Speed II

### 🤝 Alliance System
Mobs work together against common enemies:

- **Strong Alliances** (same species): 20s duration, 80% cooperation
- **Weak Alliances** (different species): 5s duration, 30% cooperation
- **Dynamic Formation**: Allies attack the same target together
- **Visual Indicators**: Purple particles show alliance connections
- **Strategic Gameplay**: Watch coordinated attacks and betrayals

### 👹 Mob Warlord Boss
A legendary boss that commands armies of minions:

- **Raid Integration**: 50% chance to spawn as raid captain on wave 3+
- **Manual Summon**: Use `/mobwar summon warlord` command
- **Boss Abilities**: 
  - Summons reinforcement mobs when damaged
  - Protects minions from player damage
  - Enhanced stats and special equipment
  - Drops unique loot
- **Epic Battles**: Warlord + mob armies vs. village defenders

### 🌳 Mob-Specific Skill Trees

**Zombies & Undead** (Zombie, Husk, Drowned, Zoglin):
- Horde Summon: 10-50% chance to call reinforcements
- Infectious Bite: Convert villagers to zombies
- Hunger Attack: Inflict starvation on hit

**Skeletons & Archers** (Skeleton, Stray, Bogged):
- Piercing Shot: Arrows pierce through multiple mobs
- Bow Potion Mastery: Fire poison/wither arrows
- Multishot: Fire multiple arrows at once

**Creepers**:
- Creeper Power: Explosion radius 3.0 → 8.0 blocks
- Creeper Potion Mastery: Lingering poison clouds

**Witches**:
- Witch Potion Mastery: Throw potions 100% faster
- Witch Harming Upgrade: Instant Damage II + Wither

**Cave Spiders**:
- Poison Mastery: Poison II (20s) + Wither I

**All Mobs** (Universal Tree):
- Health Boost (10 levels): +2 HP per level, max +20 HP
- Strength (4 levels): +20% → +80% damage
- Speed (3 levels): +20% → +60% movement
- Healing (5 levels): Regeneration effects
- Resistance (3 levels): Damage reduction + Fire Resistance
- Invisibility (5 levels): 5-80% activation chance on hit
- Shield (5 levels): 20-100% chance to equip shield

### 🎮 Universal Compatibility
Works with **ALL mobs** - vanilla and modded:

- **Auto-Detection**: Automatically classifies unknown mobs
- **Smart Defaults**: Modded mobs get universal upgrades
- **No Configuration Needed**: Just install and play
- **Tested**: Works with 400+ mod modpacks

---

## 🎯 Gameplay Modes

### Maximum Chaos
```
Config: ignoreSameSpecies = false, rangeMultiplier = 5.0
Result: EVERY mob attacks EVERY other mob at 5× detection range!
```

### Spectator Mode
```
Config: targetPlayers = false
Result: Watch mobs battle without being targeted!
```

### Rapid Evolution
```
Config: dayScalingMultiplier = 3.0, killScalingMultiplier = 2.0
Result: Mobs evolve 3× faster, reaching endgame in days!
```

### Survival Challenge
```
Config: dayScalingMultiplier = 0.5, targetPlayers = true
Result: Slow progression, you're the target - hardcore mode!
```

---

## 🎮 Commands

```bash
/mobwar help              # Show all commands
/mobwar stats             # View nearby mob stats
/mobwar summon warlord    # Summon Mob Warlord boss
/mobwar raid forceboss    # Force Warlord in next raid
/mobwar reload            # Reload configuration
```

---

## ⚙️ Configuration

**Mod Menu Integration**: Click gear icon next to mod name for instant config!

**Config File**: `config/universalmobwar.json` (auto-generates)

**Key Settings**:
- `modEnabled`: Master switch
- `evolutionSystemEnabled`: Mob leveling system
- `allianceSystemEnabled`: Alliance formation
- `targetPlayers`: Whether mobs attack players
- `dayScalingMultiplier`: Speed of evolution (default: 1.0×)
- `killScalingMultiplier`: Points per kill (default: 1.0×)
- `rangeMultiplier`: Detection range (0.1× to 5.0×)
- `debugUpgradeLog`: See upgrade decisions in chat
- `excludedMobs`: List of mobs to exclude

**Example Configuration**:
```json
{
  "modEnabled": true,
  "evolutionSystemEnabled": true,
  "dayScalingMultiplier": 2.0,
  "targetPlayers": false,
  "debugUpgradeLog": false
}
```

Apply changes with `/mobwar reload` or restart the game.

---

## 📊 Technical Details

### System Architecture
- **80 Individual Mob JSONs**: Each mob has its own configuration file
- **Data-Driven Design**: All costs, skills, and upgrades defined in JSON
- **22 Mixins**: Core systems injected into Minecraft
- **Smart Caching**: 1.5s targeting cache, 80% query reduction
- **Performance Optimized**: Batching, async tasks, FPS-based throttling

### Requirements
- **Minecraft**: 1.21.1
- **Fabric Loader**: ≥0.15.10
- **Fabric API**: ≥0.102.0+1.21.1
- **Mod Menu**: Optional (for config GUI)
- **Cloth Config**: Optional (for advanced settings)

### Data Persistence
- Mob levels and equipment save to entity NBT
- Survives chunk unload/reload
- Survives server restart
- Works in singleplayer and multiplayer

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (v0.102.0+)
3. Download `universal-mob-war-3.1.0.jar`
4. Place in `mods` folder
5. Launch and enjoy!

**Optional**: Install [Mod Menu](https://modrinth.com/mod/modmenu) for config GUI

---

## 🔨 Building from Source

**Requirements**: Python 3, Java 21, Gradle

```bash
# Clone repository
git clone https://github.com/Carter-75/UniversalMobWar.git
cd UniversalMobWar

# Validate all 80 mob configs
./universal_build.py --check

# Build the mod
./universal_build.py --build

# JAR output: build/libs/universal-mob-war-3.1.0.jar
```

The `universal_build.py` script validates all mob configurations, checks code compatibility with Minecraft 1.21.1, and builds the final JAR.

---

## 🎥 What You'll See

### Early Game (Days 1-10)
- Mobs spawn with basic equipment (wooden/leather)
- Small skirmishes between individuals
- Weak alliances forming temporarily
- Mobs gaining first upgrades (health, strength)

### Mid Game (Days 10-30)
- Mobs equipped with iron/diamond gear
- Coordinated group battles
- Special abilities starting to activate
- Stronger alliances lasting longer

### Late Game (Days 30+)
- Fully armored Netherite warriors
- Epic battles with explosions and potions
- Mob Warlords leading armies
- Villages under siege from evolved hordes

---

## 📄 License

**Copyright © 2024 Carter. All rights reserved.**

✅ **Allowed**:
- Use in modpacks (with credit)
- Public/private servers
- Videos and content creation
- Share official download links

❌ **Not Allowed**:
- Redistributing the JAR file
- Modifying or decompiling
- Claiming as your own

**Attribution**: "Universal Mob War by Carter"

---

## 🐛 Bug Reports

Found a bug? Report it with:
- Minecraft version
- Fabric Loader version
- Other mods installed
- Steps to reproduce
- Screenshots/logs

---

## 🎉 Summary

Universal Mob War transforms Minecraft into a living, breathing battlefield where:
- **80 different mobs** each have unique upgrade paths
- **Mobs battle each other** in epic wars across the world
- **Evolution system** turns weak mobs into unstoppable warriors
- **Legendary bosses** like the Mob Warlord command armies
- **Alliances form** creating dynamic team battles
- **100% compatible** with vanilla and modded content

**Watch the world burn!** 🔥

---

**Made with ❤️ for Minecraft 1.21.1**

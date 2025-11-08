# Universal Mob War v2.0 - Evolution Update (Fabric 1.21.1)

![Version](https://img.shields.io/badge/version-2.0.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow)

## 🎯 What This Mod Does

**Transform Minecraft into an evolving battlefield!** Every mob attacks different species and gains power from their victories!

### Core Features:
- **Mob Evolution System** - Mobs gain levels, stats, weapons, and armor from kills
- **Alliance System** - Mobs fighting the same enemy become temporary allies
- **MOB WARLORD BOSS** - Epic boss that commands an army of 20 mobs!
- **Complete Customization** - 7+ gamerules to fine-tune the chaos
- **Player Immunity Option** - Watch the war unfold without being targeted
- **Range Control** - Scale detection range from 0.01x to 100x
- **Neutral Mob Control** - Make passive mobs join the fight

**Works on Fabric 1.21.1, singleplayer or multiplayer. Compatible with ALL modded mobs!**

---

## 🆕 What's New in v2.0

### Evolution System
- Mobs gain **levels** and **kills** that persist
- +0.5 hearts health per level
- +10% damage per level  
- +0.5% speed per level
- Progressive armor & knockback resistance
- **Automatic Equipment**: High-level mobs get swords and armor (wood → iron → diamond → netherite!)
- Max level: 100

### Alliance System (Two-Tier Bonding)
**Strong Alliances (Same Species):**
- Zombies trust other zombies, skeletons trust other skeletons, etc.
- Only when same-species combat is DISABLED (default mode)
- 95% chance to form alliance, 20% chance to ignore help
- Duration: 20 seconds, Range: 80%
- Will help allies even with different targets

**Weak Alliances (Different Species):**
- Cross-species alliances are temporary and unreliable
- 30% chance to refuse, 70% chance to ignore help
- Duration: 5 seconds, Range: 50%
- Only helps when fighting the exact same target

**Chaos Mode Behavior:**
- When same-species combat is ENABLED, all alliances are weak
- No species loyalty when betrayal is possible

### New Commands
- `/mobwar help` - Show all commands
- `/mobwar stats` - View nearby mob statistics and levels
- `/mobwar reset` - Clear all mob targets (OP only)
- `/mobwar reload` - Reload configuration (OP only)

### Configuration File
- JSON config file: `config/universalmobwar.json`
- Set default gamerule values
- Exclude specific mob types
- Control visual settings
- Auto-generates on first run

### 👑 MOB WARLORD BOSS
**The ultimate challenge!** A giant witch that summons and commands armies.

**Boss Stats:**
- **1500 HP** (750 hearts!)
- **Normal Speed** (0.35) - Can chase you down!
- **20 Minion Army** - Zombies, Skeletons, Creepers, and more
- **Dual Combat** - Melee + Potion throws
- **500 XP Drop** - Endgame rewards

**Special Rules:**
- Minions **never attack the Warlord** (total loyalty)
- Minions **never attack each other** (perfect coordination)
- Works regardless of gamerules (always cooperative)
- All minions die when Warlord is defeated

**How to Summon:**
```
/summon universalmobwar:mob_warlord
```
Or use the **Mob Warlord Spawn Egg** (witch egg colors)!

---

## 📋 Key Features

✅ **Evolution System** - Mobs level up and get stronger  
✅ **Alliance System** - Temporary teamwork based on shared targets  
✅ **Mob Warlord Boss** - Epic endgame boss with 750 hearts and 20 minions  
✅ **Works with ALL mobs** (vanilla, modded, custom)  
✅ **7 Customizable Gamerules** - Control every aspect  
✅ **Player Immunity Toggle** - Spectate without danger  
✅ **Range Multiplier** - 0.01x to 100x detection range  
✅ **Neutral Mob Control** - Force passive mobs to fight  
✅ **Config File** - Persistent settings and exclusions  
✅ **Performance Optimized** - Minimal overhead  
✅ **Creative Mode Protection** - No evolution/alliances when killed by creative players  

---

## 🎮 Installation

**✅ Build successful!** The mod JAR is ready at: `build/libs/universal-mob-war-2.0.0.jar`

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (version 0.102.0 or higher)
3. Download or build the mod JAR
4. Place the `.jar` file in your `mods` folder
5. Launch Minecraft!

**When you join a world, you'll see a colorful welcome message with quick command access.**

---

## ⚙️ Game Rules (In-Game Configuration)

### 1. universalMobWarEnabled
**Default**: `true`

Turn the entire mod ON or OFF.

```bash
/gamerule universalMobWarEnabled true   # Enable (default)
/gamerule universalMobWarEnabled false  # Disable completely
```

### 2. universalMobWarIgnoreSame
**Default**: `true`

Controls same-species targeting.

```bash
/gamerule universalMobWarIgnoreSame true   # Only attack different types (default)
/gamerule universalMobWarIgnoreSame false  # TOTAL CHAOS! Same-species fights!
```

### 3. universalMobWarTargetPlayers ⭐ NEW
**Default**: `true`

Control whether mobs target players.

```bash
/gamerule universalMobWarTargetPlayers true   # Mobs attack players (default)
/gamerule universalMobWarTargetPlayers false  # Player immunity - spectate safely!
```

### 4. universalMobWarRangeMultiplier ⭐ NEW
**Default**: `100` (1.0x)  
**Range**: 1 to 10000 (0.01x to 100.0x)

Multiplies mob detection range.

```bash
/gamerule universalMobWarRangeMultiplier 100    # Normal range (1.0x)
/gamerule universalMobWarRangeMultiplier 50     # Half range (0.5x) - local battles
/gamerule universalMobWarRangeMultiplier 300    # Triple range (3.0x) - massive wars
/gamerule universalMobWarRangeMultiplier 10000  # Maximum range (100.0x) - chaos mode
```

### 5. universalMobWarNeutralAggressive ⭐ NEW
**Default**: `false`

Forces neutral mobs (Endermen, Zombie Piglins, Wolves, etc.) to always be aggressive.

```bash
/gamerule universalMobWarNeutralAggressive false  # Normal behavior (default)
/gamerule universalMobWarNeutralAggressive true   # Neutral mobs always attack
```

### 6. universalMobWarAlliances ⭐ NEW
**Default**: `true`

Enable/disable the alliance system.

```bash
/gamerule universalMobWarAlliances true   # Mobs form alliances (default)
/gamerule universalMobWarAlliances false  # Every mob for themselves
```

### 7. universalMobWarEvolution ⭐ NEW
**Default**: `true`

Enable/disable mob leveling and equipment.

```bash
/gamerule universalMobWarEvolution true   # Mobs evolve (default)
/gamerule universalMobWarEvolution false  # No leveling system
```

---

## 🎲 How It Works

### Targeting Rules

A mob will target another entity if:
- The entity is **living** (has health)
- It's **visible** (can be seen)
- It's **in range** (scaled by range multiplier)
- It's a **different mob type** (unless ignoreSame is false)
- It's a **survival player** (unless targetPlayers is false)

Automatically ignores:
- Creative/spectator players
- Non-living entities (armor stands, boats, minecarts, etc.)
- Itself
- Same-type mobs (by default)

### Evolution System

**How Mobs Level Up:**
- Mobs gain 1 kill point per mob killed
- Every 3 kills = 1 level (configurable)
- Maximum level: 100

**Stat Bonuses Per Level:**
- Health: +0.5 hearts (1 HP)
- Damage: +0.1 points (+10%)
- Speed: +0.005 (+0.5%)
- Armor: +0.1 points
- Knockback Resistance: +1%

**Equipment Progression:**
| Level | Equipment |
|-------|-----------|
| 5-9   | Nothing yet |
| 10-19 | Wooden Sword |
| 20-29 | Stone Sword + Leather Armor |
| 30-39 | Iron Sword + Chainmail Armor |
| 40-49 | Diamond Sword + Iron Armor |
| 50-59 | Diamond Sword + Diamond Armor |
| 60+   | Netherite Sword + Netherite Armor |

**Evolution persists** - Mob data is saved with the entity!

### Alliance System

**How Alliances Form:**
- Mobs attacking the **same target** MAY become allies (not guaranteed)
- Alliance range: 16 blocks
- Alliances update every 2 seconds during combat
- Two tiers: **Strong** (same species) and **Weak** (different species)

**Strong Alliances (Same Species):**
- **Requirements**: Both mobs must be same type AND same-species combat must be DISABLED
- **Formation**: 95% success rate (only 5% refuse)
- **Duration**: 20 seconds maximum
- **Help Behavior**: 80% chance to help, 80% search range
- **Coordination**: Will help allies even with different targets
- **Examples**: Zombie + Zombie, Skeleton + Skeleton, Creeper + Creeper

**Weak Alliances (Different Species):**
- **Formation**: 70% success rate (30% refuse, plus 30% per-ally refuse)
- **Duration**: 5 seconds maximum
- **Help Behavior**: 30% chance to help (70% ignore), 50% search range
- **Coordination**: Only helps when both fighting the exact same target
- **Examples**: Zombie + Skeleton, Creeper + Spider, any cross-species

**Chaos Mode (Same-Species Combat Enabled):**
- When `/gamerule universalMobWarIgnoreSame false`
- ALL alliances become weak (including same-species)
- No special bonding when species can betray each other
- Same stats as weak alliances above

**Alliance Expiration:**
- Alliances break **IMMEDIATELY** when target dies
- Alliances break **IMMEDIATELY** when target changes
- Time-based expiration: 5s (weak) or 20s (strong)
- Automatically cleaned up to prevent memory leaks

**Combat Priority System:**
1. Continue attacking current target (HIGHEST priority - always)
2. Help allied mob (varies by alliance strength):
   - Strong: 80% chance, wider range, any target
   - Weak: 30% chance, narrow range, same target only
3. Find nearest valid target

---

## 💻 Commands

### Player Commands

```bash
/mobwar help           # Show all commands and gamerules
/mobwar stats          # Display nearby mob statistics
```

### Operator Commands

```bash
/mobwar reset          # Clear all mob targets in the world
/mobwar reload         # Reload configuration file
```

### Boss Commands

```bash
/summon universalmobwar:mob_warlord    # Summon the Mob Warlord boss
```

Or use the **Mob Warlord Spawn Egg** from the creative inventory!

---

## 🔧 Configuration File

Located at: `config/universalmobwar.json`

**Auto-generates on first run with defaults.**

### Example Configuration:

```json
{
  "modEnabled": true,
  "ignoreSameSpecies": true,
  "targetPlayers": true,
  "neutralMobsAlwaysAggressive": false,
  "allianceSystemEnabled": true,
  "evolutionSystemEnabled": true,
  "rangeMultiplier": 1.0,
  
  "maxLevel": 100,
  "killsPerLevel": 3,
  "giveEquipmentToMobs": true,
  
  "allianceDurationTicks": 100,
  "sameSpeciesAllianceDurationTicks": 400,
  "allianceRange": 16.0,
  "allianceBreakChance": 0.3,
  "sameSpeciesAllianceBreakChance": 0.05,
  
  "excludedMobs": [],
  
  "showTargetLines": true,
  "showHealthBars": true,
  "showMobLabels": true,
  "showLevelParticles": true
}
```

### Configuration Options:

- **excludedMobs**: List of entity IDs to exclude (e.g., `["minecraft:villager", "minecraft:iron_golem"]`)
- **maxLevel**: Maximum mob level (default: 100)
- **killsPerLevel**: Kills needed per level (default: 3)
- **giveEquipmentToMobs**: Auto-equip high-level mobs (default: true)
- **allianceDurationTicks**: Weak alliance duration (default: 100 = 5 seconds)
- **sameSpeciesAllianceDurationTicks**: Strong alliance duration (default: 400 = 20 seconds)
- **allianceBreakChance**: Weak alliance refuse chance (default: 0.3 = 30%)
- **sameSpeciesAllianceBreakChance**: Strong alliance refuse chance (default: 0.05 = 5%)
- **Visual settings**: Control spectator mode enhancements

---

## 🔄 Behavior Examples

### Default Mode (All Systems Enabled)

- Zombies attack skeletons, creepers, cows, **YOU**, etc.
- A zombie kills a cow → gains 1 kill point
- After 3 kills → zombie reaches Level 1 (gains +0.5 hearts, +0.1 damage, etc.)
- **Two zombies attack same skeleton** → STRONG alliance (95% success, 20 sec duration)
  - They trust each other and actively coordinate attacks
- **Zombie + Skeleton attack same creeper** → weak alliance (70% success, 5 sec duration)
  - They might cooperate but mostly focus on their own fight
- Alliances break instantly when target dies or if they switch targets
- At Level 10+ → zombie gets a wooden sword
- At Level 50+ → zombie gets diamond sword and diamond armor!

### Player Immunity Mode

```bash
/gamerule universalMobWarTargetPlayers false
```

- Mobs completely ignore players (even Survival mode)
- Perfect for spectating or building during chaos
- Mobs still fight each other and evolve normally

### Chaos Mode

```bash
/gamerule universalMobWarIgnoreSame false
```

- Same-species mobs attack each other!
- Zombies fight zombies, skeletons fight skeletons
- **Ultimate chaos** - everyone vs everyone

### Ranged War Mode

```bash
/gamerule universalMobWarRangeMultiplier 500
```

- 5x detection range (5.0x)
- Mobs detect targets from much further away
- Creates large-scale battlefield scenarios

### Silent Hills Mode

```bash
/gamerule universalMobWarRangeMultiplier 20
/gamerule universalMobWarNeutralAggressive true
```

- Very short range (0.2x) - only close encounters
- Even neutral mobs are aggressive
- Tense, dangerous atmosphere

---

## 🛡️ Creative Mode Protection

**Important**: Creative mode players don't trigger evolution progression:

✅ Mobs killed by creative players **do not** grant evolution XP  
✅ Only **Survival mode** kills count for mob leveling  
✅ Alliances form normally regardless of player mode

This prevents accidental evolution manipulation during building or testing.

---

## 🔍 Compatibility

**Works with:**
- ✅ All vanilla Minecraft mobs
- ✅ Modded mobs from other Fabric mods
- ✅ Custom mobs from datapacks
- ✅ Servers and singleplayer
- ✅ All mob types (hostile, passive, neutral, boss)

**Respects:**
- Boss-specific mechanics (Ender Dragon, Wither, etc.)
- Mod-specific AI behaviors
- Custom mob attributes

**Does not:**
- Break vanilla mob behaviors
- Modify base mob AI (only adds targeting goals)
- Cause compatibility issues with other mods

---

## 🔨 Building from Source

```bash
# Windows
gradlew clean build

# Linux/macOS
./gradlew clean build
```

**Output JAR**: `build/libs/universal-mob-war-2.0.0.jar`

---

## 📁 Project Structure

```
universal-mob-war/
├─ build.gradle
├─ settings.gradle
├─ gradle.properties
├─ src/
│  └─ main/
│     ├─ java/mod/universalmobwar/
│     │  ├─ UniversalMobWarMod.java          # Main mod class
│     │  ├─ command/
│     │  │  └─ MobWarCommand.java            # /mobwar commands
│     │  ├─ config/
│     │  │  └─ ModConfig.java                # Config system
│     │  ├─ data/
│     │  │  └─ MobWarData.java               # Mob evolution data
│     │  ├─ entity/
│     │  │  └─ MobWarlordEntity.java         # Boss entity (1500 HP, 20 minions!)
│     │  ├─ goal/
│     │  │  └─ UniversalTargetGoal.java      # Enhanced targeting AI
│     │  ├─ system/
│     │  │  ├─ AllianceSystem.java           # Alliance management
│     │  │  └─ EvolutionSystem.java          # Leveling & equipment
│     │  ├─ util/
│     │  │  └─ TargetingUtil.java            # Targeting helpers
│     │  ├─ client/
│     │  │  └─ MobWarVisuals.java            # Visual enhancements
│     │  └─ mixin/
│     │     ├─ GameRulesAccessor.java        # Gamerule registration
│     │     ├─ MobEntityAccessor.java        # Goal selector access
│     │     ├─ MobRevengeBlockerMixin.java   # Same-species blocking
│     │     ├─ MobDeathTrackerMixin.java     # Kill tracking
│     │     ├─ NeutralMobBehaviorMixin.java  # Neutral mob control
│     │     ├─ WarlordMinionProtectionMixin.java  # Boss minion protection
│     │     └─ WarlordDamageProtectionMixin.java  # Boss damage prevention
│     └─ resources/
│        ├─ fabric.mod.json
│        ├─ universalmobwar.mixins.json
│        └─ icon.png
```

---

## 🔍 Technical Details

- **Namespace**: `mod.universalmobwar`
- **Minecraft Version**: 1.21.1
- **Fabric Loader**: 0.16.5+
- **Fabric API**: 0.102.0+1.21.1
- **Java**: 21

### System Details:
- Evolution data stored in entity NBT
- Alliances tracked with UUID maps and timestamps
- Boss uses custom entity with spawn egg registration
- Minion protection via dual mixins (targeting + damage)
- Gamerules use Fabric's game rule system
- Commands registered via Fabric Command API v2
- Configuration uses JSON with Gson
- Mixins target LivingEntity, MobEntity, and GameRules

---

## 📜 License

MIT License - Free to use, modify, and distribute.

---

## 🤝 Credits

**Created by Carter**

Special thanks to the Fabric community for excellent documentation and tools!

---

## 🎮 Tips & Tricks

### Survival Strategy

1. **Watch for High-Level Mobs**: Mobs with weapons/armor are dangerous!
2. **Use Alliances**: Lure mobs to fight your enemies
3. **Player Immunity**: Toggle off to safely build shelters
4. **Scout with Stats**: Use `/mobwar stats` to check threat levels
5. **Emergency Reset**: `/mobwar reset` if things get too chaotic

### Server Recommendations

- Default settings work great for most servers
- Consider `targetPlayers false` for creative build servers
- Use `rangeMultiplier 50` (0.5x) for performance on large servers
- Exclude peaceful mobs if you want them safe: add to config `excludedMobs`

### Content Creation

- Perfect for challenging Let's Plays
- Great for PvE server events
- Use evolution system for progressive difficulty
- Spectator mode with player immunity for cinematic shots

---

**Enjoy the evolution of chaos!** 🗡️⚔️🛡️

*Have fun watching mobs become warriors and conquerors!*

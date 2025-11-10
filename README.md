# Universal Mob War v2.0 - Evolution Update

![Version](https://img.shields.io/badge/version-2.0.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow)

**Transform Minecraft into an evolving battlefield where mobs fight each other, level up, and form alliances!**

Compatible with **all vanilla and modded mobs**. Optimized for large modpacks (tested with 400+ mods).

---

## 📋 Key Features

### ⚔️ Combat & Evolution
- **Universal Mob Combat** - All mobs attack different species automatically
- **Evolution System** - Mobs gain levels, stats, weapons, and armor from kills (max level 100)
- **Alliance System** - Two-tier system: strong same-species alliances, weak cross-species alliances
- **Betrayal Detection** - Minions who attack allies are marked as traitors

### 👑 Mob Warlord Boss
- **Epic Boss Fight** - 2x-sized giant witch with 1500 HP (750 hearts!)
- **🌟 Super Self-Healing** - Throws golden potions when health drops below 70%
- **🌟 Mob Recruitment** - Takes over nearby mobs and adds them to its army every 3 seconds
- **Fast Spawning** - Summons new minions every 2 seconds (aggressive reinforcement!)
- **27 Summonable Mobs** - Commands an army of up to 20 minions
- **Smart AI** - Heals low-health minions, avoids friendly fire, prioritizes threats
- **4 Potion Types** - Harmful, super healing (golden), regular healing, and buff potions
- **Particle Connections** - Purple lines to loyal minions, red lines to betrayers
- **Raid Integration** - 1.5% chance to spawn in final raid wave (can be forced with command)

### 🎮 Customization
- **7 Game Rules** - Fine-tune every aspect of the combat
- **Range Control** - Scale mob detection from 0.01x to 100x
- **Player Immunity** - Toggle to spectate without being targeted
- **Config File** - Persistent settings and mob exclusions
- **Creative Mode Protection** - Creative players don't trigger evolution/alliances

---

## 🆕 What's New in v2.0

### Mob Warlord Boss
- Giant witch boss (actually 2x size!) with boss bar
- **🌟 NEW: Super self-healing** - golden potions when below 70% health (Instant Health IV + Absorption III!)
- **🌟 NEW: Mob recruitment** - takes over nearby hostile/neutral mobs every 3 seconds
- **🌟 NEW: Faster spawning** - summons allies every 2 seconds (was 5 seconds)
- Smart combat AI: heals minions, avoids friendly fire, buffs allies
- Summons 27 different mob types (including Vexes!)
- Custom potions: harmful (debuffs), healing (Instant Health II + buffs), support (Strength/Speed/Resistance)
- Particle connections showing loyalty (purple) vs betrayal (red)
- Smart creeper AI - creepers flee if 3+ allies nearby to avoid friendly fire
- **Raid spawning** - 1.5% chance on final wave OR force with `/mobwar raid forceboss`
- **Context-aware targeting**:
  - **In raids**: Targets villagers → iron golems → players (only if interfering)
  - **Normal spawn**: Targets players → all other mobs
  - Avoids attacking raid mobs (pillagers, vindicators, etc.) when spawned in raids

### Betrayal System
- Automatic detection when minions attack each other
- Betrayers lose protection - boss and loyal minions can target them
- Visual indicator: red angry particle connections instead of purple
- Action bar message: "⚔ A minion has betrayed the Warlord! ⚔"
- Status persists through saves

### Alliance System (Two-Tier)
**Strong Alliances (Same Species)**:
- 95% formation chance, lasts 20 seconds
- 20% chance to ignore help requests
- 80% detection range
- Will help allies even with different targets
- Only when `universalMobWarIgnoreSame` is true (default)

**Weak Alliances (Different Species)**:
- 70% formation chance, lasts 5 seconds
- 70% chance to ignore help requests
- 50% detection range
- Only helps when fighting same target

**Chaos Mode** (when `universalMobWarIgnoreSame` is false):
- All alliances become weak, even same-species
- Total warfare with minimal coordination

### Evolution System
- Mobs gain XP and levels from kills (max level 100)
- **Stat Bonuses per Level**:
  - +0.5 hearts health
  - +10% attack damage
  - +0.5% movement speed
  - Progressive armor and knockback resistance
- **Automatic Equipment**:
  - Level 10+: Weapons (wood → stone → iron → diamond → netherite)
  - Level 20+: Armor sets (leather → chainmail → iron → diamond → netherite)
- **Creative Mode Protection**: No evolution XP when killed by creative players

---

## 🎮 Commands

### Basic Commands
```
/mobwar help                    - Show all commands
/mobwar stats                   - View nearby mob levels
/mobwar reset                   - Clear all mob targets (OP)
/mobwar reload                  - Reload config file (OP)
```

### Boss Summoning
```
/mobwar summon warlord          - Summon Mob Warlord at your location (OP)
/summon universalmobwar:mob_warlord  - Alternative summon command
```

### Raid Boss (NEW!)
```
/mobwar raid forceboss          - Guarantee boss spawn in next raid (OP)
```
Use this command, then start a raid. The boss will spawn on the final wave with a dramatic announcement!

---

## ⚙️ Game Rules

Use `/gamerule <name> <value>` to configure:

| Game Rule | Default | Description |
|-----------|---------|-------------|
| `universalMobWarEnabled` | true | Master toggle for entire mod |
| `universalMobWarIgnoreSame` | true | If true, same-species don't fight (strong alliances) |
| `universalMobWarTargetPlayers` | true | If false, mobs ignore players (spectator mode) |
| `universalMobWarNeutralAggressive` | false | Make neutral mobs (endermen, iron golems) always hostile |
| `universalMobWarAlliances` | true | Enable alliance system |
| `universalMobWarEvolution` | true | Enable mob leveling and equipment |
| `universalMobWarRangeMultiplier` | 100 | Detection range multiplier (1-10000 = 0.01x to 100x) |

### Examples
```
/gamerule universalMobWarIgnoreSame false    # Enable chaos mode (same-species can fight)
/gamerule universalMobWarTargetPlayers false # Player immunity (spectate safely)
/gamerule universalMobWarRangeMultiplier 500 # 5x detection range
```

---

## 🔮 Mob Warlord Boss Details

### Stats
- **Health**: 1500 HP (750 hearts!)
- **Size**: 2x witch (2.4m wide × 7.2m tall)
- **Speed**: 0.35 (fast!)
- **Damage**: 12 hearts melee + custom potions
- **Armor**: 10 points
- **Knockback Resistance**: 80%
- **XP Drop**: 500 (massive!)
- **Max Minions**: 20 simultaneously

### Combat Abilities

**Melee Attack** (Close Range):
- 12 hearts damage + powerful knockback
- Purple witch particles + dragon breath particles

**Custom Potions** (Long Range):
- **Harmful Potion** (70% chance) - Dark purple:
  - Poison II (10 sec), Weakness II (15 sec), Slowness II (10 sec), Wither I (5 sec)
- **Healing Potion** - Deep pink with hearts:
  - Instant Health II, Regeneration II (10 sec), Resistance II (15 sec)
  - Only used on minions below 50% health when safe
- **Buff Potion** (30% chance) - Bright purple:
  - Strength II (20 sec), Speed II (20 sec), Resistance I (20 sec)
  - Used to support minions when combat is safe

**Smart AI Priority System**:
0. **🌟 SELF-HEALING** - Boss heals itself when below 70% health (HIGHEST PRIORITY!)
   - **Super Healing Potion** (Golden): Instant Health IV (8 hearts), Regeneration III (20s), Resistance III (30s), Absorption III (6 extra hearts)
   - Shorter cooldown for rapid self-healing
1. **Self-Defense** - Attacks threats first
2. **Heal Minions** - Throws healing potions to injured minions (<50% HP)
3. **Attack Enemies** - Avoids friendly fire by checking splash radius
4. **Buff Minions** - Strengthens army when safe

**Minion Protection**:
- Attacks anyone who hurts its minions
- Forgives friendly fire from minions
- Targets betrayers who attack other minions
- Coordinated assault: all minions target boss's current target

**🆕 Mob Recruitment System**:
- **Takes over nearby mobs** every 3 seconds
- Recruits 1-2 hostile/neutral mobs within 16 blocks
- 50% success chance per mob
- Recruited mobs get purple portal + enchant particles
- Recruited mobs become permanent minions with full loyalty
- Works in addition to spawning new minions!

### Summonable Mobs (27 Types)

**Hostile Mobs (22)**:
- Undead: Zombie, Skeleton, Husk, Stray, Drowned, Wither Skeleton
- Common: Creeper, Spider, Cave Spider
- Magical: Witch, Blaze, Enderman, **Vex**, Evoker
- Nether: Zombified Piglin, Piglin, Piglin Brute, Hoglin
- Illagers: Vindicator, Pillager, Ravager

**Neutral Mobs (5)** - Only if `universalMobWarNeutralAggressive` is enabled:
- Iron Golem, Wolf, Polar Bear, Panda, Bee

**Special Minion Behavior**:
- **Smart Creepers**: Flee if 3+ allies nearby to avoid mass friendly fire
- **Total Loyalty**: Never attack boss or each other (regardless of gamerules)
- **Betrayal System**: If a minion attacks another, they become a traitor (red particles)
- **Death Link**: All minions die instantly when boss is defeated

### Particle Connections
- 💜 **Purple Portal Particles**: Loyal minions
- 🔴 **Red Angry Particles**: Betrayers
- Updates every second
- Makes it easy to see who's allied to the boss

### Raid Boss Behavior

**When Spawned in Raid** (1.5% chance on final wave):
- Targets villagers (highest priority)
- Targets iron golems (second priority)
- Avoids targeting other raid mobs (pillagers, vindicators, ravagers, witches, vexes)
- Only attacks players if they interfere (attack villagers/golems/minions)
- Dramatic spawn message: "💀 THE MOB WARLORD HAS JOINED THE RAID! 💀"
- Wither spawn sound plays for all players

**When Summoned Normally**:
- Targets players actively
- Targets all mobs (except minions)
- Full alliance system for summoned minions
- Slight preference for raid-type mobs as summons

### How to Get Spawn Egg
Look in Creative Inventory → Spawn Eggs tab → **Mob Warlord Spawn Egg** (witch colors: dark green with bright green spots)

---

## 📊 Alliance System Details

### Combat Priority (All Mobs)
1. **Continue Attacking Current Target** - Commitment to current fight
2. **Help Allied Mobs** - When allies are being attacked
3. **Find Nearest Valid Target** - Based on species compatibility

### Alliance Formation
- Mobs attacking the **same target** become temporary allies
- Alliance strength depends on species compatibility and gamerules
- Alliances break when target dies or changes
- Random chance to refuse alliance (varies by type)

### Alliance Behavior

**Strong Alliances** (same-species, ignore-same ON):
- Duration: 20 seconds
- Formation: 95% chance
- Help Response: 80% chance, even with different targets
- Detection Range: 80% of normal
- Break Chance: 5% per check
- Examples: Zombies help zombies, skeletons help skeletons

**Weak Alliances** (different-species OR chaos mode):
- Duration: 5 seconds
- Formation: 70% chance
- Help Response: 30% chance, only for same target
- Detection Range: 50% of normal
- Break Chance: 30% per check
- Examples: Zombie + skeleton, any combo in chaos mode

### Chaos Mode (`universalMobWarIgnoreSame false`)
- Same-species can fight each other
- ALL alliances become weak
- No species loyalty
- Total warfare with minimal coordination
- Still avoids attacking Mob Warlord minions (unless betrayed)

---

## 📝 Configuration File

Located at: `config/universalmobwar.json`

**Default Configuration**:
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

**Customization Options**:
- `excludedMobs` - List of entity IDs to exclude (e.g., `["minecraft:villager", "minecraft:allay"]`)
- `maxLevel` - Maximum mob level (default: 100)
- `killsPerLevel` - Kills required per level (default: 3)
- `allianceRange` - Range for alliance detection (default: 16 blocks)
- Visual settings for spectator mode enhancements

Use `/mobwar reload` after editing the config file.

---

## 🎯 Testing the Raid Boss

### Method 1: Guaranteed Spawn
```
1. /mobwar raid forceboss
2. Find or create a village
3. Get Bad Omen effect (kill a raid captain - pillager with banner)
4. Enter the village to trigger raid
5. Fight through waves 1-6
6. Boss spawns on final wave with announcement!
```

### Method 2: Natural Spawn (1.5% chance)
```
1. Start any raid normally
2. Reach final wave (wave 6+)
3. 1.5% chance boss will spawn
4. Very rare - use forceboss for testing!
```

---

## 💡 Tips & Strategies

### Surviving the Mob Warlord
- **Stay Mobile**: Boss has long range attacks
- **Prioritize Minions**: Reduce the army size first
- **Watch for Betrayers**: Red particle connections = friendly fire opportunity
- **Interrupt Healing**: Stop boss from healing minions
- **Bring Milk**: Clear debuff potions (Poison, Weakness, Slowness, Wither)
- **In Raids**: Boss will ignore you if you don't attack villagers/golems

### Spectator Mode (Safe Observation)
```
/gamerule universalMobWarTargetPlayers false
```
Mobs will ignore you - perfect for watching the chaos!

### Maximum Chaos
```
/gamerule universalMobWarIgnoreSame false
/gamerule universalMobWarNeutralAggressive true
/gamerule universalMobWarRangeMultiplier 1000
```
Everything fights everything at 10x detection range!

### Peaceful Evolution
```
/gamerule universalMobWarTargetPlayers false
/gamerule universalMobWarIgnoreSame true
```
Mobs fight each other but leave you alone. Watch them evolve naturally!

---

## 🔧 Technical Details

### Compatibility
- **Fabric 1.21.1** (Loader ≥0.16.5)
- **Fabric API** ≥0.102.0+1.21.1
- Works with **all vanilla mobs**
- Works with **all modded mobs** (automatically detected)
- Tested with **400+ mod modpacks**
- Compatible with **Iris Shaders**

### Performance
- Optimized for large modpacks
- Entity lookups limited to 10 per cycle
- Particle rendering throttled
- All operations have cooldowns
- No significant performance impact

### Data Persistence
- Mob levels and kills saved in entity NBT data
- Alliance timestamps tracked per session
- Boss minion relationships saved
- Betrayer status persists through saves
- Config file for default settings

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1 (version 0.16.5 or higher)
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (version 0.102.0 or higher)
3. Download `universal-mob-war-2.0.0.jar` from the official download page
4. Place in `mods` folder
5. Launch game!

Config file will be auto-generated at `config/universalmobwar.json`

---

## 🔨 Building from Source

If you want to build the mod yourself for personal use:

1. Clone this repository
2. Run `./gradlew build` (Linux/Mac) or `gradlew.bat build` (Windows)
3. Find the compiled jar in `build/libs/`

**Note**: Build artifacts (`build/`, `.gradle/`) are already in `.gitignore` and should not be committed to version control.

---

## 🎮 Welcome Message

When players join, they see:
```
═══════════════════════════════════════════════════
    UNIVERSAL MOB WAR v2.0 - EVOLUTION UPDATE!
═══════════════════════════════════════════════════

⚔ NEW FEATURES:
  • Evolution System - Mobs level up, gain stats & equipment!
  • Alliance System - Mobs team up against common enemies!
  • Player Immunity - Toggle to spectate safely!
  • Range Control - 0.01x to 100x detection range!
  • Neutral Mob Control - Force passive mobs to fight!

📋 Quick Commands:
  • /mobwar help - Full command list
  • /mobwar stats - View nearby mob levels

⚙ Key Game Rules (use /gamerule):
  • universalMobWarEnabled - Turn mod on/off
  • universalMobWarTargetPlayers - Player immunity toggle
  • universalMobWarRangeMultiplier - Scale range (1-10000)
  • universalMobWarEvolution - Enable leveling system
  • universalMobWarAlliances - Enable alliance system
    Type /mobwar help for all 7 game rules!

═══════════════════════════════════════════════════
    Watch mobs evolve into warriors! Good luck!
═══════════════════════════════════════════════════
```

---

## 📄 License & Usage Rights

**Copyright © 2024 Carter. All rights reserved.**

### ✅ You MAY:
- Use this mod in modpacks (with proper attribution)
- Use this mod on servers (public or private)
- Create videos/content featuring this mod
- Share where to obtain this mod (link to the official download page)

### ❌ You MAY NOT:
- Edit, modify, or decompile this mod
- Claim this mod as your own
- **Redistribute or share the direct download link to the mod file**
- Redistribute modified versions
- Remove or alter credits
- Re-upload this mod to other platforms or file-sharing sites

### 📋 Attribution Requirements:
If you use this mod in a modpack, server, or content, you **MUST**:
1. Credit **Carter** as the original creator
2. Include a link to the **official download page** (not the direct file link)
3. Clearly state that you did not create this mod

**Example Attribution**:
```
Universal Mob War by Carter
Download: [Link to official download page]
Not created by [Your Name/Server Name]
```

### 🔒 Distribution Policy:
This mod is distributed through a controlled download page. You may direct users to the official download location, but you may not:
- Provide direct download links to the .jar file
- Re-host the mod files elsewhere
- Bypass the official download process

**Violation of these terms may result in removal requests or DMCA takedowns.**

## 🐛 Bug Reports

Report issues with:
- Minecraft version
- Fabric Loader version
- Other mods installed
- Steps to reproduce

---

**Enjoy the chaos!** 🎉

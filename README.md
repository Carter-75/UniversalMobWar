# Universal Mob War v2.0+ (Evolution & Scaling)

![Version](https://img.shields.io/badge/version-2.0.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow)

**Battlefield overhaul: mobs fight, evolve, form alliances, and scale globally!**

**Works with all vanilla & modded mobs.**

---

## 📋 Features Overview

- **Unified Evolution System (v2.0):**
  - **Day Scaling:** Mobs spawn stronger based on the world day (configurable).
  - **Kill Scaling:** Mobs gain additional skill points by killing others (configurable multiplier).
  - **Hyper-Lethality:** Damage scales faster than Armor to prevent stalemates.
  - **Gear Progression:** Mobs equip better armor and weapons as they level up.
  - **Stalemate Breaker:** If a fight lasts >15s, the attacker gains Berserk buffs (Strength/Speed) to force a conclusion.

- **Combat & AI:**
  - **Universal Targeting:** Mobs attack anything that isn't their species (configurable).
  - **Smart Targeting:** Optimized AI that filters targets efficiently (no lag).
  - **Alliance System:** Mobs can form alliances to team up against stronger foes.
  - **Player Immunity:** Option to make mobs ignore players (Spectator Mode).

- **Customization:**
  - All options are available via Mod Menu or `config/universalmobwar.json`.
  - **Gamerules:** `universalMobWarEnabled`, `universalMobWarEvolution`, etc.

---


- **Mod Menu:** Click gear icon in Mods list for instant config
- **Config file:** `config/universalmobwar.json` (all options, including scaling system)
- **Key options:**
  - `scalingEnabled`, `dayScalingMultiplier`, `killScalingMultiplier`, `maxTier`, `allowBossScaling`, `allowModdedScaling`, `restrictEffectsToMobTheme`, `debugLogging`
  - All legacy evolution/alliance/gameplay options still available

---
- Upgrades: health, armor, speed, damage, resistances, aggro, immunities, more
- Archetype paths: zombies, skeletons, spiders, creepers, witches, illagers, end/nether/warden, all modded (auto-detect)

---
- `/mobwar raid forceboss` — Force boss in next raid


---
  "allowBossScaling": true,
  "restrictEffectsToMobTheme": true,
  "debugLogging": false,
  "ignoreSameSpecies": true,
  "targetPlayers": true,
  "killsPerLevel": 3,
  "giveEquipmentToMobs": true,
  "excludedMobs": [],
  "showTargetLines": true,

---
- **No lag**: scaling logic only runs if enabled, all operations staggered, memory efficient
- **Data**: mob levels/kills in NBT, config auto-generated, all settings hot-reloadable
**© 2024 Carter. All rights reserved.**


---

## 🎮 Commands
/mobwar help                    - Show all commands
/mobwar stats                   - View nearby mob levels

```
/mobwar summon warlord          - Summon Mob Warlord at your location (OP)
### Raid Boss (NEW!)
```
/mobwar raid forceboss          - Guarantee boss spawn in next raid (OP)
```
Use this command, then start a raid. The boss will spawn on the final wave with a dramatic announcement!

---


## ⚙️ Settings & Mod Menu (No More Gamerules)

All configuration is now handled via the config file or the in-game Mod Menu. **Gamerules are deprecated**—every option is a setting, and all are available in the Mod Menu UI for instant changes.

**Key settings (all available in Mod Menu):**
| Setting | Default | Description |
|---------|---------|-------------|
| `modEnabled` | true | Master toggle for entire mod |
| `scalingEnabled` | true | Enable global mob scaling system |
| `dayScalingMultiplier` | 1.0 | World-day scaling multiplier |
| `killScalingMultiplier` | 1.0 | Kill-based scaling multiplier |
| `maxTier` | 20 | Maximum scaling tier (hard cap) |
| `allowBossScaling` | true | Allow scaling for boss mobs |
| `allowModdedScaling` | true | Allow scaling for modded mobs |
| `restrictEffectsToMobTheme` | true | Only apply effects that fit mob archetype |
| `debugLogging` | false | Enable debug logging |
| `ignoreSameSpecies` | true | If true, same-species don't fight (strong alliances) |
| `targetPlayers` | true | If false, mobs ignore players (spectator mode) |
| `neutralMobsAlwaysAggressive` | false | Make neutral mobs (endermen, iron golems) always hostile |
| `allianceSystemEnabled` | true | Enable alliance system |
| `evolutionSystemEnabled` | true | Enable mob leveling and equipment |
| `rangeMultiplier` | 1.0 | Detection range multiplier (0.01x to 100x) |
| `maxLevel` | 100 | Maximum mob level (legacy evolution) |
| `killsPerLevel` | 3 | Kills required per level |
| `giveEquipmentToMobs` | true | Allow mobs to receive equipment upgrades |
| `allianceDurationTicks` | 100 | Weak alliance duration (ticks) |
| `sameSpeciesAllianceDurationTicks` | 400 | Strong alliance duration (ticks) |
| `allianceRange` | 16.0 | Alliance detection range |
| `allianceBreakChance` | 0.3 | Weak alliance break chance |
| `sameSpeciesAllianceBreakChance` | 0.05 | Strong alliance break chance |
| `excludedMobs` | [] | List of entity IDs to exclude |
| `showTargetLines` | true | Show minion/target lines |
| `showHealthBars` | true | Show mob health bars |
| `showMobLabels` | true | Show mob name/level labels |
| `showLevelParticles` | true | Show level-up particles |

**Performance & Visual Tuning:**
- `targetingCacheMs`, `targetingMaxQueriesPerTick`, `mobDataSaveDebounceMs`, `enableBatching`, `enableAsyncTasks`, `maxParticlesPerConnection`, `maxDrawnMinionConnections`, `minFpsForVisuals` (see config file for advanced users)

**How to change settings:**
- Use the Mod Menu in-game (recommended)
- Or edit `config/universalmobwar.json` and use `/mobwar reload` to apply changes

**Examples:**
```json
{
  "scalingEnabled": false,         // Disable all scaling (legacy evolution only)
  "maxTier": 10,                   // Hard cap at tier 10
  "allowModdedScaling": false,     // Only vanilla mobs scale
  "excludedMobs": ["minecraft:villager"]
}
```

---

## 🛡️ Hard Caps, Shields, Effects, and Deterministic Scaling

- **Hard Caps:** All upgrades, stats, and tiers are capped by config (`maxTier`, `maxLevel`). No mob can exceed these limits.
- **Shield Chance:** Some archetype upgrades grant a chance to spawn with a shield (see skill tree and archetype table).
- **Potion Effects:** Upgrades may grant potion effects (e.g., resistance, strength, poison, fire resistance) based on archetype and tier. Effects are only applied if `restrictEffectsToMobTheme` is true, and are deterministic per mob/tier.
- **Deterministic Scaling:** All scaling, upgrades, and effects are deterministic and based on world state, mob archetype, and config. No random stat inflation.
- **Modded Mob Support:** All modded mobs are auto-classified to the closest vanilla archetype and follow its upgrade path.

---

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

**Minion Protection & Control**:
- Attacks anyone who hurts its minions
- Forgives friendly fire from minions
- Targets betrayers who attack other minions
- Coordinated assault: all minions target boss's current target
- **Tethering**: Minions stay within 16 blocks of the boss - if they wander or chase enemies beyond this range, they immediately return to the boss's side

**🆕 Mob Conversion System**:
- **Converts defeated enemies into minions**
- When the boss (not minions) kills a mob, 50% chance to convert instead of killing
- Converted mobs are instantly healed to full health and join the boss
- Dramatic conversion effect with soul, enchant, and portal particles
- Only the BOSS's kills trigger conversion (minion kills don't count)
- Works for all mobs except other Warlords
- Adds up to 20 total minions

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
- **Tethering System**: Minions stay within 16 blocks of the boss - if they chase enemies too far, they automatically return
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

## 🧬 Unified Mob Progression & Skill Tree System (v2.0+)

The new **Evolution System** implements a complex, day-based skill tree that allows mobs to evolve unique traits, gear, and abilities over time.

### 📈 Skill Point Accumulation
Mobs gain "Skill Points" (poi) based on two factors:
1. **World Age (Days):**
   - **Days 0-10:** 0.1 points/day
   - **Days 11-15:** 0.5 points/day
   - **Days 16-20:** 1.0 points/day
   - **Days 21-25:** 1.5 points/day
   - **Days 26-30:** 3.0 points/day
   - **Days 31+:** 5.0 points/day (Max scaling)
2. **Kills:**
   - Mobs gain additional points for every kill they secure.
   - Formula: `Total Points = (Day Points * dayScalingMultiplier) + (Kills * killScalingMultiplier)`

### 🌳 Skill Trees
Mobs spend points on upgrades in specific trees based on their type.

#### 1. General Tree (All Hostile Mobs)
Cost: 2 points per upgrade.
- **Healing:** Regeneration I → V
- **Health Boost:** +2 HP per level (up to +20 HP)
- **Resistance:** Resistance I → III + Fire Resistance
- **Strength:** Strength I → IV
- **Invisibility:** Chance to spawn invisible or gain invisibility bursts
- **Shields:** Chance to equip shields in offhand (Levels 1-5)
- **Equipment Durability:** Per-item upgrades (Mainhand, Offhand, Armor). Starts at 1 durability (broken) and scales to 100% (Level 10). Mobs prioritize upgrading broken gear.
- **Equipment Drop Chance:** Per-item upgrades. Starts at 100% drop chance and decreases by 5% per level (Level 20 = 0% chance).

#### 2. Passive Tree (All Passive Mobs)
Cost: 2 points per upgrade.
- **Healing:** Regeneration I → III
- **Health Boost:** +2 HP per level (up to +6 HP)
- **Resistance:** Resistance I

#### 3. Equipment Trees (Hostile Mobs with Gear)
Mobs upgrade their gear with enchantments. Each upgrade costs points and adds a random enchantment level.
- **Sword Tree:** Sharpness, Fire Aspect, Knockback, Unbreaking, Smite, Bane of Arthropods, Looting.
- **Bow Tree:** Power, Punch, Flame, Infinity, Unbreaking, Potion Arrows (Mastery 1-10).
- **Armor Tree:** Protection, Fire Protection, Blast Protection, Projectile Protection, Thorns, Unbreaking.

#### 4. Special Archetype Trees
- **Zombie Tree (z):**
  - **Infectious Bite:** Inflicts Hunger on hit.
  - **Horde Summon:** Chance to summon reinforcements when hurt.
- **Projectile Tree (pro):**
  - **Piercing:** Arrows pierce through enemies.
  - **Multishot:** Fires multiple projectiles at once.
- **Potion Mastery Trees:**
  - **Witch:** Throws stronger negative potions (Mastery 1-10).
  - **Creeper:** Explosions leave lingering potion clouds (Mastery 1-10).
  - **Cave Spider:** Increases poison level on hit (Mastery 1-10).

### 🛡️ Gear Progression
Mobs automatically equip better base gear as they max out their current gear's potential.

- **Progression Paths:**
  - **Standard:** Wood → Stone → Iron → Diamond → Netherite
  - **Piglin:** Gold Sword → Netherite Sword
  - **Piglin Brute:** Gold Axe → Netherite Axe
  - **Drowned:** Trident (No tier changes)

- **Per-Item Upgrades:**
  - Each equipment slot (Head, Chest, Legs, Feet, Mainhand, Offhand) tracks its progress **independently**.
  - Example: A Zombie might have a Diamond Helmet (because it maxed out Iron Helmet enchants) but still wear Iron Boots.
  - Once a specific item has maxed out all available enchantments for its current tier, it automatically upgrades to the next material tier and resets enchantments.

- **Downgrade on Break:**
  - If a mob's item breaks (reaches 0 durability), it is **not lost**.
  - Instead, it **downgrades** to the previous tier (e.g., Netherite Chestplate → Diamond Chestplate).
  - The downgraded item starts with 0 enchantments.
  - If a Wood or Gold item breaks, it is destroyed permanently.

- **Drop Chance:** Equipment drop chance decreases as the mob becomes more powerful (100% → 1%).

### ⏳ Visual Progression (New!)
- Mobs no longer spawn instantly maxed out.
- Instead, they spawn with a "Skill Point Budget" and spend it over time (approx. 2 upgrades per second).
- You can watch a mob visually evolve: armor pieces appearing one by one, weapons upgrading from Wood to Netherite, and enchantments glowing as they are applied.
- **Smart Spending:** Mobs have a chance to "save" points for expensive upgrades rather than spending everything on cheap ones immediately.

---



*Modded mobs auto-detect the closest vanilla archetype and follow its path.*

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
**Note**: In chaos mode, even multiple Mob Warlord bosses will fight EACH OTHER!

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

### Strategic AI System
- **Smart Targeting**: All mobs (zombies, skeletons, etc.) prioritize Mob Warlords over regular minions
- **Reasoning**: Killing a Warlord instantly kills all 20 of its minions
- **Example**: A zombie fighting against a Warlord + 15 minions will attack the Warlord first
- **Result**: More strategic and realistic combat behavior

### Performance
- **Highly Optimized**: Spatial caching, query rate limiting, and intelligent adaptive scheduling with minimal overhead
- **FPS Impact**: Near-zero overhead - supports unlimited Warlords with no lag
- **Adaptive Delays**: Operations scale from 0.1-1.0s based on real-time server load (fast when quiet, slower when busy)
- **Smart Scheduler**: All operations never overlap (25-100ms dynamic gap) + anti-starvation protection
- **Ultra-Low Overhead**: Delay adjustments only every 50ms, cleanup only when needed (>50 entries)
- **Targeting System (Ultra-Optimized)**:
  - Spatial caching (1s per chunk) reduces queries by 80%
  - Query rate limiting (50/tick) prevents CPU spikes
  - Smart validation ordered by cost (cheapest checks first)
  - Skip sorting for single targets
  - Skip visibility checks for close targets (< 4 blocks)
  - Early player filtering when player targeting disabled
  - Conditional cache cleanup (only when > 30 entries)
- **Staggered Operations**: All Warlords use UUID-based tick offsets (no simultaneous operations)
- **Kill Event Optimization**: Adaptive scheduled evolution (0.1-0.5s), reduced particles, staggered minion deaths
- **Alliance System**: Adaptive scheduled updates (0.15-0.6s) prevent overlap with other operations
- **Particle System**: Optimized density (55% fewer particles) and update intervals
- **Anti-Starvation**: Max 10 queued operations per type, 2s forced execution if waiting too long
- **Smart Validation**: Only checks 3 random minions per cycle
- **Memory Efficient**: Maps auto-cleanup when needed, queue resets, minimal allocations
- Tested with 400+ mod modpacks - runs smoothly even with 10+ Warlords

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

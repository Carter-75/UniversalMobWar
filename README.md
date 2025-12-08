# Universal Mob War v3.1 (Evolution & Scaling)

![Version](https://img.shields.io/badge/version-3.1.0-blue) ![Minecraft](https://img.shields.io/badge/minecraft-1.21.1-green) ![Loader](https://img.shields.io/badge/loader-Fabric-yellow) ![Progressive](https://img.shields.io/badge/costs-100%25%20progressive-brightgreen)

**Battlefield overhaul: mobs fight, evolve, form alliances, and scale with 100% progressive costs!**

**Works with all vanilla & modded mobs. True RPG progression system with 8,000+ point curve!**

---

## 📋 Core Features

### **🔥 Unified Evolution System (100% Progressive Costs)**
Mobs gain **Skill Points** over time and through combat. Points are **spent like currency** with **PROGRESSIVE COSTS** - every upgrade gets more expensive as you level it up!

- **Skill Point Accumulation:**
  - **Day Scaling:** Points earned based on world day (tiered: 0.1 → 0.5 → 1.0 → 1.5 → 3.0 → 5.0 pts/day)
  - **Kill Scaling:** Bonus points for every mob killed (configurable multiplier)
  - **Formula:** `Total Points = (Day Points × dayMultiplier) + (Kills × killMultiplier)`

- **100% Progressive Cost System (v3.1):**
  - **ALL 47 SKILLS** have progressive costs - NO flat costs!
  - Example: Health Boost costs `2/3/4/5/6/7/8/9/10/11 pts` (increases per level)
  - Example: Healing costs `1/2/3/4/5 pts` (progressive, not flat)
  - Example: Durability costs `10/12/14/16/18/20/22/24/26/28 pts` (major investment)
  - **27 Progressive Enchantments** - every enchant level costs more
  - Total progression: **~8,000 points** (2.3x longer than before)
  - True RPG curve: Early levels cheap, endgame expensive
  
- **Point Spending System:**
  - Mobs start with a "point budget" based on day + kills
  - Each upgrade **costs MORE per level** (progressive scaling)
  - Spent points are **deducted** from available points (like money!)
  - **Smart Saving:** 50% chance to skip cheap upgrades (<5 pts) when expensive ones (>=10 pts) are available
  - When all points are spent, mob is fully upgraded
  - New points from days/kills allow more upgrades

- **Visual Progression:**
  - Mobs spawn with equipment and **upgrade over time**
  - Watch armor pieces appear, weapons upgrade Wood → Netherite
  - Enchantment particles show when upgrades are applied
  - Progressive evolution (not instant maxing)

- **Equipment Tier System:**
  - Standard Path: Wood → Stone → Iron → Diamond → Netherite
  - Piglin Path: Gold → Netherite
  - Each tier requires maxing enchantments first
  - Independent upgrades per armor piece (helmet can be diamond while boots are iron)
  - Downgrade mechanic: Broken items drop one tier (Netherite → Diamond)

### **🤝 Alliance System**
- **Dynamic Formation:** Mobs fighting the same target become temporary allies
- **Two Alliance Types:**
  - **Strong (Same Species):** 20s duration, 80% help chance, low break chance
  - **Weak (Different Species):** 5s duration, 30% help chance, high break chance
- **Combat Coordination:** Allies assist each other, prioritize helping friends
- **Visual Indicators:** Purple particle connections between allies

### **⚔️ Combat & AI**
- **Universal Targeting:** Mobs attack any non-allied mob (configurable)
- **Smart Prioritization:** Keep current target → Help allies → Find nearest enemy
- **Player Control:** Toggle player immunity for spectator mode
- **Range Scaling:** Detection range multiplier (0.1× to 5.0×)
- **Stalemate Breaker:** 15s+ fights trigger berserk buffs

### **🎮 Full Customization**
- **Mod Menu Integration:** Click gear icon for instant config changes
- **Config File:** `config/universalmobwar.json` (hot-reloadable with `/mobwar reload`)
- **Performance Tuning:** Batching, caching, async tasks, FPS-based visual throttling
- **Mob Exclusion:** Blacklist specific mobs from system

---

## 🌳 Complete Skill Tree System

### **📊 How Points Work (Currency System)**

```
Example: Zombie on Day 15 with 10 Kills

1. Calculate Total Points:
   Day Points: 6.5 (Days 1-10: 10×0.1 + Days 11-15: 5×0.5 = 1.0 + 2.5)
   Kill Points: 10.0 (10 kills × 1.0 multiplier)
   TOTAL AVAILABLE: 16.5 points

2. Mob Spends Points (Deducted Like Money):
   Starting Balance: 16.5 pts
   
   - Health Boost I → Costs 2 pts → Balance: 14.5 pts
   - Healing I → Costs 1 pt → Balance: 13.5 pts
   - Wooden Sword (Free) → Balance: 13.5 pts
   - Sharpness I → Costs 3 pts → Balance: 10.5 pts
   - Leather Cap (Free) → Balance: 10.5 pts
   - Protection I → Costs 3 pts → Balance: 7.5 pts
   - Health Boost II → Costs 2 pts → Balance: 5.5 pts
   - Strength I → Costs 3 pts → Balance: 2.5 pts
   - Healing II → Costs 1 pt → Balance: 1.5 pts
   
   Mob is now FULLY UPGRADED (1.5 pts remaining, but all affordable upgrades purchased)

3. New Points Unlock More Upgrades:
   Zombie kills 5 more mobs → +5 pts → New Balance: 6.5 pts
   Can now purchase: Sharpness II (3 pts), Fire Aspect I (4 pts), etc.

4. Smart Saving Example:
   Zombie has 50 pts total, 15 pts spent, 35 pts available
   Available Upgrades:
     - Health Boost IV (2 pts) ← CHEAP
     - Healing III (3 pts) ← CHEAP
     - Horde Summon I (10 pts) ← EXPENSIVE
     - Shield Chance I (8 pts) ← MODERATE
   
   50% Chance: Skip cheap upgrades, buy Horde Summon or Shield
   50% Chance: Buy any affordable upgrade (including cheap ones)
   
   Result: Mob intelligently saves for big upgrades sometimes!
```

### **🌐 General Tree (ALL Mobs + Modded)**

**Applies to:** All hostile mobs, all modded mobs without specific classification

**Tier 1: Basic Survival (0-15 points)**
- **Healing (5 levels):** Regeneration I → II, duration 3s → 10s (1-5 pts each)
- **Health Boost (10 levels):** +2 HP per level, max +20 HP (2 pts each)
- **Resistance (3 levels):** Resistance I → II, then + Fire Resistance (4 pts each)
- **Strength (4 levels):** +20% → +80% melee damage (3 pts each)

**Tier 2: Equipment Basics (15-50 points)**
- **Wooden Sword/Axe Equipped** (Free)
- **Sword Enchants:** Sharpness I-V, Fire Aspect I-II, Knockback I-II, Mending, Unbreaking I-III
- **Armor Set:** Leather Helmet/Chest/Legs/Boots (Free)
- **Armor Enchants:** Protection I-IV (all pieces), Thorns I-III, Unbreaking I-III

**Tier 3: Advanced Upgrades (50-150 points)**
- **Speed (3 levels):** +20% → +60% movement speed (6 pts each)
- **Invisibility (12 levels):** Start visible 10m/hour → Nearly permanent invisibility (5 pts each)
- **Shield (5 levels):** 20% → 100% chance to equip shield (8 pts each)
- **Tier Upgrade:** Once 10 enchant levels on weapon → Upgrade to Stone Sword

**Tier 4: Epic Gear (150-300 points)**
- **Iron → Diamond Equipment**
- **Durability Upgrades (10 levels per item):** 1% → 100% durability (5 pts each)
- **Drop Chance Reduction (20 levels per item):** 100% → 5% drop chance (3 pts each)
- **Equipment Break Mastery (5 levels):** 10% → 50% chance to damage enemy gear (15-35 pts)

**Tier 5: Endgame (300+ points)**
- **Netherite Equipment Set**
- **Max Enchantments:** Sharpness V, Protection IV, Thorns III, Mending on all
- **Perfect Durability:** All items 100% durability
- **Near-Zero Drop Chance:** Equipment rarely drops (tier bonus: ×5% for Netherite)

---

### **🧟 Zombie/Undead Tree**

**Applies to:** Zombie, Husk, Drowned, Zombified Piglin, Zoglin, Zombie Villager

**Zombie-Specific Skills:**
- **Horde Summon (5 levels):** 10% → 50% chance to summon reinforcement when damaged
  - Cost: 10, 15, 20, 25, 30 points
  - Reinforcement is same mob type, tagged to prevent infinite spawning
  
- **Infectious Bite (3 levels):** 33% → 100% chance to convert villagers to zombie villagers
  - Cost: 8, 12, 16 points
  
- **Hunger Attack (3 levels):** Inflict Hunger I (5s) → Hunger III (12s) on hit
  - Cost: 6, 10, 14 points

**Example Build (Day 50, 120 kills = 178 pts):**
```
General Tree: Health 40 (20 hearts), Strength IV, Speed II, Resistance III
Equipment: Diamond Sword + Sharpness IV + Fire Aspect II + Looting II
          Full Diamond Armor + Protection III + Thorns II
Zombie Skills: Horde Summon III (30% summon chance)
               Infectious Bite II (66% convert chance)
               Hunger Attack II
Visual: [Lv.25] Zombie ⚔ (Yellow health bar, yellow level tag)
```

---

### **💀 Skeleton/Archer Tree**

**Applies to:** Skeleton, Stray, Bogged

**Ranged-Specific Skills:**
- **Piercing Shot (4 levels):** Arrows pierce 1 → 4 mobs
  - Cost: 8, 12, 16, 20 points
  
- **Bow Potion Mastery (5 levels):** 20% → 100% chance for random potion effect on arrows
  - Cost: 10, 15, 20, 25, 30 points
  - Potion pool: Slowness, Weakness, Poison, Instant Damage, Wither II
  
- **Multishot (3 levels):** Fire 1 → 3 extra arrows (spread ±10° each)
  - Cost: 15, 25, 35 points

**Bow Enchantments:**
- Power I-V (2 pts each)
- Punch I-II (4 pts each)
- Flame I (8 pts)
- Infinity I (12 pts)
- Unbreaking I-III (3 pts each)
- Mending (10 pts)

**Example Build (Day 35, 80 kills = 138 pts):**
```
General Tree: Health 32, Resistance II, Speed I
Equipment: Bow + Power IV + Flame + Infinity + Unbreaking III
          Full Iron Armor + Protection II
Archer Skills: Piercing Shot II (pierce 2 mobs)
               Bow Potion Mastery III (60% poison arrows)
               Multishot I (+1 arrow = 2 total)
Visual: [Lv.20] Skeleton ⚔ (Gold health bar, gold level tag)
```

---

### **💥 Creeper Tree**

**Applies to:** Creeper

**Creeper-Specific Skills:**
- **Creeper Power (5 levels):** Explosion radius 3.0 → 8.0 blocks
  - Cost: 10, 15, 20, 25, 30 points
  - Damage scales with radius
  
- **Creeper Potion Mastery (3 levels):** Explosion creates lingering potion cloud
  - Cost: 12, 18, 24 points
  - Level 1: Slowness (10s)
  - Level 2: + Weakness (10s)
  - Level 3: + Poison (8s)

**Example Build (Day 25, 50 kills = 68 pts):**
```
General Tree: Health 28, Resistance II, Speed II
Equipment: Full Leather Armor + Protection I + Blast Protection II
Creeper Skills: Creeper Power III (6.0 block radius)
                Creeper Potion Mastery II (Slowness + Weakness cloud)
Visual: [Lv.15] Creeper (Yellow health bar, green level tag)
Threat Level: Devastating explosion with debuff cloud
```

---

### **🧙 Witch Tree**

**Applies to:** Witch

**Witch-Specific Skills:**
- **Witch Potion Mastery (5 levels):** Throw potions 20% → 100% faster
  - Cost: 10, 15, 20, 25, 30 points
  - Reduces potion throw cooldown significantly
  
- **Witch Harming Upgrade (3 levels):** Instant Damage I → II, then + Wither II
  - Cost: 12, 18, 24 points
  - Makes damage potions far more lethal

**Example Build (Day 40, 90 kills = 188 pts):**
```
General Tree: Health 36, Resistance III + Fire Res, Speed II
Equipment: Full Iron Armor + Protection III + Blast Protection II
Witch Skills: Witch Potion Mastery IV (80% faster throw rate)
              Witch Harming Upgrade III (Instant Damage II + Wither II)
Visual: [Lv.30] Witch ⚔ (Red health bar, red level tag)
Threat Level: Rapid-fire lethal potions, extremely tanky
```

---

### **🕷️ Cave Spider Tree**

**Applies to:** Cave Spider

**Cave Spider-Specific Skills:**
- **Cave Spider Poison Mastery (5 levels):** Poison I (7s) → Poison II (20s) + Wither I
  - Cost: 8, 12, 16, 20, 24 points
  - Level 1: Default poison (Poison I, 7s)
  - Level 2: Poison II (7s)
  - Level 3: Poison II (10s)
  - Level 4: Poison II (15s)
  - Level 5: Poison II (20s) + Wither I

**Example Build (Day 20, 30 kills = 38 pts):**
```
General Tree: Health 26, Strength II, Speed I
Cave Spider Skills: Poison Mastery III (Poison II for 10s)
Visual: [Lv.10] Cave Spider (Yellow health bar, green tag)
Threat Level: Long-lasting deadly poison
```

---

### **📦 Modded Mob Support**

**How Unknown Mobs Are Handled:**

1. **Classification:**
   - Check if mob has GENERIC_ATTACK_DAMAGE > 0
   - If YES → Category "g" (hostile, gets General Tree)
   - If NO → Category "gp" (passive, gets Passive Tree)

2. **Weapon Detection:**
   - Check main hand item
   - If empty → Add "nw" (no weapon)
   - If bow → Add "bow"
   - If sword → Add "sword"
   - If axe → Add "axe"

3. **Archetype Assignment:**
   - All unknowns default to "universal" archetype
   - Follow General Tree exclusively

4. **Result:**
   - Modded mob "Lycanite's Wendigo" → ["g", "nw"] → Gets General Tree
   - Can upgrade: Health, Strength, Speed, Armor, Resistance, Invisibility
   - Works seamlessly with vanilla system!

---

## 💰 Point Economy Examples

### **Zombie Progression Timeline**

```
DAY 1 (0 kills)
Points: 0.1 total
Status: Not enough for any upgrade (minimum 1 pt needed)
Equipment: Bare hands
Health: 20 (10 hearts)

DAY 5 (3 kills)
Points: 0.5 + 3.0 = 3.5 total
Spent: Health Boost I (2) + Healing I (1) + Strength I (3) = 6 pts
Status: Can only afford 3.5 pts worth → Gets Health Boost I + Healing I
Equipment: Still bare hands
Health: 22 (11 hearts) + Regen I

DAY 10 (8 kills)
Points: 1.0 + 8.0 = 9.0 total
New Spending: 5.5 pts available
Upgrades: + Health Boost II (2) + Strength I (3) → 5 pts spent
Remaining: 0.5 pts (saving for Wooden Sword path)
Equipment: Bare hands
Health: 24 (12 hearts) + Regen I + Strength I

DAY 15 (15 kills)
Points: 6.5 + 15.0 = 21.5 total
Spent So Far: 6.0 pts
New Budget: 15.5 pts
Upgrades: Wooden Sword (0) + Sharpness II (6) + Leather Armor (0) + Protection I (3) + Health Boost III (2)
Equipment: Wooden Sword + Sharpness II, Leather Cap + Protection I
Health: 26 (13 hearts)
Spent: 17.0 total (under budget by spending pattern)

DAY 25 (60 kills)
Points: 18.0 + 60.0 = 78.0 total
Major Milestone: Sword tier upgrade!
- Maxed Wooden Sword enchants (10 levels) → UPGRADE to Stone Sword
- Enchants reset, start fresh
Equipment: Stone Sword + Sharpness II, Full Leather Armor + Protection II
Health: 34 (17 hearts) + Strength II
Spent: ~45 pts

DAY 35 (120 kills)
Points: 58.0 + 120.0 = 178.0 total
Equipment: Diamond Sword + Sharpness IV + Fire Aspect II, Full Iron Armor
Special Skills: Horde Summon III (30% chance), Infectious Bite II (66%)
Health: 42 (21 hearts) + Strength III + Speed II
Spent: ~110 pts
Remaining: 68 pts (saving for Netherite tier)

DAY 50 (300 kills)
Points: 148.0 + 300.0 = 448.0 total
ENDGAME BUILD:
Equipment: Netherite Sword (Sharp V, Fire II, Looting III, Mending, Unbreaking III)
          Full Netherite Armor (Protection IV, Thorns III, Mending, Unbreaking III)
          Shield (100% chance, Unbreaking III)
Special Skills: Horde Summon V (50%), Infectious Bite III (100%), Hunger Attack III,
                Equipment Break V (50%), Invisibility X
Health: 60 (30 hearts) + Strength IV + Speed III + Resistance III + Fire Res
Drop Chance: ~5% (equipment almost never drops)
Durability: 100% on all items
Visual: [Lv.50] Zombie ⚔ (Dark Red health bar)
Spent: 448 pts (fully maxed)
```

---

## 🤝 Alliance System Detailed

### **Alliance Formation Rules**

```
Trigger: Mob A attacks Target X

Step 1: Find nearby mobs (16 block radius)
Step 2: For each nearby mob B:
  - Is mob B attacking same target X?
    YES → Form alliance
    NO → Ignore

Step 3: Determine alliance type:
  - Same species (Zombie + Zombie) → STRONG alliance
  - Different species (Zombie + Skeleton) → WEAK alliance
```

### **Alliance Statistics**

| Attribute | Strong Alliance | Weak Alliance |
|-----------|----------------|---------------|
| Duration | 20 seconds | 5 seconds |
| Formation Chance | 95% | 70% |
| Help Probability | 80% | 30% |
| Help Range | 12.8 blocks (80%) | 8 blocks (50%) |
| Break Chance | 5% per check | 30% per check |
| Target Flexibility | Help even with different targets | Only help if same target |

### **Combat Benefits**

1. **Coordinated Attacks:** Multiple mobs focus-fire single target
2. **Combat Assistance:** Allies switch to help when friend is attacked
3. **No Friendly Fire:** Allies temporarily ignore each other (unless betrayal)

### **Visual Indicators**

- **Purple Portal Particles:** Loyal allies (strong alliance)
- **White Particles:** Weak alliance connections
- **Red Angry Particles:** Betrayers (attacked ally)

---

## ⚙️ Configuration Guide

### **All Settings (Mod Menu or config file)**

#### **General Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `modEnabled` | true | Master switch for entire mod |
| `evolutionSystemEnabled` | true | Enable mob leveling & equipment system |
| `allianceSystemEnabled` | true | Enable alliance formation |
| `ignoreSameSpecies` | true | Same-species don't fight (strong alliances) |
| `targetPlayers` | true | If false, mobs ignore players (spectator mode) |
| `neutralMobsAlwaysAggressive` | false | Make iron golems, endermen always hostile |
| `disableNaturalMobSpawns` | false | Prevent all natural mob spawning |
| `rangeMultiplier` | 1.0 | Detection range multiplier (0.1× to 5.0×) |

#### **Scaling Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `scalingEnabled` | true | Enable day/kill scaling system |
| `dayScalingMultiplier` | 1.0 | Multiplier for day-based points |
| `killScalingMultiplier` | 1.0 | Multiplier for kill-based points |
| `maxTier` | 20 | Maximum scaling tier (legacy, not actively used) |
| `allowBossScaling` | true | Allow scaling for boss mobs |
| `allowModdedScaling` | true | Allow scaling for modded mobs |

#### **Performance Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `performanceMode` | false | Optimize for low-end PCs |
| `enableBatching` | true | Process mobs in batches to reduce lag |
| `enableAsyncTasks` | true | Use background threads for heavy calculations |
| `targetingCacheMs` | 1500 | How long to cache targeting queries (ms) |
| `targetingMaxQueriesPerTick` | 50 | Maximum targeting queries per tick |
| `debugLogging` | false | Enable detailed system logging |
| `debugUpgradeLog` | true | Log upgrade decisions to chat |

#### **Visual Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `showTargetLines` | true | Draw red lines from mobs to targets |
| `showHealthBars` | true | Show health bars above mobs |
| `showMobLabels` | true | Show mob name and level labels |
| `showLevelParticles` | true | Show particles when mobs level up |
| `disableParticles` | false | Disable all mod particles |
| `minFpsForVisuals` | 30 | Disable visuals if FPS drops below this |

#### **Alliance Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `allianceDurationTicks` | 100 | Weak alliance duration (5 seconds) |
| `sameSpeciesAllianceDurationTicks` | 400 | Strong alliance duration (20 seconds) |
| `allianceRange` | 16.0 | Detection range for alliance formation |
| `allianceBreakChance` | 0.3 | Weak alliance break probability (30%) |
| `sameSpeciesAllianceBreakChance` | 0.05 | Strong alliance break probability (5%) |

#### **Exclusion Settings**
| Setting | Default | Description |
|---------|---------|-------------|
| `excludedMobs` | [] | List of entity IDs to exclude (e.g., `["minecraft:villager"]`) |

### **Config File Location**

`config/universalmobwar.json`

**Example Configuration:**
```json
{
  "modEnabled": true,
  "evolutionSystemEnabled": true,
  "scalingEnabled": true,
  "dayScalingMultiplier": 2.0,
  "killScalingMultiplier": 1.5,
  "targetPlayers": false,
  "performanceMode": false,
  "debugUpgradeLog": false,
  "excludedMobs": ["minecraft:villager", "minecraft:allay"]
}
```

**Apply Changes:** Use `/mobwar reload` after editing config file.

---

## 🎮 Commands

```
/mobwar help                    - Show all commands
/mobwar stats                   - View nearby mob levels and stats
/mobwar summon warlord          - Summon Mob Warlord boss (OP only)
/mobwar raid forceboss          - Force Warlord in next raid (OP only)
/mobwar reload                  - Reload config file
```

---

## 🔧 Technical Details

### **Compatibility**
- **Minecraft:** 1.21.1
- **Fabric Loader:** ≥0.15.10
- **Fabric API:** ≥0.102.0+1.21.1
- **Mod Menu:** Optional (≥11.0.2 for config GUI)
- **Cloth Config:** Optional (≥15.0.140 for advanced config)

### **Modded Mob Support**
- Works with ALL modded mobs automatically
- Auto-detects closest vanilla archetype
- Falls back to General Tree if no match
- Tested with 400+ mod modpacks

### **Performance Optimizations**
- **Spatial Caching:** Targeting queries cached 1.5s per chunk (80% query reduction)
- **Staggered Updates:** UUID-based tick offsets prevent simultaneous operations
- **Query Rate Limiting:** Max 50 queries/tick prevents CPU spikes
- **Adaptive Batching:** Processes multiple mobs per tick when possible
- **Distance Culling:** Only upgrades mobs near players (64 block radius)
- **Memory Efficient:** Automatic cleanup of dead mob UUIDs every 60s

### **Data Persistence**
- Mob levels, kills, and upgrades saved in entity NBT
- Survives chunk unload/reload
- Survives server restart
- Alliance data is session-based (cleared on restart)

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/) for Minecraft 1.21.1
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (version 0.102.0+)
3. Install [Mod Menu](https://modrinth.com/mod/modmenu) (optional, for config GUI)
4. Install [Cloth Config](https://modrinth.com/mod/cloth-config) (optional, for advanced config)
5. Download `universal-mob-war-3.1.0.jar`
6. Place in `mods` folder
7. Launch game!

Config file auto-generates at `config/universalmobwar.json`

---

## 🔨 Building from Source

```bash
git clone https://github.com/Carter-75/UniversalMobWar.git
cd UniversalMobWar
./gradlew build  # Linux/Mac
gradlew.bat build  # Windows
```

Compiled jar in `build/libs/`

---

## 💡 Tips & Strategies

### **Maximum Chaos Mode**
```
Config:
  ignoreSameSpecies: false
  neutralMobsAlwaysAggressive: true
  rangeMultiplier: 5.0
  
Result: Every mob attacks every other mob at 5× range!
```

### **Safe Spectator Mode**
```
Config:
  targetPlayers: false
  
Result: Watch mobs fight without being targeted!
```

### **Rapid Evolution**
```
Config:
  dayScalingMultiplier: 3.0
  killScalingMultiplier: 2.0
  
Result: Mobs gain points 3× faster from days, 2× from kills!
```

### **Survival Challenge**
```
Config:
  dayScalingMultiplier: 0.5
  killScalingMultiplier: 0.5
  targetPlayers: true
  
Result: Slow progression, you're a target, challenging early game!
```

---

## 📄 License & Usage Rights

**Copyright © 2024 Carter. All rights reserved.**

### ✅ You MAY:
- Use this mod in modpacks (with attribution)
- Use on servers (public/private)
- Create videos/content featuring this mod
- Share official download link

### ❌ You MAY NOT:
- Redistribute the .jar file directly
- Modify or decompile
- Claim as your own
- Remove credits

### 📋 Attribution Required:
```
Universal Mob War by Carter
Download: [Official Download Page Link]
```

---

## 🐛 Bug Reports

Report with:
- Minecraft version
- Fabric Loader version
- List of other mods
- Steps to reproduce
- Screenshots/logs if applicable

---

**Enjoy watching mobs evolve into unstoppable warriors!** 🎉
served.**

### ✅ You MAY:
- Use this mod in modpacks (with attribution)
- Use on servers (public/private)
- Create videos/content featuring this mod
- Share official download link

### ❌ You MAY NOT:
- Redistribute the .jar file directly
- Modify or decompile
- Claim as your own
- Remove credits

### 📋 Attribution Required:
```
Universal Mob War by Carter
Download: [Official Download Page Link]
```

---

## 🐛 Bug Reports

Report with:
- Minecraft version
- Fabric Loader version
- List of other mods
- Steps to reproduce
- Screenshots/logs if applicable

---

**Enjoy watching mobs evolve into unstoppable warriors!** 🎉

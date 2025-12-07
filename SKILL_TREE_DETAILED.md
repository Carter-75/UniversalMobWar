# Universal Mob War - Complete Skill Tree Documentation

## 🎯 Core Concept: Point-Based Currency System

**Skill Points** function exactly like money in an RPG:
- **Earned** from days alive + kills made
- **Spent** on upgrades (each costs points)
- **Deducted** from available balance when purchased
- **Tracked** via `spentPoints` variable (running total)
- **Budget** = `totalPoints - spentPoints`

---

## 💰 Point Accumulation Formula

```
Total Points = Day Scaling Points + Kill Scaling Points

Day Scaling Points = Base Day Points × dayScalingMultiplier
Kill Scaling Points = Kill Count × killScalingMultiplier
```

### **Day Scaling (Tiered System)**

| Day Range | Points/Day | Cumulative Example |
|-----------|------------|-------------------|
| 0-10      | 0.1        | Day 10 = 1.0 pts |
| 11-15     | 0.5        | Day 15 = 3.5 pts (1.0 + 2.5) |
| 16-20     | 1.0        | Day 20 = 8.5 pts (3.5 + 5.0) |
| 21-25     | 1.5        | Day 25 = 16.0 pts (8.5 + 7.5) |
| 26-30     | 3.0        | Day 30 = 31.0 pts (16.0 + 15.0) |
| 31+       | 5.0        | Day 35 = 56.0 pts (31.0 + 25.0) |

### **Example Calculation**

```
Zombie on Day 35 with 100 kills:

Day Points:
  Days 1-10:  10 × 0.1 = 1.0
  Days 11-15:  5 × 0.5 = 2.5
  Days 16-20:  5 × 1.0 = 5.0
  Days 21-25:  5 × 1.5 = 7.5
  Days 26-30:  5 × 3.0 = 15.0
  Days 31-35:  5 × 5.0 = 25.0
  Total Day Points = 56.0

Kill Points:
  100 × 1.0 (default multiplier) = 100.0

Total Available Points = 56.0 + 100.0 = 156.0 points

If mob has spentPoints = 120.0:
  Available to Spend = 156.0 - 120.0 = 36.0 points
```

---

## 🌳 Complete Skill Trees

### **🌐 GENERAL TREE (Category "g")** 
**Applies to:** All hostile mobs + modded mobs with attack damage

#### **Tier 1: Basic Survival (Cost: 1-5 points each)**

```
HEALING (5 levels)
├─ Level 1: Regeneration I for 3 seconds (1 point)
├─ Level 2: Regeneration I for 5 seconds (2 points)
├─ Level 3: Regeneration II for 5 seconds (3 points)
├─ Level 4: Regeneration II for 8 seconds (4 points)
└─ Level 5: Regeneration II for 10 seconds (5 points)
Total Cost: 15 points
Effect: Permanent status effect

HEALTH_BOOST (10 levels)
├─ Each Level: +2 max health (1 heart) (2 points each)
└─ Level 10: +20 max health (10 hearts)
Total Cost: 20 points
Effect: Increases GENERIC_MAX_HEALTH attribute

RESISTANCE (3 levels)
├─ Level 1: Resistance I permanent (4 points)
├─ Level 2: Resistance II permanent (4 points)
└─ Level 3: Resistance II + Fire Resistance permanent (4 points)
Total Cost: 12 points
Effect: Permanent damage reduction

STRENGTH (4 levels)
├─ Level 1: +20% melee damage (3 points)
├─ Level 2: +40% melee damage (3 points)
├─ Level 3: +60% melee damage (3 points)
└─ Level 4: +80% melee damage (3 points)
Total Cost: 12 points
Effect: Multiplies GENERIC_ATTACK_DAMAGE attribute
```

#### **Tier 2: Mobility & Stealth (Cost: 5-6 points each)**

```
SPEED (3 levels)
├─ Level 1: +20% movement speed (6 points)
├─ Level 2: +40% movement speed (6 points)
└─ Level 3: +60% movement speed (6 points)
Total Cost: 18 points
Effect: Increases GENERIC_MOVEMENT_SPEED attribute

INVISIBILITY_MASTERY (12 levels)
├─ Level 1: Invisible 10 minutes/hour (50 min visible) (5 points)
├─ Level 2: Invisible 9 minutes/hour (5 points)
├─ Level 3: Invisible 8 minutes/hour (5 points)
├─ Level 4: Invisible 7 minutes/hour (5 points)
├─ Level 5: Invisible 6 minutes/hour (5 points)
├─ Level 6: Invisible 5 minutes/hour (5 points)
├─ Level 7: Invisible 4 minutes/hour (5 points)
├─ Level 8: Invisible 3 minutes/hour (5 points)
├─ Level 9: Invisible 2 minutes/hour (5 points)
├─ Level 10: Invisible 1 minute/hour (5 points)
├─ Level 11: Invisible 30 seconds/hour (5 points)
└─ Level 12: Invisible 15 seconds/hour (nearly permanent) (5 points)
Total Cost: 60 points
Effect: Toggles invisibility on timer (InvisibilitySkillMixin)
```

#### **Tier 3: Equipment (Cost: Varies, enchants 3-10 pts)**

```
SWORD PROGRESSION
Base Path: Wood → Stone → Iron → Diamond → Netherite
Piglin Path: Gold → Netherite
Upgrade Trigger: Once 10 total enchant levels accumulated

SWORD ENCHANTMENTS (Random selection):
├─ Sharpness I-V (3 points per level) [Max: 15 pts]
├─ Fire Aspect I-II (4 points per level) [Max: 8 pts]
├─ Knockback I-II (3 points per level) [Max: 6 pts]
├─ Smite I-V (3 points per level) [Max: 15 pts]
├─ Bane of Arthropods I-V (3 points per level) [Max: 15 pts]
├─ Looting I-III (5 points per level) [Max: 15 pts]
├─ Unbreaking I-III (3 points per level) [Max: 9 pts]
└─ Mending (10 points)

ARMOR PROGRESSION (Per Piece)
Base Path: Leather → Chain → Iron → Diamond → Netherite
Upgrade Trigger: Once 10 total enchant levels per piece

ARMOR ENCHANTMENTS (All Pieces):
├─ Protection I-IV (3 points per level) [Max: 12 pts]
├─ Fire Protection I-IV (3 points per level) [Max: 12 pts]
├─ Blast Protection I-IV (3 points per level) [Max: 12 pts]
├─ Projectile Protection I-IV (3 points per level) [Max: 12 pts]
├─ Thorns I-III (4 points per level) [Max: 12 pts]
├─ Unbreaking I-III (3 points per level) [Max: 9 pts]
└─ Mending (10 points)

HELMET-SPECIFIC:
├─ Aqua Affinity (6 points)
└─ Respiration I-III (4 points per level) [Max: 12 pts]

LEGGINGS-SPECIFIC:
└─ Swift Sneak I-III (6 points per level) [Max: 18 pts]

BOOTS-SPECIFIC:
├─ Feather Falling I-IV (3 points per level) [Max: 12 pts]
├─ Depth Strider I-III (4 points per level) [Max: 12 pts]
├─ Soul Speed I-III (5 points per level) [Max: 15 pts]
└─ Frost Walker I-II (6 points per level) [Max: 12 pts]

SHIELD CHANCE (5 levels)
├─ Level 1: 20% chance to equip shield (8 points)
├─ Level 2: 40% chance (8 points)
├─ Level 3: 60% chance (8 points)
├─ Level 4: 80% chance (8 points)
└─ Level 5: 100% chance (always has shield) (8 points)
Total Cost: 40 points
Shield inherits Unbreaking/Mending from chest armor enchants
```

#### **Tier 4: Equipment Mastery (Cost: 3-5 points per level)**

```
DURABILITY UPGRADES (Per Equipment Slot: 10 levels each)
├─ Level 0: 1 durability point (almost broken) (Mob prioritizes fixing)
├─ Level 1: 10% durability restored (5 points)
├─ Level 2: 20% durability (5 points)
├─ Level 3: 30% durability (5 points)
├─ Level 4: 40% durability (5 points)
├─ Level 5: 50% durability (5 points)
├─ Level 6: 60% durability (5 points)
├─ Level 7: 70% durability (5 points)
├─ Level 8: 80% durability (5 points)
├─ Level 9: 90% durability (5 points)
└─ Level 10: 100% durability (brand new) (5 points)
Total Cost per Slot: 50 points
Slots: Mainhand, Offhand, Head, Chest, Legs, Feet (300 pts total for all)

DROP_CHANCE REDUCTION (Per Equipment Slot: 20 levels each)
├─ Level 0: 100% drop chance (always drops) (Base)
├─ Level 1: 95% drop chance (-5%) (3 points)
├─ Level 2: 90% drop chance (3 points)
...
├─ Level 19: 10% drop chance (3 points)
└─ Level 20: 5% drop chance (near-zero) (3 points)
Total Cost per Slot: 60 points
Slots: Mainhand, Offhand, Head, Chest, Legs, Feet (360 pts total for all)

TIER BONUS MULTIPLIER (Applied to drop chance):
├─ Tier 0 (Wood/Leather): Final Drop = Calculated × 0.50
├─ Tier 1 (Stone/Chain): Final Drop = Calculated × 0.40
├─ Tier 2 (Iron): Final Drop = Calculated × 0.30
├─ Tier 3 (Gold): Final Drop = Calculated × 0.20
├─ Tier 4 (Diamond): Final Drop = Calculated × 0.10
└─ Tier 5 (Netherite): Final Drop = Calculated × 0.05

Example: Level 10 drop (50% base) + Netherite tier:
  50% × 0.05 = 2.5% final drop chance
```

#### **Tier 5: Advanced Skills (Cost: 15-35 points)**

```
EQUIPMENT_BREAK_MASTERY (5 levels)
├─ Level 1: 10% chance to damage enemy equipment on hit (15 points)
├─ Level 2: 20% chance (20 points)
├─ Level 3: 30% chance + damages weapons too (25 points)
├─ Level 4: 40% chance (30 points)
└─ Level 5: 50% chance + double damage to items (35 points)
Total Cost: 125 points
Effect: Damages enemy's armor/weapons (EquipmentBreakMixin)
```

---

### **🧟 ZOMBIE TREE (Category "z")**
**Applies to:** Zombie, Husk, Drowned, Zombified Piglin, Zoglin, Zombie Villager

```
HORDE_SUMMON (5 levels)
├─ Level 1: 10% chance to summon 1 reinforcement when damaged (10 points)
├─ Level 2: 20% chance (15 points)
├─ Level 3: 30% chance (20 points)
├─ Level 4: 40% chance (25 points)
└─ Level 5: 50% chance (30 points)
Total Cost: 100 points
Mechanics:
  - Triggers on taking damage
  - Summons same mob type (e.g., Zombie summons Zombie)
  - Reinforcement tagged "umw_horde_reinforcement" to prevent infinite recursion
  - Reinforcement has 'horde_summon' skill disabled (level = 0)

INFECTIOUS_BITE (3 levels)
├─ Level 1: 33% chance to convert villager to zombie villager on kill (8 points)
├─ Level 2: 66% chance (12 points)
└─ Level 3: 100% chance (always converts) (16 points)
Total Cost: 36 points
Effect: InfectiousBiteMixin converts villagers on death

HUNGER_ATTACK (3 levels)
├─ Level 1: Inflict Hunger I for 5 seconds on hit (6 points)
├─ Level 2: Inflict Hunger II for 8 seconds on hit (10 points)
└─ Level 3: Inflict Hunger III for 12 seconds on hit (14 points)
Total Cost: 30 points
Effect: InfectiousBiteMixin applies Hunger status on attack
```

**Zombie Example Build (Day 50, 300 kills = 448 pts):**
```
Spent Breakdown:
  General Tree: Health Boost X (20 pts), Healing V (15 pts), 
                Strength IV (12 pts), Resistance III (12 pts),
                Speed III (18 pts), Invisibility X (50 pts) = 127 pts
  
  Sword Path: Netherite Sword + all enchants maxed = ~80 pts
  
  Armor Path: Full Netherite Armor + all enchants maxed = ~120 pts
  
  Shield: Level 5 (100% chance) = 40 pts
  
  Durability: All slots at 100% = 60 pts (prioritized)
  
  Drop Chance: All slots at 0% = 60 pts
  
  Zombie Skills: Horde Summon V (100 pts), 
                 Infectious Bite III (36 pts),
                 Hunger Attack III (30 pts) = 166 pts

Total Spent: 127 + 80 + 120 + 40 + 60 + 60 + 166 = 653 pts
ERROR: Over budget! Mob cannot afford everything.

Actual Allocation (Priority System):
  1. Critical Survival: Health, Healing, Resistance = 127 pts
  2. Equipment Basics: Netherite Sword + Armor = ~200 pts
  3. Zombie Core: Horde V, Infectious III, Hunger III = 166 pts
  4. Remaining points for drop chance/durability
Total: 448 pts exactly (mob fully upgraded)
```

---

### **💀 SKELETON TREE (Categories "bow" + "pro")**
**Applies to:** Skeleton, Stray, Bogged

```
BOW ENCHANTMENTS (Category "bow")
├─ Power I-V (2 points per level) [Max: 10 pts]
├─ Punch I-II (4 points per level) [Max: 8 pts]
├─ Flame (8 points)
├─ Infinity (12 points)
├─ Unbreaking I-III (3 points per level) [Max: 9 pts]
└─ Mending (10 points)
Total Max Cost: ~57 points

NOTE: Bows do NOT have tier progression (no Wood → Stone bow)
      All enchants applied to single bow item

PIERCING_SHOT (4 levels) (Category "pro")
├─ Level 1: Arrows pierce through 1 mob (8 points)
├─ Level 2: Pierce through 2 mobs (12 points)
├─ Level 3: Pierce through 3 mobs (16 points)
└─ Level 4: Pierce through 4 mobs (20 points)
Total Cost: 56 points
Effect: ProjectileSkillMixin sets pierce level on arrow

BOW_POTION_MASTERY (5 levels) (Category "pro")
├─ Level 1: 20% chance for random potion effect (10 points)
├─ Level 2: 40% chance (15 points)
├─ Level 3: 60% chance (20 points)
├─ Level 4: 80% chance (25 points)
└─ Level 5: 100% chance (always) (30 points)
Total Cost: 100 points
Potion Pool (random selection per arrow):
  - Slowness (20 seconds)
  - Weakness (20 seconds)
  - Poison (10 seconds)
  - Instant Damage
  - Wither II (5 seconds)
Effect: BowPotionMixin adds random potion to arrow on spawn

MULTISHOT (3 levels) (Category "pro")
├─ Level 1: Fire +1 extra arrow (2 total) (15 points)
├─ Level 2: Fire +2 extra arrows (3 total) (25 points)
└─ Level 3: Fire +3 extra arrows (4 total) (35 points)
Total Cost: 75 points
Mechanics:
  - Extra arrows spread ±10° per arrow
  - ProjectileSkillMixin spawns copies with spread velocity
  - Tagged "umw_multishot_extra" to prevent infinite recursion
  - Tagged "umw_processed" to prevent re-processing
```

**Skeleton Example Build (Day 35, 80 kills = 138 pts):**
```
Spent Breakdown:
  General: Health 32 (6 × 2 pts), Resistance II (8 pts), Speed I (6 pts) = 26 pts
  Bow Enchants: Power IV (8 pts), Flame (8 pts), 
                Infinity (12 pts), Unbreaking III (9 pts) = 37 pts
  Armor: Full Iron Armor + Protection II all pieces = ~40 pts
  Archer Skills: Piercing II (20 pts), Bow Potion III (45 pts), Multishot I (15 pts) = 80 pts

Total Spent: 26 + 37 + 40 + 80 = 183 pts
ERROR: Over budget!

Actual: Skip Multishot, reduce potion to level 2
  26 + 37 + 40 + 20 + 25 = 148 pts → Still over!

Final Allocation:
  General: 26 pts
  Bow: 37 pts
  Armor: 30 pts (partial)
  Skills: Piercing II (20 pts), Bow Potion II (25 pts) = 45 pts
Total: 138 pts exactly
```

---

### **💥 CREEPER TREE (Category "creeper")**
**Applies to:** Creeper only

```
CREEPER_POWER (5 levels)
├─ Level 1: Explosion radius 3.0 → 4.0 blocks (10 points)
├─ Level 2: Explosion radius 5.0 blocks (15 points)
├─ Level 3: Explosion radius 6.0 blocks (20 points)
├─ Level 4: Explosion radius 7.0 blocks (25 points)
└─ Level 5: Explosion radius 8.0 blocks (30 points)
Total Cost: 100 points
Effect: CreeperExplosionMixin increases explosion power

CREEPER_POTION_MASTERY (3 levels)
├─ Level 1: Explosion creates Slowness lingering cloud (10s) (12 points)
├─ Level 2: + Weakness lingering cloud (10s) (18 points)
└─ Level 3: + Poison lingering cloud (8s) (24 points)
Total Cost: 54 points
Effect: CreeperExplosionMixin spawns potion cloud at blast site
```

---

### **🧙 WITCH TREE (Category "witch")**
**Applies to:** Witch only

```
WITCH_POTION_MASTERY (5 levels)
├─ Level 1: Throw potions 20% faster (10 points)
├─ Level 2: 40% faster (15 points)
├─ Level 3: 60% faster + longer duration (20 points)
├─ Level 4: 80% faster (25 points)
└─ Level 5: 100% faster (double throw rate) (30 points)
Total Cost: 100 points
Effect: WitchPotionMixin reduces potion throw cooldown

WITCH_HARMING_UPGRADE (3 levels)
├─ Level 1: Instant Damage I potions (12 points)
├─ Level 2: Instant Damage II potions (18 points)
└─ Level 3: + Wither II effect added (24 points)
Total Cost: 54 points
Effect: WitchPotionMixin upgrades damage potion level
```

---

### **🕷️ CAVE SPIDER TREE (Category "cave_spider")**
**Applies to:** Cave Spider only

```
CAVE_SPIDER_POISON_MASTERY (5 levels)
├─ Level 1: Poison I for 7 seconds (default vanilla) (8 points)
├─ Level 2: Poison II for 7 seconds (12 points)
├─ Level 3: Poison II for 10 seconds (16 points)
├─ Level 4: Poison II for 15 seconds (20 points)
└─ Level 5: Poison II for 20 seconds + Wither I (24 points)
Total Cost: 80 points
Effect: CaveSpiderMixin applies upgraded poison on attack
```

---

### **🐷 PIGLIN SPECIAL MECHANICS**

Piglins have **deterministic randomness** for weapon choice:

```
At Spawn:
  Check mob.getUuid().hashCode() % 2
  
  If EVEN (0): Add category "sword" → Melee Piglin
  If ODD (1): Add category "bow" → Ranged Piglin

This ensures:
  - Same piglin always gets same weapon type across reloads
  - 50/50 split between melee and ranged
  - No random changes on chunk reload
```

**Piglin Equipment Paths:**
- **Melee Piglin:** Gold Sword → Netherite Sword (skips other tiers)
- **Ranged Piglin:** Bow + standard bow enchants
- **Piglin Brute:** Gold Axe → Netherite Axe (always melee, always axe)

---

### **📦 MODDED MOB TREE (Category "g" + "nw")**

**How Unknown Mobs Are Classified:**

```
Step 1: Get mob's translation key
  Example: "entity.lycanitesmobs.wendigo"

Step 2: Extract mob name
  Remove "entity." and mod prefix → "wendigo"

Step 3: Check if in vanilla list
  NOT FOUND → Fallback to attribute detection

Step 4: Check GENERIC_ATTACK_DAMAGE attribute
  Has attribute? → Category "g" (hostile)
  No attribute? → Category "gp" (passive)

Step 5: Check main hand item
  Empty hand? → Add category "nw" (no weapon)
  Bow? → Add category "bow"
  Sword? → Add category "sword"

Step 6: Assign archetype
  If has "g" → Archetype = "universal"
  If has "gp" → Archetype = "passive"

Result: Modded mob follows GENERAL TREE exclusively
```

**Modded Mob Example: "Terra's Gorgon"**
```
Translation Key: "entity.terra.gorgon"
Attack Damage: 8.0 (has attribute)
Main Hand: Empty

Classification: ["g", "nw"]
Archetype: "universal"

Available Upgrades:
  ✓ General Tree: Health, Healing, Resistance, Strength, Speed, Invisibility
  ✓ Armor Progression: Leather → Netherite (all pieces)
  ✓ Equipment Durability/Drop Chance
  ✗ Weapon Tree: Skipped (has "nw" tag)
  ✗ Special Skills: None (not zombie/skeleton/etc.)

Progression: Same as vanilla hostile mob without weapon
```

---

## 💸 Point Spending Mechanics

### **How Upgrades Are Chosen**

```
Every Tick (for mobs with unspent points):

1. Load current state from PowerProfile
   - Check spentPoints vs totalPoints
   - If spentPoints >= totalPoints → STOP (fully upgraded)

2. Calculate available budget
   - availablePoints = totalPoints - spentPoints

3. Collect all possible upgrades
   - General upgrades (health, strength, etc.)
   - Category upgrades (zombie, skeleton, etc.)
   - Equipment upgrades (enchants, tiers)
   - Check each upgrade's cost vs available points

4. Filter affordable upgrades
   - Keep only upgrades where cost <= availablePoints
   - Separate into "cheap" (<5 pts) and "expensive" (>=10 pts)

5. SMART SAVING MECHANIC (50% chance):
   - If expensive upgrades (>=10 pts) are available
   - AND mob still has more points coming
   - 50% chance to SKIP cheap upgrades (<5 pts)
   - This lets mob save for big purchases!
   
   Example:
     Available: Health Boost (2 pts), Horde Summon (10 pts)
     50% chance: Only consider Horde Summon
     50% chance: Consider both options
     
   Result: Mob doesn't waste all points on tiny upgrades

6. Choose one upgrade randomly
   - From filtered list (possibly excluding cheap ones)
   - Apply upgrade immediately

7. Deduct cost from budget
   - spentPoints += upgradeCost
   - Save new spentPoints to PowerProfile

8. Apply visual effects
   - Equipment appears/changes on mob
   - Enchantment particles spawn
   - Health bar updates

9. Repeat next tick (if points remaining)
```

### **Upgrade Priority System**

Mob AI prioritizes certain upgrades:

```
PRIORITY 1 (Emergency):
  - Equipment durability < 10% → Prioritize fixing
  - Health < 50% max → Prioritize Health Boost
  
PRIORITY 2 (Core Survival):
  - Basic health, healing, resistance
  - Costs 1-5 points (affordable early)
  
PRIORITY 3 (Equipment Foundation):
  - Wooden/Leather equipment (free)
  - First tier enchants
  
PRIORITY 4 (Balanced Growth):
  - Random selection from all categories
  - Mob becomes well-rounded
  
PRIORITY 5 (Endgame):
  - Tier upgrades (requires 10 enchant levels first)
  - Expensive skills (multishot, invisibility)
  - Drop chance reduction
```

### **Tier Upgrade Trigger**

```
When mob accumulates 10 total enchant levels on an item:

Example: Wooden Sword
  - Sharpness III (3 levels)
  - Fire Aspect II (2 levels)
  - Knockback II (2 levels)
  - Unbreaking III (3 levels)
  Total: 10 enchant levels

Trigger: checkTierUpgrades() called
  → Remove Wooden Sword
  → Equip Stone Sword
  → Reset enchant levels to 0
  → Start accumulating enchants again

Progression continues: Stone → Iron → Diamond → Netherite
Each tier requires 10 enchant levels to unlock next
```

---

## 📊 Complete Example: Zombie Day 1 → Day 50

```
DAY 1, KILL 0
Available: 0.1 pts
Spent: 0 pts
Status: NOT ENOUGH POINTS (min 1 pt needed)
Equipment: Bare hands
Stats: 20 HP, 2.5 damage

DAY 5, KILL 3
Available: 0.5 + 3.0 = 3.5 pts
Spent: 0 pts
Upgrades:
  - Health Boost I (2 pts) → Spent: 2.0
  - Healing I (1 pt) → Spent: 3.0
Remaining: 0.5 pts
Equipment: Bare hands
Stats: 22 HP + Regen I, 2.5 damage

DAY 10, KILL 10
Available: 1.0 + 10.0 = 11.0 pts
Spent: 3.0 pts
Budget: 8.0 pts
Upgrades:
  - Health Boost II (2 pts) → Spent: 5.0
  - Strength I (3 pts) → Spent: 8.0
  - Wooden Sword (0 pts) → Spent: 8.0
Remaining: 3.0 pts (saving for Sharpness)
Equipment: Wooden Sword
Stats: 24 HP + Regen I, 3.0 damage (+20% Strength)

DAY 15, KILL 20
Available: 6.5 + 20.0 = 26.5 pts
Spent: 8.0 pts
Budget: 18.5 pts
Upgrades:
  - Sharpness II (6 pts) → Spent: 14.0
  - Leather Cap (0 pts)
  - Protection I (3 pts) → Spent: 17.0
  - Healing II (2 pts) → Spent: 19.0
Remaining: 7.5 pts
Equipment: Wooden Sword + Sharpness II, Leather Cap + Protection I
Stats: 24 HP + Regen I (5s), 3.0 dmg, +20% Str

DAY 25, KILL 60
Available: 18.0 + 60.0 = 78.0 pts
Spent: 19.0 pts
Budget: 59.0 pts
Major Event: Sword maxed enchants (10 levels) → TIER UPGRADE!
  - Wooden Sword → Stone Sword (enchants reset)
Upgrades:
  - Health Boost III-V (6 pts) → Spent: 25.0
  - Strength II (3 pts) → Spent: 28.0
  - Stone Sword + Sharpness III (9 pts) → Spent: 37.0
  - Full Leather Armor + Enchants → Spent: ~55.0
  - Horde Summon I (10 pts) → Spent: 65.0
Remaining: 13.0 pts
Equipment: Stone Sword + Sharpness III, Full Leather Armor + Protection II
Stats: 30 HP + Regen II, 3.6 dmg (+40% Str)
Special: 10% Horde Summon chance

DAY 35, KILL 120
Available: 58.0 + 120.0 = 178.0 pts
Spent: 65.0 pts
Budget: 113.0 pts
Progression: Stone → Iron Sword, Leather → Iron Armor
Upgrades:
  - Iron Sword + Sharpness IV + Fire II → ~25 pts
  - Full Iron Armor + Protection III → ~40 pts
  - Health Boost VI-VIII (6 pts) → Spent: ~136.0
  - Speed II (6 pts) → Spent: ~142.0
  - Horde Summon III (20+15 pts) → Spent: ~177.0
Equipment: Iron Sword + Sharpness IV + Fire II, Full Iron Armor
Stats: 36 HP + Regen II, 4.2 dmg (+40% Str), +40% Speed
Special: 30% Horde Summon, 25% chance shield

DAY 50, KILL 300
Available: 148.0 + 300.0 = 448.0 pts
Spent: 177.0 pts
Budget: 271.0 pts
ENDGAME ALLOCATION:
  - Netherite Sword (all enchants maxed) → ~80 pts
  - Full Netherite Armor (all enchants) → ~120 pts
  - Shield 100% chance → 40 pts
  - Horde V + Infectious III + Hunger III → 166 pts
  - Durability all slots 100% → 60 pts (prioritized 6 slots × 10 pts)
  - Drop Chance reduction → 60 pts
  - Health Boost X, Strength IV, Speed III, Resistance III, Invisibility X → ~130 pts
Total Spent: 448.0 pts (FULLY MAXED)
Equipment: Netherite Sword (Sharp V, Fire II, Looting III, Mending, Unbreaking III)
          Full Netherite Armor (Prot IV, Thorns III, Mending, Unbreaking III)
          Shield (Unbreaking III)
Stats: 60 HP (30 hearts), 6.0 base dmg × 1.8 (Str IV) = 10.8 dmg before Sharp V
Special: 50% Horde Summon, 100% Infectious Bite, Hunger III, Invis 15s/hr
Drop: 2-5% chance (near zero with Netherite tier bonus)
Visual: [Lv.50] Zombie ⚔ (Dark Red bar, unstoppable)
```

---

## 🎯 Key Takeaways

1. **Points = Currency:** Spent and deducted like money in an RPG
2. **Budget System:** `availablePoints = totalPoints - spentPoints`
3. **Incremental Progression:** Mobs upgrade 1-20 times per second (not instant)
4. **Visual Evolution:** Equipment appears, tiers upgrade, enchants glow
5. **Tier Gating:** Must max enchants (10 levels) before tier upgrade
6. **Independent Slots:** Each armor piece upgrades separately
7. **Modded Support:** Unknown mobs get General Tree automatically
8. **Deterministic:** Same mob UUID = same progression path (no random stat inflation)

---

**This is a complete RPG progression system embedded into Minecraft mobs!** 🎮

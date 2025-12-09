# Universal Mob War - Progression System Guide

## Overview

The Scaling System allows mobs to get stronger over time by earning and spending points on upgrades. This is a **fully JSON-driven system** - all upgrade costs and effects are defined in JSON config files.

## Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                        SCALING SYSTEM                                │
│                                                                      │
│  ┌──────────────────┐    ┌──────────────────┐    ┌───────────────┐  │
│  │  MobDataMixin    │───▶│  ScalingSystem   │───▶│ mob_configs/  │  │
│  │  (calls on tick) │    │  (ONE file)      │    │ *.json        │  │
│  └──────────────────┘    └──────────────────┘    └───────────────┘  │
│                                                                      │
│  Flow:                                                               │
│  1. MobDataMixin.mobTick() called for every mob                     │
│  2. ScalingSystem.processMobTick() loads JSON config for that mob   │
│  3. Points calculated from world age (daily_scaling in JSON)        │
│  4. Points spent on upgrades (80% buy / 20% save logic)             │
│  5. Effects applied (potions, equipment, abilities)                 │
│                                                                      │
│  To add a new mob: Just create mob_configs/[mobname].json           │
│  No new Java code needed!                                           │
└─────────────────────────────────────────────────────────────────────┘
```

## Key Files

| File | Purpose |
|------|---------|
| `system/ScalingSystem.java` | THE ONE FILE that handles all mob scaling |
| `mixin/MobDataMixin.java` | Calls ScalingSystem on every mob tick |
| `mob_configs/*.json` | JSON config for each mob's upgrade tree |
| `data/MobWarData.java` | Stores skill points and upgrade levels per mob |

## How It Works

### 1. Point Calculation
Points are earned based on world age (in Minecraft days):

| Days | Points per Day |
|------|----------------|
| 0-10 | 0.1 |
| 11-15 | 0.5 |
| 16-20 | 1.0 |
| 21-25 | 1.5 |
| 26-30 | 3.0 |
| 31+ | 5.0 |

Plus 1 point per player kill by that mob type.

### 2. Spending Logic
Every ~5 seconds, mobs attempt to spend their points:
- List all affordable upgrades
- Pick one randomly
- **80% chance**: Buy it immediately, continue spending
- **20% chance**: Save points, stop spending for this cycle

### 3. Effects Applied
Based on upgrade levels, mobs receive:
- **Potion effects**: Regeneration, Health Boost, Resistance, Strength, Speed
- **Equipment**: (Coming soon) Weapons, armor, shields
- **Special abilities**: (Coming soon) Horde summon, piercing shot, etc.

## Adding a New Mob

### Step 1: Create the JSON Config
Create `src/main/resources/mob_configs/[mobname].json`:

```json
{
  "mob_name": "Zombie",
  "entity_class": "net.minecraft.entity.mob.ZombieEntity",
  "mob_type": "hostile",
  "tree_name": "Undead Warrior",

  "point_system": {
    "daily_scaling": [
      { "days_min": 0,  "days_max": 10, "points_per_day": 0.1 },
      { "days_min": 11, "days_max": 15, "points_per_day": 0.5 },
      { "days_min": 16, "days_max": 20, "points_per_day": 1.0 },
      { "days_min": 21, "days_max": 25, "points_per_day": 1.5 },
      { "days_min": 26, "days_max": 30, "points_per_day": 3.0 },
      { "days_min": 31, "days_max": -1, "points_per_day": 5.0 }
    ],
    "kill_scaling": 1,
    "buy_chance": 0.80,
    "save_chance": 0.20
  },

  "tree": {
    "passive_potion_effects": {},
    
    "hostile_neutral_potion_effects": {
      "healing": [
        { "level": 1, "cost": 1 },
        { "level": 2, "cost": 2 },
        { "level": 3, "cost": 3 },
        { "level": 4, "cost": 4 },
        { "level": 5, "cost": 5 }
      ],
      "health_boost": [
        { "level": 1, "cost": 2 },
        { "level": 2, "cost": 3 },
        { "level": 3, "cost": 4 }
      ],
      "resistance": [
        { "level": 1, "cost": 4 },
        { "level": 2, "cost": 6 },
        { "level": 3, "cost": 8 }
      ],
      "strength": [
        { "level": 1, "cost": 3 },
        { "level": 2, "cost": 5 },
        { "level": 3, "cost": 7 },
        { "level": 4, "cost": 9 }
      ],
      "speed": [
        { "level": 1, "cost": 6 },
        { "level": 2, "cost": 9 },
        { "level": 3, "cost": 12 }
      ]
    },

    "weapon": {},
    "shield": {},
    "helmet": {},
    "chestplate": {},
    "leggings": {},
    "boots": {},
    "special_abilities": {}
  }
}
```

### Step 2: That's It!
The ScalingSystem automatically:
- Detects the new JSON file on startup
- Maps the entity class to the config
- Processes upgrades for that mob type

**No Java code changes needed!**

## Mob Types

| Type | Potion Effects Section |
|------|------------------------|
| `passive` | Uses `passive_potion_effects` (regeneration, resistance, health_boost) |
| `neutral` | Uses `hostile_neutral_potion_effects` |
| `hostile` | Uses `hostile_neutral_potion_effects` |

## Currently Implemented Mobs (10/80)

| Mob | Type | Special Trees |
|-----|------|---------------|
| Allay | passive | - |
| Armadillo | passive | - |
| Axolotl | neutral | - |
| Bat | passive | - |
| Bee | neutral | - |
| Blaze | hostile | ranged |
| Bogged | hostile | zombie, ranged |
| Breeze | hostile | ranged |
| Camel | passive | - |
| Cat | passive | - |

## Upgrade Categories

### Potion Effects (All Mobs)

**Passive Mobs:**
- Regeneration (3 levels)
- Resistance (1 level)
- Health Boost (3 levels)

**Hostile/Neutral Mobs:**
- Healing (5 levels) - Regeneration + on-damage burst healing
- Health Boost (10 levels) - +2 HP per level
- Resistance (3 levels) - Damage reduction + fire resistance at level 3
- Strength (4 levels) - Increased damage
- Speed (3 levels) - Movement speed
- Invisibility on Hit (5 levels) - Chance to go invisible when hit

### Equipment (Coming Soon)
- Weapons: Swords, axes, bows, crossbows, tridents
- Armor: Leather → Chainmail → Iron → Diamond → Netherite
- Shield: Available to mobs with main-hand weapons

### Special Abilities (Coming Soon)
- **Zombie Tree**: Horde Summon, Hunger Attack
- **Ranged Tree**: Piercing Shot, Bow Potion Mastery, Multishot
- **Creeper Tree**: Creeper Power, Potion Cloud
- **Witch Tree**: Potion Throw Speed, Extra Potion Bag
- **Cave Spider Tree**: Poison Mastery

## Debugging

Enable debug logging in `config/universalmobwar.json`:
```json
{
  "debugLogging": true
}
```

Check the game log for:
```
[ScalingSystem] Loading mob configurations...
[ScalingSystem] Loaded 10 mob configurations
```

## Config Options

In `config/universalmobwar.json`:

| Option | Default | Description |
|--------|---------|-------------|
| `scalingEnabled` | true | Enable/disable the entire scaling system |
| `dayScalingMultiplierPercent` | 100 | Multiply daily point gain (100 = 1.0x) |
| `killScalingMultiplierPercent` | 100 | Multiply kill point gain (100 = 1.0x) |
| `buyChancePercent` | 80 | Chance to buy an upgrade (vs save) |
| `saveChancePercent` | 20 | Chance to save points and stop |
| `allowBossScaling` | false | Allow boss mobs to scale |
| `allowModdedScaling` | false | Allow modded mobs to scale |

## Technical Details

### MobDataMixin Integration
```java
@Inject(method = "mobTick", at = @At("HEAD"))
private void universalmobwar$onMobTick(CallbackInfo ci) {
    MobEntity self = (MobEntity)(Object)this;
    ScalingSystem.processMobTick(self, self.getWorld(), universalMobWarData);
}
```

### ScalingSystem Key Methods
- `initialize()` - Load all JSON configs on startup
- `getConfigForMob(MobEntity)` - Find JSON config for a mob
- `processMobTick(MobEntity, World, MobWarData)` - Main entry point
- `calculateWorldAgePoints(World, JsonObject)` - Calculate points from world age
- `spendPoints(...)` - Spend points on upgrades
- `applyEffects(...)` - Apply potion effects based on levels

## Contributing

1. Pick an unimplemented mob from the list of 80
2. Copy an existing JSON config as a template
3. Adjust the `mob_name`, `entity_class`, and `mob_type`
4. Define the upgrade tree with costs
5. Test in-game
6. Submit a PR!

See `skilltree.txt` for the complete list of 80 mobs and their intended configurations.

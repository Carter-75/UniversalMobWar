# Universal Mob War - Skilltree Implementation Plan

## Overview
The new `skilltree.json` provides a complete, data-driven specification for all mob behaviors, equipment, and upgrades. The code must be refactored to read from this JSON file instead of using hardcoded logic.

## Key Changes from Old System

### 1. JSON-Driven Configuration
**OLD**: Hardcoded mob categories and costs in Java code  
**NEW**: All mob definitions, costs, and trees loaded from `skilltree.json`

### 2. 80%/20% Spending Logic
**OLD**: Deterministic spending based on weighted selection  
**NEW**: On each upgrade opportunity:
- 80% chance to BUY the selected upgrade
- 20% chance to SAVE points and stop spending for this cycle

### 3. Per-Mob Equipment Rules
**OLD**: General categories determine equipment  
**NEW**: Each mob in `mob_list` has explicit weapon/armor/shield configuration

### 4. Progressive Costs from JSON
**OLD**: Hardcoded cost arrays  
**NEW**: All costs loaded from JSON with proper progressive scaling

## Implementation Tasks

### ✅ COMPLETED
1. Created `MobDefinition.java` - Represents a single mob's config
2. Created `SkillTreeConfig.java` - Parses and loads skilltree.json
3. Refactored `ArchetypeClassifier.java` - Now loads from JSON

### 🔄 IN PROGRESS  
4. Refactor `UpgradeSystem.java` to use JSON costs

### ⏳ PENDING
5. Implement 80%/20% buy/save logic in spending loop
6. Fix Durability Mastery costs (5/7/9/.../23 progressive)
7. Fix Drop Mastery costs (5/7/9/.../23 progressive)
8. Update weapon/armor equipping logic per mob definition
9. Verify all mixins still work correctly
10. Test build and fix compilation errors

## Critical Cost Changes

### Universal Upgrades (All Hostile/Neutral Mobs)

**Healing**: `1/2/3/4/5` pts (5 levels)
- L1-2: Permanent Regen I-II
- L3-5: Burst Regen III-V on damage

**Health Boost**: `2/3/4/5/6/7/8/9/10/11` pts (10 levels)
- +2 HP per level

**Resistance**: `4/6/8` pts (3 levels)
- L1-2: Resistance I-II
- L3: + Fire Resistance I

**Strength**: `3/5/7/9` pts (4 levels)
- +Strength I-IV

**Speed**: `6/9/12` pts (3 levels)
- +Speed I-III

**Invisibility on Hit**: `8/12/16/20/25` pts (5 levels)
- Chance to go invisible when damaged

### Item Masteries (All Items)

**Drop Mastery**: `5/7/9/11/13/15/17/19/21/23` pts (10 levels)
- 100% → 90% → ... → 1% drop chance

**Durability Mastery**: `10/12/14/16/18/20/22/24/26/28` pts (10 levels)
- 1% → 10% → ... → 100% durability

### Shared Trees

**Zombie Tree (z)**:
- Horde Summon: `10/15/20/25/30` pts (5 levels)
- Hunger Attack: `6/10/14` pts (3 levels)

**Ranged Tree (r)**:
- Piercing Shot: `8/12/16/20` pts (4 levels)
- Bow Potion Mastery: `10/15/20/25/30` pts (5 levels)
- Multishot: `15/25/35` pts (3 levels)

### Specific Trees

**Creeper**:
- Creeper Power: `10/15/20/25/30` pts (5 levels, radius 4.25-10.0)
- Creeper Potion Cloud: `12/18/24/30` pts (4 levels)

**Witch**:
- Potion Throw Speed: `10/15/20/25/30` pts (5 levels)
- Extra Potion Bag: `12/18/24` pts (3 levels)

**Cave Spider**:
- Poison Mastery: `0/8/12/16/20/24` pts (6 levels, L1 is free)

### Enchant Costs

**Sword/Axe**:
- Sharpness: `3/4/5/6/7` (I-V)
- Fire Aspect: `4/5` (I-II)
- Knockback: `3/4` (I-II)
- Looting: `5/7/9` (I-III)
- Unbreaking: `3/4/5` (I-III)
- Mending: `10` (flat)

**Armor (per piece)**:
- Protection: `3/4/5/6` (I-IV)
- Fire/Blast/Projectile Protection: `3/4/5/6` (I-IV)
- Thorns: `4/5/6` (I-III)
- Unbreaking: `3/4/5` (I-III)
- Mending: `10` (flat)

**Boots Special**:
- Feather Falling: `3/4/5/6` (I-IV)
- Depth Strider: `4/5/6` (I-III)
- Soul Speed: `5/6/7` (I-III)
- Frost Walker: `6/7` (I-II)

**Helmet Special**:
- Aqua Affinity: `6` (flat)
- Respiration: `4/5/6` (I-III)

**Leggings Special**:
- Swift Sneak: `6/8/10` (I-III)

**Bow/Crossbow**:
- Power: `2/3/4/5/6` (I-V)
- Punch: `4/5` (I-II)
- Flame: `8` (flat)
- Infinity: `12` (flat)
- Unbreaking: `3/4/5` (I-III)
- Mending: `10` (flat)

**Trident**:
- Initial cost: `5` pts to equip
- Loyalty: `4/5/6` (I-III)
- Impaling: `3/4/5/6/7` (I-V)
- Riptide: `5/6/7` (I-III)
- Channeling: `8` (flat)
- Unbreaking: `3/4/5` (I-III)
- Mending: `10` (flat)

## Mob Equipment Rules from JSON

### Ranged Mobs (START WITH WEAPON)
- Skeleton, Stray, Bogged → Bow
- Drowned → Trident
- Pillager, Illusioner → Crossbow
- Piglin (50%) → Crossbow

### Melee Mobs (START NAKED, EARN WEAPON)
- Zombie, Husk, Zombie_Villager, Giant → Earn Normal Sword (wood→netherite)
- Wither Skeleton → Earn Stone Sword (stone→netherite, no wood)
- Vindicator → Earn Iron Axe (iron→netherite, no lower tiers)
- Piglin (50%) → Earn Gold Sword (gold→netherite only)
- Piglin Brute → Earn Gold Axe (gold→netherite only)

### Armor Rules (ALL START NAKED)
- Normal Armor Mobs → Earn Full Normal Armor (leather→netherite, all 4 pieces)
- Piglin/Piglin Brute → Earn Full Gold Armor (gold→netherite, all 4 pieces)
- All armor earned piece-by-piece, independent progression

### Shield Rules
- Available to: Any mob that CAN hold a main-hand weapon
- Cost: `10` pts for shield chance level 1
- NOT available to mobs with "none" weapon

## Implementation Notes

### UpgradeSystem Changes Needed

1. **Replace hardcoded costs with JSON lookups**:
   ```java
   // OLD
   int healingCost = healingLvl + 1;
   
   // NEW
   List<UpgradeCost> costs = SkillTreeConfig.getInstance().getUniversalUpgrade("healing");
   int healingCost = costs.get(healingLvl).cost;
   ```

2. **Implement 80%/20% logic in spending loop**:
   ```java
   // After selecting upgrade
   if (rand.nextDouble() < 0.80) {
       // BUY - apply upgrade and continue loop
       collector.apply(index, state);
   } else {
       // SAVE - stop spending for this cycle
       break;
   }
   ```

3. **Load weapon/armor from mob definition**:
   ```java
   MobDefinition def = ArchetypeClassifier.getMobDefinition(mob);
   if (def != null) {
       // Use def.weaponType to determine starting equipment
       // Use def.armorType to determine armor progression
   }
   ```

4. **Fix Item Mastery costs**:
   ```java
   // Drop Mastery: 5/7/9/11/13/15/17/19/21/23
   // Durability Mastery: 10/12/14/16/18/20/22/24/26/28
   List<UpgradeCost> dropCosts = SkillTreeConfig.getInstance().getUniversalUpgrade("drop_mastery");
   List<UpgradeCost> durCosts = SkillTreeConfig.getInstance().getUniversalUpgrade("durability_mastery");
   ```

## Testing Checklist

### Equipment Tests
- [ ] Skeleton spawns with bow (tier 0)
- [ ] Zombie spawns naked, gets wooden sword after enchant purchase
- [ ] Wither Skeleton spawns naked, gets stone sword (not wood) after enchant purchase
- [ ] Drowned spawns with trident
- [ ] Pillager spawns with crossbow
- [ ] Piglin 50/50 splits between gold sword and crossbow
- [ ] Piglin Brute spawns naked, earns gold axe
- [ ] Vindicator spawns naked, earns iron axe

### Cost Tests
- [ ] Healing L1 costs 1 pt, L5 costs 5 pts
- [ ] Health Boost L1 costs 2 pts, L10 costs 11 pts
- [ ] Durability Mastery L1 costs 10 pts, L10 costs 28 pts
- [ ] Drop Mastery L1 costs 5 pts, L10 costs 23 pts
- [ ] Sharpness I costs 3 pts, V costs 7 pts

### Logic Tests
- [ ] 80%/20% spending: Mobs sometimes save points instead of always buying
- [ ] Mob definitions load correctly from JSON
- [ ] Category assignment matches mob_list in JSON
- [ ] All mixins still function (horde summon, infectious bite, etc.)

## Files Modified

1. ✅ `MobDefinition.java` - NEW
2. ✅ `SkillTreeConfig.java` - NEW
3. ✅ `ArchetypeClassifier.java` - REFACTORED
4. 🔄 `UpgradeSystem.java` - IN PROGRESS
5. ⏳ `ModConfig.java` - May need updates for gold armor tiers
6. ⏳ All Mixins - Verify compatibility

## Next Steps

1. Update `UpgradeSystem.addGeneralUpgrades()` to use JSON costs
2. Update `UpgradeSystem.addStatUpgrades()` for Durability/Drop Mastery
3. Implement 80%/20% buy/save logic in `simulateWithDebug()` and `simulate()`
4. Update equipment application logic in `applyStateToMob()`
5. Test build
6. Fix any compilation errors
7. Commit all changes with comprehensive documentation

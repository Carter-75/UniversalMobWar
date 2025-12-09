# Verification Issues Found - Deep Analysis

## 🔴 CRITICAL ISSUES

### 1. Equipment Logic Not Using MobDefinition
**Problem:** `applyStateToMob()` still uses the old category-based system to determine starting equipment.

**Current Code:**
```java
boolean hasSword = state.getCategoryCount("sword") > 0 || state.getItemTier("sword") > 0;
if (hasSword && state.getItemTier("sword") >= 0) {
    // Equip sword
}
```

**What It Should Do:**
```java
MobDefinition def = ArchetypeClassifier.getMobDefinition(mob);
if (def != null) {
    // Check if mob STARTS with weapon (ranged mobs)
    if (def.startsWithWeapon) {
        // Equip base weapon at tier 0
    } else {
        // Only equip if upgrades purchased
        if (state.getCategoryCount("sword") > 0) {
            // Equip earned weapon
        }
    }
}
```

**Impact:** 
- ❌ Zombies might get swords immediately instead of earning them
- ❌ Skeletons might not start with bow
- ❌ Equipment rules not following skilltree.json

---

### 2. Wither Skeleton Tree Assignment is WRONG
**In skilltree.json:**
```json
"Wither_Skeleton": { 
    "weapon": "stone_sword", 
    "trees": ["r"]  // ← WRONG! "r" = ranged/projectile tree
}
```

**Problem:** 
- Wither Skeletons use SWORDS (melee), not bows
- "r" tree gives Piercing Shot, Bow Potion Mastery, Multishot
- These skills don't apply to swords!

**Fix Needed:**
```json
"Wither_Skeleton": { 
    "weapon": "stone_sword", 
    "trees": []  // No special tree, just general hostile tree
}
```

---

### 3. Bogged Has Zombie Tree
**In skilltree.json:**
```json
"Bogged": { 
    "weapon": "bow", 
    "trees": ["z","r"]  // Has zombie AND ranged trees
}
```

**Problem:**
- Bogged is a skeleton variant (archer)
- Zombie tree gives: Horde Summon, Hunger Attack
- These don't make sense for a skeleton variant

**Decision Needed:**
- Is this intentional? (Making Bogged a hybrid zombie-skeleton)
- Or should it only have ["r"] like regular Skeleton?

**Current Minecraft Behavior:**
- Bogged is a poisonous skeleton variant from swamps
- Does NOT summon reinforcements like zombies
- Shoots poison arrows

**Recommended Fix:**
```json
"Bogged": { 
    "weapon": "bow", 
    "trees": ["r"]  // Only ranged tree, remove zombie tree
}
```

---

### 4. Shield Availability Per Mob Not Implemented
**Problem:** Shield logic doesn't check `MobDefinition.shield`

**Current Code:**
```java
int shieldLvl = state.getLevel("shield_chance");
if (shieldLvl == 0) {
    addOpt(options, state, "shield_chance", "g", "offhand", 10);
}
```

**What It Should Check:**
```java
MobDefinition def = ArchetypeClassifier.getMobDefinition(mob);
if (def != null && def.shield) {  // ← Check if mob CAN have shield
    int shieldLvl = state.getLevel("shield_chance");
    if (shieldLvl == 0) {
        addOpt(options, state, "shield_chance", "g", "offhand", 10);
    }
}
```

**Impact:**
- ❌ Cave Spiders could get shields (shield: false in JSON)
- ❌ Creepers could get shields (shield: false in JSON)
- ❌ Blazes could get shields (shield: false in JSON)

**Mobs with shield: false:**
- Allay, Armadillo, Axolotl, Bat, Bee, Blaze, Breeze, Camel, Cat, Cave_Spider, Chicken, Cod, Cow, Creaking, Creeper, Dolphin, Donkey, Elder_Guardian, Ender_Dragon, Enderman, Endermite, Evoker, Fox, Frog, Glow_Squid, Goat, Guardian, Hoglin, Horse, Magma_Cube, Mooshroom, Mule, Ocelot, Panda, Parrot, Phantom, Pig, Polar_Bear, Pufferfish, Rabbit, Ravager, Salmon, Sheep, Shulker, Silverfish, Slime, Sniffer, Spider, Squid, Strider, Tadpole, Tropical_Fish, Turtle, Vex, Villager, Wandering_Trader, Warden, Wither

**Mobs with shield: true:**
- Bogged, Drowned, Giant, Husk, Illusioner, Piglin, Piglin_Brute, Pillager, Skeleton, Stray, Vindicator, Wither_Skeleton, Zombie, Zombie_Villager

---

## ⚠️ MODERATE ISSUES

### 5. Armor Starting Conditions Not Checked
**Problem:** Armor equipping doesn't verify mob should have armor

**Current Logic:**
```java
if (state.getCategoryCount("armor") > 0 || state.getItemTier("head") > 0 || ...) {
    // Equip armor
}
```

**What It Should Check:**
```java
MobDefinition def = ArchetypeClassifier.getMobDefinition(mob);
if (def != null && def.armorType != ArmorType.NONE) {
    // Armor is available for this mob
    if (state.getCategoryCount("armor") > 0) {
        // Equip earned armor
    }
}
```

**Mobs with armor: "none":**
- All passives, Cave Spider, Creeper, most hostiles without explicit armor definition

**Mobs with armor: "full_normal":**
- Bogged, Drowned, Giant, Husk, Illusioner, Skeleton, Stray, Vindicator, Wither_Skeleton, Zombie, Zombie_Villager

**Mobs with armor: "full_gold":**
- Piglin, Piglin_Brute

---

### 6. Weapon Tier Initialization Not Using MobDefinition
**Problem:** Initial weapon tier not set based on MobDefinition.weaponType

**Current Behavior:**
- All mobs start at tier 0
- Wither Skeleton has special hardcoded check to jump to tier 1

**What It Should Do:**
```java
MobDefinition def = ArchetypeClassifier.getMobDefinition(mob);
if (def != null) {
    switch (def.weaponType) {
        case STONE_SWORD -> state.setItemTier("sword", 1); // Start at stone (tier 1)
        case IRON_AXE -> state.setItemTier("axe", 2); // Start at iron (tier 2)
        case GOLD_SWORD -> state.setItemTier("sword", 0); // Gold path, tier 0 = gold
        case GOLD_AXE -> state.setItemTier("axe", 0); // Gold path, tier 0 = gold
        case BOW, CROSSBOW, TRIDENT -> state.setItemTier(type, 0); // Base weapon
        default -> state.setItemTier(type, 0);
    }
}
```

---

### 7. Piglin 50/50 Split Implementation Check
**Current Code in ArchetypeClassifier:**
```java
if (mobName.equals("piglin")) {
    if (mob.getUuid().hashCode() % 2 == 0) {
        categories.add("sword");
        categories.remove("bow");
    } else {
        categories.add("bow");
        categories.remove("sword");
    }
}
```

**JSON Spec:**
```json
"Piglin": { 
    "weapon": "gold_sword_or_crossbow_50%",
    "trees": ["r_if_crossbow"]
}
```

**Issue:** The `"r_if_crossbow"` tree logic is not properly implemented!

**Current Code:**
```java
case "r_if_crossbow" -> {
    if (mobName.equals("piglin") && mob.getUuid().hashCode() % 2 == 1) {
        categories.add("pro");
    }
}
```

**This is CORRECT!** ✅ Ranged Piglins get "pro" tree, melee don't.

---

## ✅ VERIFIED CORRECT

### Upgrade Costs
All upgrade costs are correctly loaded from JSON:
- ✅ Healing: 1/2/3/4/5
- ✅ Health Boost: 2/3/4/5/6/7/8/9/10/11
- ✅ Resistance: 4/6/8
- ✅ Strength: 3/5/7/9
- ✅ Speed: 6/9/12
- ✅ Invisibility: 8/12/16/20/25
- ✅ Durability: 10/12/14/16/18/20/22/24/26/28
- ✅ Drop: 5/7/9/11/13/15/17/19/21/23

### 80%/20% Logic
✅ Correctly implemented in both `simulate()` and `simulateWithDebug()`

### Tree Assignments
✅ Correctly loading from JSON:
- zombie_z tree: Horde_Summon, Hunger_Attack
- ranged_r tree: Piercing_Shot, Bow_Potion_Mastery, Multishot
- creeper tree: Creeper_Power, Creeper_Potion_Cloud
- witch tree: Potion_Throw_Speed, Extra_Potion_Bag
- cave_spider tree: Poison_Mastery

---

## 🔧 FIXES REQUIRED

### Priority 1: Fix Equipment Logic
Update `applyStateToMob()` to use MobDefinition for:
1. Starting weapon determination
2. Starting armor determination
3. Shield availability
4. Weapon tier initialization

### Priority 2: Fix skilltree.json
1. Change Wither_Skeleton trees from ["r"] to []
2. Decide on Bogged: ["z","r"] vs ["r"]
3. Verify all mob tree assignments

### Priority 3: Implement Shield Checks
Add MobDefinition check in `addGeneralUpgrades()` for shield availability

### Priority 4: Test Everything
Build and test in-game with corrected logic

---

## 📋 Testing Checklist (After Fixes)

### Equipment Tests
- [ ] Skeleton spawns WITH bow (tier 0)
- [ ] Zombie spawns NAKED, gets wood sword after Sharpness I
- [ ] Wither Skeleton spawns NAKED, gets stone sword (tier 1) after Sharpness I
- [ ] Drowned spawns WITH trident (tier 0)
- [ ] Pillager spawns WITH crossbow but NO armor
- [ ] Piglin 50% spawn with gold sword, 50% with crossbow
- [ ] Piglin with crossbow gets ranged tree skills
- [ ] Piglin with sword does NOT get ranged tree skills
- [ ] Cave Spider cannot purchase shield (shield: false)
- [ ] Creeper cannot purchase shield (shield: false)
- [ ] Zombie CAN purchase shield (shield: true)

### Cost Tests
- [ ] Drop Mastery L1 costs 5 pts (not 10)
- [ ] Durability L1 costs 10 pts
- [ ] Shield costs 10 pts (single purchase)
- [ ] Cave Spider Poison L1 costs 0 pts (FREE)

### Logic Tests
- [ ] Mobs sometimes save points (20% chance)
- [ ] Wither Skeleton does NOT get bow skills
- [ ] Bogged gets correct skills (verify tree assignment)

---

## 🎯 Recommended Action Plan

1. **Fix skilltree.json** - Correct Wither_Skeleton and Bogged trees
2. **Refactor applyStateToMob()** - Use MobDefinition for all equipment
3. **Add shield check** - Verify mob.shield before offering shield upgrade
4. **Test build** - Requires Java 17+
5. **In-game testing** - Verify all behaviors
6. **Final commit** - Document all fixes

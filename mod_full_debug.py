#!/usr/bin/env python3
"""
COMPREHENSIVE DEBUG SYSTEM FOR UNIVERSAL MOB WAR MOD v3.0
Checks everything: costs, triggers, effects, equipment, spawn blocking, mixins
"""
import os
import re
import sys
from pathlib import Path

# ANSI colors
class C:
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    MAGENTA = '\033[95m'
    CYAN = '\033[96m'
    WHITE = '\033[97m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

def log(msg, color=C.WHITE, prefix=""):
    print(f"{color}{prefix}{msg}{C.RESET}")

def error(msg):
    log(f"❌ ERROR: {msg}", C.RED)

def warning(msg):
    log(f"⚠️  WARNING: {msg}", C.YELLOW)

def success(msg):
    log(f"✅ {msg}", C.GREEN)

def info(msg):
    log(f"ℹ️  {msg}", C.CYAN)

def header(msg):
    log("=" * 80, C.BLUE)
    log(msg.center(80), C.BOLD + C.CYAN)
    log("=" * 80, C.BLUE)

class DebugSystem:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.report = []
        
    def check_all(self):
        header("UNIVERSAL MOB WAR - COMPREHENSIVE DEBUG CHECK")
        
        self.check_project_structure()
        self.check_skilltree_spec()
        self.check_upgrade_costs()
        self.check_mixin_implementations()
        self.check_equipment_tiers()
        self.check_spawn_blocker()
        self.check_potion_effects()
        self.check_enchant_costs()
        self.generate_report()
        
    def check_project_structure(self):
        header("PROJECT STRUCTURE CHECK")
        required = [
            "src/main/java/mod/universalmobwar/UniversalMobWarMod.java",
            "src/main/java/mod/universalmobwar/system/UpgradeSystem.java",
            "src/main/java/mod/universalmobwar/system/EvolutionSystem.java",
            "src/main/java/mod/universalmobwar/data/PowerProfile.java",
            "src/main/java/mod/universalmobwar/data/MobWarData.java",
            "src/main/java/mod/universalmobwar/config/ModConfig.java",
            "src/main/resources/universalmobwar.mixins.json",
            "skilltree.txt"
        ]
        
        for path in required:
            if os.path.exists(path):
                success(f"Found: {path}")
            else:
                error(f"Missing: {path}")
                self.errors.append(f"Missing required file: {path}")
    
    def check_skilltree_spec(self):
        header("SKILLTREE.TXT SPECIFICATION CHECK")
        
        if not os.path.exists("skilltree.txt"):
            error("skilltree.txt not found!")
            return
            
        with open("skilltree.txt", "r") as f:
            content = f.read()
        
        # Check all skill definitions
        checks = [
            ("HEALING (5 levels, 1/2/3/4/5 pts", "Healing costs"),
            ("HEALTH BOOST (10 levels × 2 pts", "Health Boost costs"),
            ("RESISTANCE (3 levels × 4 pts", "Resistance costs"),
            ("STRENGTH (4 levels × 3 pts", "Strength costs"),
            ("SPEED (3 levels × 6 pts", "Speed costs"),
            ("INVISIBILITY MASTERY (5 levels × 5 pts", "Invisibility costs"),
            ("SHIELD CHANCE (5 levels × 8 pts", "Shield costs"),
            ("Horde Summon 1–5 (10/15/20/25/30 pts", "Horde Summon costs"),
            ("Infectious Bite 1–3 (8/12/16 pts", "Infectious Bite costs"),
            ("Hunger Attack 1–3 (6/10/14 pts", "Hunger Attack costs"),
            ("Piercing Shot 1–4 (8/12/16/20 pts", "Piercing Shot costs"),
            ("Bow Potion Mastery 1–5 (10/15/20/25/30 pts", "Bow Potion costs"),
            ("Multishot 1–3 (15/25/35 pts", "Multishot costs"),
            ("Creeper Power 1–5 (10/15/20/25/30 pts", "Creeper Power costs"),
            ("Creeper Potion 1–3 (12/18/24 pts", "Creeper Potion costs"),
            ("Potion Throw Speed 1–5 (10/15/20/25/30 pts", "Witch Throw Speed costs"),
            ("Harming Upgrade 1–3 (12/18/24 pts", "Witch Harming costs"),
            ("Poison Mastery 1–5 (8/12/16/20/24 pts", "Poison Mastery costs"),
            ("Durability Mastery level 10", "Durability Mastery"),
            ("Drop Mastery level 10", "Drop Mastery"),
            ("10 levels × 10 pts each", "Mastery cost structure"),
        ]
        
        for pattern, name in checks:
            if pattern in content:
                success(f"{name}: FOUND")
            else:
                error(f"{name}: NOT FOUND")
                self.errors.append(f"Skilltree missing: {name}")
        
        # Check progressive mechanics
        progressive_checks = [
            ("Progressive hunger effect on hit", "Hunger progressive"),
            ("Progressive chance and potion strength", "Bow Potion progressive"),
            ("Progressive explosion radius", "Creeper Power progressive"),
            ("Progressive lingering potion clouds", "Creeper Potion progressive"),
            ("Progressive throw speed and accuracy", "Witch Throw Speed progressive"),
            ("Progressive instant damage potion", "Witch Harming progressive"),
            ("Progressive poison effect on hit", "Poison Mastery progressive"),
        ]
        
        for pattern, name in progressive_checks:
            if pattern in content:
                success(f"{name}: VERIFIED")
            else:
                warning(f"{name}: Description may need update")
    
    def check_upgrade_costs(self):
        header("UPGRADE SYSTEM COSTS CHECK")
        
        upgrade_file = "src/main/java/mod/universalmobwar/system/UpgradeSystem.java"
        if not os.path.exists(upgrade_file):
            error(f"{upgrade_file} not found!")
            return
            
        with open(upgrade_file, "r") as f:
            content = f.read()
        
        # Check individual skill costs (not shared category counter)
        cost_checks = [
            # Healing: 1/2/3/4/5 progressive
            (r'int healingCost = healingLvl \+ 1;', "Healing progressive cost (1-5 pts)"),
            # Health Boost: 2 pts each
            (r'addOpt\(options, state, "health_boost", "g", "general", 2\)', "Health Boost flat 2 pts"),
            # Resistance: 4 pts each
            (r'addOpt\(options, state, "resistance", "g", "general", 4\)', "Resistance flat 4 pts"),
            # Strength: 3 pts each
            (r'addOpt\(options, state, "strength", "g", "general", 3\)', "Strength flat 3 pts"),
            # Speed: 6 pts each
            (r'addOpt\(options, state, "speed", "g", "general", 6\)', "Speed flat 6 pts"),
            # Invisibility: 5 pts each
            (r'addOpt\(options, state, "invis_mastery", "g", "general", 5\)', "Invisibility flat 5 pts"),
            # Shield: 8 pts each
            (r'addOpt\(options, state, "shield_chance", "g", "offhand", 8\)', "Shield flat 8 pts"),
            # Durability: 10 pts each
            (r'addOpt\(options, state, "durability_" \+ slot, "stats", slot, 10', "Durability 10 pts"),
            # Drop Chance: 10 pts each
            (r'addOpt\(options, state, "drop_chance_" \+ slot, "stats", slot, 10\)', "Drop Chance 10 pts"),
        ]
        
        for pattern, name in cost_checks:
            if re.search(pattern, content):
                success(f"{name}: CORRECT")
            else:
                error(f"{name}: NOT FOUND or INCORRECT")
                self.errors.append(f"Cost error: {name}")
        
        # Check mob-specific progressive costs
        mob_costs = [
            (r'int hungerCost = \(hungerLvl == 0\) \? 6 : \(hungerLvl == 1\) \? 10 : 14', "Hunger Attack (6/10/14)"),
            (r'int infectCost = \(infectLvl == 0\) \? 8 : \(infectLvl == 1\) \? 12 : 16', "Infectious Bite (8/12/16)"),
            (r'int hordeCost = 10 \+ \(hordeLvl \* 5\)', "Horde Summon (10/15/20/25/30)"),
            (r'int pierceCost = 8 \+ \(pierceLvl \* 4\)', "Piercing Shot (8/12/16/20)"),
            (r'int multiCost = \(multiLvl == 0\) \? 15 : \(multiLvl == 1\) \? 25 : 35', "Multishot (15/25/35)"),
            (r'int bowPotCost = 10 \+ \(bowPotLvl \* 5\)', "Bow Potion (10/15/20/25/30)"),
            (r'int powerCost = 10 \+ \(powerLvl \* 5\)', "Creeper Power (10/15/20/25/30)"),
            (r'int potCost = 12 \+ \(potLvl \* 6\)', "Creeper Potion (12/18/24)"),
            (r'int throwCost = 10 \+ \(throwLvl \* 5\)', "Witch Throw Speed (10/15/20/25/30)"),
            (r'int harmCost = 12 \+ \(harmLvl \* 6\)', "Witch Harming (12/18/24)"),
            (r'int poisonCost = 8 \+ \(poisonLvl \* 4\)', "Poison Mastery (8/12/16/20/24)"),
        ]
        
        for pattern, name in mob_costs:
            if re.search(pattern, content):
                success(f"{name}: PROGRESSIVE")
            else:
                error(f"{name}: NOT PROGRESSIVE")
                self.errors.append(f"Progressive cost missing: {name}")
    
    def check_mixin_implementations(self):
        header("MIXIN IMPLEMENTATIONS CHECK")
        
        mixin_dir = "src/main/java/mod/universalmobwar/mixin"
        if not os.path.isdir(mixin_dir):
            error(f"Mixin directory not found: {mixin_dir}")
            return
        
        # Check each mixin file
        mixins = [
            ("UniversalBaseTreeMixin.java", [
                ("Healing burst on taking damage", r'method = "damage".*universalmobwar\$onHealingDamage'),
                ("Invisibility on damage", r'method = "damage".*universalmobwar\$onDamage'),
                ("Speed effect", r'StatusEffects\.SPEED'),
                ("Strength effect", r'StatusEffects\.STRENGTH'),
            ]),
            ("HordeSummonMixin.java", [
                ("Triggers on hit (tryAttack)", r'method = "tryAttack"'),
                ("5 levels max", r'if \(level > 0 && level <= 5\)'),
                ("10-50% chance", r'float chance = 0\.10f \+ \(level \* 0\.08f\)'),
            ]),
            ("InfectiousBiteMixin.java", [
                ("33/66/100% chance", r'float chance = level \* 0\.33f'),
            ]),
            ("CaveSpiderMixin.java", [
                ("5 levels", r'if \(level == 5\)'),
                ("Poison II + Wither I at L5", r'StatusEffects\.WITHER'),
            ]),
            ("CreeperExplosionMixin.java", [
                ("Explosion radius 3.0-8.0", r'3\.0f \+ \(level \* 1\.25f\)'),
                ("Progressive potion clouds", r'creeper_potion_mastery'),
            ]),
            ("WitchPotionMixin.java", [
                ("Progressive throw speed", r'witch_potion_mastery'),
                ("Progressive harming", r'witch_harming_upgrade'),
            ]),
            ("BowPotionMixin.java", [
                ("Progressive bow potion", r'bow_potion_mastery'),
                ("5 levels", r'if \(level > 0 && level <= 5\)'),
            ]),
            ("NaturalMobSpawnBlockerMixin.java", [
                ("Blocks ALL MobEntity", r'entity instanceof MobEntity'),
                ("Allows player-spawned", r'umw_player_spawned|umw_summoned'),
            ]),
        ]
        
        for filename, checks in mixins:
            filepath = os.path.join(mixin_dir, filename)
            if not os.path.exists(filepath):
                error(f"Mixin not found: {filename}")
                continue
                
            with open(filepath, "r") as f:
                content = f.read()
            
            info(f"\nChecking {filename}:")
            for desc, pattern in checks:
                if re.search(pattern, content, re.DOTALL):
                    success(f"  {desc}")
                else:
                    error(f"  {desc}: NOT FOUND")
                    self.errors.append(f"{filename}: {desc}")
    
    def check_equipment_tiers(self):
        header("EQUIPMENT TIER CHECK")
        
        config_file = "src/main/java/mod/universalmobwar/config/ModConfig.java"
        if not os.path.exists(config_file):
            error(f"{config_file} not found!")
            return
            
        with open(config_file, "r") as f:
            content = f.read()
        
        # Check that all tier lists have minecraft: prefix
        tier_patterns = [
            (r'"minecraft:wooden_sword"', "Sword tiers"),
            (r'"minecraft:leather_helmet"', "Helmet tiers"),
            (r'"minecraft:leather_chestplate"', "Chest tiers"),
            (r'"minecraft:leather_leggings"', "Legs tiers"),
            (r'"minecraft:leather_boots"', "Boots tiers"),
            (r'"minecraft:golden_sword"', "Gold sword tiers"),
            (r'"minecraft:golden_axe"', "Gold axe tiers"),
        ]
        
        for pattern, name in tier_patterns:
            if re.search(pattern, content):
                success(f"{name}: Correct format")
            else:
                warning(f"{name}: May be missing minecraft: prefix")
    
    def check_spawn_blocker(self):
        header("SPAWN BLOCKER CHECK")
        
        blocker_file = "src/main/java/mod/universalmobwar/mixin/NaturalMobSpawnBlockerMixin.java"
        if not os.path.exists(blocker_file):
            error(f"{blocker_file} not found!")
            return
            
        with open(blocker_file, "r") as f:
            content = f.read()
        
        checks = [
            ("Blocks ALL MobEntity", r'entity instanceof MobEntity'),
            ("Checks player-spawned tag", r'hasCommandTag\("umw_player_spawned"\)'),
            ("Checks summoned tag", r'hasCommandTag\("umw_summoned"\)'),
            ("Checks horde reinforcement tag", r'hasCommandTag\("umw_horde_reinforcement"\)'),
            ("Cancels spawn", r'cir\.setReturnValue\(false\)'),
        ]
        
        for desc, pattern in checks:
            if re.search(pattern, content):
                success(desc)
            else:
                error(f"{desc}: NOT FOUND")
                self.errors.append(f"Spawn blocker: {desc}")
    
    def check_potion_effects(self):
        header("POTION EFFECTS PROGRESSIVE CHECK")
        
        effect_checks = [
            ("CaveSpiderMixin.java", [
                ("Level 1: Poison I (7s)", r'new StatusEffectInstance\(StatusEffects\.POISON, 140, 0\)'),
                ("Level 2: Poison I (14s)", r'new StatusEffectInstance\(StatusEffects\.POISON, 280, 0\)'),
                ("Level 3: Poison II (14s)", r'new StatusEffectInstance\(StatusEffects\.POISON, 280, 1\)'),
                ("Level 4: Poison II (20s)", r'new StatusEffectInstance\(StatusEffects\.POISON, 400, 1\)'),
                ("Level 5: +Wither I (10s)", r'new StatusEffectInstance\(StatusEffects\.WITHER, 200, 0\)'),
            ]),
            ("InfectiousBiteMixin.java", [
                ("Hunger I-III progressive", r'new StatusEffectInstance\(StatusEffects\.HUNGER, 200, level - 1\)'),
            ]),
            ("CreeperExplosionMixin.java", [
                ("Progressive potion mastery", r'creeper_potion_mastery'),
            ]),
            ("WitchPotionMixin.java", [
                ("Progressive throw speed", r'baseVelocity \* speedMultiplier'),
                ("Progressive harming", r'witch_harming_upgrade'),
            ]),
            ("BowPotionMixin.java", [
                ("Progressive bow potion", r'bow_potion_mastery'),
            ]),
        ]
        
        mixin_dir = "src/main/java/mod/universalmobwar/mixin"
        for filename, checks in effect_checks:
            filepath = os.path.join(mixin_dir, filename)
            if not os.path.exists(filepath):
                warning(f"Skipping {filename}: not found")
                continue
                
            with open(filepath, "r") as f:
                content = f.read()
            
            info(f"\nChecking {filename}:")
            for desc, pattern in checks:
                if re.search(pattern, content, re.DOTALL):
                    success(f"  {desc}")
                else:
                    warning(f"  {desc}: May need verification")
    
    def check_enchant_costs(self):
        header("ENCHANT COSTS CHECK (Manual Verification Required)")
        
        info("Enchant costs are defined in ModConfig and should match:")
        info("  • Sharpness, Smite, Bane: 3 pts each")
        info("  • Fire Aspect, Thorns: 4 pts each")
        info("  • Looting: 5 pts each")
        info("  • Mending: 10 pts")
        info("  • Unbreaking, Knockback: 3 pts each")
        warning("Manual verification needed - check ModConfig.java arrays")
    
    def generate_report(self):
        header("DEBUG REPORT SUMMARY")
        
        if not self.errors:
            success("✅ NO CRITICAL ERRORS FOUND!")
        else:
            error(f"❌ {len(self.errors)} CRITICAL ERROR(S) FOUND:")
            for i, err in enumerate(self.errors, 1):
                print(f"  {i}. {err}")
        
        if self.warnings:
            warning(f"⚠️  {len(self.warnings)} WARNING(S):")
            for i, warn in enumerate(self.warnings, 1):
                print(f"  {i}. {warn}")
        
        # Write detailed report
        with open("mod_debug_report.txt", "w") as f:
            f.write("=" * 80 + "\n")
            f.write("UNIVERSAL MOB WAR - COMPREHENSIVE DEBUG REPORT\n")
            f.write("=" * 80 + "\n\n")
            
            if self.errors:
                f.write("CRITICAL ERRORS:\n")
                for err in self.errors:
                    f.write(f"  ❌ {err}\n")
                f.write("\n")
            
            if self.warnings:
                f.write("WARNINGS:\n")
                for warn in self.warnings:
                    f.write(f"  ⚠️  {warn}\n")
                f.write("\n")
            
            if not self.errors:
                f.write("✅ ALL CRITICAL CHECKS PASSED!\n\n")
            
            f.write("End of report.\n")
        
        success("Full report saved to: mod_debug_report.txt")
        print()

if __name__ == "__main__":
    os.chdir("/home/user/webapp/UniversalMobWar")
    debugger = DebugSystem()
    debugger.check_all()
    sys.exit(0 if not debugger.errors else 1)

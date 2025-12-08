#!/usr/bin/env python3
"""
COMPREHENSIVE DEBUG & BUILD & TEST SYSTEM FOR UNIVERSAL MOB WAR MOD v3.1
Checks everything: costs, triggers, effects, equipment, spawn blocking, mixins
Also includes full build, commit, and IN-GAME TESTING functionality
Tests EVERY mob from 0 points → MAX points with FULL progression
"""
import os
import re
import sys
import subprocess
import shutil
import time
import json
import datetime
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
        
        # Check for cleaned up files
        obsolete = [
            "build_and_commit.py",
            "build_log.txt"
        ]
        
        info("\nChecking for obsolete files...")
        for path in obsolete:
            if os.path.exists(path):
                warning(f"Obsolete file still exists: {path}")
            else:
                success(f"Cleaned up: {path}")
    
    def check_skilltree_spec(self):
        header("SKILLTREE.TXT SPECIFICATION CHECK")
        
        if not os.path.exists("skilltree.txt"):
            error("skilltree.txt not found!")
            return
            
        with open("skilltree.txt", "r", encoding="utf-8") as f:
            content = f.read()
        
        # Check all skill definitions
        checks = [
            ("HEALING (5 levels, 1/2/3/4/5 pts", "Healing costs"),
            ("HEALTH BOOST (10 levels, 2/3/4/5/6/7/8/9/10/11 pts", "Health Boost costs"),
            ("RESISTANCE (3 levels, 4/6/8 pts", "Resistance costs"),
            ("STRENGTH (4 levels, 3/5/7/9 pts", "Strength costs"),
            ("SPEED (3 levels, 6/9/12 pts", "Speed costs"),
            ("INVISIBILITY MASTERY (5 levels, 5/7/9/11/13 pts", "Invisibility costs"),
            ("SHIELD CHANCE (5 levels, 8/11/14/17/20 pts", "Shield costs"),
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
            ("10 levels, 10/12/14/16/18/20/22/24/26/28 pts", "Mastery cost structure"),
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
        
        # Check individual skill costs (ALL progressive, no flat costs)
        cost_checks = [
            # Healing: 1/2/3/4/5 progressive
            (r'int healingCost = healingLvl \+ 1;', "Healing progressive cost (1-5 pts)"),
            # Health Boost: 2/3/4/5/6/7/8/9/10/11 progressive
            (r'int healthBoostCost = 2 \+ healthBoostLvl;', "Health Boost progressive (2-11 pts)"),
            # Resistance: 4/6/8 progressive
            (r'int resistCost = 4 \+ \(resistLvl \* 2\);', "Resistance progressive (4/6/8 pts)"),
            # Strength: 3/5/7/9 progressive
            (r'int strengthCost = 3 \+ \(strengthLvl \* 2\);', "Strength progressive (3/5/7/9 pts)"),
            # Speed: 6/9/12 progressive
            (r'int speedCost = 6 \+ \(speedLvl \* 3\);', "Speed progressive (6/9/12 pts)"),
            # Invisibility: 5/7/9/11/13 progressive
            (r'int invisCost = 5 \+ \(invisLvl \* 2\);', "Invisibility progressive (5/7/9/11/13 pts)"),
            # Shield: 8/11/14/17/20 progressive
            (r'int shieldCost = 8 \+ \(shieldLvl \* 3\);', "Shield progressive (8/11/14/17/20 pts)"),
            # Durability: 10/12/14/16/18/20/22/24/26/28 progressive
            (r'int durCost = 10 \+ \(durLvl \* 2\);', "Durability progressive (10-28 pts)"),
            # Drop Chance: 10/12/14/16/18/20/22/24/26/28 progressive
            (r'int dropCost = 10 \+ \(dropLvl \* 2\);', "Drop Chance progressive (10-28 pts)"),
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
        header("MIXIN IMPLEMENTATIONS CHECK - ALL 22 MIXINS")
        
        mixin_dir = "src/main/java/mod/universalmobwar/mixin"
        if not os.path.isdir(mixin_dir):
            error(f"Mixin directory not found: {mixin_dir}")
            return
        
        # Check ALL mixin files comprehensively
        mixins = [
            ("UniversalBaseTreeMixin.java", [
                ("Healing burst on taking damage", r'method = "damage"'),
                ("Healing cooldown 60s", r'1200|cooldown'),
                ("Invisibility on damage", r'StatusEffects\.INVISIBILITY'),
                ("Invisibility cooldown 60s", r'1200|cooldown'),
                ("Speed effect applied", r'StatusEffects\.SPEED'),
                ("Speed progressive amplifier", r'speedLevel'),
                ("Strength effect applied", r'StatusEffects\.STRENGTH'),
                ("Strength progressive amplifier", r'strengthLevel'),
            ]),
            ("HordeSummonMixin.java", [
                ("Mixin targets MobEntity", r'@Mixin\(MobEntity\.class\)'),
                ("Triggers on successful hit", r'method = "tryAttack"'),
                ("Max 5 levels check", r'level > 5'),
                ("Progressive chance 10-50%", r'0\.10f'),
                ("Prevents infinite summon loops", r'umw_horde_reinforcement'),
                ("Sets summon to level 0", r'horde_summon'),
                ("Marks summoned mobs", r'umw_summoned'),
            ]),
            ("InfectiousBiteMixin.java", [
                ("Mixin targets ZombieEntity", r'@Mixin\(ZombieEntity\.class\)'),
                ("Triggers on kill", r'method = "onKilledOther"'),
                ("Progressive chance 33-100%", r'level \* 0\.33f'),
                ("Converts villager to zombie", r'convertTo.*ZOMBIE_VILLAGER'),
                ("Hunger Attack on hit", r'method = "tryAttack"'),
                ("Hunger I-III progressive", r'amplifier = level - 1'),
                ("Hunger durations 10/15/20s", r'200|300|400'),
            ]),
            ("CaveSpiderMixin.java", [
                ("Mixin targets CaveSpiderEntity", r'@Mixin\(CaveSpiderEntity\.class\)'),
                ("Triggers on attack", r'method = "tryAttack"'),
                ("Max 5 levels check", r'level > 5'),
                ("Poison I at L1-L2", r'StatusEffects\.POISON.*140|280.*0\)'),
                ("Poison II at L3-L4", r'StatusEffects\.POISON.*280|400.*1\)'),
                ("Wither I added at L5", r'StatusEffects\.WITHER.*200.*0\)'),
            ]),
            ("CreeperExplosionMixin.java", [
                ("Mixin targets CreeperEntity", r'@Mixin\(CreeperEntity\.class\)'),
                ("Explosion method injection", r'method = "explode"'),
                ("Progressive radius 3.0-8.0", r'3\.0f.*powerLevel - 1.*1\.25f'),
                ("Creeper potion mastery", r'creeper_potion_mastery'),
                ("Level 1: Slowness I cloud", r'SLOWNESS'),
                ("Level 2: +Weakness I", r'WEAKNESS'),
                ("Level 3: +Poison I", r'POISON'),
                ("Lingering area cloud", r'AreaEffectCloudEntity'),
            ]),
            ("WitchPotionMixin.java", [
                ("Mixin targets WitchEntity", r'@Mixin\(WitchEntity\.class\)'),
                ("Intercepts shootAt method", r'method = "shootAt"'),
                ("Witch potion mastery skill", r'witch_potion_mastery'),
                ("Progressive throw speed", r'speeds.*0\.75f.*1\.25f'),
                ("Progressive accuracy", r'inaccuracies.*8\.0f.*2\.0f'),
                ("Harming upgrade skill", r'witch_harming_upgrade'),
                ("Level 1: Instant Damage I", r'HARMING'),
                ("Level 2: Instant Damage II", r'STRONG_HARMING'),
                ("Level 3: +Wither I", r'WITHER.*200'),
            ]),
            ("BowPotionMixin.java", [
                ("Mixin targets AbstractSkeletonEntity", r'@Mixin\(AbstractSkeletonEntity\.class\)'),
                ("Intercepts arrow creation", r'createArrowProjectile'),
                ("Max 5 levels check", r'level > 5'),
                ("Progressive chance 20-100%", r'0\.20|bow_potion_mastery'),
                ("Level 1: Slowness I", r'SLOWNESS'),
                ("Level 2: Slowness II or Weakness", r'WEAKNESS'),
                ("Level 3: Poison I", r'POISON'),
                ("Level 4: Instant Damage", r'INSTANT_DAMAGE|HARMING'),
                ("Level 5: Wither I", r'WITHER'),
            ]),
            ("NaturalMobSpawnBlockerMixin.java", [
                ("Blocks ALL MobEntity", r'entity instanceof MobEntity'),
                ("Checks player-spawned tag", r'umw_player_spawned'),
                ("Checks summoned tag", r'umw_summoned'),
                ("Checks horde reinforcement", r'umw_horde_reinforcement'),
                ("Allows custom named mobs", r'hasCustomName'),
                ("Cancels natural spawn", r'setReturnValue\(false\)'),
                ("Config toggle support", r'disableNaturalMobSpawns'),
            ]),
            ("MobUpgradeTickMixin.java", [
                ("Mixin targets MobEntity", r'@Mixin\(MobEntity\.class\)'),
                ("Injects into tick method", r'method = "tick"'),
                ("Applies upgrade logic", r'UpgradeSystem|applyUpgrades'),
                ("Performance optimization", r'age % 20|isClient'),
            ]),
            ("MobDeathTrackerMixin.java", [
                ("Mixin targets LivingEntity", r'@Mixin\(LivingEntity\.class\)'),
                ("Tracks mob deaths", r'onDeath'),
                ("Handles kill tracking", r'MobEntity|victim'),
            ]),
            ("MobDataMixin.java", [
                ("Implements persistence interface", r'implements'),
                ("Stores MobWarData", r'MobWarData'),
                ("Read/write from NBT", r'readNbt|writeNbt|Nbt'),
            ]),
            ("EquipmentBreakMixin.java", [
                ("Handles item damage", r'ItemStack'),
                ("Equipment tier logic", r'tier|EquipmentSlot'),
                ("Equipment breaking", r'durability|drop|break|equipStack'),
            ]),
            ("ProjectileSkillMixin.java", [
                ("Handles piercing shot", r'piercing'),
                ("Handles multishot", r'multishot'),
                ("Projectile spawning logic", r'ProjectileEntity|spawnEntity'),
            ]),
            ("InvisibilitySkillMixin.java", [
                ("Tick based checking", r'tick'),
                ("Invisibility handling", r'Invisibility|INVISIBILITY'),
            ]),
            ("UniversalSummonerTrackingMixin.java", [
                ("Tracks summoner", r'summon'),
                ("Summoner tracking logic", r'SummonerTracker|SpawnReason'),
            ]),
            ("RaidSpawningMixin.java", [
                ("Raid system integration", r'Raid'),
                ("Warlord boss spawning", r'Warlord|wavesSpawned'),
            ]),
            ("NeutralMobBehaviorMixin.java", [
                ("Modifies neutral mob AI", r'NeutralMob'),
                ("Behavior modification", r'target|behavior'),
            ]),
            ("MobRevengeBlockerMixin.java", [
                ("Prevents revenge targeting", r'revenge|target'),
                ("Alliance checking", r'alliance|ally'),
            ]),
            ("WarlordMinionProtectionMixin.java", [
                ("Protects warlord minions", r'warlord|minion'),
                ("Damage protection", r'damage'),
            ]),
            ("GameRulesAccessor.java", [
                ("Accessor mixin", r'@Accessor|@Invoker'),
                ("Provides game rule access", r'GameRules'),
            ]),
            ("MobEntityAccessor.java", [
                ("Accessor mixin", r'@Accessor|@Invoker'),
                ("Provides mob entity access", r'MobEntity'),
            ]),
            ("PersistentProjectileEntityAccessor.java", [
                ("Accessor mixin", r'@Accessor|@Invoker'),
                ("Provides projectile access", r'PersistentProjectileEntity'),
            ]),
        ]
        
        for filename, checks in mixins:
            filepath = os.path.join(mixin_dir, filename)
            if not os.path.exists(filepath):
                error(f"Mixin not found: {filename}")
                continue
                
            with open(filepath, "r", encoding="utf-8") as f:
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
            ("Checks player-spawned tag", r'getCommandTags\(\)\.contains\("umw_player_spawned"\)'),
            ("Checks summoned tag", r'getCommandTags\(\)\.contains\("umw_summoned"\)'),
            ("Checks horde reinforcement tag", r'getCommandTags\(\)\.contains\("umw_horde_reinforcement"\)'),
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
        with open("mod_debug_report.txt", "w", encoding="utf-8") as f:
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

def build_project():
    """Build the project with gradle."""
    header("GRADLE BUILD")
    
    gradlew = "./gradlew" if os.name != "nt" else "gradlew.bat"
    
    # Clean first
    info("Running gradle clean...")
    try:
        subprocess.run([gradlew, "clean"], check=True, timeout=300)
        success("Clean completed")
    except Exception as e:
        error(f"Clean failed: {e}")
        return False
    
    # Build
    info("Running gradle build...")
    try:
        result = subprocess.run([gradlew, "build", "--stacktrace"], 
                              capture_output=True, text=True, timeout=600)
        if result.returncode == 0:
            success("Build successful!")
            info("JAR location: build/libs/")
            return True
        else:
            error("Build failed!")
            print(result.stdout)
            print(result.stderr)
            return False
    except Exception as e:
        error(f"Build failed: {e}")
        return False

def commit_and_push(message="Automated fix"):
    """Commit and push changes."""
    header("GIT COMMIT & PUSH")
    
    try:
        # Add all
        subprocess.run(["git", "add", "-A"], check=True)
        success("Files staged")
        
        # Check if there are changes
        result = subprocess.run(["git", "diff", "--cached", "--name-only"], 
                              capture_output=True, text=True)
        if not result.stdout.strip():
            info("No changes to commit")
            return True
        
        # Commit
        subprocess.run(["git", "commit", "-m", message], check=True)
        success(f"Committed with message: {message}")
        
        # Push
        subprocess.run(["git", "push", "origin", "main"], check=True)
        success("Pushed to origin/main")
        return True
        
    except Exception as e:
        error(f"Git operation failed: {e}")
        return False

def generate_ingame_tests():
    """Generate comprehensive in-game test commands for FULL progression testing"""
    header("IN-GAME TEST SUITE GENERATION")
    
    # All vanilla mobs with their skill trees
    MOBS = {
        "hostile": [
            ("zombie", "Zombie", ["horde_summon", "infectious_bite", "hunger_attack"], 8000),
            ("husk", "Husk", ["horde_summon", "infectious_bite", "hunger_attack"], 8000),
            ("drowned", "Drowned", ["horde_summon", "infectious_bite", "trident"], 8500),
            ("skeleton", "Skeleton", ["bow_potion_mastery", "piercing_shot", "multishot"], 8200),
            ("stray", "Stray", ["bow_potion_mastery", "piercing_shot", "multishot"], 8200),
            ("creeper", "Creeper", ["creeper_power", "creeper_potion_mastery"], 7500),
            ("witch", "Witch", ["witch_potion_mastery", "witch_harming_upgrade"], 7800),
            ("cave_spider", "Cave Spider", ["poison_mastery"], 7200),
            ("spider", "Spider", [], 7000),
            ("enderman", "Enderman", [], 7000),
            ("zombified_piglin", "Zombified Piglin", ["horde_summon"], 7500),
            ("piglin", "Piglin", ["shield_chance"], 7800),
            ("piglin_brute", "Piglin Brute", ["shield_chance"], 7800),
            ("blaze", "Blaze", [], 7000),
            ("wither_skeleton", "Wither Skeleton", [], 7500),
        ],
    }
    
    test_file = []
    test_file.append("# " + "=" * 78)
    test_file.append("# UNIVERSAL MOB WAR v3.1 - COMPREHENSIVE IN-GAME TEST SUITE")
    test_file.append(f"# Generated: {datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    test_file.append("# Tests EVERY mob from 0 points → MAX points (full progression)")
    test_file.append("# " + "=" * 78)
    test_file.append("")
    test_file.append("# SETUP: Creative superflat world, cheats enabled")
    test_file.append("# Run each test, wait for progression, log results")
    test_file.append("")
    
    # Generate progression tests for each mob
    for category, mobs in MOBS.items():
        test_file.append(f"\n# {category.upper()} MOBS - FULL PROGRESSION TESTS")
        test_file.append("#" * 80)
        
        for mob_id, mob_name, skills, max_points in mobs:
            test_file.append(f"\n# === {mob_name} - 0 → {max_points} points ===")
            test_file.append(f"# Special skills: {', '.join(skills) if skills else 'None'}")
            test_file.append("")
            
            # Test progression levels
            levels = [0, 50, 150, 500, 1500, 3000, 5000, max_points]
            
            for points in levels:
                test_file.append(f"# Test at {points} points:")
                test_file.append(f"/kill @e[type=minecraft:{mob_id}]")
                test_file.append(f"/summon minecraft:{mob_id} ~ ~ ~ {{Tags:[\"test_{mob_id}_{points}\"]}}")
                test_file.append(f"# Simulate {points} points by setting day/kills")
                
                # Calculate days and kills for point target
                if points == 0:
                    test_file.append(f"/time set 0")
                    test_file.append(f"# Expected: No upgrades, base stats")
                elif points <= 50:
                    days = int(points / 0.5)
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Basic upgrades (Healing I, Health Boost I-II)")
                elif points <= 150:
                    days = 30
                    kills = points - 15  # ~15 pts from days
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Early gear (Wooden/Stone), basic enchants")
                elif points <= 500:
                    days = 40
                    kills = points - 60
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Iron gear, Protection II-III, Sharpness III")
                elif points <= 1500:
                    days = 50
                    kills = points - 150
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Diamond gear, high enchants, special skills L2-L3")
                elif points <= 3000:
                    days = 60
                    kills = points - 200
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Near-max diamond, starting Netherite, skills L4")
                elif points <= 5000:
                    days = 70
                    kills = points - 250
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: Full Netherite, max enchants, skills L5")
                else:
                    days = 80
                    kills = points - 300
                    test_file.append(f"/time set {days * 24000}")
                    test_file.append(f"# Expected: MAXED - All skills L5/L10, perfect gear")
                
                test_file.append(f"# Wait 120 seconds for full upgrade application")
                test_file.append(f"/mobwar stats")
                test_file.append(f"/data get entity @e[type=minecraft:{mob_id},tag=test_{mob_id}_{points},limit=1]")
                test_file.append("")
                
                # Check specific upgrades at this level
                test_file.append(f"# Verify upgrades at {points} points:")
                if points >= 50:
                    test_file.append(f"#   - Healing: Level 1-2")
                    test_file.append(f"#   - Health Boost: Level 1-5 (+2-10 HP)")
                if points >= 150:
                    test_file.append(f"#   - Weapon equipped (Wood/Stone)")
                    test_file.append(f"#   - Armor pieces (Leather/Chain)")
                    test_file.append(f"#   - Sharpness I-II")
                if points >= 500:
                    test_file.append(f"#   - Iron tier equipment")
                    test_file.append(f"#   - Protection II-III")
                    test_file.append(f"#   - Strength I-II")
                    test_file.append(f"#   - Special skill Level 1")
                if points >= 1500:
                    test_file.append(f"#   - Diamond tier")
                    test_file.append(f"#   - Sharpness IV-V")
                    test_file.append(f"#   - Fire Aspect, Looting")
                    test_file.append(f"#   - Special skills Level 2-3")
                if points >= 3000:
                    test_file.append(f"#   - Starting Netherite")
                    test_file.append(f"#   - Max enchants (Sharp V, Prot IV)")
                    test_file.append(f"#   - Mending, Unbreaking III")
                    test_file.append(f"#   - Special skills Level 4")
                if points >= 5000:
                    test_file.append(f"#   - Full Netherite set")
                    test_file.append(f"#   - All enchants maxed")
                    test_file.append(f"#   - Special skills Level 5")
                    test_file.append(f"#   - Durability Mastery 10")
                    test_file.append(f"#   - Drop Mastery 10")
                if points >= max_points:
                    test_file.append(f"#   - FULLY MAXED")
                    test_file.append(f"#   - All 47 skills at max level")
                    test_file.append(f"#   - Perfect equipment")
                    test_file.append(f"#   - 1% drop chance")
                
                test_file.append("")
    
    # Add mixin-specific tests
    test_file.append("\n\n# MIXIN VERIFICATION TESTS")
    test_file.append("#" * 80)
    
    mixin_tests = [
        ("# HEALING BURST TEST", [
            "/summon minecraft:zombie ~ ~ ~ {Tags:[\"test_healing\"]}",
            "# Wait 60s for healing upgrades",
            "/effect give @e[type=zombie,tag=test_healing,limit=1] minecraft:instant_damage 1 10",
            "# Watch for Regen III-V burst (20-80% chance, 10-20s duration, 60s CD)",
        ]),
        ("# INVISIBILITY TEST", [
            "/summon minecraft:skeleton ~ ~ ~ {Tags:[\"test_invis\"]}",
            "# Wait 60s for invisibility upgrades",
            "/effect give @e[type=skeleton,tag=test_invis,limit=1] minecraft:instant_damage 1 5",
            "# Watch for invisibility (5-80% chance, 5-20s duration, 60s CD)",
        ]),
        ("# HORDE SUMMON TEST", [
            "/summon minecraft:zombie ~ ~ ~ {Tags:[\"test_horde\"]}",
            "# Wait 60s for horde summon L5",
            "/summon minecraft:iron_golem ~ ~2 ~5 {Tags:[\"target\"]}",
            "# Let zombie attack golem, watch for 50% summon rate",
            "# Verify summoned mobs have umw_horde_reinforcement tag",
        ]),
        ("# POISON MASTERY TEST", [
            "/summon minecraft:cave_spider ~ ~ ~ {Tags:[\"test_poison\"]}",
            "# Wait 60s for poison L5",
            "/summon minecraft:iron_golem ~ ~2 ~5",
            "# Let spider attack, verify Poison II (20s) + Wither I (10s)",
        ]),
        ("# CREEPER EXPLOSION TEST", [
            "/summon minecraft:creeper ~ ~ ~ {Tags:[\"test_creeper\"],Fuse:1}",
            "# Wait 60s for creeper power L5",
            "# Measure explosion radius (should be ~8 blocks)",
            "# Verify lingering cloud: Slowness II + Weakness I + Poison I",
        ]),
        ("# WITCH POTION TEST", [
            "/summon minecraft:witch ~ ~ ~ {Tags:[\"test_witch\"]}",
            "# Wait 60s for witch mastery L5 + harming L3",
            "/summon minecraft:iron_golem ~ ~2 ~10",
            "# Count throws/minute (should be 66% faster)",
            "# Verify Instant Damage II + Wither I potions",
        ]),
        ("# BOW POTION TEST", [
            "/summon minecraft:skeleton ~ ~ ~ {Tags:[\"test_bow\"]}",
            "# Wait 60s for bow potion L5",
            "/summon minecraft:iron_golem ~ ~2 ~15",
            "# Verify 100% of arrows have deadly potion effects",
            "# Check for: Poison II, Instant Damage, Wither",
        ]),
    ]
    
    for test_name, commands in mixin_tests:
        test_file.append(f"\n{test_name}")
        for cmd in commands:
            test_file.append(cmd)
        test_file.append("")
    
    # Add equipment tier tests
    test_file.append("\n# EQUIPMENT TIER PROGRESSION TEST")
    test_file.append("#" * 80)
    test_file.append("# Test: Zombie from 0 → MAX, verify each tier")
    test_file.append("/summon minecraft:zombie ~ ~ ~ {Tags:[\"tier_test\"]}")
    test_file.append("# Monitor equipment over time:")
    test_file.append("#   0-100 pts: Wooden Sword, Leather Armor")
    test_file.append("#   100-300 pts: Stone Sword, Chain/Iron Armor")
    test_file.append("#   300-800 pts: Iron Sword, Iron Armor")
    test_file.append("#   800-2000 pts: Diamond Sword, Diamond Armor")
    test_file.append("#   2000-5000 pts: Netherite Sword, Netherite Armor")
    test_file.append("#   5000+ pts: Max durability, 1% drop chance")
    
    # Write test file
    test_path = Path("IN_GAME_FULL_PROGRESSION_TESTS.txt")
    with open(test_path, "w") as f:
        f.write("\n".join(test_file))
    
    success(f"Full progression test suite generated: {test_path}")
    success(f"Total test lines: {len(test_file)}")
    success(f"Coverage: EVERY mob from 0 → MAX points")
    
    return test_path

if __name__ == "__main__":
    import argparse
    
    parser = argparse.ArgumentParser(description="Universal Mob War Debug & Build & Test System")
    parser.add_argument("--build", action="store_true", help="Run gradle build")
    parser.add_argument("--commit", action="store_true", help="Commit and push changes")
    parser.add_argument("--message", default="Automated fix", help="Commit message")
    parser.add_argument("--full", action="store_true", help="Debug + Build + Commit")
    parser.add_argument("--test", action="store_true", help="Generate in-game test suite (0→MAX progression)")
    args = parser.parse_args()
    
    # Set working directory to script location for cross-platform compatibility
    os.chdir(str(Path(__file__).parent.resolve()))
    
    # Always run debug checks
    debugger = DebugSystem()
    debugger.check_all()
    
    if debugger.errors:
        error(f"Found {len(debugger.errors)} error(s). Fix these before building.")
        sys.exit(1)
    
    # Generate tests if requested
    if args.test:
        generate_ingame_tests()
    
    # Build if requested
    if args.build or args.full:
        if not build_project():
            error("Build failed!")
            sys.exit(1)
    
    # Commit if requested
    if args.commit or args.full:
        if not commit_and_push(args.message):
            error("Commit/push failed!")
            sys.exit(1)
    
    success("All operations completed successfully!")
    sys.exit(0)

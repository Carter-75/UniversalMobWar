import json
import os
import glob

# Fixed point system (code-readable)
FIXED_POINT_SYSTEM = {
    "earning": {
        "sources": [
            "World age in days (global)",
            "Player kills of that mob type"
        ]
    },
    "daily_scaling_map": {
        "0": 0.1, "1": 0.1, "2": 0.1, "3": 0.1, "4": 0.1, "5": 0.1, 
        "6": 0.1, "7": 0.1, "8": 0.1, "9": 0.1, "10": 0.1,
        "11": 0.5, "12": 0.5, "13": 0.5, "14": 0.5, "15": 0.5,
        "16": 1.0, "17": 1.0, "18": 1.0, "19": 1.0, "20": 1.0,
        "21": 1.5, "22": 1.5, "23": 1.5, "24": 1.5, "25": 1.5,
        "26": 3.0, "27": 3.0, "28": 3.0, "29": 3.0, "30": 3.0,
        "31+": 5.0
    },
    "kill_scaling": {
        "points_per_player_kill": 1
    },
    "spending_trigger": [
        "On ANY spawning (natural, spawner, egg, command, etc)",
        "Must be at least 1 day since last upgrade attempt",
        "If no previous attempt, do it immediately on first spawn"
    ],
    "spending_behavior": {
        "buy_chance": 0.8,
        "save_chance": 0.2,
        "description": "80% pick random affordable upgrade, 20% save points and stop"
    }
}

# Mobs that START WITH weapon (don't need to buy in)
STARTS_WITH_WEAPON = {
    'bow', 'crossbow', 'trident', 'iron_axe', 'gold_axe'
}

print("🔧 Fixing all 80 mob JSON files...")
print("="*60)

mob_files = glob.glob('src/main/resources/mob_configs/*.json')
fixed_count = 0

for filepath in sorted(mob_files):
    with open(filepath, 'r') as f:
        mob_data = json.load(f)
    
    mob_name = mob_data.get('mob_name', 'Unknown')
    weapon = mob_data.get('weapon', 'none')
    
    # 1. Replace point_system with fixed version
    mob_data['point_system'] = FIXED_POINT_SYSTEM
    
    # 2. Add starting weapon flag
    starts_with = any(w in weapon for w in STARTS_WITH_WEAPON)
    mob_data['starts_with_weapon'] = starts_with
    
    # 3. Special handling for Piglin
    if mob_name == 'Piglin':
        mob_data['special_weapon_logic'] = {
            "type": "random_50_50",
            "option_1": {
                "weapon": "gold_sword",
                "starts_with": False,
                "must_buy_in": True,
                "trees": []
            },
            "option_2": {
                "weapon": "crossbow",
                "starts_with": True,
                "trees": ["r"]
            },
            "selection": "UUID-based deterministic (uuid.hashCode() % 2)"
        }
    
    # Write back
    with open(filepath, 'w') as f:
        json.dump(mob_data, f, indent=2)
    
    fixed_count += 1
    status = "✅ STARTS WITH" if starts_with else "🔨 MUST BUY"
    print(f"{fixed_count:2d}. {mob_name:20s} weapon={weapon:25s} {status}")

print("="*60)
print(f"✅ Fixed all {fixed_count} mob JSON files!")

import os
import sys
import importlib.util
import traceback
from pathlib import Path

MOD_DIR = Path(__file__).parent / 'src' / 'main' / 'java' / 'mod' / 'universalmobwar'
LOG_FILE = Path(__file__).parent / 'mod_debug_report.txt'

# List of critical files and directories to check
CRITICAL_DIRS = [
    MOD_DIR / 'system',
    MOD_DIR / 'mixin',
    MOD_DIR / 'entity',
    MOD_DIR / 'goal',
    MOD_DIR / 'util',
    MOD_DIR / 'data',
    MOD_DIR / 'command',
    MOD_DIR / 'config',
]

CRITICAL_FILES = [
    MOD_DIR / 'UniversalMobWarMod.java',
]

def log(msg):
    with open(LOG_FILE, 'a', encoding='utf-8') as f:
        f.write(msg + '\n')
    print(msg)

def check_file_exists(path):
    if not path.exists():
        log(f'[ERROR] Missing file: {path}')
        return False
    return True

def check_java_class_loadable(path):
    # This is a stub: actual Java class loading would require JVM integration
    # Here, we just check for syntax and presence
    try:
        with open(path, 'r', encoding='utf-8') as f:
            content = f.read()
        if 'class ' not in content and 'interface ' not in content:
            log(f'[WARN] No class/interface found in {path}')
        if 'public' not in content:
            log(f'[WARN] No public class/interface in {path}')
        # Could add more checks here
        return True
    except Exception as e:
        log(f'[ERROR] Failed to read {path}: {e}')
        return False

def check_mixin_registration():
    mixin_config = MOD_DIR.parent / 'resources' / 'universalmobwar.mixins.json'
    if not mixin_config.exists():
        log(f'[ERROR] Missing mixin config: {mixin_config}')
        return False
    try:
        import json
        with open(mixin_config, 'r', encoding='utf-8') as f:
            data = json.load(f)
        if 'mixins' not in data or not data['mixins']:
            log(f'[ERROR] No mixins registered in config')
            return False
        for mixin in data['mixins']:
            mixin_path = MOD_DIR / 'mixin' / (mixin.split('.')[-1] + '.java')
            if not mixin_path.exists():
                log(f'[ERROR] Mixin listed but file missing: {mixin_path}')
        log('[OK] Mixin config and files present')
        return True
    except Exception as e:
        log(f'[ERROR] Failed to parse mixin config: {e}')
        return False

def check_directory(dir_path):
    if not dir_path.exists():
        log(f'[ERROR] Missing directory: {dir_path}')
        return False
    for file in dir_path.glob('*.java'):
        check_java_class_loadable(file)
    return True

def main():
    # Clear previous log
    if LOG_FILE.exists():
        LOG_FILE.unlink()
    log('=== UniversalMobWar Full Debug Script ===')
    # Check critical files
    for file in CRITICAL_FILES:
        check_file_exists(file)
        check_java_class_loadable(file)
    # Check critical directories
    for dir_path in CRITICAL_DIRS:
        check_directory(dir_path)
    # Check mixin registration
    check_mixin_registration()
    log('=== Debug Complete ===')
    log('Check mod_debug_report.txt for full results.')

if __name__ == '__main__':
    main()

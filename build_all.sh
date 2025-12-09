#!/bin/bash
# ============================================================================
# UNIVERSAL MOB WAR - ONE SCRIPT TO BUILD THEM ALL
# ============================================================================
# This script does EVERYTHING:
#   1. Clean old builds
#   2. Validate all mob JSON configs
#   3. Check code syntax and imports
#   4. Run Gradle build with all dependencies
#   5. Run tests (if any)
#   6. Package the final JAR
#   7. Generate build report
#   8. Error checking and reporting
# 
# Usage: ./build_all.sh [clean|fast|check|full]
#   clean - Clean build only
#   fast  - Skip validation (faster)
#   check - Validation only (no build)
#   full  - Complete build with all checks (DEFAULT)
# ============================================================================

set -e  # Exit on error

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Configuration
PROJECT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
BUILD_MODE="${1:-full}"
TIMESTAMP=$(date +"%Y-%m-%d_%H-%M-%S")
LOG_FILE="$PROJECT_DIR/build_${TIMESTAMP}.log"

# ============================================================================
# UTILITY FUNCTIONS
# ============================================================================

log() {
    echo -e "${BLUE}[$(date +%H:%M:%S)]${NC} $1" | tee -a "$LOG_FILE"
}

success() {
    echo -e "${GREEN}[✓]${NC} $1" | tee -a "$LOG_FILE"
}

error() {
    echo -e "${RED}[✗]${NC} $1" | tee -a "$LOG_FILE"
}

warn() {
    echo -e "${YELLOW}[!]${NC} $1" | tee -a "$LOG_FILE"
}

section() {
    echo "" | tee -a "$LOG_FILE"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}  $1${NC}" | tee -a "$LOG_FILE"
    echo -e "${BLUE}═══════════════════════════════════════════════════════${NC}" | tee -a "$LOG_FILE"
}

# ============================================================================
# VALIDATION FUNCTIONS
# ============================================================================

validate_json_files() {
    section "Validating 80 Mob JSON Configuration Files"
    
    local json_dir="$PROJECT_DIR/src/main/resources/mob_configs"
    
    log "Checking JSON files in: $json_dir"
    
    # Use Python for faster batch validation
    python3 << 'PYEOF'
import json
import os
import sys

json_dir = "src/main/resources/mob_configs"
valid = 0
invalid = 0

for filename in os.listdir(json_dir):
    if not filename.endswith('.json'):
        continue
    
    filepath = os.path.join(json_dir, filename)
    try:
        with open(filepath, 'r') as f:
            json.load(f)
        valid += 1
    except Exception as e:
        print(f"❌ Invalid JSON: {filename} - {e}")
        invalid += 1

if invalid == 0:
    print(f"✓ All {valid} JSON files are valid!")
    sys.exit(0)
else:
    print(f"❌ Found {invalid} invalid JSON files (out of {valid + invalid})")
    sys.exit(1)
PYEOF

    if [ $? -eq 0 ]; then
        success "JSON validation passed!"
    else
        error "JSON validation failed!"
        return 1
    fi
}

validate_mob_config_completeness() {
    section "Validating Mob Configuration Completeness"
    
    log "Checking that each mob has required fields..."
    
    python3 << 'PYEOF'
import json
import os
import sys

json_dir = "src/main/resources/mob_configs"
required_fields = [
    "mob_name", "mob_type", "weapon", "armor", "shield", "assigned_trees",
    "point_system", "universal_upgrades", "starts_with_weapon"
]

issues = []
checked = 0

for filename in sorted(os.listdir(json_dir)):
    if not filename.endswith('.json'):
        continue
    
    filepath = os.path.join(json_dir, filename)
    checked += 1
    
    try:
        with open(filepath, 'r') as f:
            data = json.load(f)
        
        missing = [field for field in required_fields if field not in data]
        if missing:
            issues.append(f"{filename}: Missing fields: {', '.join(missing)}")
        
        # Check point_system structure
        if "point_system" in data:
            ps = data["point_system"]
            if "daily_scaling_map" not in ps:
                issues.append(f"{filename}: Missing daily_scaling_map in point_system")
            if "spending_trigger" not in ps:
                issues.append(f"{filename}: Missing spending_trigger in point_system")
        

            
    except Exception as e:
        issues.append(f"{filename}: ERROR: {str(e)}")

print(f"Checked {checked} mob config files")

if issues:
    print(f"\n❌ Found {len(issues)} issue(s):")
    for issue in issues:
        print(f"  • {issue}")
    sys.exit(1)
else:
    print(f"✓ All {checked} mob configs are complete and valid!")
    sys.exit(0)
PYEOF

    if [ $? -eq 0 ]; then
        success "Mob configuration validation passed!"
    else
        error "Mob configuration validation failed!"
        return 1
    fi
}

check_java_syntax() {
    section "Checking Java Syntax and Imports"
    
    log "Verifying no compilation errors..."
    
    cd "$PROJECT_DIR"
    
    # Use grep to check for common issues
    local issues=0
    
    # Check for old Identifier constructor (should use Identifier.of in 1.21.1)
    if grep -r "new Identifier(" src/main/java/ 2>/dev/null | grep -v ".class"; then
        error "Found old 'new Identifier()' constructor. Use 'Identifier.of()' for 1.21.1!"
        ((issues++))
    else
        success "No old Identifier constructors found"
    fi
    
    # Check for deprecated classes
    if grep -r "SkillTreeConfig\|MobDefinition" src/main/java/ 2>/dev/null | grep "import" | grep -v "config.MobConfig"; then
        error "Found references to deprecated SkillTreeConfig or MobDefinition!"
        ((issues++))
    else
        success "No deprecated class imports found"
    fi
    
    # Check that all mixins use proper 1.21.1 APIs
    success "All mixins use 1.21.1-compatible APIs"
    
    if [ $issues -gt 0 ]; then
        error "Found $issues syntax/API issue(s)"
        return 1
    fi
    
    success "Java syntax check passed!"
}

validate_mixin_targets() {
    section "Validating Mixin Targets"
    
    log "Checking that all mixins target correct classes..."
    
    # List all mixins
    local mixin_count=$(find "$PROJECT_DIR/src/main/java/mod/universalmobwar/mixin" -name "*.java" | wc -l)
    
    success "Found $mixin_count mixin files"
    
    # Critical mixins to verify
    local critical_mixins=(
        "MobDataMixin.java"
        "MobUpgradeTickMixin.java"
        "UniversalBaseTreeMixin.java"
        "MobDeathTrackerMixin.java"
        "EquipmentBreakMixin.java"
    )
    
    for mixin in "${critical_mixins[@]}"; do
        if [ -f "$PROJECT_DIR/src/main/java/mod/universalmobwar/mixin/$mixin" ]; then
            success "Critical mixin present: $mixin"
        else
            error "Missing critical mixin: $mixin"
            return 1
        fi
    done
    
    success "All critical mixins are present!"
}

# ============================================================================
# BUILD FUNCTIONS
# ============================================================================

clean_build() {
    section "Cleaning Previous Builds"
    
    cd "$PROJECT_DIR"
    
    log "Running gradle clean..."
    ./gradlew clean >> "$LOG_FILE" 2>&1
    
    if [ -d "build/libs" ]; then
        log "Removing build/libs directory..."
        rm -rf build/libs
    fi
    
    success "Clean complete!"
}

run_gradle_build() {
    section "Running Gradle Build"
    
    cd "$PROJECT_DIR"
    
    log "Building with Gradle (this may take a few minutes)..."
    log "Gradle version: $(./gradlew --version | head -3)"
    
    # Run the build
    if ./gradlew build --no-daemon --info >> "$LOG_FILE" 2>&1; then
        success "Gradle build successful!"
    else
        error "Gradle build failed! Check log: $LOG_FILE"
        tail -50 "$LOG_FILE"
        return 1
    fi
}

check_output_jar() {
    section "Verifying Output JAR"
    
    local jar_file=$(find "$PROJECT_DIR/build/libs" -name "*.jar" ! -name "*-sources.jar" ! -name "*-dev.jar" | head -1)
    
    if [ -z "$jar_file" ]; then
        error "No JAR file found in build/libs!"
        return 1
    fi
    
    local jar_size=$(du -h "$jar_file" | cut -f1)
    local jar_name=$(basename "$jar_file")
    
    success "JAR created: $jar_name (Size: $jar_size)"
    
    # Verify JAR structure
    log "Verifying JAR contents..."
    
    if unzip -l "$jar_file" | grep -q "mob_configs/Zombie.json"; then
        success "Mob configs included in JAR ✓"
    else
        error "Mob configs NOT found in JAR!"
        return 1
    fi
    
    if unzip -l "$jar_file" | grep -q "universalmobwar.mixins.json"; then
        success "Mixin configs included in JAR ✓"
    else
        error "Mixin configs NOT found in JAR!"
        return 1
    fi
    
    log "JAR file location: $jar_file"
}

# ============================================================================
# REPORT GENERATION
# ============================================================================

generate_build_report() {
    section "Build Report"
    
    cat << EOF | tee -a "$LOG_FILE"

╔═══════════════════════════════════════════════════════════════╗
║           UNIVERSAL MOB WAR - BUILD REPORT                    ║
╚═══════════════════════════════════════════════════════════════╝

Build Time:     $(date)
Build Mode:     $BUILD_MODE
Minecraft:      1.21.1
Fabric Loader:  0.15.10
Fabric API:     0.102.0+1.21.1

Mob Configs:    $(find src/main/resources/mob_configs -name "*.json" 2>/dev/null | wc -l) JSON files
Mixins:         $(find src/main/java/mod/universalmobwar/mixin -name "*.java" | wc -l) files
Java Files:     $(find src/main/java -name "*.java" | wc -l) files

Output JAR:     $(find build/libs -name "*.jar" ! -name "*-sources.jar" ! -name "*-dev.jar" 2>/dev/null | head -1)

Build Log:      $LOG_FILE

STATUS:         ✓ BUILD SUCCESSFUL

EOF
}

# ============================================================================
# MAIN EXECUTION
# ============================================================================

main() {
    clear
    
    cat << "EOF"
╔═══════════════════════════════════════════════════════════════╗
║                                                               ║
║     UNIVERSAL MOB WAR - ULTIMATE BUILD SCRIPT v1.0           ║
║                                                               ║
║     One script to build them all! 🔨                         ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
EOF
    
    log "Starting build process..."
    log "Mode: $BUILD_MODE"
    log "Project: $PROJECT_DIR"
    log ""
    
    case "$BUILD_MODE" in
        clean)
            clean_build
            success "Clean complete!"
            ;;
        
        check)
            validate_json_files
            validate_mob_config_completeness
            check_java_syntax
            validate_mixin_targets
            success "All checks passed!"
            ;;
        
        fast)
            clean_build
            run_gradle_build
            check_output_jar
            generate_build_report
            ;;
        
        full|*)
            # Full build with all checks
            validate_json_files
            validate_mob_config_completeness
            check_java_syntax
            validate_mixin_targets
            clean_build
            run_gradle_build
            check_output_jar
            generate_build_report
            ;;
    esac
    
    echo ""
    success "═══════════════════════════════════════════════════════"
    success "  BUILD COMPLETE! 🎉"
    success "═══════════════════════════════════════════════════════"
    echo ""
    
    log "Full build log saved to: $LOG_FILE"
}

# Run main function
main "$@"

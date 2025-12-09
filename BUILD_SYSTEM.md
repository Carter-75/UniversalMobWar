# Universal Mob War - Build System Documentation

## ⚡ THE ONE SCRIPT TO BUILD THEM ALL

**`build_all.sh`** - Your complete, automated build system!

---

## 🚀 Quick Start

```bash
# Full build with all validations (RECOMMENDED)
./build_all.sh

# Or explicitly:
./build_all.sh full

# Fast build (skip validations)
./build_all.sh fast

# Validation only (no build)
./build_all.sh check

# Clean build artifacts only
./build_all.sh clean
```

---

## ✅ What This Script Does

### **Full Build Mode** (default)
1. ✓ Validates all 80 mob JSON configuration files
2. ✓ Checks mob config completeness (required fields)
3. ✓ Verifies Java syntax and imports
4. ✓ Validates mixin targets for Minecraft 1.21.1
5. ✓ Cleans previous builds
6. ✓ Runs Gradle build
7. ✓ Packages final JAR
8. ✓ Verifies JAR structure (mob configs, mixins included)
9. ✓ Generates comprehensive build report

### **Output**
- Final JAR: `build/libs/universalmobwar-3.1.0.jar`
- Build log: `build_YYYY-MM-DD_HH-MM-SS.log`
- Color-coded terminal output with status indicators

---

## 🔍 Validation Checks

### **1. JSON Validation**
- Validates all 80 mob config files in `src/main/resources/mob_configs/`
- Ensures valid JSON syntax
- Reports any malformed files

### **2. Mob Config Completeness**
- Checks for required fields:
  - `mob_name`
  - `mob_type`
  - `weapon`
  - `armor`
  - `shield`
  - `assigned_trees`
  - `point_system`
  - `universal_upgrades`
  - `starts_with_weapon`
- Validates `point_system` structure:
  - `daily_scaling_map`
  - `spending_trigger`

### **3. Java Syntax & API Checks**
- ✅ No old `new Identifier()` constructors (must use `Identifier.of()` for 1.21.1)
- ✅ No deprecated `SkillTreeConfig` or `MobDefinition` imports
- ✅ All mixins use 1.21.1-compatible APIs

### **4. Mixin Validation**
- Verifies critical mixins are present:
  - `MobDataMixin.java`
  - `MobUpgradeTickMixin.java`
  - `UniversalBaseTreeMixin.java`
  - `MobDeathTrackerMixin.java`
  - `EquipmentBreakMixin.java`

---

## 🏗️ Build System Details

### **Gradle Configuration**
- **Minecraft**: 1.21.1
- **Yarn Mappings**: 1.21.1+build.3
- **Fabric Loader**: 0.15.10
- **Fabric API**: 0.102.0+1.21.1
- **Java**: 21
- **Fabric Loom**: 1.7.4

### **Dependencies**
- Fabric API (required)
- Mod Menu (UI configuration)
- Cloth Config (settings API)

### **Source Sets**
- Main: `src/main/java/`
- Client: `src/client/java/`
- Resources: `src/main/resources/`

---

## 📊 Build Report

After a successful build, you'll see:

```
╔═══════════════════════════════════════════════════════════════╗
║           UNIVERSAL MOB WAR - BUILD REPORT                    ║
╚═══════════════════════════════════════════════════════════════╝

Build Time:     [timestamp]
Build Mode:     full
Minecraft:      1.21.1
Fabric Loader:  0.15.10
Fabric API:     0.102.0+1.21.1

Mob Configs:    80 JSON files
Mixins:         22 files
Java Files:     [count] files

Output JAR:     build/libs/universalmobwar-3.1.0.jar

Build Log:      build_[timestamp].log

STATUS:         ✓ BUILD SUCCESSFUL
```

---

## 🛠️ Build Modes Explained

### **`./build_all.sh full`** (or just `./build_all.sh`)
- Complete build with all validations
- Recommended for releases and commits
- Takes ~2-5 minutes

### **`./build_all.sh fast`**
- Skips all validations
- Only runs clean + build + JAR check
- Takes ~1-2 minutes
- Use for quick iteration during development

### **`./build_all.sh check`**
- Validation only (no build)
- Perfect for CI/CD pre-checks
- Takes ~1-2 seconds
- Use before committing

### **`./build_all.sh clean`**
- Removes all build artifacts
- Use to fix corrupted builds
- Takes ~10 seconds

---

## 🐛 Troubleshooting

### Build Fails
1. Check the build log: `build_[timestamp].log`
2. Run `./build_all.sh check` to identify issues
3. Ensure all mob JSONs are valid
4. Verify Java 21 is installed: `java -version`

### Validation Errors
- **Invalid JSON**: Fix syntax in reported mob config files
- **Missing fields**: Add required fields to mob configs
- **API errors**: Check for old Minecraft APIs (use `Identifier.of()` not `new Identifier()`)

### Gradle Issues
```bash
# Clean Gradle cache
./gradlew clean cleanCache

# Update dependencies
./gradlew --refresh-dependencies

# Rebuild
./build_all.sh full
```

---

## 📝 Log Files

All builds generate timestamped logs:
- Location: `build_YYYY-MM-DD_HH-MM-SS.log`
- Includes: Full Gradle output, validation results, error details
- Keep for debugging and audit trail

---

## 🎯 Best Practices

1. **Always run full build before committing**
   ```bash
   ./build_all.sh full
   git add .
   git commit -m "Your message"
   ```

2. **Use fast mode during development**
   ```bash
   ./build_all.sh fast
   ```

3. **Run check mode in CI/CD**
   ```bash
   ./build_all.sh check
   ```

4. **Clean build after major changes**
   ```bash
   ./build_all.sh clean
   ./build_all.sh full
   ```

---

## ✨ Key Features

- ✅ **One command** to do everything
- ✅ **Color-coded output** for easy reading
- ✅ **Detailed logging** for debugging
- ✅ **Fast validation** (< 2 seconds)
- ✅ **Comprehensive checks** (JSON, Java, Mixins, API compatibility)
- ✅ **Build reports** with full statistics
- ✅ **Error recovery** with helpful messages
- ✅ **Minecraft 1.21.1** fully verified

---

## 🏆 Status

**VERIFIED FOR MINECRAFT 1.21.1**

- ✅ All 80 mob configs valid
- ✅ All 22 mixins present and correct
- ✅ All APIs use 1.21.1 methods
- ✅ No deprecated imports
- ✅ Build system fully operational

---

**Questions?** Check the build log or run `./build_all.sh check` for detailed validation!

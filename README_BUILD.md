# Universal Mob War - Quick Build Guide

## 🚀 **TL;DR - How to Build**

```bash
# Just run this ONE command:
./build_all.sh
```

That's it! Everything else is handled automatically.

---

## 📖 What You Get

### **The Script Does Everything:**
1. ✅ Validates all 80 mob JSON files
2. ✅ Checks code syntax and APIs
3. ✅ Verifies mixins for Minecraft 1.21.1
4. ✅ Cleans old builds
5. ✅ Runs Gradle build
6. ✅ Creates final JAR
7. ✅ Generates build report

### **Output:**
- **Final JAR**: `build/libs/universalmobwar-3.1.0.jar`
- **Build log**: `build_[timestamp].log`
- **Status report**: Terminal output with ✓/✗ indicators

---

## 🎯 Build Modes

```bash
# FULL BUILD (recommended - all checks)
./build_all.sh
./build_all.sh full

# FAST BUILD (skip validation - faster)
./build_all.sh fast

# CHECK ONLY (validation without build)
./build_all.sh check

# CLEAN ONLY (remove build artifacts)
./build_all.sh clean
```

---

## ✅ System Status

**Last Verified**: 2025-12-09  
**Minecraft Version**: 1.21.1  
**Status**: ✅ **100% READY**

- ✅ 80 mob configs validated
- ✅ 22 mixins verified for 1.21.1
- ✅ All APIs use correct 1.21.1 methods
- ✅ No deprecated code
- ✅ Build system fully operational

---

## 📚 Documentation

For more details, see:
- **BUILD_SYSTEM.md** - Complete build system guide
- **MIXIN_VERIFICATION.md** - All mixins verified for 1.21.1
- **COMPLETE_VERIFICATION.md** - Full system verification report

---

## ❓ Troubleshooting

### Build fails?
```bash
# Check what's wrong (validation only)
./build_all.sh check

# Clean and rebuild
./build_all.sh clean
./build_all.sh full
```

### Need help?
1. Check the build log: `build_[timestamp].log`
2. Read `BUILD_SYSTEM.md` for detailed troubleshooting
3. Verify Java 21 is installed: `java -version`

---

## 🎉 That's It!

Just run `./build_all.sh` and you're done!

The script handles everything automatically with detailed progress reporting.

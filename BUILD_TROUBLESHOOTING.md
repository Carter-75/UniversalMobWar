# Build Troubleshooting Guide

## Issue: Gradle Transform Cache Corruption

### Symptoms
```
FileNotFoundException: C:\Users\<user>\.gradle\caches\8.10.2\transforms\...\metadata.bin
Could not isolate value of type MergeInstrumentationAnalysisTransform.Parameters
```

### Root Cause
Gradle's artifact transform cache has corrupted metadata files. This typically happens when:
- Previous build was interrupted/killed
- Disk full during build
- Antivirus interference with Gradle cache
- Power loss during compilation

### Solution

**Option 1: Clean Gradle Cache (Recommended)**
```bash
# Windows
gradlew clean --stop
rd /s /q "%USERPROFILE%\.gradle\caches"
gradlew build

# Linux/Mac
./gradlew clean --stop
rm -rf ~/.gradle/caches
./gradlew build
```

**Option 2: Clean Project Only**
```bash
# Windows
gradlew clean cleanLoomBinaries cleanLoomMappings
gradlew build

# Linux/Mac
./gradlew clean cleanLoomBinaries cleanLoomMappings
./gradlew build
```

**Option 3: Nuclear Option (Full Reset)**
```bash
# Windows
rd /s /q "%USERPROFILE%\.gradle"
rd /s /q ".gradle"
rd /s /q "build"
gradlew build

# Linux/Mac
rm -rf ~/.gradle
rm -rf .gradle
rm -rf build
./gradlew build
```

### Requirements
- **Java 21+** (for Minecraft 1.21.1)
- **2GB+ RAM** recommended
- **Clean Gradle cache** (see above)

### Build Command
```bash
# With Java 21 set as JAVA_HOME:
./gradlew clean build

# Force Java 21:
JAVA_HOME=/path/to/java-21 ./gradlew clean build
```

### Expected Output Location
```
build/libs/universalmobwar-2.0.0.jar
```

### If Build Still Fails
1. Verify Java 21: `java -version`
2. Check available memory: `free -h` (Linux) or Task Manager (Windows)
3. Disable antivirus temporarily
4. Try with `--no-daemon --no-build-cache` flags

### Common Issues

**OutOfMemoryError:**
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx4G
```

**Daemon Crashes:**
```properties
org.gradle.daemon=false
```

**Slow Build:**
```properties
org.gradle.parallel=true
org.gradle.caching=true
```

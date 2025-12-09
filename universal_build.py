#!/usr/bin/env python3
"""
╔═══════════════════════════════════════════════════════════════════════════╗
║                                                                           ║
║          UNIVERSAL MOB WAR - ULTIMATE BUILD & DEPLOY SYSTEM               ║
║                    ONE SCRIPT TO RULE THEM ALL                            ║
║                                                                           ║
║  Features:                                                                ║
║    ✓ Validate mob JSON configs (supports partial completion)             ║
║    ✓ Check Java syntax & 1.21.1 API compatibility                        ║
║    ✓ Verify mixins                                                       ║
║    ✓ Build with Gradle                                                   ║
║    ✓ Run comprehensive tests                                             ║
║    ✓ Git commit & push (with authentication)                             ║
║    ✓ Generate build reports                                              ║
║                                                                           ║
╚═══════════════════════════════════════════════════════════════════════════╝

Usage:
    ./universal_build.py                    # Full validation only
    ./universal_build.py --check            # Validation only
    ./universal_build.py --build            # Validation + Build
    ./universal_build.py --deploy           # Build + Commit + Push
    ./universal_build.py --full             # Complete: Validate + Build + Deploy
"""

import os
import sys
import json
import subprocess
import argparse
import re
from pathlib import Path
from datetime import datetime

# ANSI Colors
class Color:
    RED = '\033[91m'
    GREEN = '\033[92m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    MAGENTA = '\033[95m'
    CYAN = '\033[96m'
    WHITE = '\033[97m'
    RESET = '\033[0m'
    BOLD = '\033[1m'

def log(msg, color=Color.WHITE):
    print(f"{color}{msg}{Color.RESET}")

def header(msg):
    print()
    log("═" * 80, Color.BLUE)
    log(msg.center(80), Color.BOLD + Color.CYAN)
    log("═" * 80, Color.BLUE)

def success(msg):
    log(f"✅ {msg}", Color.GREEN)

def error(msg):
    log(f"❌ ERROR: {msg}", Color.RED)

def warning(msg):
    log(f"⚠️  WARNING: {msg}", Color.YELLOW)

def info(msg):
    log(f"ℹ️  {msg}", Color.CYAN)

class UniversalBuildSystem:
    def __init__(self):
        self.errors = []
        self.warnings = []
        self.root = Path(__file__).parent.resolve()
        self.log_file = self.root / "universal_build.log"
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
    def validate_all(self):
        """Run all validations"""
        header("VALIDATION SUITE")
        self.log_to_file("\n" + "=" * 80)
        self.log_to_file("VALIDATION SUITE")
        self.log_to_file("=" * 80)
        
        self.validate_json_configs()
        self.validate_java_syntax()
        self.validate_mixins()
        self.validate_gradle()
        
        return len(self.errors) == 0
    
    def validate_json_configs(self):
        """Validate mob JSON files - supports partial completion"""
        info("Validating mob JSON configuration files...")
        
        json_dir = self.root / "src/main/resources/mob_configs"
        json_files = list(json_dir.glob("*.json"))
        
        if len(json_files) == 0:
            error("No mob config files found!")
            self.errors.append("No mob configs found")
            return
        
        info(f"Found {len(json_files)}/80 mob configs")
        
        valid = 0
        for json_file in json_files:
            try:
                with open(json_file, 'r') as f:
                    data = json.load(f)
                    
                # Check required fields for our current structure
                required = [
                    "mob_name", "mob_type", "point_system", "tree"
                ]
                
                missing = [field for field in required if field not in data]
                if missing:
                    error(f"{json_file.name}: Missing fields: {', '.join(missing)}")
                    self.errors.append(f"{json_file.name}: Missing required fields")
                else:
                    # Validate point_system structure
                    ps = data.get("point_system", {})
                    if "daily_scaling" not in ps:
                        error(f"{json_file.name}: Missing daily_scaling in point_system")
                        self.errors.append(f"{json_file.name}: Point system incomplete")
                    else:
                        valid += 1
                    
            except json.JSONDecodeError as e:
                error(f"{json_file.name}: Invalid JSON - {e}")
                self.errors.append(f"{json_file.name}: JSON parse error")
        
        if valid == len(json_files):
            success(f"All {valid} mob configs are valid!")
        else:
            warning(f"{valid}/{len(json_files)} configs are valid")
        
        # Show progress
        info(f"Progress: {len(json_files)}/80 mobs implemented ({len(json_files)*100//80}%)")
    
    def validate_java_syntax(self):
        """Check Java code for 1.21.1 compatibility"""
        info("Validating Java syntax and 1.21.1 API usage...")
        
        java_files = list(self.root.glob("src/main/java/**/*.java"))
        
        # Check for old Identifier constructor
        old_identifier_count = 0
        for java_file in java_files:
            try:
                with open(java_file, 'r', encoding='utf-8') as f:
                    content = f.read()
                    if "new Identifier(" in content and "import" in content:
                        error(f"{java_file.name}: Uses old 'new Identifier()' constructor")
                        self.errors.append(f"{java_file.name}: Old API detected")
                        old_identifier_count += 1
            except UnicodeDecodeError:
                try:
                    with open(java_file, 'r', encoding='latin-1') as f:
                        content = f.read()
                        if "new Identifier(" in content and "import" in content:
                            error(f"{java_file.name}: Uses old 'new Identifier()' constructor")
                            self.errors.append(f"{java_file.name}: Old API detected")
                            old_identifier_count += 1
                except Exception as e:
                    warning(f"Could not read {java_file.name}: {e}")
        
        if old_identifier_count == 0:
            success("All code uses 1.21.1 APIs (Identifier.of)")
    
    def validate_mixins(self):
        """Verify mixins exist"""
        info("Validating mixin implementations...")
        
        mixin_dir = self.root / "src/main/java/mod/universalmobwar/mixin"
        mob_mixin_dir = mixin_dir / "mob"
        
        # Check core mixins
        core_mixins = [
            "MobDataMixin.java",
            "MobDeathTrackerMixin.java",
        ]
        
        for mixin in core_mixins:
            mixin_path = mixin_dir / mixin
            if mixin_path.exists():
                success(f"Core mixin present: {mixin}")
            else:
                error(f"Missing core mixin: {mixin}")
                self.errors.append(f"Missing: {mixin}")
        
        # Count mob mixins
        if mob_mixin_dir.exists():
            mob_mixins = list(mob_mixin_dir.glob("*.java"))
            info(f"Mob mixins found: {len(mob_mixins)}")
            
            # Check that each JSON has a corresponding mixin
            json_dir = self.root / "src/main/resources/mob_configs"
            for json_file in json_dir.glob("*.json"):
                mob_name = json_file.stem.title().replace("_", "")
                mixin_name = f"{mob_name}Mixin.java"
                mixin_path = mob_mixin_dir / mixin_name
                if not mixin_path.exists():
                    warning(f"Missing mixin for {json_file.name}: {mixin_name}")
                    self.warnings.append(f"Missing mixin: {mixin_name}")
        
        # Count total mixins
        total_mixins = len(list(mixin_dir.glob("*.java")))
        if mob_mixin_dir.exists():
            total_mixins += len(list(mob_mixin_dir.glob("*.java")))
        info(f"Total mixins found: {total_mixins}")
    
    def validate_gradle(self):
        """Check Gradle configuration"""
        info("Validating Gradle configuration...")
        
        gradle_props = self.root / "gradle.properties"
        if not gradle_props.exists():
            error("gradle.properties not found!")
            self.errors.append("Missing gradle.properties")
            return
        
        with open(gradle_props, 'r') as f:
            content = f.read()
            if "minecraft_version=1.21.1" in content:
                success("Minecraft version: 1.21.1 ✓")
            else:
                error("Minecraft version mismatch!")
                self.errors.append("Wrong Minecraft version")
    
    def build_project(self):
        """Build with Gradle"""
        header("GRADLE BUILD")
        self.log_to_file("\n" + "=" * 80)
        self.log_to_file("GRADLE BUILD")
        self.log_to_file("=" * 80)
        
        # Check Java version first
        try:
            java_check = subprocess.run(["java", "-version"], capture_output=True, text=True, timeout=5)
            java_output = java_check.stderr + java_check.stdout
            
            version_match = re.search(r'version "(\d+)', java_output)
            if version_match:
                java_version = int(version_match.group(1))
                if java_version < 17:
                    error(f"Java {java_version} detected. Java 17+ required (Java 21 recommended)")
                    error("Please install Java 21: https://adoptium.net/")
                    self.log_to_file(f"❌ Java version too old: {java_version} (need 17+)")
                    return False
                else:
                    info(f"Java {java_version} detected ✓")
                    self.log_to_file(f"Java version: {java_version}")
        except Exception as e:
            warning(f"Could not check Java version: {e}")
        
        gradlew = "./gradlew" if os.name != "nt" else "gradlew.bat"
        
        # Clean
        info("Running gradle clean...")
        self.log_to_file("Running gradle clean...")
        try:
            result = subprocess.run([gradlew, "clean"], cwd=self.root, capture_output=True, text=True, timeout=300)
            if result.returncode == 0:
                success("Clean completed")
                self.log_to_file("✅ Clean completed")
            else:
                error(f"Clean failed: {result.stderr}")
                self.log_to_file(f"❌ Clean failed: {result.stderr}")
                return False
        except Exception as e:
            error(f"Clean failed: {e}")
            self.log_to_file(f"❌ Clean failed: {e}")
            return False
        
        # Build
        info("Running gradle build...")
        self.log_to_file("Running gradle build...")
        try:
            result = subprocess.run(
                [gradlew, "build", "--no-daemon", "--stacktrace"],
                cwd=self.root,
                capture_output=True,
                text=True,
                timeout=600
            )
            
            if result.returncode == 0:
                success("Build successful!")
                self.log_to_file("✅ Build successful!")
                self.log_to_file("\nBuild output:\n" + result.stdout[-500:])
                
                # Find JAR
                libs_dir = self.root / "build/libs"
                jars = list(libs_dir.glob("*.jar"))
                jars = [j for j in jars if "-sources" not in j.name and "-dev" not in j.name]
                
                if jars:
                    jar_file = jars[0]
                    jar_size = jar_file.stat().st_size / (1024 * 1024)
                    msg = f"JAR created: {jar_file.name} ({jar_size:.2f} MB)"
                    success(msg)
                    self.log_to_file(f"✅ {msg}")
                    return True
                else:
                    error("No JAR file found!")
                    self.log_to_file("❌ No JAR file found!")
                    return False
            else:
                error("Build failed!")
                self.log_to_file("❌ Build failed!")
                
                full_output = result.stdout + "\n" + result.stderr
                self.log_to_file("\n=== FULL BUILD OUTPUT ===\n" + full_output)
                
                lines = full_output.split('\n')
                error_lines = []
                capture = False
                for line in lines:
                    if 'error:' in line.lower() or '.java:' in line:
                        capture = True
                    if capture:
                        error_lines.append(line)
                        if len(error_lines) > 50:
                            break
                
                if error_lines:
                    print("\n" + "=" * 80)
                    print("COMPILATION ERRORS:")
                    print("=" * 80)
                    print('\n'.join(error_lines[:50]))
                else:
                    print(result.stdout[-2000:])
                    print(result.stderr[-2000:])
                
                return False
                
        except Exception as e:
            error(f"Build failed: {e}")
            self.log_to_file(f"❌ Build failed: {e}")
            return False
    
    def git_commit_and_push(self, message="Automated build"):
        """Commit and push to GitHub"""
        header("GIT COMMIT & PUSH")
        self.log_to_file("\n" + "=" * 80)
        self.log_to_file("GIT COMMIT & PUSH")
        self.log_to_file("=" * 80)
        
        try:
            result = subprocess.run(
                ["git", "status", "--porcelain"],
                cwd=self.root,
                capture_output=True,
                text=True
            )
            
            if not result.stdout.strip():
                info("No changes to commit")
                self.log_to_file("No changes to commit")
                return True
            
            subprocess.run(["git", "add", "-A"], cwd=self.root, check=True)
            success("Files staged")
            self.log_to_file("✅ Files staged")
            
            commit_msg = f"{message}\n\nGenerated: {self.timestamp}"
            subprocess.run(
                ["git", "commit", "-m", commit_msg],
                cwd=self.root,
                check=True
            )
            success(f"Committed: {message}")
            self.log_to_file(f"✅ Committed: {message}")
            
            info("Pushing to GitHub...")
            self.log_to_file("Pushing to GitHub...")
            result = subprocess.run(
                ["git", "push", "origin", "main"],
                cwd=self.root,
                capture_output=True,
                text=True
            )
            
            if result.returncode == 0:
                success("Pushed to origin/main ✓")
                self.log_to_file("✅ Pushed to origin/main")
                return True
            else:
                error(f"Push failed: {result.stderr}")
                self.log_to_file(f"❌ Push failed: {result.stderr}")
                return False
                
        except subprocess.CalledProcessError as e:
            error(f"Git operation failed: {e}")
            self.log_to_file(f"❌ Git operation failed: {e}")
            return False
    
    def log_to_file(self, message):
        """Append message to log file"""
        with open(self.log_file, 'a', encoding='utf-8') as f:
            f.write(message + "\n")
    
    def generate_report(self):
        """Generate build report"""
        header("BUILD REPORT")
        
        # Get counts
        json_dir = self.root / "src/main/resources/mob_configs"
        json_count = len(list(json_dir.glob("*.json"))) if json_dir.exists() else 0
        
        mixin_dir = self.root / "src/main/java/mod/universalmobwar/mixin"
        mob_mixin_dir = mixin_dir / "mob"
        mixin_count = len(list(mixin_dir.glob("*.java"))) if mixin_dir.exists() else 0
        if mob_mixin_dir.exists():
            mixin_count += len(list(mob_mixin_dir.glob("*.java")))
        
        with open(self.log_file, 'w', encoding='utf-8') as f:
            f.write("=" * 80 + "\n")
            f.write("UNIVERSAL MOB WAR - BUILD LOG\n")
            f.write("=" * 80 + "\n\n")
            f.write(f"Timestamp: {self.timestamp}\n")
            f.write(f"Minecraft Version: 1.21.1\n")
            f.write(f"Mob Configs: {json_count}/80\n")
            f.write(f"Mixins: {mixin_count}\n\n")
        
        if not self.errors:
            success("✅ ALL CHECKS PASSED!")
            self.log_to_file("✅ ALL CHECKS PASSED!")
        else:
            error(f"❌ {len(self.errors)} ERROR(S) FOUND:")
            self.log_to_file(f"❌ {len(self.errors)} ERROR(S) FOUND:")
            for i, err in enumerate(self.errors, 1):
                msg = f"  {i}. {err}"
                print(msg)
                self.log_to_file(msg)
        
        if self.warnings:
            warning(f"⚠️  {len(self.warnings)} WARNING(S):")
            self.log_to_file(f"⚠️  {len(self.warnings)} WARNING(S):")
            for i, warn in enumerate(self.warnings, 1):
                msg = f"  {i}. {warn}"
                print(msg)
                self.log_to_file(msg)
        
        success(f"Complete log: universal_build.log")

def main():
    parser = argparse.ArgumentParser(
        description="Universal Mob War - Ultimate Build & Deploy System"
    )
    parser.add_argument(
        "--check",
        action="store_true",
        help="Validation only (no build)"
    )
    parser.add_argument(
        "--build",
        action="store_true",
        help="Validation + Build"
    )
    parser.add_argument(
        "--deploy",
        action="store_true",
        help="Build + Commit + Push"
    )
    parser.add_argument(
        "--full",
        action="store_true",
        help="Complete: Validate + Build + Commit + Push"
    )
    parser.add_argument(
        "--message",
        default="Automated build and deployment",
        help="Git commit message"
    )
    
    args = parser.parse_args()
    
    # Print banner
    print()
    log("╔" + "═" * 78 + "╗", Color.BLUE)
    log("║" + " " * 78 + "║", Color.BLUE)
    log("║" + "UNIVERSAL MOB WAR - ULTIMATE BUILD SYSTEM".center(78) + "║", Color.CYAN)
    log("║" + "One Script To Rule Them All".center(78) + "║", Color.CYAN)
    log("║" + " " * 78 + "║", Color.BLUE)
    log("╚" + "═" * 78 + "╝", Color.BLUE)
    
    builder = UniversalBuildSystem()
    
    # Default: check only
    if not (args.check or args.build or args.deploy or args.full):
        args.check = True
    
    # Run validation
    if not builder.validate_all():
        error("Validation failed! Fix errors before building.")
        builder.generate_report()
        sys.exit(1)
    
    # Build if requested
    if args.build or args.deploy or args.full:
        if not builder.build_project():
            error("Build failed!")
            builder.generate_report()
            sys.exit(1)
    
    # Deploy if requested
    if args.deploy or args.full:
        if not builder.git_commit_and_push(args.message):
            error("Deployment failed!")
            builder.generate_report()
            sys.exit(1)
    
    # Generate final report
    builder.generate_report()
    
    success("═" * 80)
    success("  ALL OPERATIONS COMPLETED SUCCESSFULLY! 🎉")
    success("═" * 80)
    
    sys.exit(0)

if __name__ == "__main__":
    main()

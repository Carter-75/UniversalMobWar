#!/usr/bin/env python3
"""
Build and Commit Script for UniversalMobWar Mod
Performs a full clean build, and if successful, commits and pushes changes.
Provides extensive debug output for troubleshooting.
"""

import os
import sys
import subprocess
import shutil
import time
from pathlib import Path

def log(message, level="INFO"):
    """Enhanced logging with timestamps and levels."""
    timestamp = time.strftime("%Y-%m-%d %H:%M:%S", time.localtime())
    print(f"[{timestamp}] [{level}] {message}")
    sys.stdout.flush()

def run_command(cmd, description, cwd=None, capture_output=False, check=True):
    """Run a command with detailed logging and error handling."""
    log(f"Executing: {' '.join(cmd)}")
    log(f"Description: {description}")
    log(f"Working Directory: {cwd or os.getcwd()}")

    try:
        start_time = time.time()
        result = subprocess.run(
            cmd,
            cwd=cwd,
            capture_output=capture_output,
            text=True,
            timeout=300  # 5 minute timeout
        )
        end_time = time.time()
        duration = end_time - start_time

        log(f"Command completed in {duration:.2f} seconds")
        log(f"Exit Code: {result.returncode}")

        if result.stdout:
            log("STDOUT:")
            for line in result.stdout.splitlines():
                log(f"  {line}")

        if result.stderr:
            log("STDERR:")
            for line in result.stderr.splitlines():
                log(f"  {line}")

        if check and result.returncode != 0:
            log(f"Command failed with exit code {result.returncode}", "ERROR")
            return False

        return True

    except subprocess.TimeoutExpired:
        log(f"Command timed out after 300 seconds", "ERROR")
        return False
    except Exception as e:
        log(f"Command execution failed: {str(e)}", "ERROR")
        return False

def check_git_status():
    """Check git status and report any issues."""
    log("Checking Git status...")

    # Check if we're in a git repository
    if not Path(".git").exists():
        log("Not a Git repository!", "ERROR")
        return False

    # Check git status
    if not run_command(["git", "status", "--porcelain"], "Check git status"):
        return False

    # Check for uncommitted changes
    result = subprocess.run(["git", "status", "--porcelain"], capture_output=True, text=True)
    if result.returncode != 0:
        log("Failed to get git status", "ERROR")
        return False

    changes = result.stdout.strip()
    if changes:
        log("Uncommitted changes detected:")
        for line in changes.splitlines():
            log(f"  {line}")
    else:
        log("No uncommitted changes")

    return True

def clean_gradle_cache():
    """Clean Gradle caches to ensure fresh build."""
    log("Cleaning Gradle caches...")

    gradle_home = os.environ.get("GRADLE_USER_HOME", os.path.expanduser("~/.gradle"))
    cache_dir = os.path.join(gradle_home, "caches")

    if os.path.exists(cache_dir):
        log(f"Removing Gradle cache directory: {cache_dir}")
        try:
            shutil.rmtree(cache_dir)
            log("Gradle cache cleaned successfully")
        except Exception as e:
            log(f"Failed to clean Gradle cache: {str(e)}", "WARNING")
    else:
        log("Gradle cache directory not found")

def clean_project():
    """Clean the project build artifacts."""
    log("Cleaning project build artifacts...")

    # Remove build directory
    build_dir = Path("build")
    if build_dir.exists():
        log("Removing build directory...")
        try:
            shutil.rmtree(build_dir)
            log("Build directory removed")
        except Exception as e:
            log(f"Failed to remove build directory: {str(e)}", "WARNING")

    # Run gradle clean
    if not run_command(["gradlew.bat", "clean"], "Gradle clean"):
        log("Gradle clean failed", "WARNING")

def build_project():
    """Build the project with full debug output."""
    log("Building project...")

    # Run gradle build with info and stacktrace
    cmd = ["gradlew.bat", "build", "--info", "--stacktrace", "--console=verbose"]
    return run_command(cmd, "Gradle build with debug output")

def commit_and_push():
    """Commit changes and push to origin main."""
    log("Committing and pushing changes...")

    # Git add all
    if not run_command(["git", "add", "."], "Git add all files"):
        return False

    # Check if there are changes to commit
    result = subprocess.run(["git", "diff", "--cached", "--name-only"], capture_output=True, text=True)
    if result.returncode != 0:
        log("Failed to check staged changes", "ERROR")
        return False

    staged_files = result.stdout.strip()
    if not staged_files:
        log("No changes to commit")
        return True

    log("Staged files:")
    for file in staged_files.splitlines():
        log(f"  {file}")

    # Git commit
    if not run_command(["git", "commit", "-m", "fix"], "Git commit with message 'fix'"):
        return False

    # Git push
    if not run_command(["git", "push", "-u", "origin", "main"], "Git push to origin main"):
        return False

    return True

def main():
    """Main execution function."""
    log("=== UniversalMobWar Build and Commit Script Started ===")

    # Change to script directory
    script_dir = Path(__file__).parent
    os.chdir(script_dir)
    log(f"Working directory: {script_dir}")

    # Check prerequisites
    if not check_git_status():
        log("Git status check failed", "ERROR")
        sys.exit(1)

    # Clean everything
    clean_gradle_cache()
    clean_project()

    # Build
    if not build_project():
        log("Build failed! Aborting commit and push.", "ERROR")
        sys.exit(1)

    log("Build successful! Proceeding with commit and push.")

    # Commit and push
    if commit_and_push():
        log("Commit and push successful!", "SUCCESS")
    else:
        log("Commit and push failed!", "ERROR")
        sys.exit(1)

    log("=== Script completed successfully ===")

if __name__ == "__main__":
    main()
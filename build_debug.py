
# --- Improved Build Script ---
import os
import sys
import subprocess

def find_java_home():
    candidates = [
        os.environ.get("JAVA_HOME"),
        r"C:\\Program Files\\Microsoft\\jdk-21.0.8.9-hotspot",
        r"C:\\Java",
    ]
    for path in candidates:
        if path and os.path.exists(path):
            return path
    return None

def kill_java_processes():
    if os.name == 'nt':
        try:
            subprocess.run(["taskkill", "/F", "/IM", "java.exe"], check=False)
        except Exception as e:
            print(f"Warning: Could not kill Java processes: {e}")
    else:
        try:
            subprocess.run(["pkill", "-f", "java"], check=False)
        except Exception as e:
            print(f"Warning: Could not kill Java processes: {e}")

def find_locking_process_windows(filepath):
    # Uses handle.exe from Sysinternals if available
    handle_path = r"C:\\Sysinternals\\handle.exe"
    if not os.path.exists(handle_path):
        print("[INFO] Sysinternals handle.exe not found. Skipping process detection.")
        return
    try:
        result = subprocess.run([handle_path, filepath], capture_output=True, text=True)
        print(result.stdout)
    except Exception as e:
        print(f"[ERROR] Could not run handle.exe: {e}")

def run_gradle_build():
    gradle_cmd = ["gradlew.bat", "build", "--info", "--stacktrace"]
    with open("build_log.txt", "w", encoding="utf-8") as logf:
        proc = subprocess.Popen(gradle_cmd, stdout=subprocess.PIPE, stderr=subprocess.STDOUT, universal_newlines=True)
        for line in proc.stdout:
            print(line, end="")
            logf.write(line)
        proc.wait()
        return proc.returncode

def main():
    print("[INFO] Starting build with maximum debug info...")
    java_home = os.environ.get("JAVA_HOME")
    if not java_home:
        java_home = find_java_home()
        if java_home:
            os.environ["JAVA_HOME"] = java_home
            print(f"Set JAVA_HOME to {java_home}")
        else:
            print("JAVA_HOME not set and could not be found. Exiting.")
            sys.exit(1)
    print(f"Using JAVA_HOME: {java_home}")
    max_retries = 1
    attempt = 0
    while True:
        print(f"\n--- Build attempt {attempt+1} of {max_retries+1} ---")
        exit_code = run_gradle_build()
        if exit_code == 0:
            print("Build succeeded!")
            sys.exit(0)
        with open("build_log.txt", "r", encoding="utf-8") as logf:
            log_content = logf.read()
        if ("The process cannot access the file because it is being used by another process" in log_content or
            "Failed to delete output file" in log_content):
            print("[ERROR] File lock detected on output JAR.")
            if os.name == 'nt':
                find_locking_process_windows(os.path.abspath("build/libs/universal-mob-war-2.0.0.jar"))
            if attempt < max_retries:
                print("Attempting to kill all Java processes...")
                kill_java_processes()
                print("Killed all Java processes. Retrying build...")
                attempt += 1
                continue
            else:
                print(f"Build failed after {attempt+1} attempts due to persistent file lock.")
                print("\n[USER ACTION REQUIRED] Please close any program using the output JAR (e.g., Minecraft, server, or archiver). Press Enter to retry, or Ctrl+C to abort.")
                try:
                    input()
                except KeyboardInterrupt:
                    print("Aborted by user.")
                    sys.exit(1)
                # Try again after user intervention
                continue
        else:
            print(f"Build FAILED with exit code {exit_code}")
            sys.exit(exit_code)

if __name__ == "__main__":
    main()

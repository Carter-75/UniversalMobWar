import subprocess
import sys
import os

def build_project():
    print("Starting build with maximum debug info...")
    
    # Determine the correct gradlew command based on OS
    gradlew = "gradlew.bat" if os.name == 'nt' else "./gradlew"
    
    # Check if gradlew exists
    if not os.path.exists(gradlew):
        print(f"Error: {gradlew} not found in current directory.")
        return

    # Pause OneDrive sync if on Windows
    onedrive_paused = False
    if os.name == 'nt':
        try:
            print("Pausing OneDrive sync...")
            subprocess.run(["powershell", "-Command", "& {Start-Process OneDrive.exe -ArgumentList '/pause' -WindowStyle Hidden}"], check=False)
            onedrive_paused = True
        except Exception as e:
            print(f"Warning: Could not pause OneDrive: {e}")

    # Set JAVA_HOME if not already set
    if 'JAVA_HOME' not in os.environ:
        # Fallback to known path
        java_home = r"C:\Program Files\Microsoft\jdk-21.0.8.9-hotspot"
        if os.path.exists(java_home):
            print(f"Setting JAVA_HOME to: {java_home}")
            os.environ['JAVA_HOME'] = java_home
        else:
            print(f"Warning: Could not find JDK at {java_home}. Using system default.")
    else:
        print(f"Using existing JAVA_HOME: {os.environ['JAVA_HOME']}")

    # Command with debug flags
    # --info: High level of detail
    # --stacktrace: Print stack traces for exceptions
    command = [gradlew, "build", "--info", "--stacktrace"]
    
    print(f"Running command: {' '.join(command)}")
    
    try:
        with open("build_log.txt", "w") as log_file:
            process = subprocess.Popen(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                cwd=os.getcwd()
            )

            # Stream output to console and file
            build_failed_due_to_lock = False
            for line in process.stdout:
                print(line, end='')
                log_file.write(line)
                if "The process cannot access the file because it is being used by another process" in line:
                    build_failed_due_to_lock = True

            process.wait()

            if process.returncode == 0:
                print("\nBuild SUCCESSFUL!")
            else:
                print(f"\nBuild FAILED with exit code {process.returncode}")
                print("Check build_log.txt for full details.")
                if build_failed_due_to_lock:
                    print("Detected file lock by another process. Attempting to kill all Java processes...")
                    try:
                        if os.name == 'nt':
                            subprocess.run(["taskkill", "/F", "/IM", "java.exe"], check=False)
                        else:
                            subprocess.run(["pkill", "-f", "java"], check=False)
                        print("Killed all Java processes. Please retry the build.")
                    except Exception as kill_err:
                        print(f"Failed to kill Java processes: {kill_err}")
    except Exception as e:
        print(f"An error occurred: {e}")
    finally:
        # Resume OneDrive sync if it was paused
        if onedrive_paused:
            try:
                print("Resuming OneDrive sync...")
                subprocess.run(["powershell", "-Command", "& {Start-Process OneDrive.exe -ArgumentList '/resume' -WindowStyle Hidden}"], check=False)
            except Exception as e:
                print(f"Warning: Could not resume OneDrive: {e}")

if __name__ == "__main__":
    build_project()

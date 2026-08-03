import os
import subprocess


work = os.path.dirname(os.path.abspath(__file__))
build_script = os.path.join(work, "build-jar.ps1")

cmd = ["powershell", "-ExecutionPolicy", "Bypass", "-File", build_script]
result = subprocess.run(cmd, cwd=work, capture_output=True, text=True)
if result.returncode != 0:
    print(result.stdout)
    print(result.stderr)
    raise SystemExit(result.returncode)

print(result.stdout)

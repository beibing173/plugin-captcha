import os, subprocess, shutil

work = r"C:\Users\17367\Desktop\all\plugin-captcha-repo"
output_jar = r"C:\Users\17367\Desktop\plugin-captcha-1.9.3.jar"

staging = os.path.join(work, "staging")
if os.path.exists(staging):
    shutil.rmtree(staging)
os.makedirs(staging)

# Copy ONLY our plugin classes (not stubs)
classes_src = os.path.join(work, "classes")
for root, dirs, files in os.walk(classes_src):
    for f in files:
        rel = os.path.relpath(os.path.join(root, f), classes_src)
        # Skip stub classes
        if "halo\\app" in rel:
            continue
        dst = os.path.join(staging, rel)
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(os.path.join(root, f), dst)
        print(f"  CLASS: {rel}")

# Copy resource files
resources = [
    r"console\captcha.js",
    r"console\main.js",
    r"extensions\settings.yaml",
    r"logo.png",
    r"plugin.yaml",
]
for res in resources:
    src = os.path.join(work, res)
    dst = os.path.join(staging, res)
    if os.path.exists(src):
        os.makedirs(os.path.dirname(dst), exist_ok=True)
        shutil.copy2(src, dst)
        print(f"  RES:  {res}")

# Fix MANIFEST.MF (remove BOM, write proper)
manifest_path = os.path.join(staging, "META-INF", "MANIFEST.MF")
os.makedirs(os.path.dirname(manifest_path), exist_ok=True)
with open(manifest_path, "w", encoding="utf-8", newline="\n") as f:
    f.write("Manifest-Version: 1.0\n")
    f.write("Plugin-Main-Class: run.halo.captcha.CaptchaPlugin\n")
    f.write("Build-Jdk-Spec: 21\n")
    f.write("Implementation-Title: plugin-captcha\n")
    f.write("Implementation-Version: 2.1.7\n")
print("  META: META-INF/MANIFEST.MF (fixed)")

# Copy plugin-components.idx
idx_src = os.path.join(work, "META-INF", "plugin-components.idx")
idx_dst = os.path.join(staging, "META-INF", "plugin-components.idx")
shutil.copy2(idx_src, idx_dst)
print("  META: META-INF/plugin-components.idx")

# Create JAR
os.chdir(staging)
if os.path.exists(output_jar):
    os.remove(output_jar)

manifest = os.path.join(staging, "META-INF", "MANIFEST.MF")
cmd = ["jar", "cfm", output_jar, manifest, "."]
result = subprocess.run(cmd, capture_output=True, text=True, cwd=staging)
if result.returncode == 0:
    size = os.path.getsize(output_jar)
    print(f"\nSUCCESS! JAR created: {output_jar} ({size} bytes)")
else:
    print(f"ERROR: {result.stderr}")


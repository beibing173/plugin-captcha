import os, glob, subprocess

src_dir = r"C:\Users\17367\Desktop\all\plugin-captcha-repo\src"
stubs_dir = r"C:\Users\17367\Desktop\all\plugin-captcha-repo\stubs"
output_dir = r"C:\Users\17367\Desktop\all\plugin-captcha-repo\classes"
gradle_cache = os.path.expandvars(r"%USERPROFILE%\.gradle\caches\modules-2\files-2.1")

def find_jar(group, artifact, version_prefix):
    path = os.path.join(gradle_cache, group, artifact)
    if not os.path.isdir(path):
        print(f"  MISS: {group}/{artifact} dir not found")
        return None
    for ver in os.listdir(path):
        if ver.startswith(version_prefix):
            ver_path = os.path.join(path, ver)
            for h in os.listdir(ver_path):
                hpath = os.path.join(ver_path, h)
                if os.path.isdir(hpath):
                    for f in os.listdir(hpath):
                        if f.endswith(".jar") and "sources" not in f.lower():
                            return os.path.join(hpath, f)
    print(f"  MISS: {group}/{artifact} no matching jar")
    return None

jars = []
lookups = [
    ("org.springframework", "spring-webflux", "6."),
    ("org.springframework", "spring-web", "6."),
    ("org.springframework", "spring-core", "6."),
    ("org.springframework", "spring-context", "6."),
    ("org.springframework", "spring-beans", "6."),
    ("org.springframework", "spring-aop", "6."),
    ("org.springframework", "spring-expression", "6."),
    ("io.projectreactor", "reactor-core", "3."),
    ("com.fasterxml.jackson.core", "jackson-core", "2."),
    ("com.fasterxml.jackson.core", "jackson-databind", "2."),
    ("com.fasterxml.jackson.core", "jackson-annotations", "2."),
    ("org.slf4j", "slf4j-api", "2."),
    ("org.reactivestreams", "reactive-streams", "1."),
]

for group, artifact, ver_prefix in lookups:
    j = find_jar(group, artifact, ver_prefix)
    if j:
        jars.append(j)
        print(f"OK: {artifact}")

cp = [stubs_dir, src_dir] + jars
classpath = ";".join(cp)

os.makedirs(output_dir, exist_ok=True)

sources = []
for root, dirs, files in os.walk(src_dir):
    for f in files:
        if f.endswith(".java"):
            sources.append(os.path.join(root, f))

cmd = ["javac", "-cp", classpath, "-d", output_dir, "--release", "21"] + sources
print(f"\nCompile {len(sources)} files, {len(jars)} jars...")
result = subprocess.run(cmd, capture_output=True, text=True)
if result.returncode == 0:
    print("SUCCESS!")
else:
    print("ERRORS:")
    err = result.stderr + result.stdout
    print(err[-5000:] if len(err) > 5000 else err)


"""Provision the isolated, checksum-pinned NeoForge 1.21.1 compatibility test environment."""
import hashlib
import json
import os
from pathlib import Path
import urllib.request

root = Path(__file__).resolve().parents[1]
lock = json.loads((root / "neoforge/src/gametest/resources/modpack-lock.json").read_text())
destination = root / "neoforge/build/testmods"
destination.mkdir(parents=True, exist_ok=True)
for mod in lock["mods"]:
    name = mod["filename"]
    if Path(name).name != name or not mod["url"].startswith("https://cdn.modrinth.com/"):
        raise ValueError("Unsafe dependency location")
    target = destination / name
    if not target.exists() or hashlib.sha512(target.read_bytes()).hexdigest() != mod["sha512"]:
        request = urllib.request.Request(mod["url"], headers={"User-Agent": "CraftDroid-native-tests/1.0"})
        data = urllib.request.urlopen(request, timeout=60).read(32 * 1024 * 1024 + 1)
        if len(data) > 32 * 1024 * 1024 or hashlib.sha512(data).hexdigest() != mod["sha512"]:
            raise ValueError("Invalid dependency bytes: " + name)
        temporary = target.with_suffix(".download")
        temporary.write_bytes(data)
        os.replace(temporary, target)
    print("Verified", mod["id"], mod["version"])
if {p.name for p in destination.glob("*.jar")} != {m["filename"] for m in lock["mods"]}:
    raise ValueError("Unexpected jars in isolated test directory")

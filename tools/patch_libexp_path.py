#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""Repoint libexp.so's hard-coded payload path at this app's data dir.

libexp.so contains exactly one literal:

    /data/data/com.example.universalsystemporter/ksud

That is where the native engine bind-mounts its payload from. It was correct
for UniRoot (whose applicationId is com.example.universalsystemporter) and is
wrong for every other package, so the staged payload is silently never used.

The replacement is shorter, so it is padded with NUL bytes inside the original
string's slot. Nothing else in .rodata moves.

Usage:
    python patch_libexp_path.py <libexp.so> [package]

Writes <libexp.so>.orig once (the pristine copy) and patches <libexp.so>.
"""
import os
import sys

OLD = b"/data/data/com.example.universalsystemporter/ksud"
DEFAULT_PKG = "online.usim.go2servicemode"


def main() -> int:
    if len(sys.argv) < 2:
        print(__doc__)
        return 64
    path = sys.argv[1]
    pkg = sys.argv[2] if len(sys.argv) > 2 else DEFAULT_PKG

    new = ("/data/data/%s/ksud" % pkg).encode()
    if len(new) > len(OLD):
        print("ERROR: replacement is longer than the original slot "
              "(%d > %d). Cannot patch in place." % (len(new), len(OLD)))
        return 1

    with open(path, "rb") as f:
        data = f.read()

    hits = data.count(OLD)
    print("original size      : %d" % len(data))
    print("occurrences of OLD : %d" % hits)
    if hits != 1:
        print("ERROR: expected exactly one occurrence, refusing to patch.")
        return 1

    backup = path + ".orig"
    if not os.path.exists(backup):
        with open(backup, "wb") as f:
            f.write(data)
        print("backup written     : %s" % backup)
    else:
        print("backup exists      : %s (kept as-is)" % backup)

    slot = new + b"\x00" * (len(OLD) - len(new))
    patched = data.replace(OLD, slot, 1)
    assert len(patched) == len(data), "size changed — aborting"

    with open(path, "wb") as f:
        f.write(patched)

    print("new string         : %s" % new.decode())
    print("NUL padding        : %d bytes" % (len(slot) - len(new)))
    print("size after patch   : %d" % len(patched))

    with open(path, "rb") as f:
        check = f.read()
    if check.count(new) != 1:
        print("ERROR: read-back verification failed.")
        return 1
    if OLD in check:
        print("ERROR: old string still present.")
        return 1
    print("verify             : OK (new present, old gone)")
    return 0


if __name__ == "__main__":
    sys.exit(main())

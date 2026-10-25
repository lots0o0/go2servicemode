#!/system/bin/sh
# go2servicemode - TRANSACTION 1: establish temporary su.
#
# Deliberately the ONLY thing this script does: start the resident helper, stage
# ksud, bind-mount it over a real system binary so DEFEX accepts it, late-load
# with --allow-shell. The `am` invocation is a separate transaction and must not
# appear here.
#
# Ordering matters: the helper is started BEFORE late-load. The engine tears down
# its child processes once the exploit finishes, so anything spawned afterwards
# is killed — which is exactly how the helper used to never appear. The helper
# waits for su itself, so starting it early is safe.
#
# Logging notes:
#   * target /data/local/tmp/g2sm-tx1.log - writable by root, readable by adb;
#   * no command substitution and no long waits in this script: a sleep here is
#     a place to be killed mid-run, which used to truncate the log silently.
L=/data/local/tmp/g2sm-tx1.log
R=@DATA_DIR@/ksud-real
H=@DATA_DIR@/files/helper.sh
S=/data/local/tmp/.ksud-stage

/system/bin/echo tx1-start >> "$L" 2>&1

# 1. resident helper first, detached from this session
if [ -f "$H" ]; then
  /system/bin/setsid /system/bin/sh "$H" >> "$L" 2>&1 &
  /system/bin/echo tx1-helper-spawn=$? >> "$L" 2>&1
else
  /system/bin/echo tx1-helper-missing >> "$L" 2>&1
fi

# 2. stage ksud
if [ ! -f "$R" ]; then
  /system/bin/echo tx1-abort-no-ksud-real >> "$L" 2>&1
  exit 1
fi
/system/bin/echo tx1-ksud-real-present >> "$L" 2>&1

/system/bin/cp -f "$R" "$S"
/system/bin/echo tx1-cp=$? >> "$L" 2>&1

/system/bin/chmod 755 "$S"
/system/bin/echo tx1-chmod=$? >> "$L" 2>&1

/system/bin/mount -o bind "$S" /system/bin/atrace
/system/bin/echo tx1-mount=$? >> "$L" 2>&1

# 3. late-load; su appears a moment later and the helper picks it up
/system/bin/echo tx1-lateload-begin >> "$L" 2>&1
/system/bin/atrace late-load --allow-shell
/system/bin/echo tx1-lateload=$? >> "$L" 2>&1

/system/bin/echo tx1-end >> "$L" 2>&1
exit 0

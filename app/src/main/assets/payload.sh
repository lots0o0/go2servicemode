#!/system/bin/sh
# go2servicemode - TRANSACTION 1: establish temporary su.
#
# Deliberately the ONLY thing this script does: stage ksud, bind-mount it over a
# real system binary so DEFEX accepts it, late-load with --allow-shell.
# The `am` invocation is a separate transaction and must not appear here.
#
# Logging notes (learned the hard way):
#   * target /data/local/tmp/g2sm-tx1.log - writable by root, readable by adb,
#     so the host can read it without run-as or su;
#   * no command substitution in the log lines - an earlier version produced a
#     0-byte log, which made every failure look like "the script never ran".
L=/data/local/tmp/g2sm-tx1.log
R=@DATA_DIR@/ksud-real
S=/data/local/tmp/.ksud-stage

/system/bin/echo tx1-start >> "$L" 2>&1

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

/system/bin/echo tx1-lateload-begin >> "$L" 2>&1
/system/bin/atrace late-load --allow-shell
/system/bin/echo tx1-lateload=$? >> "$L" 2>&1

# late-load returns as soon as its daemon is forked; the bind-mount of
# /system/bin/su lands a moment later. The first run reported
# "su-missing" purely because of this race, so wait a bounded window.
i=0
while [ $i -lt 50 ]; do
  [ -e /system/bin/su ] && break
  /system/bin/sleep 0.1
  i=$((i+1))
done
if [ -e /system/bin/su ]; then
  /system/bin/echo tx1-su-present-tries=$i >> "$L" 2>&1
else
  /system/bin/echo tx1-su-missing-tries=$i >> "$L" 2>&1
fi

# Hand the privileged path to a resident helper. The app can never see this su
# (ksud mounted it in a namespace the app does not share), so transaction 2
# reaches it only by writing a line into the helper's FIFO.
/system/bin/sh @DATA_DIR@/files/helper.sh >> "$L" 2>&1 &
/system/bin/echo tx1-helper-pid=$! >> "$L" 2>&1

/system/bin/echo tx1-end >> "$L" 2>&1
exit 0

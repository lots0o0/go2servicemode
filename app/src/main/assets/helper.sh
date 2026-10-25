#!/system/bin/sh
# TRANSACTION 2 helper - resident, started as root by transaction 1.
#
# Why this exists: the app cannot exec su. ksud bind-mounts /system/bin/su in a
# mount namespace the app does not share (verified on device: as app uid the
# path does not exist, while `adb shell su -c id` returns u:r:ksu:s0). And the
# payload's own context (vendor_modprobe) SIGKILLs app_process, so `am` cannot
# run there either. This helper is the only place a privileged command runs.
#
# Why a mailbox and not a FIFO: SELinux denies the app write access to
# app_data_file:fifo_file (silently - no avc entry), and a FIFO created by root
# has no MLS categories, so the app could not even open it. Files created by the
# APP carry its categories (s0:cXXX,...) and root can both read and write them,
# so the channel is: app creates -> app writes req -> helper reads it.
#
# Started before late-load on purpose: su does not exist yet, so this script
# waits for it while transaction 1 finishes staging and triggering.
D=@DATA_DIR@/files/tx2
REQ="$D/req"
OUT="$D/out.log"
ST="$D/status"

if [ ! -f "$REQ" ] || [ ! -f "$OUT" ] || [ ! -f "$ST" ]; then
  echo "helper: channel files missing in $D" >> /data/local/tmp/g2sm-helper.err 2>&1
  exit 1
fi

# Announce liveness immediately: the app decides "ready" from this file.
echo "$$" > "$ST"
echo "helper-up $(date '+%H:%M:%S') pid=$$" >> "$OUT"

# Wait for su to land (late-load mounts it just after it returns).
i=0
while [ $i -lt 50 ]; do
  [ -x /system/bin/su ] && break
  /system/bin/sleep 0.2
  i=$((i+1))
done
if [ -x /system/bin/su ]; then
  echo "helper-su-ready tries=$i" >> "$OUT"
else
  echo "helper-su-missing tries=$i" >> "$OUT"
fi

# req is two lines: "<seq>" then the command. Parsing it this way avoids any
# string surgery on the command - an earlier "seq|cmd" form was executed
# verbatim because the prefix strip did not happen, and `su -c "1|id"` piped
# the literal `1` into `id`.
last=""
beat=0
while : ; do
  { read -r seq; read -r cmd; } < "$REQ" 2>/dev/null
  if [ -n "$seq" ] && [ "$seq" != "$last" ] && [ -n "$cmd" ]; then
    last="$seq"
    echo "=== $(date '+%H:%M:%S') $cmd" >> "$OUT"
    /system/bin/su -c "$cmd" >> "$OUT" 2>&1
    echo "=== rc=$?" >> "$OUT"
  fi
  # Heartbeat: the app cannot stat a root process, so liveness is expressed as
  # a fresh mtime on the status file. Without it a dead helper still looks
  # "ready" forever because the last pid stays on disk.
  beat=$((beat+1))
  if [ $beat -ge 5 ]; then
    beat=0
    echo "$$" > "$ST"
  fi
  /system/bin/sleep 0.2
done

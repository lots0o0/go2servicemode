#!/system/bin/sh
# go2servicemode - TRANSACTION 1: temporary su, plus the helper script that the
# app later starts through Shizuku.
#
# The helper cannot be started from here: once the engine cleans up it restores
# SELinux and this domain loses every useful permission. It is written to
# /data/local/tmp while the window is open, and started afterwards with
# su -c from Shizuku's shell context, so it ends up in the ksu domain.
L=/data/local/tmp/g2sm-tx1.log
R=@DATA_DIR@/ksud-real
S=/data/local/tmp/.ksud-stage
HB=/data/local/tmp/g2sm-helper.sh

/system/bin/echo tx1-start >> "$L" 2>&1
/system/bin/setenforce 0
/system/bin/echo tx1-setenforce=$? / enforce=$(/system/bin/cat /sys/fs/selinux/enforce) >> "$L" 2>&1

/system/bin/printf '%s\n' 'D=@DATA_DIR@/files/tx2' 'REQ=$D/req' 'OUT=/data/local/tmp/g2sm-tx2-out.log' 'ST=/data/local/tmp/g2sm-tx2-status' 'echo "up $(date +%H:%M:%S) pid=$$ ctx=$(cat /proc/self/attr/current)" >> $OUT' 'chmod 666 $OUT $ST 2>/dev/null' 'last=' 'while : ; do' '  { read -r seq; read -r cmd; } < $REQ 2>/dev/null' '  if [ -n "$seq" ] && [ "$seq" != "$last" ] && [ -n "$cmd" ]; then' '    last=$seq' '    echo "=== $(date +%H:%M:%S) $cmd" >> $OUT' '    /system/bin/sh -c "$cmd" >> $OUT 2>&1' '    echo "=== rc=$?" >> $OUT' '  fi' '  echo $$ > $ST' '  /system/bin/sleep 0.3' 'done' > "$HB"
/system/bin/chmod 755 "$HB"
/system/bin/echo tx1-helper-bytes=$(/system/bin/wc -c < "$HB") >> "$L" 2>&1

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
/system/bin/echo tx1-end >> "$L" 2>&1
exit 0
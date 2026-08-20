package io.github.elkir0.rpivoicesetup;

import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Build;
import android.speech.SpeechRecognizer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class DeviceDiagnostics {
    static final String AUDIO_APEX = "/vendor/apex/com.android.hardware.audio.rpi.apex";
    static final String USB_POLICY = "/vendor/etc/usb_audio_policy_configuration.xml";

    static final String RPI4_STOCK_APEX =
            "27b5332e841f50e6b3435b5b512c86981443764e54a517262976bc98f0b587f3";
    static final String RPI4_PATCHED_APEX =
            "a8c735ede091b92770cb74162baaa826c69708a26d7fce0d59d62b082d989ccc";
    static final String RPI4_STOCK_POLICY =
            "29d18b8e3ca51dc1f6e54fd39886a60e2372a9e1f610ae9d285311e312b732d5";
    static final String RPI4_PATCHED_POLICY =
            "bb1c738411bd04e612cd5b907fef7674e34b9f939456f4fc33430f9b2b8153a9";
    static final String RPI5_STOCK_APEX =
            "ccf26258c38acdb741566023eefff0a2f288d807f69439904f08ac319b831f9a";
    static final String RPI5_PATCHED_APEX =
            "1781c9c08197fdcc789350c71c759b9cdfe085683a93dad206037375908af0ff";

    enum PatchState { PATCHED, STOCK, UNKNOWN, UNREADABLE }

    static final class InputDevice {
        final int id;
        final int type;
        final String name;
        final String address;
        final int[] channels;
        final int[] sampleRates;

        InputDevice(AudioDeviceInfo device) {
            id = device.getId();
            type = device.getType();
            name = String.valueOf(device.getProductName());
            address = device.getAddress();
            channels = device.getChannelCounts();
            sampleRates = device.getSampleRates();
        }

        boolean isUsb() {
            return type == AudioDeviceInfo.TYPE_USB_DEVICE
                    || type == AudioDeviceInfo.TYPE_USB_HEADSET;
        }

        String summary() {
            return name + " · " + typeName(type) + " · canaux " + join(channels)
                    + " · fréquences " + join(sampleRates);
        }
    }

    static final class Snapshot {
        String model;
        String generation;
        String androidVersion;
        String buildId;
        String fingerprint;
        String locale;
        boolean googleInstalled;
        boolean geminiInstalled;
        boolean speechRecognizer;
        List<InputDevice> inputs = new ArrayList<>();
        String apexSha256;
        String policySha256;
        PatchState apexState;
        PatchState policyState;
        String capturedAt;

        InputDevice usbInput() {
            for (InputDevice input : inputs) if (input.isUsb()) return input;
            return null;
        }

        boolean supportedPi() {
            return "Pi 4".equals(generation) || "Pi 5".equals(generation);
        }

        String patchLabel() {
            if (apexState == PatchState.PATCHED
                    && (policyState == PatchState.PATCHED || "Pi 5".equals(generation))) {
                return "Correctif audio reconnu";
            }
            if (apexState == PatchState.STOCK) return "APEX audio d’origine détecté";
            if (apexState == PatchState.UNREADABLE) return "APEX audio illisible par l’application";
            return "Build audio non répertoriée";
        }

        String report(Context context) {
            StringBuilder out = new StringBuilder();
            out.append("Raspberry Voice Setup — rapport sans audio\n")
                    .append("Date: ").append(capturedAt).append('\n')
                    .append("Modèle: ").append(model).append(" (").append(generation).append(")\n")
                    .append("Android: ").append(androidVersion).append('\n')
                    .append("Build ID: ").append(buildId).append('\n')
                    .append("Fingerprint: ").append(fingerprint).append('\n')
                    .append("Locale: ").append(locale).append('\n')
                    .append("Google: ").append(yesNo(googleInstalled)).append('\n')
                    .append("Gemini: ").append(yesNo(geminiInstalled)).append('\n')
                    .append("Reconnaissance vocale: ").append(yesNo(speechRecognizer)).append('\n')
                    .append("APEX SHA-256: ").append(orUnavailable(apexSha256)).append('\n')
                    .append("État APEX: ").append(apexState).append('\n')
                    .append("Audio Policy SHA-256: ").append(orUnavailable(policySha256)).append('\n')
                    .append("État Audio Policy: ").append(policyState).append('\n')
                    .append("Entrées audio:\n");
            if (inputs.isEmpty()) out.append("- aucune\n");
            for (InputDevice input : inputs) out.append("- ").append(input.summary()).append('\n');

            android.content.SharedPreferences prefs = context.getSharedPreferences("tests", 0);
            if (prefs.contains("mic_frames")) {
                out.append("Dernier test micro:\n")
                        .append("- frames: ").append(prefs.getLong("mic_frames", 0)).append('\n')
                        .append("- niveau max: ").append(prefs.getFloat("mic_max_db", -90)).append(" dBFS\n")
                        .append("- erreurs: ").append(prefs.getInt("mic_errors", 0)).append('\n')
                        .append("- route: ").append(prefs.getString("mic_route", "inconnue")).append('\n');
            }
            if (prefs.contains("speech_result")) {
                out.append("Reconnaissance réussie: oui\n");
            }
            return out.toString();
        }
    }

    private DeviceDiagnostics() {}

    static Snapshot capture(Context context) {
        Snapshot snapshot = new Snapshot();
        snapshot.model = readModel();
        snapshot.generation = generation(snapshot.model);
        snapshot.androidVersion = Build.VERSION.RELEASE + " (API " + Build.VERSION.SDK_INT + ")";
        snapshot.buildId = Build.ID;
        snapshot.fingerprint = Build.FINGERPRINT;
        snapshot.locale = context.getResources().getConfiguration().getLocales().get(0).toLanguageTag();
        snapshot.googleInstalled = packageInstalled(context, "com.google.android.googlequicksearchbox");
        snapshot.geminiInstalled = packageInstalled(context, "com.google.android.apps.bard");
        snapshot.speechRecognizer = SpeechRecognizer.isRecognitionAvailable(context);
        snapshot.capturedAt = DateFormat.getDateTimeInstance().format(new Date());

        AudioManager audioManager = context.getSystemService(AudioManager.class);
        if (audioManager != null) {
            for (AudioDeviceInfo device : audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (device.isSource()) snapshot.inputs.add(new InputDevice(device));
            }
        }

        snapshot.apexSha256 = sha256(AUDIO_APEX);
        snapshot.policySha256 = sha256(USB_POLICY);
        snapshot.apexState = apexState(snapshot.generation, snapshot.apexSha256);
        snapshot.policyState = policyState(snapshot.generation, snapshot.policySha256);
        return snapshot;
    }

    private static String readModel() {
        String model = readSmallFile("/proc/device-tree/model");
        if (model.isEmpty()) model = Build.MODEL;
        return model.replace("\u0000", "").trim();
    }

    private static String generation(String model) {
        String value = model.toLowerCase(Locale.ROOT).replace('_', ' ');
        if (value.contains("pi 5")) return "Pi 5";
        if (value.contains("pi 4")) return "Pi 4";
        return "Autre";
    }

    private static boolean packageInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException ignored) {
            return false;
        }
    }

    private static String readSmallFile(String path) {
        File file = new File(path);
        if (!file.canRead()) return "";
        byte[] buffer = new byte[(int) Math.min(file.length(), 4096)];
        try (FileInputStream input = new FileInputStream(file)) {
            int count = input.read(buffer);
            return count <= 0 ? "" : new String(buffer, 0, count, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            return "";
        }
    }

    private static String sha256(String path) {
        File file = new File(path);
        if (!file.canRead()) return null;
        try (FileInputStream input = new FileInputStream(file)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[128 * 1024];
            int count;
            while ((count = input.read(buffer)) > 0) digest.update(buffer, 0, count);
            StringBuilder value = new StringBuilder();
            for (byte b : digest.digest()) value.append(String.format(Locale.US, "%02x", b));
            return value.toString();
        } catch (Exception ignored) {
            return null;
        }
    }

    private static PatchState apexState(String generation, String sha) {
        if (sha == null) return PatchState.UNREADABLE;
        if (RPI4_PATCHED_APEX.equals(sha) || RPI5_PATCHED_APEX.equals(sha)) return PatchState.PATCHED;
        if (RPI4_STOCK_APEX.equals(sha) || RPI5_STOCK_APEX.equals(sha)) return PatchState.STOCK;
        return PatchState.UNKNOWN;
    }

    private static PatchState policyState(String generation, String sha) {
        if (sha == null) return PatchState.UNREADABLE;
        if (RPI4_PATCHED_POLICY.equals(sha)) return PatchState.PATCHED;
        if (RPI4_STOCK_POLICY.equals(sha)) return PatchState.STOCK;
        return "Pi 5".equals(generation) ? PatchState.UNKNOWN : PatchState.UNKNOWN;
    }

    static String typeName(int type) {
        switch (type) {
            case AudioDeviceInfo.TYPE_USB_DEVICE: return "USB";
            case AudioDeviceInfo.TYPE_USB_HEADSET: return "casque USB";
            case AudioDeviceInfo.TYPE_BUILTIN_MIC: return "micro intégré";
            case AudioDeviceInfo.TYPE_BLUETOOTH_SCO: return "Bluetooth SCO";
            case AudioDeviceInfo.TYPE_WIRED_HEADSET: return "casque filaire";
            default: return "type " + type;
        }
    }

    static String join(int[] values) {
        if (values == null || values.length == 0) return "auto";
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) out.append('/');
            out.append(values[i]);
        }
        return out.toString();
    }

    private static String yesNo(boolean value) { return value ? "oui" : "non"; }
    private static String orUnavailable(String value) { return value == null ? "indisponible" : value; }
}


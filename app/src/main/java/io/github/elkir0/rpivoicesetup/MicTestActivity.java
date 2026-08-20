package io.github.elkir0.rpivoicesetup;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public final class MicTestActivity extends Activity {
    private static final int AUDIO_PERMISSION = 100;
    private static final int SAMPLE_RATE = 48_000;

    private TextView status;
    private TextView level;
    private TextView details;
    private ProgressBar meter;
    private Button toggle;
    private volatile boolean running;
    private AudioRecord recorder;
    private Thread captureThread;
    private long frames;
    private int errors;
    private float maxDb = -90f;
    private String route = "inconnue";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildUi();
    }

    private void buildUi() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.NAVY);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 18), Ui.dp(this, 24), Ui.dp(this, 24));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = Ui.text(this, "ÉTAPE 1 · MICROPHONE", 12, Ui.CYAN);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(eyebrow);
        root.addView(Ui.title(this, "Le signal arrive-t-il vraiment ?"));
        root.addView(Ui.text(this,
                "Parlez normalement pendant 10 secondes. Le test demande du mono PCM 16 bits à 48 kHz et préfère explicitement l’entrée USB.",
                15, Ui.MUTED));

        LinearLayout card = Ui.card(this);
        Ui.marginTop(card, 16);
        status = Ui.text(this, "Prêt à tester", 18, Ui.WHITE);
        status.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        card.addView(status);

        meter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(90);
        meter.setProgress(0);
        LinearLayout.LayoutParams meterParams = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 28));
        meterParams.setMargins(0, Ui.dp(this, 16), 0, Ui.dp(this, 8));
        card.addView(meter, meterParams);

        level = Ui.text(this, "−∞ dBFS", 28, Ui.CYAN);
        level.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
        level.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(level);
        details = Ui.text(this, "Aucune frame lue", 14, Ui.MUTED);
        details.setGravity(Gravity.CENTER_HORIZONTAL);
        card.addView(details);
        root.addView(card);

        toggle = Ui.button(this, "Démarrer le test", v -> toggle());
        root.addView(toggle);
        Button back = Ui.secondaryButton(this, "Retour au tableau de bord", v -> finish());
        Ui.marginTop(back, 10);
        root.addView(back);
        setContentView(scroll);
    }

    private void toggle() {
        if (running) {
            stopCapture();
            return;
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, AUDIO_PERMISSION);
            return;
        }
        startCapture();
    }

    @Override public void onRequestPermissionsResult(int requestCode, String[] permissions,
                                                     int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == AUDIO_PERMISSION && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCapture();
        } else {
            status.setText("Permission microphone refusée");
            status.setTextColor(Ui.RED);
        }
    }

    private void startCapture() {
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            status.setText("Permission microphone requise");
            status.setTextColor(Ui.AMBER);
            return;
        }
        AudioManager manager = getSystemService(AudioManager.class);
        AudioDeviceInfo usb = null;
        if (manager != null) {
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_INPUTS)) {
                if (device.isSource() && (device.getType() == AudioDeviceInfo.TYPE_USB_DEVICE
                        || device.getType() == AudioDeviceInfo.TYPE_USB_HEADSET)) {
                    usb = device;
                    break;
                }
            }
        }
        if (usb == null) {
            status.setText("Aucun microphone USB détecté");
            status.setTextColor(Ui.RED);
            return;
        }

        int minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
        if (minBuffer <= 0) {
            status.setText("Configuration 48 kHz mono refusée : " + minBuffer);
            status.setTextColor(Ui.RED);
            return;
        }

        try {
            recorder = new AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(new AudioFormat.Builder()
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setSampleRate(SAMPLE_RATE)
                            .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                            .build())
                    .setBufferSizeInBytes(Math.max(minBuffer * 2, 9600))
                    .build();
            boolean preferred = recorder.setPreferredDevice(usb);
            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                throw new IllegalStateException("AudioRecord non initialisé");
            }
            recorder.startRecording();
            if (recorder.getRecordingState() != AudioRecord.RECORDSTATE_RECORDING) {
                throw new IllegalStateException("capture non démarrée");
            }
            running = true;
            frames = 0;
            errors = 0;
            maxDb = -90f;
            status.setText(preferred ? "Capture USB active — parlez maintenant"
                    : "Capture active — préférence USB refusée");
            status.setTextColor(preferred ? Ui.GREEN : Ui.AMBER);
            toggle.setText("Arrêter et enregistrer le résultat");
            AudioDeviceInfo routed = recorder.getRoutedDevice();
            route = routed == null ? usb.getProductName().toString()
                    : routed.getProductName() + " / " + DeviceDiagnostics.typeName(routed.getType());
            final AudioRecord activeRecorder = recorder;
            captureThread = new Thread(() -> captureLoop(activeRecorder), "usb-mic-probe");
            captureThread.start();
        } catch (Exception error) {
            releaseRecorder();
            status.setText("Échec d’ouverture : " + error.getMessage());
            status.setTextColor(Ui.RED);
        }
    }

    private void captureLoop(AudioRecord activeRecorder) {
        short[] buffer = new short[2400];
        long lastUpdate = 0;
        while (running && activeRecorder == recorder) {
            int count = activeRecorder.read(buffer, 0, buffer.length, AudioRecord.READ_BLOCKING);
            if (count <= 0) {
                errors++;
                continue;
            }
            frames += count;
            double sum = 0;
            int peak = 0;
            int zeroes = 0;
            for (int i = 0; i < count; i++) {
                int value = buffer[i];
                sum += (double) value * value;
                peak = Math.max(peak, Math.abs(value));
                if (value == 0) zeroes++;
            }
            double rms = Math.sqrt(sum / count);
            float db = rms <= 0 ? -90f
                    : (float) Math.max(-90d, 20d * Math.log10(rms / 32768d));
            maxDb = Math.max(maxDb, db);
            long now = android.os.SystemClock.elapsedRealtime();
            if (now - lastUpdate >= 100) {
                lastUpdate = now;
                float zeroRatio = zeroes * 100f / count;
                int shownPeak = peak;
                float shownDb = db;
                runOnUiThread(() -> updateMeter(shownDb, shownPeak, zeroRatio));
            }
        }
    }

    private void updateMeter(float db, int peak, float zeroRatio) {
        if (!running) return;
        meter.setProgress(Math.max(0, Math.min(90, Math.round(db + 90))));
        level.setText(String.format(Locale.FRANCE, "%.1f dBFS", db));
        level.setTextColor(db > -48 ? Ui.GREEN : db > -65 ? Ui.AMBER : Ui.CYAN);
        details.setText(String.format(Locale.FRANCE,
                "%d frames · pic %d · zéros %.1f %% · route %s", frames, peak, zeroRatio, route));
    }

    private void stopCapture() {
        if (!running && recorder == null) return;
        running = false;
        releaseRecorder();
        getSharedPreferences("tests", 0).edit()
                .putLong("mic_frames", frames)
                .putFloat("mic_max_db", maxDb)
                .putInt("mic_errors", errors)
                .putString("mic_route", route)
                .putLong("mic_time", System.currentTimeMillis())
                .apply();
        boolean signal = frames >= SAMPLE_RATE && maxDb > -55f && errors == 0;
        status.setText(signal ? "Signal USB validé" : "Signal trop faible ou capture incomplète");
        status.setTextColor(signal ? Ui.GREEN : Ui.AMBER);
        details.setText(String.format(Locale.FRANCE,
                "%d frames · maximum %.1f dBFS · %d erreur(s) · %s",
                frames, maxDb, errors, route));
        toggle.setText("Recommencer le test");
    }

    private void releaseRecorder() {
        AudioRecord current = recorder;
        recorder = null;
        if (current != null) {
            try { current.stop(); } catch (Exception ignored) {}
            current.release();
        }
    }

    @Override protected void onPause() {
        if (running) stopCapture();
        super.onPause();
    }

    @Override protected void onDestroy() {
        running = false;
        releaseRecorder();
        super.onDestroy();
    }
}

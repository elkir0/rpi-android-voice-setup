package io.github.elkir0.rpivoicesetup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.RecognizerIntent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class MainActivity extends Activity {
    private static final int SPEECH_REQUEST = 42;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private LinearLayout content;
    private TextView loading;
    private DeviceDiagnostics.Snapshot snapshot;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        buildShell();
        refresh();
    }

    @Override protected void onResume() {
        super.onResume();
        if (snapshot != null) refresh();
    }

    @Override protected void onDestroy() {
        executor.shutdownNow();
        super.onDestroy();
    }

    private void buildShell() {
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Ui.NAVY);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(Ui.dp(this, 24), Ui.dp(this, 18), Ui.dp(this, 24), Ui.dp(this, 28));
        scroll.addView(content, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        setContentView(scroll);
    }

    private void refresh() {
        content.removeAllViews();
        TextView eyebrow = Ui.text(this, "RASPBERRY VOICE SETUP · MVP", 12, Ui.CYAN);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        content.addView(eyebrow);
        content.addView(Ui.title(this, "Configurons la voix, étape par étape"));
        TextView intro = Ui.text(this,
                "Diagnostic commun Pi 4 / Pi 5, sans modifier le système ni enregistrer votre voix.",
                15, Ui.MUTED);
        content.addView(intro);
        loading = Ui.text(this, "Analyse du Raspberry Pi et de l’audio…", 16, Ui.AMBER);
        Ui.marginTop(loading, 18);
        content.addView(loading);

        executor.execute(() -> {
            DeviceDiagnostics.Snapshot result = DeviceDiagnostics.capture(getApplicationContext());
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                snapshot = result;
                renderDashboard();
            });
        });
    }

    private void renderDashboard() {
        content.removeView(loading);
        content.addView(deviceCard());
        content.addView(stepCard("1", "Microphone USB",
                snapshot.usbInput() == null ? "Aucun micro USB détecté" : snapshot.usbInput().summary(),
                snapshot.usbInput() == null ? "À FAIRE" : "DÉTECTÉ",
                snapshot.usbInput() == null ? Ui.AMBER : Ui.GREEN,
                "Tester le niveau réel", v -> startActivity(new Intent(this, MicTestActivity.class))));

        boolean speechDone = getSharedPreferences("tests", 0).contains("speech_result");
        content.addView(stepCard("2", "Reconnaissance Google",
                speechDone
                        ? "Dernier résultat : " + getSharedPreferences("tests", 0)
                                .getString("speech_result", "reconnu")
                        : "Prononcez une phrase pour valider toute la chaîne de capture.",
                speechDone ? "VALIDÉ" : "À TESTER",
                speechDone ? Ui.GREEN : Ui.BLUE,
                "Lancer le test Google", v -> launchSpeechRecognition()));

        boolean french = snapshot.locale.toLowerCase(Locale.ROOT).startsWith("fr");
        content.addView(stepCard("3", "Langue Android et Assistant",
                "Locale actuelle : " + snapshot.locale + ". Voice Match doit utiliser la même langue.",
                french ? "FRANÇAIS" : "À ALIGNER",
                french ? Ui.GREEN : Ui.AMBER,
                "Ouvrir les langues", v -> openIntent(new Intent(Settings.ACTION_LOCALE_SETTINGS))));

        content.addView(stepCard("4", "Voice Match",
                "Parcours guidé pour le micro USB à capture unique, avec la manipulation ADB sécurisée.",
                "GUIDE", Ui.CYAN,
                "Commencer le guide", v -> startActivity(new Intent(this, VoiceMatchActivity.class))));

        LinearLayout actions = Ui.card(this);
        actions.addView(Ui.sectionTitle(this, "Outils"));
        ButtonRow.add(actions,
                Ui.secondaryButton(this, "Réglages Assistant", v -> openAssistantSettings()),
                Ui.secondaryButton(this, "Actualiser", v -> refresh()));
        View share = Ui.button(this, "Partager le rapport de diagnostic", v -> shareReport());
        Ui.marginTop(share, 10);
        actions.addView(share);
        content.addView(actions);
    }

    private LinearLayout deviceCard() {
        LinearLayout card = Ui.card(this);
        LinearLayout heading = Ui.horizontal(this);
        TextView title = Ui.sectionTitle(this, snapshot.generation + " · Android " + snapshot.androidVersion);
        heading.addView(title, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        heading.addView(Ui.status(this, snapshot.supportedPi() ? "COMPATIBLE" : "À VÉRIFIER",
                snapshot.supportedPi() ? Ui.GREEN : Ui.AMBER));
        card.addView(heading);
        card.addView(Ui.text(this, snapshot.model, 15, Ui.WHITE));
        card.addView(Ui.text(this, "Build " + snapshot.buildId + " · " + snapshot.locale, 13, Ui.MUTED));

        int patchColor = snapshot.apexState == DeviceDiagnostics.PatchState.PATCHED
                ? Ui.GREEN : snapshot.apexState == DeviceDiagnostics.PatchState.STOCK ? Ui.AMBER : Ui.BLUE;
        TextView patch = Ui.text(this, snapshot.patchLabel(), 14, patchColor);
        patch.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        Ui.marginTop(patch, 8);
        card.addView(patch);

        String apps = "Google " + (snapshot.googleInstalled ? "installé" : "absent")
                + " · Gemini " + (snapshot.geminiInstalled ? "installé" : "absent")
                + " · reconnaissance " + (snapshot.speechRecognizer ? "disponible" : "indisponible");
        card.addView(Ui.text(this, apps, 13, Ui.MUTED));
        if (snapshot.supportedPi()) {
            View repository = Ui.secondaryButton(this,
                    "Voir le correctif " + snapshot.generation,
                    v -> openFixRepository());
            Ui.marginTop(repository, 9);
            card.addView(repository);
        }
        return card;
    }

    private LinearLayout stepCard(String number, String title, String detail, String status,
                                  int statusColor, String action, View.OnClickListener listener) {
        LinearLayout card = Ui.card(this);
        LinearLayout heading = Ui.horizontal(this);
        TextView titleView = Ui.sectionTitle(this, number + "  " + title);
        heading.addView(titleView, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        heading.addView(Ui.status(this, status, statusColor));
        card.addView(heading);
        TextView detailView = Ui.text(this, detail, 14, Ui.MUTED);
        detailView.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 9));
        card.addView(detailView);
        card.addView(Ui.button(this, action, listener));
        return card;
    }

    private void launchSpeechRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, snapshot == null ? "fr-FR" : snapshot.locale);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Dites : test microphone bonjour");
        intent.putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3);
        try {
            startActivityForResult(intent, SPEECH_REQUEST);
        } catch (ActivityNotFoundException error) {
            showMessage("Reconnaissance indisponible",
                    "Aucun service de reconnaissance vocale ne répond à l’intent Android.");
        }
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != SPEECH_REQUEST) return;
        ArrayList<String> results = data == null ? null
                : data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        if (resultCode == RESULT_OK && results != null && !results.isEmpty()) {
            getSharedPreferences("tests", 0).edit()
                    .putString("speech_result", results.get(0))
                    .putLong("speech_time", System.currentTimeMillis())
                    .apply();
            showMessage("Reconnaissance validée", "Google a reconnu :\n\n« " + results.get(0) + " »");
        } else {
            showMessage("Test non validé",
                    "Aucune phrase n’a été retournée. Testez d’abord le niveau du microphone USB.");
        }
    }

    private void openAssistantSettings() {
        Intent intent = new Intent("com.google.android.googlequicksearchbox.action.ASSISTANT_SETTINGS");
        intent.setPackage("com.google.android.googlequicksearchbox");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            openIntent(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        }
    }

    private void openFixRepository() {
        if (snapshot == null || !snapshot.supportedPi()) return;
        String repository = "Pi 4".equals(snapshot.generation)
                ? "https://github.com/elkir0/rpi4-android-usb-wakeword-fix"
                : "https://github.com/elkir0/rpi5-android-usb-wakeword-fix";
        openIntent(new Intent(Intent.ACTION_VIEW, Uri.parse(repository)));
    }

    private void openIntent(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            showMessage("Écran indisponible", "Cette version d’Android ne fournit pas ce réglage.");
        }
    }

    private void shareReport() {
        if (snapshot == null) return;
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "Rapport Raspberry Voice Setup");
        share.putExtra(Intent.EXTRA_TEXT, snapshot.report(this));
        startActivity(Intent.createChooser(share, "Partager le rapport"));
    }

    private void showMessage(String title, String message) {
        new AlertDialog.Builder(this).setTitle(title).setMessage(message)
                .setPositiveButton("OK", null).show();
    }

    private static final class ButtonRow {
        static void add(LinearLayout parent, View left, View right) {
            ContextHolder.add(parent, left, right);
        }
    }

    private static final class ContextHolder {
        static void add(LinearLayout parent, View left, View right) {
            LinearLayout row = Ui.horizontal(parent.getContext());
            int gap = Ui.dp(parent.getContext(), 10);
            LinearLayout.LayoutParams leftParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            leftParams.setMargins(0, 0, gap / 2, 0);
            LinearLayout.LayoutParams rightParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1);
            rightParams.setMargins(gap / 2, 0, 0, 0);
            row.addView(left, leftParams);
            row.addView(right, rightParams);
            Ui.marginTop(row, 10);
            parent.addView(row);
        }
    }
}

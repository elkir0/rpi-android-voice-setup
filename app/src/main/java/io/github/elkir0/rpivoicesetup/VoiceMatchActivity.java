package io.github.elkir0.rpivoicesetup;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class VoiceMatchActivity extends Activity {
    private static final String PREPARE_COMMAND =
            "./tools/rpi-voice-setup prepare-enrollment --serial IP:PORT";
    private static final String FINISH_COMMAND =
            "./tools/rpi-voice-setup finish-enrollment --serial IP:PORT";

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
        root.setPadding(Ui.dp(this, 24), Ui.dp(this, 18), Ui.dp(this, 24), Ui.dp(this, 28));
        scroll.addView(root, new ScrollView.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView eyebrow = Ui.text(this, "ÉTAPE 4 · VOICE MATCH", 12, Ui.CYAN);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(eyebrow);
        root.addView(Ui.title(this, "Entraînement guidé, sans mauvaise manipulation"));
        root.addView(Ui.text(this,
                "Certains microphones USB n’acceptent qu’une capture à la fois. Le listener HOTWORD doit alors être libéré juste avant l’entraînement, puis réarmé après.",
                15, Ui.MUTED));

        root.addView(guideCard("1", "Valider le micro",
                "Revenez au tableau de bord et obtenez un signal vert au test 48 kHz mono.",
                "Ouvrir le test micro", v -> startActivity(new Intent(this, MicTestActivity.class))));
        root.addView(guideCard("2", "Aligner la langue",
                "Android, l’application Google et l’Assistant doivent utiliser la même variante de français.",
                "Ouvrir les langues Android", v -> open(new Intent(Settings.ACTION_LOCALE_SETTINGS))));
        root.addView(guideCard("3", "Préparer Voice Match",
                "Ouvrez « Hey Google et Voice Match », puis restez juste avant le bouton Réentraîner.",
                "Ouvrir les réglages Assistant", v -> openAssistantSettings()));
        root.addView(commandCard("4", "Libérer uniquement le listener HOTWORD",
                "Sur l’ordinateur, lancez le compagnon. Il refuse d’agir si le processus n’est pas unique ou ne correspond pas exactement au processus isolé Google.",
                PREPARE_COMMAND));
        root.addView(guideCard("5", "Parler immédiatement",
                "Dès que le compagnon confirme la libération, touchez Réentraîner et prononcez toutes les phrases affichées.",
                "J’ai terminé l’entraînement", v -> showFinishDialog()));
        root.addView(commandCard("6", "Réarmer puis tester",
                "Le compagnon relance la détection HOTWORD. Redémarrez ensuite Android et vérifiez que « Hey Google » ouvre réellement l’Assistant.",
                FINISH_COMMAND));

        TextView warning = Ui.text(this,
                "Sécurité : l’application ne modifie jamais /vendor, ne tue aucun processus et ne collecte aucun enregistrement audio.",
                13, Ui.AMBER);
        warning.setPadding(0, Ui.dp(this, 6), 0, Ui.dp(this, 12));
        root.addView(warning);
        root.addView(Ui.secondaryButton(this, "Retour", v -> finish()));
        setContentView(scroll);
    }

    private LinearLayout guideCard(String number, String title, String detail,
                                   String action, View.OnClickListener listener) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.sectionTitle(this, number + "  " + title));
        TextView detailView = Ui.text(this, detail, 14, Ui.MUTED);
        detailView.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 9));
        card.addView(detailView);
        card.addView(Ui.button(this, action, listener));
        return card;
    }

    private LinearLayout commandCard(String number, String title, String detail, String command) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.sectionTitle(this, number + "  " + title));
        TextView detailView = Ui.text(this, detail, 14, Ui.MUTED);
        detailView.setPadding(0, Ui.dp(this, 5), 0, Ui.dp(this, 8));
        card.addView(detailView);
        TextView code = Ui.text(this, command, 13, Ui.CYAN);
        code.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
        code.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        code.setBackground(Ui.rounded(Ui.NAVY, 10, this));
        card.addView(code);
        View copy = Ui.secondaryButton(this, "Copier la commande", v -> copy(command));
        Ui.marginTop(copy, 8);
        card.addView(copy);
        return card;
    }

    private void copy(String value) {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText("Commande Voice Match", value));
        Toast.makeText(this, "Commande copiée", Toast.LENGTH_SHORT).show();
    }

    private void openAssistantSettings() {
        Intent intent = new Intent("com.google.android.googlequicksearchbox.action.ASSISTANT_SETTINGS");
        intent.setPackage("com.google.android.googlequicksearchbox");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            open(new Intent(Settings.ACTION_VOICE_INPUT_SETTINGS));
        }
    }

    private void open(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Réglage indisponible", Toast.LENGTH_LONG).show();
        }
    }

    private void showFinishDialog() {
        new AlertDialog.Builder(this)
                .setTitle("Dernière étape")
                .setMessage("L’entraînement n’est complet qu’après le réarmement HOTWORD, un test « Hey Google » réel et un second test après redémarrage.")
                .setNegativeButton("Fermer", null)
                .setPositiveButton("Copier la commande", (dialog, which) -> copy(FINISH_COMMAND))
                .show();
    }
}


package io.github.elkir0.rpivoicesetup;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public final class FixGuideActivity extends Activity {
    private String generation;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        generation = getIntent().getStringExtra("generation");
        if (generation == null) generation = "Autre";
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

        TextView eyebrow = Ui.text(this, "ÉTAPE 0 · CORRECTIF SYSTÈME", 12, Ui.CYAN);
        eyebrow.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(eyebrow);
        root.addView(Ui.title(this, "Installer sans risquer une autre build"));
        root.addView(Ui.text(this,
                generation + " · build " + getIntent().getStringExtra("build_id")
                        + " · état " + getIntent().getStringExtra("patch_state"),
                14, Ui.MUTED));

        root.addView(card("1", "Vérifier la compatibilité",
                "Depuis l’ordinateur, exécutez le diagnostic. Le hash APEX doit être explicitement connu ; ne contournez jamais un refus.",
                "./tools/rpi-voice-setup doctor --serial IP:PORT",
                "Copier le diagnostic", v -> copy("./tools/rpi-voice-setup doctor --serial IP:PORT")));
        root.addView(card("2", "Sauvegarder /vendor dans TWRP",
                "Conservez une sauvegarde vérifiée et le ZIP de rollback avant la première installation.",
                null, null, null));
        root.addView(card("3", "Télécharger le paquet exact",
                supported()
                        ? "La release contient les fichiers destinés uniquement à " + generation + "."
                        : "Ce matériel n’est pas encore dans la matrice de compatibilité publique.",
                null,
                supported() ? "Ouvrir la release " + generation : null,
                supported() ? v -> openRelease() : null));
        root.addView(card("4", "Flasher puis redémarrer",
                "Dans TWRP, flashez le ZIP d’installation. En cas d’erreur de hash, arrêtez-vous. Redémarrez Android puis revenez actualiser ce diagnostic.",
                null, null, null));

        TextView warning = Ui.text(this,
                "L’application ne flashe rien elle-même : la séparation TWRP protège le rollback et empêche l’installation silencieuse d’un APEX incompatible.",
                13, Ui.AMBER);
        warning.setPadding(0, 0, 0, Ui.dp(this, 12));
        root.addView(warning);
        root.addView(Ui.secondaryButton(this, "Retour", v -> finish()));
        setContentView(scroll);
    }

    private LinearLayout card(String number, String title, String detail, String command,
                              String action, View.OnClickListener listener) {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.sectionTitle(this, number + "  " + title));
        TextView detailView = Ui.text(this, detail, 14, Ui.MUTED);
        detailView.setPadding(0, Ui.dp(this, 5), 0, command == null && action == null ? 0 : Ui.dp(this, 8));
        card.addView(detailView);
        if (command != null) {
            TextView code = Ui.text(this, command, 13, Ui.CYAN);
            code.setTypeface(Typeface.MONOSPACE, Typeface.NORMAL);
            code.setPadding(Ui.dp(this, 12), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
            code.setBackground(Ui.rounded(Ui.NAVY, 10, this));
            card.addView(code);
        }
        if (action != null && listener != null) {
            View button = Ui.secondaryButton(this, action, listener);
            Ui.marginTop(button, 8);
            card.addView(button);
        }
        return card;
    }

    private boolean supported() {
        return "Pi 4".equals(generation) || "Pi 5".equals(generation);
    }

    private void openRelease() {
        String repository = "Pi 4".equals(generation)
                ? "rpi4-android-usb-wakeword-fix"
                : "rpi5-android-usb-wakeword-fix";
        Intent intent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("https://github.com/elkir0/" + repository + "/releases/latest"));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException error) {
            Toast.makeText(this, "Aucun navigateur disponible", Toast.LENGTH_LONG).show();
        }
    }

    private void copy(String value) {
        ClipboardManager clipboard = getSystemService(ClipboardManager.class);
        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostic", value));
        Toast.makeText(this, "Commande copiée", Toast.LENGTH_SHORT).show();
    }
}


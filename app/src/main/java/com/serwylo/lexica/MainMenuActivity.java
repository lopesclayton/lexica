/*
 *  Copyright (C) 2008-2009 Rev. Johnny Healey <rev.null@gmail.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.serwylo.lexica;

import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.serwylo.lexica.activities.HighScoresActivity;
import com.serwylo.lexica.activities.NewMultiplayerActivity;
import com.serwylo.lexica.databinding.SplashBinding;
import com.serwylo.lexica.db.Database;
import com.serwylo.lexica.db.GameMode;
import com.serwylo.lexica.db.GameModeRepository;
import com.serwylo.lexica.db.Result;
import com.serwylo.lexica.db.ResultRepository;
import com.serwylo.lexica.db.migration.MigrateHighScoresFromPreferences;
import com.serwylo.lexica.lang.Language;
import com.serwylo.lexica.lang.LanguageLabel;

import java.util.Locale;

public class MainMenuActivity extends AppCompatActivity {

    @SuppressWarnings("unused")
    protected static final String TAG = "Lexica";

    private String currentTheme = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentTheme = ThemeManager.getInstance().applyTheme(this);
        load();
    }

    private void splashScreen(GameMode gameMode, Result highScore) {

        Language language = new Util().getSelectedLanguageOrDefault(this);

        SplashBinding binding = SplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        binding.newGame.setOnClickListener(v -> {
            Intent intent = new Intent("com.serwylo.lexica.action.NEW_GAME").setPackage(getPackageName());
            intent.putExtra("gameMode", gameMode);
            intent.putExtra("lang", language.getName());
            startActivity(intent);
        });

        binding.gameModeButton.setOnClickListener(v -> startActivity(new Intent(this, ChooseGameModeActivity.class)));
        binding.gameModeButton.setText(gameMode.label(this));

        binding.languageButton.setText(LanguageLabel.getLabel(this, language));
        binding.languageButton.setOnClickListener(v -> startActivity(new Intent(this, ChooseLexiconActivity.class)));

        if (savedGame()) {
            binding.restoreGame.setOnClickListener(v -> startActivity(new Intent("com.serwylo.lexica.action.RESTORE_GAME").setPackage(getPackageName())));
            binding.restoreGame.setEnabled(true);
        }

        binding.about.setOnClickListener(v -> new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(R.string.app_name)
                .setItems(new String[]{getString(R.string.wigo_privacy_policy), getString(R.string.wigo_original_project),
                        getString(R.string.wigo_modified_source), getString(R.string.wigo_adaptation)}, (dialog, which) -> {
                    if (which == 3) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setTitle(R.string.app_name).setMessage(R.string.wigo_adaptation_details)
                                .setPositiveButton(android.R.string.ok, null).show();
                        return;
                    }
                    String url = which == 0
                            ? "https://s3.sa-east-1.amazonaws.com/lupper.com.br/politica-privacidade-jogos.html"
                            : which == 1 ? "https://github.com/lexica/lexica"
                            : "https://github.com/lopesclayton/lexica";
                    try {
                        Intent external = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        if (which == 0) startActivity(external); // Privacy remains freely accessible.
                        else ParentalExternalAction.start(this, external);
                    } catch (android.content.ActivityNotFoundException unavailable) {
                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                .setMessage(url).setPositiveButton(android.R.string.ok, null).show();
                    }
                }).setNegativeButton(android.R.string.cancel, null).show());

        binding.newMultiplayerGame.setOnClickListener(v -> {
            startActivity(new Intent(this, NewMultiplayerActivity.class));
        });

        binding.preferences.setOnClickListener(v -> startActivity(new Intent("com.serwylo.lexica.action.CONFIGURE").setClassName(getPackageName(), "com.serwylo.lexica.SettingsActivity")));

        long score = highScore == null ? 0 : highScore.getScore();
        binding.highScore.setText(String.format(Locale.getDefault(), "%d", score));

        // Think we can get away without yet-another button. It will lower the discoverability
        // of the feature, but simplify the home screen a bit more.
        binding.highScoreLabel.setOnClickListener(v -> startActivity(new Intent(this, HighScoresActivity.class)));
        binding.highScore.setOnClickListener(v -> startActivity(new Intent(this, HighScoresActivity.class)));

        Changelog.show(this);
    }

    public void onResume() {
        super.onResume();

        // Upon returning from settings, the user may hit the "Up" button in the toolbar (in which
        // case the main splash screen will be recreated from scratch and themed appropriately in
        // the onCreate() method), or via the "back" button, in which case we will get here.
        //
        // If the user pressed back, then we will check if they changed the theme from what we set
        // it to originally during onCreate(), and if so, force the activity to be recreated (and
        // hence rethemed).
        if (!ThemeManager.getInstance().getCurrentTheme().equals(currentTheme)) {
            ThemeManager.getInstance().forceRestartActivityToRetheme(this);
        }

        load();
    }

    public boolean savedGame() {
        return new GameSaverPersistent(this).hasSavedGame();
    }

    private void load() {
        AsyncTask.execute(() -> {

            // Force migrations to run prior to querying the database.
            // This is required because we populate default game modes, for which we need at least one to be present.
            // https://stackoverflow.com/a/55067991
            final Database db = Database.get(this);
            db.getOpenHelper().getReadableDatabase();

            Language language = new Util().getSelectedLanguageOrDefault(this);

            final GameModeRepository gameModeRepository = new GameModeRepository(getApplicationContext());
            final ResultRepository resultRepository = new ResultRepository(this);

            if (!gameModeRepository.hasGameModes()) {
                new MigrateHighScoresFromPreferences(this).initialiseDb(db.gameModeDao(), db.resultDao());
            }

            final GameMode gameMode = gameModeRepository.loadCurrentGameMode();
            final Result highScore = resultRepository.findHighScore(gameMode, language);

            runOnUiThread(() -> splashScreen(gameMode, highScore));

        });
    }

}

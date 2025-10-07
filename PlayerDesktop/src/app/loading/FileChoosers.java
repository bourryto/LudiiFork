package app.loading;

import app.App;
import app.DesktopApp;
import app.util.SettingsDesktop;

import javax.swing.*;
import java.awt.*;

public class FileChoosers {
    JFileChooser jsonFileChooser;
    JFileChooser jarFileChooser;
    JFileChooser gameFileChooser;
    JFileChooser aiDefFileChooser;
    JFileChooser saveGameFileChooser;
    JFileChooser loadTrialFileChooser;
    JFileChooser loadTournamentFileChooser;
    public FileChoosers(App app)
    {
        jsonFileChooser = FileLoading.createFileChooser(app.lastSelectedJsonPath(), ".json", "JSON files (.json)");
        jarFileChooser = FileLoading.createFileChooser(app.lastSelectedJarPath(), ".jar", "JAR files (.jar)");
        gameFileChooser = FileLoading.createFileChooser(app.lastSelectedGamePath(), ".lud", "LUD files (.lud)");
        aiDefFileChooser = FileLoading.createFileChooser(app.lastSelectedAiDefPath(), "ai.def", "AI.DEF files (ai.def)");

        // Also create file chooser for saving played games
        saveGameFileChooser = new JFileChooser(app.lastSelectedSaveGamePath());
        saveGameFileChooser.setPreferredSize(new Dimension(SettingsDesktop.defaultWidth, SettingsDesktop.defaultHeight));

        loadTrialFileChooser = new JFileChooser(app.lastSelectedLoadTrialPath());
        loadTrialFileChooser.setPreferredSize(new Dimension(SettingsDesktop.defaultWidth, SettingsDesktop.defaultHeight));

        loadTournamentFileChooser = new JFileChooser(app.lastSelectedLoadTournamentPath());
        loadTournamentFileChooser.setPreferredSize(new Dimension(SettingsDesktop.defaultWidth, SettingsDesktop.defaultHeight));
    }
}

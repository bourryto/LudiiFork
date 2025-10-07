package app;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import app.display.MainWindow;
import org.json.JSONObject;

import app.display.MainWindowClient;
import app.loading.FileLoading;
import app.loading.GameLoading;
import app.loading.TrialLoading;
import app.menu.ClientMainMenu;
import app.menu.MainMenuFunctions;
import app.util.SettingsDesktop;
import app.util.UserPreferences;
import app.utils.GameSetup;
import app.utils.SettingsExhibition;
import game.Game;
import game.rules.phase.Phase;
import main.Constants;
import main.StringRoutines;
import main.options.GameOptions;
import manager.ai.AIDetails;
import manager.ai.AIUtil;
import other.context.Context;

//-----------------------------------------------------------------------------

/**
 * The main player object.
 *
 * @author Matthew.Stephenson and cambolbro and Eric.Piette
 */
public class ClientApp extends App
{
    /** App name. */
    public static final String AppName = "Ludii Player";

    /** Minimum resolution of the application. */
    private static final int minimumViewWidth = 400;
    private static final int minimumViewHeight = 400;

    //-------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public ClientApp()
    {
        // Do nothing.
    }

    //-------------------------------------------------------------------------

    /**
     * Create the main Client application.
     */
    public void createClientApp()
    {
        // Invoke UI in the correct thread, otherwise menu may not draw
        SwingUtilities.invokeLater(new Runnable()
        {
            @Override
            public void run()
            {
                for (int i = 0; i < Constants.MAX_PLAYERS + 1; i++) // one extra for the shared player
                {
                    final JSONObject json = new JSONObject()
                            .put("AI", new JSONObject()
                                    .put("algorithm", "Human")
                            );

                    manager().aiSelected()[i] = new AIDetails(manager(), json, i, "Human");
                }
                try
                {
                    createFrame();
                }
                catch (final SQLException e)
                {
                    e.printStackTrace();
                }
            }
        });
    }

    //-------------------------------------------------------------------------

    /**
     * Gets the full frame title for displaying at the top of the application.
     */
    public String getFrameTitle(final Context context)
    {
        final Game game = context.game();
        String frameTitle = AppName + " - " + game.name();
        GameOptions gameOptions = game.description().gameOptions();

        if (manager().settingsManager().userSelections().ruleset() != Constants.UNDEFINED && !SettingsExhibition.exhibitionVersion)
        {
            final String rulesetName = game.description().rulesets().get(manager().settingsManager().userSelections().ruleset()).heading();
            frameTitle += " (" + rulesetName + ")";
        }
        else if (gameOptions.numCategories() > 0)
        {
            final List<String> optionHeadings = gameOptions.allOptionStrings(manager().settingsManager().userSelections().selectedOptionStrings());

            final boolean defaultOptionsLoaded = optionHeadings.equals(gameOptions.allOptionStrings(new ArrayList<String>()));

            if (optionHeadings.size() > 0 && !defaultOptionsLoaded)
            {
                final String appendOptions = " (" + StringRoutines.join(", ", optionHeadings) + ")";
                frameTitle += appendOptions;
            }
        }

        if (context.isAMatch())
        {
            final Context instanceContext = context.currentInstanceContext();
            frameTitle += " - " + instanceContext.game().name();
            gameOptions =  game.description().gameOptions();

            if (gameOptions != null && gameOptions.numCategories() > 0)
            {
                String appendOptions = " (";

                int found = 0;
                for (int cat = 0; cat < gameOptions.numCategories(); cat++)
                {
                    try
                    {
                        if (gameOptions.categories().get(cat).options().size() > 0)
                        {
                            final List<String> optionHeadings = gameOptions.categories().get(cat).options().get(0).menuHeadings();
                            String optionSelected = optionHeadings.get(0);
                            optionSelected = optionSelected.substring(optionSelected.indexOf('/')+1);
                            if (found > 0)
                                appendOptions += ", ";
                            appendOptions += optionSelected;
                            found++;
                        }
                    }
                    catch (final Exception e)
                    {
                        e.printStackTrace();
                        //break;
                    }
                }
                appendOptions += ")";

                if (found > 0)
                    frameTitle += appendOptions;
            }

            frameTitle += " - game #" + (manager().ref().context().completedTrials().size() + 1);
        }

        if (manager().settingsNetwork().getActiveGameId() > 0 && manager().settingsNetwork().getTournamentId() > 0)
            frameTitle += " (game " + manager().settingsNetwork().getActiveGameId() + " in tournament " + manager().settingsNetwork().getTournamentId() + ")";
        else if (manager().settingsNetwork().getActiveGameId() > 0)
            frameTitle += " (game " + manager().settingsNetwork().getActiveGameId() + ")";

        if (settingsPlayer().showPhaseInTitle() && !context.game().hasSubgames())
        {
            final int mover = context.state().mover();
            final int indexPhase = context.state().currentPhase(mover);
            final Phase phase = context.game().rules().phases()[indexPhase];
            frameTitle += " (phase " + phase.name() + ")";
        }

        return frameTitle;
    }

    //-------------------------------------------------------------------------

    /**
     * Launch the frame.
     */
    void createFrame() throws SQLException
    {
        try
        {
            UserPreferences.loadPreferences(this);
        }
        catch (final Exception e)
        {
            System.out.println("Failed to create preferences file.");
            e.printStackTrace();
        }

        try
        {
            frame = new JFrameListener(AppName, this);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

            // Logo
            try
            {
                final URL resource = this.getClass().getResource("/ludii-logo-100x100.png");
                final BufferedImage image = ImageIO.read(resource);
                frame.setIconImage(image);
            }
            catch (final IOException e)
            {
                e.printStackTrace();
            }

            view = new MainWindowClient(this);
            frame.setContentPane(view);
            frame.setSize(SettingsDesktop.defaultWidth, SettingsDesktop.defaultHeight);

            if (SettingsExhibition.exhibitionVersion)
            {
                frame.setUndecorated(true);
                frame.setResizable(false);
                frame.setSize(SettingsExhibition.exhibitionDisplayWidth, SettingsExhibition.exhibitionDisplayHeight);
            }

            try
            {
                if (settingsPlayer().defaultX() == -1 || settingsPlayer().defaultY() == -1)
                    frame.setLocationRelativeTo(null);
                else
                    frame.setLocation(settingsPlayer().defaultX(), settingsPlayer().defaultY());

                if (settingsPlayer().frameMaximised())
                    frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
            }
            catch (final Exception e)
            {
                frame.setLocationRelativeTo(null);
            }

            frame.setVisible(true);
            frame.setMinimumSize(new Dimension(minimumViewWidth, minimumViewHeight));

            FileLoading.createFileChoosers(this);
            setCurrentGraphicsDevice(frame.getGraphicsConfiguration().getDevice());

            // gets called when the app is closed (save preferences and trial)
            Runtime.getRuntime().addShutdownHook(new Thread()
            {
                @Override
                public void run()
                {
                    appClosedTasks();
                }
            });

            loadInitialGame(true);
            // Me TODO: change port so network communication is possible
            //LocalFunctions.initialiseServerSocket(this.manager(), 4444);
        }
        catch (final Exception e)
        {
            System.out.println("Failed to create application frame.");
            e.printStackTrace();
        }
    }

    //-------------------------------------------------------------------------

}

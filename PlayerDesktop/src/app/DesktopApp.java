package app;

import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

import manager.network.local.LocalFunctions;
import org.json.JSONObject;

import app.display.MainWindowDesktop;
import app.loading.FileLoading;
import app.loading.GameLoading;
import app.loading.TrialLoading;
import app.menu.MainMenu;
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
public class DesktopApp extends App
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
	public DesktopApp()
	{
		// Do nothing.
	}
	
	//-------------------------------------------------------------------------

	/**
	 * Create the main Desktop application.
	 */
	public void createDesktopApp()
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

		frameTitle += " - Port " + this.port;
		return frameTitle;
	}

	//-------------------------------------------------------------------------
	
	/**
	 * Display an error message on the status panel.
	 */
	@Override
	public void reportError(final String text)
	{
		if (view != null)
		{
			if (frame != null)
			{
				frame.setContentPane(view);
				frame.repaint();
				frame.revalidate();
			}
		}
		
		EventQueue.invokeLater(() -> 
		{
			addTextToStatusPanel(text + "\n");
		});
	}

	//---------------------------------------------------------------------------

	/** Tasks that are performed when the application is closed. */
	public void appClosedTasks()
	{
		if (SettingsExhibition.exhibitionVersion)
			return;
		
		manager().settingsNetwork().restoreAiPlayers(manager());
		
		// Close all AI objects
		for (final AIDetails ai : manager().aiSelected())
			if (ai.ai() != null)
				ai.ai().closeAI();
		
		if (manager().ref().context().game().equipmentWithStochastic())
			manager().ref().context().trial().reset(manager().ref().context().game());
		
		// Save the current trial
		final File file = new File("." + File.separator + "ludii.trl");
		TrialLoading.saveTrial(this, file);

		// Save the rest of the preferences
		UserPreferences.savePreferences(this);
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

			view = new MainWindowDesktop(this);
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
			System.out.println(manager().savedLudName());
			// BOURRYTO - COMMENT: open local port on startup
			LocalFunctions.initialiseServerSocket(this.manager(), this.port);
		}
		catch (final Exception e)
		{
			System.out.println("Failed to create application frame.");
			e.printStackTrace();
		}
	}

	//-------------------------------------------------------------------------
		
	/** 
	 * Loads the initial game. 
	 * Either the default game of the previously loaded game when the application was last closed. 
	 */
	protected void loadInitialGame(final boolean firstTry)
	{
		try
		{
			if (SettingsExhibition.exhibitionVersion)
			{
				GameLoading.loadGameFromMemory(this, SettingsExhibition.exhibitionGamePath, false);
				
				if (SettingsExhibition.againstAI)
				{
					final JSONObject json = new JSONObject().put("AI",
							new JSONObject()
							.put("algorithm", "UCT")
							);
					AIUtil.updateSelectedAI(manager(), json, 2, "UCT");
					manager().aiSelected()[2].setThinkTime(SettingsExhibition.thinkingTime);
				}
				
				bridge().settingsVC().setShowPossibleMoves(true);
				
				return;
			}
			
			if (firstTry)
				TrialLoading.loadStartTrial(this);
			
			if (manager().ref().context() == null)
			{
				if (firstTry)
					GameLoading.loadGameFromMemory(this, Constants.DEFAULT_GAME_PATH, false);
				else
				{
					settingsPlayer().setLoadedFromMemory(true);
					GameSetup.compileAndShowGame(this, Constants.FAIL_SAFE_GAME_DESCRIPTION, false);
					EventQueue.invokeLater(() -> 
					{
						setTemporaryMessage("Failed to start game. Loading default game (Tic-Tac-Toe).");
					});
				}
			}
			
			frame.setJMenuBar(new MainMenu(this));
	
			for (int i = 1; i <=  manager().ref().context().game().players().count(); i++)
				if (aiSelected()[i] != null)
					AIUtil.updateSelectedAI(manager(), manager().aiSelected()[i].object(), i, manager().aiSelected()[i].menuItemName());
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			
			if (firstTry)
			{
				// Try to load the default game.
				manager().setSavedLudName(null);
				settingsPlayer().setLoadedFromMemory(true);
				setLoadTrial(false);
				loadInitialGame(false);
			}
			
			if (manager().savedLudName() != null)
				addTextToStatusPanel("Failed to start game: " + manager().savedLudName() + "\n");
			else if (manager().ref().context().game().name() != null)
				addTextToStatusPanel("Failed to start external game description.\n");
		}
	}

	//-------------------------------------------------------------------------


	@Override
	public void updateFrameTitle(final boolean alsoUpdateMenu)
	{
		frame().setTitle(getFrameTitle(manager().ref().context()));
		
		if (alsoUpdateMenu)
		{
			frame().setJMenuBar(new MainMenu(this));
			view().createPanels();
		}
	}

	//-------------------------------------------------------------------------



}

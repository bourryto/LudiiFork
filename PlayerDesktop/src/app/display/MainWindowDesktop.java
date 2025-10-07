package app.display;

import java.awt.Color;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.JPanel;
import javax.swing.Timer;

import app.DesktopApp;
import app.PlayerApp;
import app.display.dialogs.MoveDialog.SandboxDialog;
import app.display.util.DevTooltip;
import app.display.util.ZoomBox;
import app.display.views.OverlayView;
import app.display.views.tabs.TabView;
import app.loading.FileLoading;
import app.move.MouseHandler;
import app.utils.AnimationVisualsType;
import app.utils.GUIUtil;
import app.utils.MVCSetup;
import app.utils.SettingsExhibition;
import app.utils.sandbox.SandboxValueType;
import app.views.BoardView;
import app.views.View;
import app.views.players.PlayerView;
import app.views.tools.ToolView;
import game.equipment.container.Container;
import main.Constants;
import other.context.Context;
import other.location.Location;
import other.topology.Cell;
import other.topology.Edge;
import other.topology.Vertex;
import util.LocationUtil;

//-----------------------------------------------------------------------------

/**
 * Main Window for displaying the application
 *
 * @author Matthew.Stephenson and cambolbro and Eric.Piette 
 */
public class MainWindowDesktop extends MainWindow
{

	/**
	 * Constructor.
	 */
	public MainWindowDesktop(final DesktopApp app)
	{
		super(app);
		addMouseListener(this);
		addMouseMotionListener(this);
		zoomBox = new ZoomBox(app, this);
	}

	//-------------------------------------------------------------------------

	/**
	 * Create UI panels.
	 */
	public void createPanels()
	{
		MVCSetup.setMVC(app);	
		panels.clear();
		removeAll();
		
		final boolean portraitMode = width < height;
		
		// Create board panel
		boardPanel = new BoardView(app, false);
		panels.add(boardPanel);
		
		// create the player panel
		playerPanel = new PlayerView(app, portraitMode, false);
		panels.add(playerPanel);

		// Create tool panel
		toolPanel = new ToolView(app, portraitMode);
		panels.add(toolPanel);

		// Create tab panel
		if (!app.settingsPlayer().isPerformingTutorialVisualisation())
		{
			tabPanel = new TabView(app, portraitMode);
			panels.add(tabPanel);
		}
		
		// Create overlay panel
		overlayPanel = new OverlayView(app);
		panels.add(overlayPanel());
		
		if (SettingsExhibition.exhibitionVersion)
			app.settingsPlayer().setAnimationType(AnimationVisualsType.Single);
	}

	//-------------------------------------------------------------------------

	@Override
	public void paintComponent(final Graphics g)
	{
		try
		{
			final Graphics2D g2d = (Graphics2D) g;
			g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
			g2d.setRenderingHint(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_ALPHA_INTERPOLATION, RenderingHints.VALUE_ALPHA_INTERPOLATION_QUALITY);
			g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			
			if (!app.bridge().settingsVC().thisFrameIsAnimated())
				app.contextSnapshot().setContext(app);
			
			setDisplayFont(app);
			app.graphicsCache().allDrawnComponents().clear();
			
			if (panels.isEmpty() || width != getWidth() || height != getHeight())
			{
				width = getWidth();
				height = getHeight();
				createPanels();
			}
			
			app.updateTabs(app.contextSnapshot().getContext(app));

			// Set application background colour.
			if (app.settingsPlayer().usingMYOGApp())
				g2d.setColor(Color.black);
			else if (SettingsExhibition.exhibitionVersion)
				g2d.setColor(Color.black);
			else
				g2d.setColor(Color.white);
			
			g2d.fillRect(0, 0, getWidth(), getHeight());

			// Paint each panel
			for (final View panel : panels)
				if (g.getClipBounds().intersects(panel.placement()))
					panel.paint(g2d);
			
			// Report any errors that occurred.
			reportErrors();
			
			// Delayed and invoked later to be sure the painting is complete.
			new java.util.Timer().schedule
			( 
		        new java.util.TimerTask() 
		        {
		            @Override
		            public void run() 
		            {
		            	EventQueue.invokeLater(() -> 
		    			{
		    				EventQueue.invokeLater(() -> 
		    				{
		    					isPainting = false;
		    				});
		    			});
		            }
		        }, 
		        500 
			);
		}
		catch (final Exception e)
		{
			e.printStackTrace();
			
			EventQueue.invokeLater(() -> 
			{
				setTemporaryMessage("Error painting components.");
				if(FileLoading.writeErrorFile("error_report.txt", e)){
					setTemporaryMessage("Error report file created.");
				}
			});
		}
	}


}

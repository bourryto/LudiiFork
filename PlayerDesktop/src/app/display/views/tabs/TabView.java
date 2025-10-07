package app.display.views.tabs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;

import app.Apps;
import app.ClientApp;
import app.DesktopApp;
import app.PlayerApp;
import app.display.views.tabs.pages.AnalysisPage;
import app.display.views.tabs.pages.InfoPage;
import app.display.views.tabs.pages.LudemePage;
import app.display.views.tabs.pages.MovesPage;
import app.display.views.tabs.pages.RulesPage;
import app.display.views.tabs.pages.StatusPage;
import app.display.views.tabs.pages.TurnsPage;
import app.utils.SettingsExhibition;
import app.views.View;
import com.sun.security.ntlm.Client;
import other.context.Context;

//-----------------------------------------------------------------------------

/**
 * View area containing all TabViews
 *
 * @author Matthew.Stephenson and cambolbro
 */
public class TabView extends View
{
	/** Background colour. */
	public final static Color bgColour = new Color(255, 255, 230);
	
	/** Size of tab headings. */
	public final static int fontSize = 16;  
	
	/** Tab Page values. */
	public static final int PanelStatus = 0;
	public static final int PanelMoves = 1;
	public static final int PanelTurns = 2;
	public static final int PanelAnalysis = 3;
	public static final int PanelLudeme = 4;
	public static final int PanelRules = 5;
	public static final int PanelInfo = 6;
	
	//-------------------------------------------------------------------------

	/** If the titles of the tabs has been already set. */
	private boolean titlesSet = false;
	
	/** Tab panels. */
	private final List<TabPage> pages = new ArrayList<>();
	
	//-------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public TabView(final PlayerApp app, final boolean portraitMode)
	{
		super(app);
		
		pages.clear();

		final int toolHeight;
		int boardSize;
		int startX;
		int startY;
		int width;
		int height;
		int placementHeight;

		toolHeight = Apps.getFromID(app.manager().getAppID()).view().toolPanel().placement().height;
		boardSize = Apps.getFromID(app.manager().getAppID()).view().getBoardPanel().placement().width;

		startX = boardSize;
		startY = Apps.getFromID(app.manager().getAppID()).view().getPlayerPanel().placement().height;
		width = Apps.getFromID(app.manager().getAppID()).view().getWidth() - boardSize;
		placementHeight = Apps.getFromID(app.manager().getAppID()).view().getPlayerPanel().placement.height;
		height = Apps.getFromID(app.manager().getAppID()).view().getHeight() - placementHeight - toolHeight;

		if (SettingsExhibition.exhibitionVersion)
		{
			height -= 100;
			width -= 50;
			startY += 120;
		}
		
		if (portraitMode)
		{
			boardSize = app.width();
			startX = 8;

			startY = boardSize + placementHeight + 40;	// +40 for the height of the toolView
			width = boardSize - 16;
			height = app.height() - boardSize - placementHeight - 40;
		}
		
		placement.setBounds(startX, startY, width, height);
		
		// Add tab pages
		final Rectangle tabPagePlacement = new Rectangle(placement.x + 10, placement.y + TabView.fontSize + 6, placement.width - 16, placement.height - TabView.fontSize - 20);
		final TabPage statusPage   = new StatusPage(app, tabPagePlacement, " Status ",   "", PanelStatus, this);
		final TabPage movesPage    = new MovesPage(app, tabPagePlacement, " Moves ",    "", PanelMoves, this);
		final TabPage turnsPage    = new TurnsPage(app, tabPagePlacement, " Turns",     "", PanelTurns, this);
		final TabPage analysisPage = new AnalysisPage(app, tabPagePlacement, " Analysis ", "", PanelAnalysis, this);
		final TabPage ludemePage   = new LudemePage(app, tabPagePlacement, " Ludeme  ",  "", PanelLudeme, this);
		final TabPage rulesPage    = new RulesPage(app, tabPagePlacement, " Rules ",    "", PanelRules, this);
		final TabPage infoPage     = new InfoPage(app, tabPagePlacement, " Info  ",    "", PanelInfo, this);
		pages.add(statusPage);
		pages.add(movesPage);
		pages.add(turnsPage);
		pages.add(analysisPage);
		pages.add(ludemePage);
		pages.add(rulesPage);
		pages.add(infoPage);	
		
		resetTabs();
		
		select(app.settingsPlayer().tabSelected());
		
		if (SettingsExhibition.exhibitionVersion)
			select(5);
		
		for (final View view : pages)
			Apps.getFromID(app.manager().getAppID()).view().getPanels().add(view);
	}

	//-------------------------------------------------------------------------
	
	public boolean titlesSet()
	{
		return titlesSet;
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void paint(final Graphics2D g2d)
	{
		if (SettingsExhibition.exhibitionVersion)
			return;
		
		final int x0 = placement.x;
		final int y0 = placement.y;
		final int sx = placement.width;
		final int sy = placement.height;

		if (!titlesSet)
		{
			setTitleRects();
			titlesSet = true;
		}

		g2d.setColor(Color.white);
		g2d.fillRect(x0, y0, sx, sy);

		// Title bar
		final int tx0 = placement.x;
		final int ty0 = placement.y;
		final int tsx = placement.width;
		final int tsy = fontSize + 6;

		g2d.setColor(new Color(200, 200, 200));
		g2d.fillRect(tx0, ty0, tsx, tsy);
		
		for (final TabPage page : pages)
			page.paint(g2d);
		
		paintDebug(g2d, Color.GREEN);
	}

	//-------------------------------------------------------------------------

	/**
	 * Sets rectangle bounds for all tab page titles. Used to select specific tab pages.
	 */
	public void setTitleRects()
	{
		int x = placement.x;
		final int y = placement.y;
		
		for (final TabPage page : pages)
		{
			final int wd = (int)page.titleRect().getWidth();
			final int ht = fontSize + 6;
			
			page.setTitleRect(x, y, wd, ht);
			x += wd;
		}
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Select the specified tab index
	 * @param pid
	 */
	public void select(final int pid)
	{
		for (final TabPage p : pages)
			p.show(false);
		
		pages.get(pid).show(true);
		app.settingsPlayer().setTabSelected(pid);
		app.repaint();
	}
	
	//-------------------------------------------------------------------------
	
	/**
	 * Handle click on tab title.
	 * @param pixel
	 */
	public void clickAt(final Point pixel)
	{
		for (final TabPage p : pages)
			if (p.titleRect.contains(pixel.x, pixel.y))
			{
				select(p.pageIndex);
				return;
			}
	}
	
	//-------------------------------------------------------------------------
	
	public void updateTabs(final Context context)
	{
		for(int i = 0; i < pages.size(); i++)
			pages.get(i).updatePage(context);
	}
	
	//-------------------------------------------------------------------------
	
	public void resetTabs()
	{
		for(int i = 0; i < pages.size(); i++)
			pages.get(i).reset();
	}
	
	//-------------------------------------------------------------------------

	public TabPage page(final int i)
	{
		return pages.get(i);
	}
	
	public List<TabPage> pages()
	{
		return pages;
	}
	
	//-------------------------------------------------------------------------
	
}

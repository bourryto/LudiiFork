package app.display.views.tabs;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.util.*;

import app.Apps;
import app.PlayerApp;
import app.display.views.tabs.pages.*;
import app.utils.SettingsExhibition;
import app.views.View;
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
    public static final int PanelMessages = 1;
    public static final int PanelAppInfo = 2;
	public static final int PanelMoves = 3;
	public static final int PanelTurns = 4;
    public static final int PanelJSON = 5;
	public static final int PanelLudeme = 6;
	public static final int PanelRules = 7;
	public static final int PanelInfo = 8;
	public static final int PanelAnalysis = 9;

	
	//-------------------------------------------------------------------------

	/** If the titles of the tabs has been already set. */
	private boolean titlesSet = false;
	
	/** Tab panels. */
	private final Map<Integer, TabPage> pages = new HashMap<>();
	
	//-------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
    public TabView(final PlayerApp app, final boolean portraitMode){
        this(app, portraitMode, new Integer[]{0, 1, 2, 3, 4, 5, 6, 7, 8}, 5);
    }
	public TabView(final PlayerApp app, final boolean portraitMode, Integer[] pagesToAdd, Integer selectedPage)
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

        // BOURRYTO - TODO: intial game compilation message only gets added to one window status page,
        //                  das muss ich irgendwie auseinander frickelen
		// Add tab pages
		final Rectangle tabPagePlacement = new Rectangle(placement.x + 10, placement.y + TabView.fontSize + 6, placement.width - 16, placement.height - TabView.fontSize - 20);
		for (Integer pageId : pagesToAdd){
            pages.put(pageId, getNewTabPage(pageId, tabPagePlacement));
        }

		resetTabs();
		
		select(app.settingsPlayer().tabSelected());
		
		if (SettingsExhibition.exhibitionVersion)
			select(5);
		else{select(selectedPage);}
		for (final View view : pages.values())
			Apps.getFromID(app.manager().getAppID()).view().getPanels().add(view);
	}

    private TabPage getNewTabPage(int pageId, Rectangle tabPagePlacement)
    {
        switch (pageId) {
            case PanelStatus:
                return new StatusPage(app, tabPagePlacement, " Status   ", "", PanelStatus, this);
            case PanelMessages:
                return new MessagingPage(app, tabPagePlacement, " Messages ", "", PanelMessages, this);
            case PanelAppInfo:
                return new AppInfoPage(app, tabPagePlacement, " AppInfo  ", "", PanelAppInfo, this);
            case PanelMoves:
                return new MovesPage(app, tabPagePlacement, " Moves    ", "", PanelMoves, this);
            case PanelTurns:
                return new TurnsPage(app, tabPagePlacement, " Turns    ", "", PanelTurns, this);
            case PanelAnalysis:
                return new AnalysisPage(app, tabPagePlacement, " Analysis ", "", PanelAnalysis, this);
            case PanelLudeme:
                return new LudemePage(app, tabPagePlacement, " Ludeme   ", "", PanelLudeme, this);
            case PanelRules:
                return new RulesPage(app, tabPagePlacement, " Rules    ", "", PanelRules, this);
            case PanelInfo:
                return new InfoPage(app, tabPagePlacement, " Info     ", "", PanelInfo, this);
            default:
                return new JSONPage(app, tabPagePlacement, " JSON ", "", PanelJSON, this);
        }
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
		
		for (final TabPage page : pages.values())
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
		
		for (final TabPage page : pages.values())
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
		for (final TabPage p : pages.values())
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
		for (final TabPage p : pages.values())
			if (p.titleRect.contains(pixel.x, pixel.y))
			{
				select(p.pageId);
				return;
			}
	}
	
	//-------------------------------------------------------------------------
	
	public void updateTabs(final Context context)
	{
        for (TabPage page : pages.values()){
            page.updatePage(context);
        }
	}
	
	//-------------------------------------------------------------------------
	
	public void resetTabs()
	{
        for (TabPage page : pages.values()){
            page.reset();
        }
	}
	
	//-------------------------------------------------------------------------

	public TabPage page(final int i)
	{
		return pages.get(i);
	}
	
	public Collection<TabPage> pages()
	{
		return pages.values();
	}
	
	//-------------------------------------------------------------------------
	public void addMessage(final String text)
	{
        TabPage messagingPage = this.page(PanelMessages);
        if(messagingPage instanceof MessagingPage) {
            messagingPage.addText(text + "\n");
        } else {
            System.out.println("wrong instance");
        }
    }

    // ----------------------------------------------------------------------
    public static TabView getClientTabView(final PlayerApp app, final boolean portraitMode){
            return new TabView(app, portraitMode, new Integer[]{TabView.PanelStatus,
                                                                TabView.PanelMessages,
                                                                TabView.PanelAppInfo,
                                                                TabView.PanelMoves,
                                                                TabView.PanelTurns,
                                                                TabView.PanelRules,
                                                                TabView.PanelInfo,
                                                                TabView.PanelJSON},
                    TabView.PanelAppInfo);
    }
}

package app.display.views.tabs.pages;

import app.PlayerApp;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;
import other.context.Context;

import java.awt.*;
import java.util.Arrays;

/**
 * Tab for displaying information about the Application like network information
 * 
 * @author Bourryto
 */
public class AppInfoPage extends TabPage
{

	//-------------------------------------------------------------------------

	public AppInfoPage(final PlayerApp app, final Rectangle rect, final String title, final String text, final int pageId, final TabView parent)
	{
		super(app, rect, title, text, pageId, parent);
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void updatePage(final Context context)
	{
		reset();
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void reset()
	{
		clear();

        addText(String.format("%-12s: %d", "PORT", app.getPort()));
        addText(String.format("\n%-12s: %d", "ID", app.manager().getAppID()));

        addText(String.format("\n%-12s:", "Other Ports"));
        for(Integer otherPort: app.getOtherPorts()){
            addText(String.format("\n\t%d", otherPort));
        }
        addText(String.format("\n%-12s: %s", "Game", app.manager().savedLudName()));
        //addText(String.format("\n%-12s: %s", "Out manager", Arrays.toString(app.manager().central.outgoingMessages.toArray())));
        //addText(String.format("\n%-12s: %s", "Player Index", app.manager().getMyPlayerIndex()));
//        addText(String.format("\n%-12s: %s",));
        addText(String.format("\n%-12s: %s","Class", app.getClass()));

	}
	
	//-------------------------------------------------------------------------

}

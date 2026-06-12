package app;

import java.util.LinkedList;

import app.display.MainWindow;
import app.menu.MainMenu;
import other.context.Context;

//-----------------------------------------------------------------------------

/**
 * The main player object.
 *
 * @author Matthew.Stephenson and cambolbro and Eric.Piette
 */
public class ClientApp extends App
{

    //-------------------------------------------------------------------------

    /**
     * Constructor.
     */
    public ClientApp(int port, LinkedList<Integer> otherPorts)
    {
        super(Instance.CLIENT, port, otherPorts);
        this.view = MainWindow.getClientMainWindow(this);
        this.appName = "Ludii Client";
        this.menuItems[MainMenu.MenuAnalysis] = false;
        this.menuItems[MainMenu.MenuGeneration] = false;
        this.menuItems[MainMenu.MenuRemote] = false;
        this.menuItems[MainMenu.MenuDemos] = false;
        this.menuItems[MainMenu.MenuDeveloper] = false;
        this.menuItems[MainMenu.MenuBourryto] = false;
    }


    //-------------------------------------------------------------------------

    /**
     * Gets the full frame title for displaying at the top of the application.
     */
    public String getFrameTitle(final Context context)
    {
        String frameTitle = super.getFrameTitle(context);
        frameTitle += " - Port " + this.getPort();
        return frameTitle;
    }
    //-------------------------------------------------------------------------

}

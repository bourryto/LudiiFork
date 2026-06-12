package app;

import java.util.LinkedList;

import other.context.Context;

//-----------------------------------------------------------------------------

/**
 * The main player object.
 *
 * @author Matthew.Stephenson and cambolbro and Eric.Piette
 */
public class DesktopApp extends App
{
	/**
	 * Constructor.
	 */
	public DesktopApp(int port, LinkedList<Integer> otherPorts)
	{
		super(Instance.Desktop, port, otherPorts);
        this.appName = "Ludii Desktop";
	}
    public DesktopApp(boolean multipleWindows){
        super(Instance.Desktop, multipleWindows);
        this.appName = "Ludii Desktop";
    }
	public DesktopApp(){
        super(Instance.Desktop);
        this.appName = "Ludii Desktop";
    };


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


}

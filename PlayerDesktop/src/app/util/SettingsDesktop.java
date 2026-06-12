package app.util;

import java.awt.Toolkit;

import javax.swing.JDialog;

/**
 * Desktop specific settings
 * 
 * @author Matthew.Stephenson
 */
public class SettingsDesktop
{

	/** Default display width for the program (in pixels). */
	public static int defaultWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() * .75);

	/** Default display height for the program (in pixels). */
	public static int defaultHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() * .75);
	
	/** Whether a separate dialog (settings, puzzle, etc.) is open. */
	public static JDialog openDialog = null;

    public static int windowWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth());
    public static int halfWindowWidth = (int) (Toolkit.getDefaultToolkit().getScreenSize().getWidth() / 4);
    public static int windowHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight());
    public static int halfWindowHeight = (int) (Toolkit.getDefaultToolkit().getScreenSize().getHeight() / 2);

}

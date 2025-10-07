package app;

/**
 * Main point of Entry for running the Ludii application.
 * 
 * @author Matthew.Stephenson and Dennis Soemers
 */
public class StartDesktopApp
{
	private static DesktopApp desktopApp = null;
	private static DesktopApp desktopAppTwo = null;
	
	public static void main(final String[] args)
	{
		// The actual launching
		if (args.length == 0)
		{
			desktopApp = new DesktopApp();
			desktopApp.port = 4444;
			desktopApp.createDesktopApp();
		}
		else if(args[0].equalsIgnoreCase("two")){
			desktopApp = new DesktopApp();
			desktopApp.port = 4444;
			desktopApp.createDesktopApp();

			desktopAppTwo = new DesktopApp();
			desktopAppTwo.port = 4445;
			desktopAppTwo.createDesktopApp();
		}
		else if(args[0].equals("PORT")){
			int port = Integer.parseInt(args[1]);
			if (port < 1000 || port > 9999){
				System.out.println("please choose a port im vierstelligen bereich");
			}
			else {
				desktopApp = new DesktopApp();
				desktopApp.port = port;
				desktopApp.createDesktopApp();
			}
		}
		else
		{
			PlayerCLI.runCommand(args);
		}
	}

	// Used in case any agents need DesktopApp functions.
	public static DesktopApp desktopApp()
	{
		return desktopApp;
	}
}
package app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;

/**
 * Main point of Entry for running the Ludii application.
 * 
 * @author Matthew.Stephenson and Dennis Soemers
 */
public class StartDesktopApp
{
    // BOURRYTO - OBSOLETE
	private static DesktopApp desktopApp = null;
	private static DesktopApp desktopAppTwo = null;
	private static DesktopApp desktopAppThree = null;

	public static void main(final String[] args)
	{
        System.out.println("Starting the App(s) at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy - HH:mm:ss:ns")));
		// The actual launching
		if (args.length == 0)
		{
			desktopApp = new DesktopApp(4444, new LinkedList<>());
            desktopApp.createApp();
		}
		else if(args[0].equalsIgnoreCase("two")){
			desktopApp = new DesktopApp(4444, new LinkedList<>(Collections.singletonList(4445)));
			desktopApp.multipleWindows = true;
            desktopApp.createApp();

			desktopAppTwo = new DesktopApp(4445, new LinkedList<>(Collections.singletonList(4444)));
			desktopAppTwo.multipleWindows = true;
			desktopAppTwo.createApp();
		}
		else if(args[0].equalsIgnoreCase("three")){
			desktopApp = new DesktopApp(4444, new LinkedList<>(Arrays.asList(4445, 4446)));
			desktopApp.multipleWindows = true;
			desktopApp.createApp();

			desktopAppTwo = new DesktopApp(4445, new LinkedList<>(Arrays.asList(4444, 4446)));
			desktopAppTwo.multipleWindows = true;
			desktopAppTwo.createApp();

			desktopAppThree = new DesktopApp(4446, new LinkedList<>(Arrays.asList(4444, 4445)));
            desktopAppThree.multipleWindows = true;
            desktopAppThree.createApp();
		}
		else if(args[0].equalsIgnoreCase("3")){
			desktopApp = new DesktopApp(true);
			desktopAppTwo = new DesktopApp(true);
			desktopAppThree = new DesktopApp(true);

			desktopApp.createApp();
			desktopAppTwo.createApp();
            desktopAppThree.createApp();

		}
		else if(args[0].equals("PORT")){
			int port = Integer.parseInt(args[1]);
			if (port < 1000 || port > 9999){
				System.out.println("please choose a port im vierstelligen bereich");
			}
			else {
				desktopApp = new DesktopApp(port, new LinkedList<>(Collections.singletonList(port + 1)));
				desktopApp.createApp();
			}
		}
		else
		{
			PlayerCLI.runCommand(args);
		}
	}

}
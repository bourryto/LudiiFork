package app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;

/**
 * Main point of Entry for running the Ludii application.
 * 
 * @author Matthew.Stephenson and Dennis Soemers
 */
public class StartServerApp
{

	public static void main(final String[] args)
	{
        System.out.println("Starting the App(s) at " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd MMMM yyyy - HH:mm:ss:ns")));
		int n = 2;
        
        if (args.length != 0)
		{
            try {
                n = Integer.parseInt(args[0]);
            } catch (NumberFormatException e) {
                System.out.println(e.getMessage() + "\nStarting with 2 Players");
            }
		}

        LinkedList<DesktopApp> players = new LinkedList<>();
        for (int i = 0; i < n; i++) {
            DesktopApp app = new DesktopApp(true);
            players.add(app);
            app.createApp();
        }
        ServerApp serverApp = new ServerApp();
        serverApp.createApp();
        for (DesktopApp app : players) {
            serverApp.addPlayer(app.getID(), app.getPort());
        }
        String test = serverApp.getPlayersString();
        if (test == null || test.isEmpty()){
            System.out.println("No players found");
        }
        System.out.println(test);

	}
}
package app;

import com.sun.security.ntlm.Client;

import java.util.LinkedList;

/**
 * Main point of Entry for running the Ludii application.
 *
 * @author Matthew.Stephenson and Dennis Soemers
 */
public class StartClientApp
{
    private static DesktopApp DesktopApp = null;
    private static ClientApp ClientApp = null;

    public static void main(final String[] args)
    {
        // The actual launching
        if (args.length == 0)
        {
            /*
            DesktopApp = new DesktopApp();
            DesktopApp.isClient = true;
            DesktopApp.createApp();
             */

            ClientApp = new ClientApp(4444, new LinkedList<>());
            ClientApp.createApp();
        }
        else
        {
            PlayerCLI.runCommand(args);
        }
    }

    // Used in case any agents need DesktopApp functions.
    public static DesktopApp DesktopApp()
    {
        return DesktopApp;
    }
    // Used in case any agents need ClientApp functions.
    public static ClientApp ClientApp()
    {
        return ClientApp;
    }
}
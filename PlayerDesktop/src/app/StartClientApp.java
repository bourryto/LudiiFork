package app;

import com.sun.security.ntlm.Client;

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
            DesktopApp.createDesktopApp();
             */

            ClientApp = new ClientApp();
            ClientApp.createClientApp();
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
package app.menu;

import javax.swing.*;

public class NetworkJMenuItem extends JMenuItem {
    public int port;
    public NetworkJMenuItem(String label, int port) {
        super(label);
        this.port = port;
    }
}

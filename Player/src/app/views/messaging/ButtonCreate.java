package app.views.messaging;

import app.PlayerApp;
import app.views.tools.ToolButton;

import java.awt.*;

public class ButtonCreate extends ToolButton {

    public ButtonCreate(final PlayerApp app, final String name, final int cx, final int cy, final int sx, final int sy, final int buttonIndex){
        super(app, name, cx, cy, sx, sy, buttonIndex);
    }

    @Override
    public void draw(Graphics2D g2d) {
        final int cx = (int) rect.getCenterX();
        final int cy = (int) rect.getCenterY() + 5;

        g2d.setColor(getButtonColour());

        final Font oldFont = g2d.getFont();

        // Determine button scale, so that buttons are scaled up on the mobile version.
        // The desktop version assume a toolbar height of 32 pixels, this should be 64 for mobile version.
        final double scale = scaleForDevice();

        final int fontSize = (int)(26 * scale);
        final int flags = Font.BOLD;
        final Font font = new Font("Arial", flags, fontSize);
        g2d.setFont(font);
        g2d.setColor(getButtonColour());
        g2d.drawString("Create", cx - (int)(3 * scale), cy + (int)(6 * scale));
        g2d.setFont(oldFont);
    }

    @Override
    public void press() {

    }
}

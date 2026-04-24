package app.display.views.tabs.pages;

import app.Apps;
import app.PlayerApp;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;
import app.utils.SettingsExhibition;
import other.context.Context;

import javax.swing.text.BadLocationException;
import javax.swing.text.StyleConstants;
import javax.swing.text.html.HTMLDocument;
import javax.swing.text.html.HTMLEditorKit;
import java.awt.*;
import java.io.IOException;
import java.io.StringWriter;
import java.util.LinkedList;

public class MessagingPage extends TabPage {

    public MessagingPage(final PlayerApp app, final Rectangle rect, final String title, final String text, final int pageId, final TabView parent)
    {
        super(app, rect, title, text, pageId, parent);
    }

    @Override
    public void updatePage(Context context) {
        app.settingsPlayer().setSavedMessagesTabString(text());
        //System.out.println("[" + Apps.getApp(app.manager()).getPort() + "] messages page updated, text=" + text() + "'");
    }

    @Override
    public void addText(String str) {
        super.addText(str);
        app.settingsPlayer().setSavedMessagesTabString(text());
    }

    @Override
    public void reset() {
        clear();
        addText(app.settingsPlayer().savedMessagesTabString());
    }

}

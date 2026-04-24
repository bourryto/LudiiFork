package app.views.messaging;

import app.PlayerApp;
import app.views.View;

import java.awt.*;
import java.util.LinkedList;

public class MessagingView extends View {
    LinkedList<String> incoming = new LinkedList<>();
    String outgoing;
    ButtonCreate buttonCreate;

    public MessagingView(final PlayerApp app, final boolean portraitMode, int toolHeight){
        super(app);
        if(portraitMode){
            System.out.println("Messaging doesnt support portrait view. please make window width > height");
        }

        int inputHeight = 80;
        int outputHeight = 160;
        int buttonHeight = 80;
        int messagingViewHeight = inputHeight + outputHeight + buttonHeight;


        int boardSize = app.height();
        int startX = boardSize;
        int startY = app.height() - toolHeight - messagingViewHeight;
        int width = app.width() - boardSize;

        placement.setBounds(startX, startY, width, messagingViewHeight);

        buttonCreate = new ButtonCreate(app, "create", startX, startY, buttonHeight, buttonHeight*3, 0);
    }

    @Override
    public void paint(final Graphics2D g2d){

    }
}

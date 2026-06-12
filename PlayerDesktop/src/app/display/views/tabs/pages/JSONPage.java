package app.display.views.tabs.pages;

import app.Apps;
import app.PlayerApp;
import app.display.views.tabs.TabPage;
import app.display.views.tabs.TabView;
import game.Game;
import game.equipment.container.Container;
import game.equipment.component.Component;
import main.ExportUtils;
import other.context.Context;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Tab for displaying the rules of the current game.
 * 
 * @author Matthew.Stephenson
 */
public class JSONPage extends TabPage
{
    String[] buttons = new String[]{"Game", "Board", "Moves",/* "Game-Util", "Board-Util", "Moves-Util", */"Board-Full"};
    private static final String resetOn = "game-util";
    JPanel gamePanel = new JPanel();
    JPanel buttonPanel = new JPanel();
	//-------------------------------------------------------------------------

	public JSONPage(final PlayerApp app, final Rectangle rect, final String title, final String text, final int pageId, final TabView parent)
	{
		super(app, rect, title, text, pageId, parent);

        for (String button : buttons) {
            JButton b = new JButton(button);
            b.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent evt) {
                    set(button);
                }
            });
            buttonPanel.add(b);
        }
        buttonPanel.setLayout(new FlowLayout(FlowLayout.CENTER));
        buttonPanel.setBackground(Color.GREEN);
        buttonPanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
//        buttonPanel.add(chooseGameButton);
//        buttonPanel.add(chooseBoardButton);


        textArea.setCaretPosition(0);
        gamePanel.setLayout(new BoxLayout(gamePanel, BoxLayout.Y_AXIS));
        gamePanel.setBounds(placement);
        gamePanel.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        gamePanel.setBackground(Color.red);
        gamePanel.setFocusable(false);
        gamePanel.add(buttonPanel);
        gamePanel.add(scrollPane);
        gamePanel.revalidate();
        Apps.getFromID(app.manager().getAppID()).view().remove(scrollPane);
        Apps.getFromID(app.manager().getAppID()).view().add(gamePanel);
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void updatePage(final Context context)
	{
		//reset();
	}
	
	//-------------------------------------------------------------------------

	@Override
	public void reset()
	{
		clear();
		set(resetOn);
        textArea.setCaretPosition(0);
	}
	
	//-------------------------------------------------------------------------

    public void set(String ob)
    {
        clear();
        final Game game = app.contextSnapshot().getContext(app).game();
        String jsonString = "";
        switch(ob.toLowerCase()){
            case "game":
                jsonString = game.toJSON().toString(4);
                break;
            case "board":
                jsonString = game.board().toJSON().toString(4);
                break;
            case "moves":
                jsonString = game.moves(app.contextSnapshot().getContext(app)).toJSON().toString(4);
                break;
            /*
            case "game-util":
                jsonString = ExportUtils.toJSONWrapper(app.contextSnapshot().getContext(app).game()).toString(4);
                break;
            case "board-util":
                jsonString = ExportUtils.toJSONWrapper(game.board()).toString(4);
                break;
            case "moves-util":
                jsonString = ExportUtils.toJSONWrapper(game.moves(app.contextSnapshot().getContext(app))).toString(4);
                break;

             */
            case "board-full":
                //jsonString = Arrays.toString(game.equipment().regions()) +"\n"+ Arrays.toString(game.equipment().containers()) +"\n"+ Arrays.toString(game.equipment().components());
                jsonString += "\nGraph basis and shape: " + game.board().graph().basis() + " " + game.board().graph().shape();
                for (int i = 0; i < game.equipment().containers().length; i++) {
                    Container c =  game.equipment().containers()[i];
                    String containerState = app.manager().ref().context().state().containerStates()[i].toString();
                    containerState = containerState.replace("\n", "\n\t");
                    String whos = "";
                    for (int j = 0; j < c.numSites(); j++) {
                        int whocell = app.manager().ref().context().state().containerStates()[i].whoCell(j);
                        int whatcell = app.manager().ref().context().state().containerStates()[i].whatCell(j);
                        if (app.manager().ref().context().state().containerStates()[i].isEmptyCell(j)){
                        }
                        whos += ", " + j + " owned by:" + game.equipment().components()[whocell].name() + "("+whocell+" and is what ("+ whatcell+")";
                    }
                    jsonString += "\nContainer: " + c.name() + " -> " + c.topology().cells().toString() + "\n\tnumsites: " + c.numSites() +
                                                                                                            "\n\ttracks: " + c.tracks().toString() +
                                                                                                            "\n\tstyle: " + c.style().toString() +
                                                                                                            "\n\tdefault site type: " + c.defaultSite() +
                                                                                                            "\n\tcontainer state: " + containerState +
                                                                                                            "\n\twhos state: " + whos;

                }
                for (Component c: game.equipment().components()) jsonString += "\nComponent: " + c.name() + " -> " + c.role();
                jsonString += "\nGraph Game: " + game.isGraphGame() + ", Edge Game: " + game.isEdgeGame() + ", Vertex Game: " + game.isVertexGame() + ", Cell Game: " + game.isCellGame();
                jsonString += "\nState: " + app.manager().ref().context().state().toJSON();
                break;
        }
        addText(ob + "\n===\n" + jsonString);
        textArea.setCaretPosition(0);
    }
}

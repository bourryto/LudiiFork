package app;

import app.network.Address;
import app.network.Message;
import app.utils.SettingsExhibition;
import game.Game;
import game.rules.phase.Phase;
import main.Constants;
import main.StringRoutines;
import main.options.GameOptions;
import org.json.JSONArray;
import org.json.JSONObject;
import other.context.Context;

import java.util.*;

//-----------------------------------------------------------------------------

/**
 * The main player object.
 *
 * @author Matthew.Stephenson and cambolbro and Eric.Piette
 */
public class ServerApp extends App
{
    /** {playerNumber: [id, port, successindicator], ...} successindicator 0 if not, 1 if yes*/
    private Map<Integer, Integer[]> players = new Hashtable<>();
    private int playerNumberCounter = 1;
	//-------------------------------------------------------------------------

	/**
	 * Constructor.
	 */
	public ServerApp(int port, LinkedList<Integer> otherPorts)
	{
		super(Instance.SERVER, port, otherPorts);
        this.appName = "Ludii Server";
	}
    public ServerApp(boolean multipleWindows){
        super(Instance.SERVER, multipleWindows);
        this.appName = "Ludii Server";
    }
	public ServerApp(){
        super(Instance.SERVER);
        this.appName = "Ludii Server";
    };


	//-------------------------------------------------------------------------

	/**
	 * Gets the full frame title for displaying at the top of the application.
	 */
	public String getFrameTitle(final Context context)
	{
        String frameTitle = super.getFrameTitle(context);
        frameTitle += " - Port " + this.getPort();
        return frameTitle;
	}

    // Bourryto - later: add checks and what to do when already existing, for now this is fine
    public void addPlayer(int number, int id, int port){
        this.players.put(number, new Integer[]{id, port, 0});
    }
    // Bourryto - later: add checks and what to do when already existing, for now this is fine
    public void addPlayer(int id, int port){
        // Bourryto - todo: add check if the game needs this amount of players
        while(this.players.get(playerNumberCounter) != null) {
            this.playerNumberCounter++;
        }
        Integer number = playerNumberCounter++;
        this.players.put(number, new Integer[]{id, port, 0});
        JSONObject message = new JSONObject();
        message.put("port", port);
        message.put("command", "do");
        JSONObject messageOptions = new JSONObject();
        messageOptions.put("command", "set_player");
        messageOptions.put("option", number);
        message.put("option", messageOptions);
        getCommunicationManager().sendMessage(message, new Address(port));
        new Thread(new Runnable(){
            @Override
            public void run(){
                while(players().get(number)[2] == 0){
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    getCommunicationManager().sendMessage(new JSONObject().put("command", "do").put("options", new JSONArray().put("set_player").put(number)), new Address(port));
                }
            }
        }).start();
    }

    public Map<Integer, Integer[]> players() {
        return players;
    }

    public String getPlayersString(){
        StringBuilder sb = new StringBuilder();
        for (Integer key: players.keySet()){
            sb.append(key + ": " + Arrays.toString(players.get(key)));
        }
        return sb.toString();
    }

    public void confirmPlayerIndex(int index){
        players.get(index)[2] = 1;
    }
}

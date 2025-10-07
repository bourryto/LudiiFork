package manager.network.local;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.stream.Collectors;

import game.Game;
import game.equipment.container.Container;
import game.equipment.container.board.Track;
import game.equipment.container.other.Deck;
import game.rules.play.moves.Moves;
import manager.Manager;
import other.context.Context;
import other.translation.LanguageUtils;

/**
 * Local network functions that can be called by external agents using sockets.
 * Messages are formatted as "XXXX ACTION EXTRA", where XXXX is the port number to return messages to, ACTION is the keyword for the desired task (see below), and EXTRA is any additional information.
 * Example messages include:
 * "5555 move 4" 	(make the 4th legal move)
 * "5555 legal" 	(return all legal moves)
 * "5555 player"	(return the current mover)
 * 
 * @author Matthew.Stephenson
 */
public class LocalFunctions 
{
	static ServerSocket serverSocket;
	static Socket socket;
	
	//-------------------------------------------------------------------------
	
	/**
	 * Initialise the server socket and await messages.
	 */
	public static void initialiseServerSocket(final Manager manager, final int port)
	{
		new Thread(new Runnable() 
		{
			@Override
			public void run() 
		    {
				try
				{  
					serverSocket = new ServerSocket(port);  
					
					while (true)
					{
						// Establish connection. 
						socket = serverSocket.accept();
						System.out.println("Client accepted");	// FOR-TESTING
						final DataInputStream dis = new DataInputStream(socket.getInputStream());  
						
						// Print any messages from socket.
						final String message = dis.readUTF();
						System.out.println("message= " + message); 
						
						// Reply string to respond to incoming messages.
						String reply = "";

						if (message.length() >= 9 && message.substring(5,9).equals("ping")){
							reply = "pong";
							manager.getPlayerInterface().addTextToStatusPanel(String.format("Communication with Port %s: message= %s | reply= %s", message.substring(0,4), message.substring(5,9), reply));
							initialiseClientSocket(Integer.parseInt(message.substring(0,4)), reply);
						}
						// Request about a move made.
						else if (message.length() >= 9 && message.substring(5, 9).equals("move"))
						{
							reply = "move failure";
							if (message.length() > 9) {
								final Context context = manager.ref().context();
								final Moves legal = context.game().moves(context);
								for (int i = 0; i < legal.moves().size(); i++) {
									if (i == Integer.parseInt(message.substring(10).trim())) {
										manager.ref().applyHumanMoveToGame(manager, context.game().moves(context).moves().get(i));
										reply = "move success";
									}
								}
							}
							initialiseClientSocket(Integer.parseInt(message.substring(0,4)), reply);
						}
						// Request about legal moves.
						else if (message.length() >= 10 && message.substring(5, 10).equals("legal"))
						{
							final Context context = manager.ref().context();
							final Moves legal = context.game().moves(context);
							for (int i = 0; i < legal.moves().size(); i++)
								reply += i + " - " + legal.moves().get(i).getActionsWithConsequences(context) + "\n";
							initialiseClientSocket(Integer.parseInt(message.substring(0,4)), "legal\n" + reply);
						}
						// Request about current mover.
						else if (message.length() >= 11 && message.substring(5, 11).equals("player"))
						{
							reply = Integer.toString(manager.ref().context().state().mover());
							initialiseClientSocket(Integer.parseInt(message.substring(0,4)), "player " + reply);
						}
						// ME: creating more input options
						// ME-TODO
						else if (message.length() >= 9 && message.substring(5, 9).equals("info"))
						{
							reply = getInfo(message, manager);
							initialiseClientSocket(Integer.parseInt(message.substring(0,4)), reply);
						}
						System.out.println("Reply= " + reply);
					}
				}
				catch(final Exception e)
				{
					e.printStackTrace();
					try 
					{
						serverSocket.close();
						socket.close();
					} 
					catch (final IOException e1) 
					{
						e1.printStackTrace();
					}  
				} 
		    }
		}).start();
	}

	// Me - start
	private static String respondToSending(String message, final Manager manager){
		// message = "<portnumber with length 4> sending EXTRA ..."
		// where EXTRA is the keyword I want to react to

		String[] args = message.split(" ");
		//int port = Integer.parseInt(args[0]);
		//String command = args[1]; // should be 'sending'
		String reply;

		if (args.length <= 2){return "No command after 'sending' was received.";}
		switch (args[2]){
			case "game_name":
				if(args.length <= 3){reply = "No Game_Name was received.";}
				String game_name = args[3];
				reply = "Game_Name '" + game_name +"' was received.";
				// Format of the game name is /lud/subfolder/subsubfolder/.../gamename.lud
				// ME TODO: GameLoading has a function loadGameFromName(), which does all the work of finding the game path from the game name
				//			BUT It requires the Desktop app as an argument, and i cant find a way to load a game using only the manager.
				//			Passing the App creates loop issues, so preferable a better way, i am considering a shared queue?
				//				Combined with a listener thread in the Desktop app  class after iniitializing the socket
				//manager.setSavedLudName(game_name);
				//manager.ref().setGame(manager, );
				break;
			default:
				reply = "unsupported command";
				break;
		}
		return reply;
	}

	private static String getInfo(String message, final Manager manager){
		// message = "<portnumber with length 4> info EXTRA"
		// where EXTRA is the info I want
		String extra = message.substring(10).trim();
		String reply = "unknown";
		switch (extra){
			case "game_name":
				reply = manager.ref().context().game().name();
				break;
			case "game_players":
				reply = manager.ref().context().game().players().toEnglish(manager.ref().context().game());
				reply = LanguageUtils.NumberAsText(manager.ref().context().game().players().count(), "player", "players") + (reply.isEmpty() ? "" : ": (" + reply + ")");
				break;
			case "game_rules":
				reply = manager.ref().context().game().rules().toEnglish(manager.ref().context().game());
				break;
			case "game_description_raw":
				reply = manager.ref().context().game().description().raw();
				break;
			case "game_description_expanded":
				reply = manager.ref().context().game().description().expanded();
				break;
			case "game":
				Game game = manager.ref().context().game();
				reply = game.toEnglish(game);
				reply += "\nMode:\n\t" + game.mode().mode() +
						"\nEquipment:\n\t"+ game.equipment().toEnglish(game) +
						"\nMetaRules:\n\t"+ game.metaRules();
				break;
			case "have_started":
				if(manager.ref().context().haveStarted()){
					reply = "started";
				}
				else {
					reply = "not started";
				}
				break;
			case "game_restart":
				manager.getPlayerInterface().restartGame();
				reply = "hopefully restarted";
				break;
			case "addTextToStatusPanel":
				manager.getPlayerInterface().addTextToStatusPanel("new text");
				reply = "added";
				break;
			case "setTemporaryMessage":
				manager.getPlayerInterface().setTemporaryMessage("temporary test message");
				break;
			case "board":	// ME-TODO get better board rep, with actual board descrition of current status
				//reply = manager.ref().context().board().toEnglish(manager.ref().context().game());
				Context context = manager.ref().context();
				/*
				reply = "Game Flags: " + context.game().gameFlags();
				if(context.game().isBoardless()){
					reply += "\nGame is Boardless!";
					break;
				}
				if(context.isGraphGame()){
					reply += "\nGraphGame:\n" + context.topology().graph().toString() + "\n\n---\n\nTopology:\n" + context.topology().toString();
				}
				if(context.game().isDeductionPuzzle()){
					reply +="\nGame is a Deduction Puzzle";
				}
				if(context.game().hasCard()){
					reply += "\n[" + context.game().handDeck().stream().map(Deck::toString).collect(Collectors.joining(",")) + "]";
				}
				if(context.game().usesLineOfPlay()){
					reply += "Game uses Line of Play";
				}
				if(context.game().hasTrack()){
					reply += "\n[" + context.game().board().tracks().stream().map(Track::toString).collect(Collectors.joining(",")) + "]";
				}
				reply += "\n" + context.game().board();
				*/
				reply += "\n\n" + context.getBoardRep();
				// TODO ME: add representation where pieces are
				break;
			case "state":
				reply = manager.ref().context().state().toString();
				break;
				/* example
				mvr=1, nxt=2, prv=0.
				[ContainerState type = class other.state.container.ContainerFlatState
				Empty = {chunk 5 = 1, chunk 6 = 1, chunk 7 = 1, chunk 8 = 1, chunk 9 = 1, chunk 11 = 1, chunk 12 = 1, chunk 13 = 1, chunk 15 = 1, chunk 16 = 1, chunk 17 = 1, chunk 18 = 1, chunk 19 = 1}
				Who = {chunk 0 = 1, chunk 1 = 2, chunk 2 = 1, chunk 3 = 2, chunk 4 = 1, chunk 10 = 1, chunk 14 = 2, chunk 20 = 2, chunk 21 = 1, chunk 22 = 2, chunk 23 = 1, chunk 24 = 2}
				]
				 */
			case "equipment":
				reply = manager.ref().context().game().equipment().toEnglish(manager.ref().context().game());
				break;
				/* example
				on a 5x5 rectangle board with square tiling.
				All players play with Queens.
				Rules for Pieces:
					 Queens slide from the location of the piece in the adjacent direction through [between] is in the set of empty cells.
				 */
			case "container":
				reply = "[";
				for(Container container: manager.ref().context().game().equipment().containers()){
					reply += container.toEnglish(manager.ref().context().game()) +
							"\n\tTopology: " + container.topology().graph().toString() +
							"\n\tnumSites: " + container.numSites() +
							"\n\tStyle: " + container.style().name() +
							"\n\tlabel: " + container.name() +
							"\n\tindex: " + container.index() +
							"\n\trole: " + container.role().toString();

				}
				reply += "]\n";
				break;
				/* example
				[5x5 rectangle board with square tiling,
				]
				 */
			default:
				reply = "unsupported command";
				break;
		}
		return reply;
	}


	// Me - end
	
	//-------------------------------------------------------------------------
	
	/**
	 * Initialise the client socket when a message needs to be sent.
	 */
	public static void initialiseClientSocket(final int port, final String Message)
	{
		new Thread(new Runnable() 
		{
			@Override
			public void run() 
		    {
				try (final Socket clientSocket = new Socket("localhost",port))
				{  
					try(final DataOutputStream dout = new DataOutputStream(clientSocket.getOutputStream()))
					{
						dout.writeUTF(Message);  
						dout.flush();  
						dout.close();  
						clientSocket.close();  
					}
					catch(final Exception e)
					{
						e.printStackTrace();
					}  	 
				}
				catch(final Exception e)
				{
					e.printStackTrace();
				}  	 
			}
		}).start();
	}
	
	//-------------------------------------------------------------------------
	
}

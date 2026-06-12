package manager.network.local;

import game.Game;
import game.equipment.container.Container;
import manager.Manager;

import java.awt.List;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.stream.Collectors;

public class CommunicationManager {
    Mailbox mailbox;
    Thread mailboxThread;
    Dictionary<Integer, Pen> penDictionary;
    LinkedList<Pen> penList;
    Ear ear;
    Thread earThread;
    int port;
    LinkedList<Integer> otherPorts;
    Manager manager;


    public CommunicationManager(int port, LinkedList<Integer> otherPorts, Manager manager){
        this.port = port;
        this.otherPorts = otherPorts;
        this.penDictionary = new Hashtable<>();
        this.penList = new LinkedList<>();
        this.manager = manager;
        mailbox = new Mailbox(port, this);
        mailboxThread = new Thread(mailbox);
        mailboxThread.start();
        try {
            ear = new Ear(mailbox, port);
            earThread = new Thread(ear);
            earThread.start();
            for(Integer otherPort : otherPorts){
                this.addPen(new Pen(port, otherPort));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        print("now running");
    }

    public void sendMessage(String message, int recipientPort){
        if(!otherPorts.contains(recipientPort)){
            print("Trying to send a message to a port not recognized. Options are: Error in code, maybe lost connections, or some function not yet implemented." +
                    "this is for a reason, i currently only want to support ports i know are within my code, but maybe a function for later");
            // it this is whished:
            // Pen newPen = new Pen(this.port, recipientPort);
            // this.addPen(newPen);
            // if (!newPen.isConnected){
            //      print("Message Could not be delivered, no Pen connection to the Recipient Port");
            //      return;
            // }
        }
        this.penDictionary.get(recipientPort).sendMessage(message);

    }

    public void broadcastMessage(String message){
        for (Pen pen: penList){
            pen.sendMessage(message);
        }
    }

    public void print(String text){
        System.out.println("[" + port + "] " + text);
    }

    // BOURRYTO - LATER: create the new pen here and check that it doenst already exists
    private void addPen(Pen pen){
        this.penList.add(pen);
        this.penDictionary.put(pen.otherPort, pen);
    }

    private static class Pen implements Runnable{
        private Socket socket = null;
        private DataOutputStream out = null;
        private int port;
        private int otherPort;
        // Currently only works with one other port, if multiple other servers should be communicated with,
        // Make Pen not an object but a dict of pens in the communicationsManager, with the otherPort as the key, and the pen object as value,
        // when sendMessage(), pull the right pen from the dict, and make it send the message
        // if this is implemented, the other port can be passed in the constructor, and never changed

        public Pen(int port, int otherPort){
            this.port = port;
            this.otherPort = otherPort;
            try{
                connect();
            } catch (IOException e) {
                print("could not connect to server on construction, try again later");
            }
        }

        public void connect() throws IOException {
            if (this.out == null) {
                this.socket = new Socket("127.0.0.1", otherPort);
                this.out = new DataOutputStream(this.socket.getOutputStream());
                //print("connected to ear on port " + otherPort);
                sendMessage("connect");
            }
            else {
                sendMessage("connected");
            }

        }

        public synchronized void run(){
            while(true){

            }
        }

        public void sendMessage(String message) {
            if (out == null){
                print("sending Message '" + message + "' unsuccesfull, no connection to other port, trying to build this connection");
                try {
                    connect();
                } catch (IOException e) {print("could not connect while sending message, try to connect bevor sending message"); return;}
            }
            try  {
                out.writeUTF("" + this.port + " " + message);
                out.flush();
                //print(" send message '" + message + "' to port " + this.otherPort);
            } catch (final Exception e) {
                try {
                    out.close();
                    socket.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                e.printStackTrace();
            }
        }

        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }

        public boolean isConnected(){
            if(out == null) return false;
            return true;
        }
    }

    private class Ear implements Runnable {
        Mailbox mailbox;
        int port;
        ServerSocket serverSocket;
        LinkedList<Socket> sockets;
        LinkedList<DataInputStream> ins;

        public Ear(Mailbox mailbox, int port) throws IOException {
            this.port = port;
            this.mailbox = mailbox;
            try {
                this.serverSocket = new ServerSocket(port);
            }
            catch (IOException e) {
                System.out.println("Could not create Serversocket on port " + port +". Is another instance of this program running and using the same port numbers?");
                throw new RuntimeException(e);
            }
            this.sockets = new LinkedList<>();
            this.ins = new LinkedList<>();
        }
        public void run(){
            // make a thread that adds accepted serversockets to sockts
            // make a thread that listens to each ins seperatly
            while (true) {
                try {
                    Socket newSocket = serverSocket.accept();
                    this.sockets.add(newSocket);
                    //print("connection to a pen established");
                    DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
                    ins.add(newIn);

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            while (true) {
                                try {
                                    String message = newIn.readUTF();
                                    if (!message.isEmpty()) {
                                        mailbox.deliverMail(message);
                                    }
                                } catch (IOException e) {
                                    if (newIn == null){
                                        break;
                                    }
                                    print("Excemption while reading datainputstream: " + e.getMessage());
                                }
                            }
                        }
                    }).start();
                } catch (final Exception e) {
                    e.printStackTrace();
                    print("connection lost");
                    try {
                        serverSocket.close();
                        this.serverSocket = new ServerSocket(port);
                    } catch (final IOException e1) {
                        e1.printStackTrace();
                    }
                }

            }
        }
        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }
    }

    private class Mailbox implements Runnable{
        LinkedList<String> incoming = new LinkedList<>();
        int port;
        CommunicationManager communicationManager;

        public Mailbox(int port, CommunicationManager communicationManager){
            this.port = port;
            this.communicationManager = communicationManager;
        }

        public synchronized void deliverMail(String message){
            incoming.add(message);
            //print("added mail to inbox");
            notify();
        }

        public synchronized void run(){
            while (true){
                try {
                    if (incoming.isEmpty()) {
                        wait();
                    }
                    String message = incoming.poll();
                    new Postbote(message, communicationManager).start();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }
    }

    private class Postbote extends Thread{
        String message;
        CommunicationManager communicationManager;

        public Postbote(String message, CommunicationManager communicationManager){
            this.message = message;
            this.communicationManager = communicationManager;
        }

        @Override
        public void run() {
            // BOURRYTO - LATER: Maybe check if command is 'sending' bevor splitting, sending returns text with sooooo many spaces
            String[] messageComponents = message.split(" ");
            if (messageComponents.length == 0){return;}
            int senderPort = Integer.parseInt(messageComponents[0]);
            String content;
            if (messageComponents.length == 1){ content = "Empty Message";}
            else {content = message.substring(messageComponents[0].length()+1);}
            this.communicationManager.manager.getPlayerInterface().addIncomingMessage("from " + senderPort + ": " + content);
            //print("got message from " + senderPort + ": '" + content+ "'");

            switch (messageComponents[1].toLowerCase()){
                case "connect":
                    try {
                        communicationManager.penDictionary.get(senderPort).connect();
                    } catch (IOException e) {
                        print("could not connect to " + senderPort);
                    }
                    break;
                case "connected":
                    print("Bidirectional connected to " + senderPort);
                    break;
                case "ping":
                    communicationManager.sendMessage("pong 0", senderPort);
                    break;
                case "pong":
                    if (messageComponents.length >= 3 && Integer.parseInt(messageComponents[2]) < 4){
                        communicationManager.sendMessage("pong " + (Integer.parseInt(messageComponents[2]) + 1), senderPort);
                    }
                    break;
                case "get":
                    communicationManager.sendMessage(parseGet(messageComponents), senderPort);
                    break;
                case "sending":
                    // this is where we get infos we asked for
                    parseSending(messageComponents);
                    break;
                case "do":
                    parseDo(messageComponents);
                    break;
                case "help":
                    communicationManager.sendMessage(parseHelp(), senderPort);
            }
        }

        /* Supported Commands
        game
        board
        state
        equipment
        container
         */
        private String parseGet(String[] messageComponents){
            String reply = "";
            switch (messageComponents[2]){
                case "game":
                    Game game = communicationManager.manager.ref().context().game();
                    reply = game.toEnglish(game);
                    reply += "\nMode:\n\t" + game.mode().mode() +
                            "\nEquipment:\n\t"+ game.equipment().toEnglish(game) +
                            "\nMetaRules:\n\t"+ game.metaRules();
                    break;
                case "board":	// ME-TODO get better board rep, with actual board descrition of current status
                    reply = manager.ref().context().board().toEnglish(manager.ref().context().game());
                    //Context context = communicationManager.manager.ref().context();
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
                    reply += "\n\n" + context.getBoardRep();
                    // TODO ME: add representation where pieces are
				*/
                    break;
                case "state":
                    reply = communicationManager.manager.ref().context().state().toString();
                    break;
				/* example
				mvr=1, nxt=2, prv=0.
				[ContainerState type = class other.state.container.ContainerFlatState
				Empty = {chunk 5 = 1, chunk 6 = 1, chunk 7 = 1, chunk 8 = 1, chunk 9 = 1, chunk 11 = 1, chunk 12 = 1, chunk 13 = 1, chunk 15 = 1, chunk 16 = 1, chunk 17 = 1, chunk 18 = 1, chunk 19 = 1}
				Who = {chunk 0 = 1, chunk 1 = 2, chunk 2 = 1, chunk 3 = 2, chunk 4 = 1, chunk 10 = 1, chunk 14 = 2, chunk 20 = 2, chunk 21 = 1, chunk 22 = 2, chunk 23 = 1, chunk 24 = 2}
				]
				 */
                case "equipment":
                    reply = communicationManager.manager.ref().context().game().equipment().toEnglish(communicationManager.manager.ref().context().game());
                    break;
				/* example
				on a 5x5 rectangle board with square tiling.
				All players play with Queens.
				Rules for Pieces:
					 Queens slide from the location of the piece in the adjacent direction through [between] is in the set of empty cells.
				 */
                case "container":
                    reply = "[";
                    for(Container container: communicationManager.manager.ref().context().game().equipment().containers()){
                        reply += container.toEnglish(communicationManager.manager.ref().context().game()) +
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
            return "sending " + messageComponents[2].toLowerCase() + " " + reply;
        }
        /* Commands to suppport
        [ ] game
        [ ] board
        [ ] state
        [ ] equipment
        [ ] container
         */
        private void parseSending(String[] messageComponents){
            switch (messageComponents[2]){
                // handle replies to get-type messages
                // like loading a game, doing a move maybe?
                case "game":
                    break;
                case "board":
                    break;
                case "state":
                    break;
                case "equipment":
                    break;
                case "container":
                    break;
                default:
                    break;
            }
        }

        private void parseDo(String[] messageComponents) {
            String text;
            switch (messageComponents[2].toLowerCase()) {
                case "game_restart":
                    communicationManager.manager.getPlayerInterface().restartGame();
                    break;
                case "add_text_to_status_panel":
                    text = Arrays.stream(messageComponents).skip(3).collect(Collectors.joining(" "));
                    communicationManager.manager.getPlayerInterface().addTextToStatusPanel(messageComponents.length > 3 ? text : "add Text To Status Panel");
                    break;
                case "set_temporary_message":
                    text = Arrays.stream(messageComponents).skip(3).collect(Collectors.joining(" "));
                    communicationManager.manager.getPlayerInterface().setTemporaryMessage(messageComponents.length > 3 ? text : "temporary test message");
                    break;
//                case "load_game_from_name":
//                    // this assumes we all work with the same database, should for now always be true but maybe not forever depending on added features
//                    String gameName = messageComponents[3];
//                    Apps.getApp(communicationManager.manager).loadGameFromName(gameName, (java.util.List<String>) new List(), false);
            }
        }

        private String parseHelp(){
            return "Format of a message is as following: PORTNUMBER COMMAND SUBCOMMAND0 SUBCOMMAND1 SUBCOMMAND2 ...\n" +
                    "Possible Commands and their subcommands are:\n" +
                    "\n\tconnect" +
                    "\n\t-> recipiant tries to establish a pen connection to sender" +
                    "\n\tping" +
                    "\n\t-> simple ping pong game, will respond with pong 0" +
                    "\n\tpong ANY_NUMBER" +
                    "\n\t-> if the number is smaller than (currently) 4, will respont with pong ANY_NUMBER+1" +
                    "\n\tget" +
                    "\n\t\tgame" +
                    "\n\t\t-> replies with game description, Mode, equipemnt and meta rules" +
                    "\n\t\tboard" +
                    "\n\t\t-> board description, currently not board with how it looks, but just the type of board" +
                    "\n\t\tstate" +
                    "\n\t\t-> State of the container" +
                    "\n\t\tequipment" +
                    "\n\t\t-> eqipment of the game, but not the current game in its state, but the game as a concept" +
                    "\n\t\tcontainter" +
                    "\n\t\t-> container as string, topologie, sitenumber, style, label, index and role for each container" +
                    "\n\tsending SOME_INFO_YOU_REQUESTED" +
                    "\n\t-> currently not supported, this is where info i requested by a get command, should be used to do something\n\t   this can totatally have an insane number of subcommands, as its just text that is send" +
                    "\n\tdo" +
                    "\n\t\tgame_restart" +
                    "\n\t\t-> the current game of the recipiant will be restarted" +
                    "\n\t\tadd_text_to_status_panel SOME_TEXT" +
                    "\n\t\t-> SOME_TEXT will be displayed on the status panel of the recipiant" +
                    "\n\t\tset_temporary_message SOME_TEXT" +
                    "\n\t\t-> SOME_TEXT will appear as a temporay message under the board of the recipiant";
        }


        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }
    }
}
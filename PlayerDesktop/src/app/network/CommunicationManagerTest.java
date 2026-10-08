/**
 * Delete this later, this is a version of the commmunication manager
 * without connection to acutal game, it can run seperaly and
 * it sends only the shortest answerxs without actual real content
 *
 * but its good for testing on laptop
 *
 */















package app.network;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

import org.json.JSONObject;

/** JSON Message Format:
 *
 * { "from":    {   "ip": String ipAddress,
 *                  "port": Integer port
 *              },
 *   "to":      {   "ip": String ipAddress,
 *                  "port": Integer port
 *              },
 *   "timestamp":{  "standard": "UNIX",
 *                  "format": "milliseconds",
 *                  "time": Integer timestamp
 *               },
 *   "replyTo": {Message},
 *   "content": {   "command": String command,
 *                  "option": Option option
 *              }
 * }
 *
 * Option can be:
 * String option
 * or
 * {   "command": String command,
 *     "option": Option option
 * }
 */

public class CommunicationManagerTest {
    Mailbox mailbox;
    Thread mailboxThread;
    Postoffice postoffice;
    Thread postofficeThread;
    Dictionary<Integer, Pen> penDictionary;
    LinkedList<Pen> penList;
    Ear ear;
    Thread earThread;
    int port;
    LinkedList<Integer> otherPorts;
    Logger logger;


    // Bourryto TODO: add reaction to "disconnect" message
    public CommunicationManagerTest(int port, LinkedList<Integer> otherPorts){
        this.logger = Logger.getLogger("CommTest");
        this.logger.info("Logger started");
        this.port = port;
        this.otherPorts = otherPorts;
        this.penDictionary = new Hashtable<>();
        this.penList = new LinkedList<>();
        mailbox = new Mailbox(port, this);
        mailboxThread = new Thread(mailbox);
        mailboxThread.start();
        postoffice = new Postoffice(this);
        postofficeThread = new Thread(postoffice);
        postofficeThread.start();
        try {
            ear = new Ear(mailbox, port);
            earThread = new Thread(ear);
            earThread.start();
            for(Integer otherPort : otherPorts){
                Pen newPen = new Pen(port, otherPort, true);
                this.addPen(newPen);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        print("now running");
    }

    public void sendMessage(Request request){
        if(!otherPorts.contains(request.getTo().port)){
            print("Trying to send a message to a port not recognized: " + request.getTo() + ". Options are: Error in code, maybe lost connections, or some function not yet implemented." +
                    "this is for a reason, i currently only want to support ports i know are within my code, but maybe a function for later");
            System.out.println("other ports: " + otherPorts);
            // if it is desired:
            // Pen newPen = new Pen(this.port, recipientPort);
            // this.addPen(newPen);
            // if (!newPen.isConnected){
            //      print("Message Could not be delivered, no Pen connection to the Recipient Port");
            //      return;
            // }
            return;
        }
        this.penDictionary.get(request.getTo().port).sendMessage(request);
    }

    public void sendMessage(JSONObject content, Address recipient, JSONObject replyTo){
        sendMessage(new Request(content, recipient, new Address(this.port), replyTo));
    }

    public void sendMessage(JSONObject content, Address recipient){
        sendMessage(new Request(content, recipient, new Address(this.port), new JSONObject()));
    }

    public void broadcastMessage(Request request){
        print("broadcasting message: " + request);
        for (Pen pen: penList){
            pen.sendMessage(request);
        }
    }

    public void print(String text){
        System.out.println("[" + port + "] " + text);
    }

    // BOURRYTO - LATER: create the new pen here and check that it doenst already exists
    void addPen(Pen pen){
        this.penList.add(pen);
        this.penDictionary.put(pen.otherPort, pen);
        if (!otherPorts.contains(pen.otherPort)){otherPorts.add(pen.otherPort);}
    }

    static class Pen implements Runnable{
        private Socket socket = null;
        private DataOutputStream out = null;
        private int port;
        private int otherPort;
        // Currently only works with one other port, if multiple other servers should be communicated with,
        // Make Pen not an object but a dict of pens in the communicationsManager, with the otherPort as the key, and the pen object as value,
        // when sendMessage(), pull the right pen from the dict, and make it send the message
        // if this is implemented, the other port can be passed in the constructor, and never changed

        public Pen(int port, int otherPort, boolean shouldRequestConnectionBack){
            this.port = port;
            this.otherPort = otherPort;
            try{
                connect(shouldRequestConnectionBack);
            } catch (IOException e) {
                print("could not connect to server on construction, try again later");
            }
        }

        public void connect(boolean shouldRequestConnectionBack) throws IOException {
            if (this.out == null) {
                this.socket = new Socket("127.0.0.1", otherPort);
                this.out = new DataOutputStream(this.socket.getOutputStream());
                //print("connected to ear on port " + otherPort);
                if (shouldRequestConnectionBack) {
                    sendMessage("connect");
                }
                sendMessage("connected");
            }
            else {
                sendMessage("already_connected");
            }

        }

        // BOURRYTO - TODO: I think im doing the thread wrong, i should look over it
        public synchronized void run(){
            while(true){

            }
        }

        public void sendMessage(Request request) {
            if (out == null){
                print("sending Message '" + request + "' unsuccesfull, no connection to other port, trying to build this connection");
                try {
                    connect(false);
                } catch (IOException e) {print("could not connect while sending message, try to connect bevor sending message"); return;}
            }
            try  {
                out.writeUTF(request.toString());
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
        public void sendMessage(String message){
            JSONObject content = new JSONObject();
            content.put("command", message);
            content.put("option", "");
            sendMessage(new Request(content, new Address(this.otherPort), new Address(this.port), new JSONObject()));
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
        LinkedList<BufferedReader> buffIns;

        public Ear(Mailbox mailbox, int port) throws IOException {
            this.port = port;
            this.mailbox = mailbox;
            try {
                this.serverSocket = new ServerSocket(port);
                print("Created server socket on port " + port);
            }
            catch (IOException e) {
                print("Could not create Serversocket on port " + port +". Is another instance of this program running and using the same port numbers?");
                throw new RuntimeException(e);
            }
            this.sockets = new LinkedList<>();
            this.buffIns = new LinkedList<>();
        }
        public void run(){
            // make a thread that adds accepted serversockets to sockts
            // make a thread that listens to each ins seperatly
            while (true) {
                try {
                    Socket newSocket = serverSocket.accept();
                    this.sockets.add(newSocket);
                    print("connection to a pen established");
                    //DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
                    //ins.add(newIn);
                    BufferedReader newBuffIn = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));

                    buffIns.add(newBuffIn);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            System.out.println("Running Ear");
                            while (true) {
                                try {
                                    //String message = newIn.readUTF();
                                    String message = newBuffIn.readLine();
                                    if (message == null) {
                                        //System.out.println("Exeption: message is null");
                                        continue;
                                    } else {
                                        System.out.println("incoming message: " + message);
                                    }
                                    if (!message.isEmpty()) {
                                        mailbox.deliverMail(message);
                                    }
                                } catch (IOException e) {
                                    //if (newIn == null){
                                    // TODO: close unused buffins - not sure if ever? how do in know
                                    print("Excemption while reading BufferedReader: " + e.getMessage());
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

    private class Postoffice implements Runnable {
        CommunicationManagerTest communicationManager;
        LinkedList<Request> outgoingRequests;

        public Postoffice(CommunicationManagerTest communicationManager) {
            this.communicationManager = communicationManager;
            this.outgoingRequests = new LinkedList<>();
        }

        public void run() {
            Request request;
            while (true) {
                try {
                    request = this.outgoingRequests.pop();
                    if (request != null) {
                        //System.out.println("communicationmanager outgoing messages lsit size " + this.central.outgoingMessages.size());
                        if (request.getTo().ip.equals("255.255.255.255")){
                            this.communicationManager.broadcastMessage(request);
                        } else {
                            this.communicationManager.sendMessage(request);
                        }
                    }
                } catch (NoSuchElementException e) {

                }
            }
        }
    }

    private class Mailbox implements Runnable{
        LinkedList<String> incoming = new LinkedList<>();
        int port;
        CommunicationManagerTest communicationManager;

        public Mailbox(int port, CommunicationManagerTest communicationManager){
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
        Request request;
        CommunicationManagerTest communicationManager;
        final Integer maxPongs = 3;

        public Postbote(String message, CommunicationManagerTest communicationManager){
            this.request = new Request(message);
            this.communicationManager = communicationManager;
        }

        @Override
        public void run() {
            // How i would parse the option if neccessary:
            // (messageComplex.getJSONObject("content").get("option").getClass() == JSONObject.class)
            JSONObject content = this.request.getRequestLine();
            if (content.isEmpty()){return;}
            String command = content.getString("command");
            // BOURRYTO - LATER: Maybe check if command is 'sending' bevor splitting, sending returns text with sooooo many spaces

            print("got message from " + this.request.senderPort + ": '" + content.toString(2)+ "'");

            switch (command.toLowerCase()){
                case "connect":
                    try {
                        if(communicationManager.penDictionary.get(this.request.senderPort) == null){
                            communicationManager.addPen(new Pen(communicationManager.port, this.request.senderPort, false));
                        } else {
                            communicationManager.penDictionary.get(this.request.senderPort).connect(false);
                        }
                    } catch (IOException e) {
                        print("could not connect to " + this.request.senderPort);
                    }
                    break;
                case "connected":
                    //print("Bidirectional connected to " + this.message.getFrom());
                    break;
                case "ping":
                    JSONObject reply = new JSONObject();
                    reply.put("command", "pong");
                    reply.put("counter", 0);
                    communicationManager.sendMessage(reply, this.request.getFrom(), request.jsonObject);
                    break;
                case "pong":
                    Integer pongCounter = request.getRequestLine().getInt("counter");
                    if (pongCounter < maxPongs){
                        JSONObject pong = new JSONObject();
                        pong.put("command", "pong");
                        pong.put("counter", pongCounter + 1);
                        communicationManager.sendMessage(pong, this.request.getFrom(), request.jsonObject);
                    }
                    break;
                case "get":
                    communicationManager.sendMessage(parseGet(request), this.request.getFrom(), this.request.jsonObject);
                    break;
                case "sending":
                    // this is where we get infos we asked for
                    parseSending(request);
                    break;
                case "do":
                    parseDo(request);
                    break;
                case "did":
                    parseDid(request);
                    break;
                case "help":
                    JSONObject help = new JSONObject();
                    help.put("command", "sending");
                    help.put("option", parseHelp());
                    communicationManager.sendMessage(help, this.request.getFrom(), request.jsonObject);
            }
        }

        /* Supported Commands
        game
        board
        state
        equipment
        container
         */
        private JSONObject parseGet(Request request){
            String reply = "";
            String option = request.getRequestLine().getString("option");
            switch (option.toLowerCase()){
                case "game":
                    reply = "this is a game";
                    break;
                case "board":	// ME-TODO get better board rep, with actual board descrition of current status
                    reply = "this is the board";
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
                    reply = "this is the state";
                    break;
				/* example
				mvr=1, nxt=2, prv=0.
				[ContainerState type = class other.state.container.ContainerFlatState
				Empty = {chunk 5 = 1, chunk 6 = 1, chunk 7 = 1, chunk 8 = 1, chunk 9 = 1, chunk 11 = 1, chunk 12 = 1, chunk 13 = 1, chunk 15 = 1, chunk 16 = 1, chunk 17 = 1, chunk 18 = 1, chunk 19 = 1}
				Who = {chunk 0 = 1, chunk 1 = 2, chunk 2 = 1, chunk 3 = 2, chunk 4 = 1, chunk 10 = 1, chunk 14 = 2, chunk 20 = 2, chunk 21 = 1, chunk 22 = 2, chunk 23 = 1, chunk 24 = 2}
				]
				 */
                case "equipment":
                    reply = "this is the equipment";
                    break;
				/* example
				on a 5x5 rectangle board with square tiling.
				All players play with Queens.
				Rules for Pieces:
					 Queens slide from the location of the piece in the adjacent direction through [between] is in the set of empty cells.
				 */
                case "container":
                    reply = "[cointainers]";
                    break;
				/* example
				[5x5 rectangle board with square tiling,
				]
				 */
                case "legal":
                    reply = "this is the list of legal mmoves";
                    break;
                default:
                    reply = "unsupported command";
                    break;
            }
            JSONObject response = new JSONObject();
            response.put("command", "sending");
            response.put(option, reply);
            return response;
        }
        /* Commands to suppport
        [ ] game
        [ ] board
        [ ] state
        [ ] equipment
        [ ] container
         */
        private void parseSending(Request request){
            String option = request.getRequestLine().getString("option");
            switch (option.toLowerCase()){
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

        private void parseDo(Request request) {
            // BOURRYTO - TODO: add confirmation message for all messages
            String option = request.getRequestLine().getJSONObject("option").getString("command");
            String text = "";
            switch (option.toLowerCase()) {
                case "game_restart":
                    print("restarted game");
                    break;
                case "add_text_to_status_panel":
                    text = request.getRequestLine().getJSONObject("option").getString("option");
                    print("added following text to status panel: " + text);
                    break;
                case "set_temporary_message":
                    text = request.getRequestLine().getJSONObject("option").getString("option");
                    print("set temporary Message: " + text);
                    break;
                case "load_game_from_name":
                    // BOURRYTO - TODO: works and loads, but message isnt added to message panel
                    // this assumes we all work with the same database, should for now always be true but maybe not forever depending on added features
                    String gameName = request.getRequestLine().getJSONObject("option").getString("option");
                    // BOURRYTO - LATER: add game options support
                    // currently not supporting gameoptions, we pass an empty list. this could be solved by sending game options along with the name
                    print("Loaded following game: " + gameName);
                    break;
                // BOURRYTO - TODO : ISSUES WITH CAPTURING MOVES NOT BEING ABLE TO APPLY
                case "move":
                    String move = request.getRequestLine().getJSONObject("option").getString("option");
                    print("did move: " + move);
                    break;
                case "set_player":
                    int playerIndex = request.getRequestLine().getJSONObject("option").getInt("option");
                    print("set player to following index: " + playerIndex);
                    break;
            }
            JSONObject reply = new JSONObject();
            reply.put("command", "did");
            reply.put("option", request.getRequestLine().getJSONObject("option"));
            sendMessage(reply, request.getFrom(), request.jsonObject);
        }

        private void parseDid(Request request){
            switch (request.getRequestLine().getString("option").toLowerCase()) {
                case "set_player":
                    print("did set player");
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
                    "\n\t\t-> SOME_TEXT will appear as a temporay message under the board of the recipiant" +
                    "\n\t\tmove SOME_MOVE" +
                    "\n\t\t-> the Recipiant will check if SOME_MOVE is a legal move, and if yes, apply that move";
        }


        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }
    }

    public static void main(String[] args) {
        CommunicationManagerTest cmt = new CommunicationManagerTest(4444, new LinkedList<>());
    }
}
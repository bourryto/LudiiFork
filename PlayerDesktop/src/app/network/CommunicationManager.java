package app.network;

import app.Apps;
import app.PlayerApp;
//import central.Central;
import game.Game;
import game.equipment.container.Container;
import game.rules.play.moves.Moves;
import main.collections.FastArrayList;
import manager.Manager;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import other.action.Action;
import other.action.ActionType;
import other.context.Context;
import other.move.Move;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.logging.Logger;

/*
* Manages all local Network Communication
*
 */

public class CommunicationManager {
    //public Central central;
    Mailbox mailbox;
    Thread mailboxThread;
    Postoffice postoffice;
    Thread postofficeThread;
    Dictionary<Integer, Pen> penDictionary;
    LinkedList<Pen> penList;
    Dictionary<Integer, Pen> notifyPens = new Hashtable<>();
    Ear ear;
    Thread earThread;
    int port;
    LinkedList<Integer> otherPorts;
    Manager manager; // Manager connects us to all functions and data in the Application
    Logger logger;
    Dictionary<Integer, Integer> portPlayerMapper = new Hashtable<>();
    Dictionary<Integer, Integer> playerPortMapper = new Hashtable<>();
    final static String adminPassword = "inject";

    public static CommunicationManager getFromCountingUpPorts(int port, Manager manager) {
        LinkedList<Integer> otherPorts = new LinkedList<>();
        for (int i = PlayerApp.firstPortNumber; i < port; i++) {
            otherPorts.add(i);
        }
        return new CommunicationManager(port, otherPorts, manager);
    }

    public CommunicationManager(int port, LinkedList<Integer> otherPorts, Manager manager){
        this.logger = Logger.getLogger("CommTest");
        this.logger.info("Logger started");
        this.port = port;
        this.otherPorts = otherPorts;
        this.penDictionary = new Hashtable<>();
        this.penList = new LinkedList<>();
        this.manager = manager;
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
                Pen newPen = new Pen(port, otherPort);
                newPen.connect(true);
                this.addPen(newPen);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }


        print("now running");
    }

    // Converts request to message and sends it according to the desired port. Assures connection exists
    public void sendMessage(Request request){
        if(!otherPorts.contains(request.getTo().port)){
            print("Trying to send a message to a port not recognized: " + request.getTo() + ". Options are: Error in code, maybe lost connections, or some function not yet implemented." +
                    "this is for a reason, i currently only want to support ports i know are within my code, but maybe a function for later");
            System.out.println("other ports: " + otherPorts);
            Pen newPen = new Pen(this.port, request.getTo().port);
            try {
                newPen.connect(false);
            } catch (IOException e) {
                System.out.println("Couldn't connect new Pen to other Server");
            }
            this.addPen(newPen);
            sendMessage(request);
            // it this is whished:
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

    public void sendMessage(JSONObject request_line, Address recipient, JSONObject replyTo){
        sendMessage(new Request(request_line, recipient, new Address(this.port), replyTo));
    }
    public void sendMessage(JSONObject request_line, Address recipient){
        sendMessage(new Request(request_line, recipient, new Address(this.port), new JSONObject()));
    }

    public void broadcastMessage(String message){
        broadcastMessage(new Request(message));
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

    // adding new communication partner
    void addPen(Pen pen){
        this.penList.add(pen);
        this.penDictionary.put(pen.otherPort, pen);
        if (!otherPorts.contains(pen.otherPort)){otherPorts.add(pen.otherPort);}
        Apps.getApp(manager).addOtherPort(pen.otherPort);
    }

    // mappes ports to the players they want to controll
    void addPlayer(int port, int playerIndex) {
        this.portPlayerMapper.put(port, playerIndex);
        this.playerPortMapper.put(playerIndex, port);
    }
    void resetPlayers(){
        this.playerPortMapper = new Hashtable<>();
        this.portPlayerMapper = new Hashtable<>();
    }

    public LinkedList<Integer> getOtherPorts(){return this.otherPorts;}

    public Manager manager(){return manager;}

    // if set, ports will be notified when its their players turn
    public void setNotify(int mover, int port){
        Pen pen = penDictionary.get(port);
        pen.notifyOnTurn = true;
        pen.notifyMover = mover;
        notifyPens.put(mover, pen);
    }

    // sends messages
    static class Pen implements Runnable{
        private Socket socket = null;
        private DataOutputStream out = null;
        private int port;
        private int otherPort;
        private boolean notifyOnTurn = false;
        private int notifyMover;
        // Currently only works with one other port, if multiple other servers should be communicated with,
        // Make Pen not an object but a dict of pens in the communicationsManager, with the otherPort as the key, and the pen object as value,
        // when sendMessage(), pull the right pen from the dict, and make it send the message
        // if this is implemented, the other port can be passed in the constructor, and never changed

        public Pen(int port, int otherPort){
            this.port = port;
            this.otherPort = otherPort;
            /*
            try{
                connect(shouldRequestConnectionBack);
            } catch (IOException e) {
                print("could not connect to server on construction, try again later");
            }

             */
        }
        public void connect(boolean shouldRequestConnectionBack)throws IOException{connect(shouldRequestConnectionBack, new JSONObject());}
        public void connect(boolean shouldRequestConnectionBack, JSONObject replyTo) throws IOException {
            // TODO BOURRYTO - what does keep alive means? if python restarts this should find a way
            if (this.socket != null) {
                System.out.println("Out: " + this.out +
                        "\nSocket: " + this.socket +
                        "\nIs Socket Closed: " + this.socket.isClosed() +
                        "\nIs Socket Connected: " + this.socket.isConnected() +
                        "\nSocket Out: " + this.socket.getOutputStream()
                );
            }
            if (this.out == null || this.socket == null || this.socket.isClosed() || !this.socket.isConnected()) {
                print("something is wrong (it could be that this is the first connection, in that case everything is fine)");
                this.socket = new Socket("127.0.0.1", otherPort);
                this.out = new DataOutputStream(this.socket.getOutputStream());
                print("connected to ear on port " + otherPort);
                if (shouldRequestConnectionBack) {
                    JSONObject request_line = new JSONObject();
                    request_line.put("method", "connect");
                    request_line.put("success", true);
                    request_line.put("request_target", "");
                    sendMessage(new Request(request_line, new Address(this.otherPort), new Address(this.port), replyTo));
                }
                else {
                    sendMessage("connected", replyTo);
                    JSONObject request_line = new JSONObject();
                    request_line.put("method", "connected");
                    request_line.put("success", true);
                    request_line.put("request_target", "");
                    sendMessage(new Request(request_line, new Address(this.otherPort), new Address(this.port), replyTo));
                }
            }
            else {
                this.socket = new Socket("127.0.0.1", otherPort);
                this.out = new DataOutputStream(this.socket.getOutputStream());
                print("already connected");
                JSONObject content = new JSONObject();
                content.put("method", "already_connected");
                content.put("success", true);
                content.put("request_target", "");
                content.put("error_message", "");
                sendMessage(new Request(content, new Address(this.otherPort), new Address(this.port), replyTo));
            }

        }
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
            if (this.socket.isClosed()){
                print("The Socket closed, but im trying to revivify it");
                try {
                    this.socket = new Socket("127.0.0.1", otherPort);
                    this.out = new DataOutputStream(this.socket.getOutputStream());
                } catch (Exception e){
                    System.out.println("Socket closed, could not connect to socket");
                    return;
                }
            }

            try  {
                out.writeUTF(request.toString());
                out.flush();
                print(" send message '" + request.getRequestLine().toString()+ "' full: '"+ request + "' to port " + this.otherPort);
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
        public void sendMessage(String message, JSONObject replyTO){
            JSONObject request_line = new JSONObject();
            request_line.put("method", message);
            request_line.put("request_target", "");
            sendMessage(new Request(request_line, new Address(this.otherPort), new Address(this.port), replyTO));
        }
        public void sendMessage(String message){
            sendMessage(message, new JSONObject());
        }

        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }

        public boolean isConnected(){
            if(out == null || socket.isClosed() || !socket.isConnected()) return false;
            return true;
        }

        public void notifyOnMove(){
            this.sendMessage("notify");
        }
    }

    // listens for messages
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
            this.ins = new LinkedList<>();
            this.buffIns = new LinkedList<>();
        }
        public void run(){
            // make a thread that adds accepted serversockets to sockts
            // make a thread that listens to each ins seperatly
            while (true) {
                try {
                    Socket newSocket = serverSocket.accept();
                    this.sockets.add(newSocket);
                    print("Ear established connection");
                    //DataInputStream newIn = new DataInputStream(newSocket.getInputStream());
                    //ins.add(newIn);
                    BufferedReader newBuffIn = new BufferedReader(new InputStreamReader(newSocket.getInputStream()));

                    buffIns.add(newBuffIn);
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            print("Running Ear");
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

    // if undelivered messages, sends the messages
    private class Postoffice implements Runnable {
        CommunicationManager communicationManager;
        //Central central;
        LinkedList<Request> outgoingRequests;

        public Postoffice(CommunicationManager communicationManager) {
            this.communicationManager = communicationManager;
            //this.central = communicationManager.central();
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

    // Is a thread listening to new incoming messages. Starts new thread to handle these messages per message
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

    // handles an incoming message, calls the correct functions per case
    private class Postbote extends Thread{
        Request request;
        CommunicationManager communicationManager;
        final Integer maxPongs = 3;

        public Postbote(String message, CommunicationManager communicationManager){
            this.request = new Request(message);
            this.communicationManager = communicationManager;
        }

        @Override
        public void run() {
            // How i would parse the option if neccessary:
            // (messageComplex.getJSONObject("content").get("option").getClass() == JSONObject.class)
            JSONObject content = this.request.getRequestLine();
            this.communicationManager.manager.getPlayerInterface().addIncomingMessage("from " + this.request.senderPort + ": " + content);
            if (content.isEmpty()) {
                JSONObject response = new JSONObject();
                response.put("method", "replying");
                response.put("request_target", "error");
                // todo - better error handling!
                communicationManager.sendMessage(parseGet(request), this.request.getFrom(), this.request.jsonObject);
                return;
            }
            String method = content.getString("method");
            // BOURRYTO - LATER: Maybe check if method is 'sending' bevor splitting, sending returns text with sooooo many spaces

            print("got message from " + this.request.senderPort + ": '" + content.toString(2)+ "'");

            switch (method.toLowerCase()){
                case "connect":
                    try {
                        if(communicationManager.penDictionary.get(this.request.senderPort) == null){
                            System.out.println("creating new pen");
                            communicationManager.addPen(new Pen(communicationManager.port, this.request.senderPort));
                        }
                        System.out.println("connecting to pen");
                        communicationManager.penDictionary.get(this.request.senderPort).connect(false, request.jsonObject);
                    } catch (IOException e) {
                        print("could not connect to " + this.request.senderPort);
                    }
                    break;
                case "connected":
                    //print("Bidirectional connected to " + senderPort);
                    break;
                case "ping":
                    JSONObject reply = new JSONObject();
                    reply.put("method", "pong");
                    reply.put("request_target", "0"); // todo - change this, request target should not be int
                    reply.put("success", true);
                    reply.put("error_message", "");
                    communicationManager.sendMessage(reply, this.request.getFrom(), request.jsonObject);
                    break;
                    // TODO - change counter to request_target, but i need to do that on the interface too, and i dont want to open it rn
                case "pong":
                    int pongCounter = -1;
                    try {
                        pongCounter = Integer.parseInt(request.getRequestLine().getString("request_target"));
                    } catch(JSONException e){
                        try{
                            pongCounter = request.getRequestLine().getInt("request_target");
                        } catch(JSONException f){
                            System.out.println("Pong Counter neither Parsable String nor integer");
                        }
                    }
                    if (pongCounter == -1){
                        JSONObject pong = new JSONObject();
                        pong.put("method", "pong");
                        pong.put("request_target", "");
                        pong.put("success", false);
                        pong.put("error_message", "Request Target was neither Integer nor String representation of Integer.");
                        communicationManager.sendMessage(pong, this.request.getFrom(), request.jsonObject);
                        break;
                    }
                    if (pongCounter < maxPongs){
                        JSONObject pong = new JSONObject();
                        pong.put("method", "pong");
                        pong.put("request_target", Integer.toString(pongCounter+1));
                        pong.put("success", true);
                        pong.put("error_message", "");
                        communicationManager.sendMessage(pong, this.request.getFrom(), request.jsonObject);
                    }
                    break;
                case "get":
                    communicationManager.sendMessage(parseGet(request), this.request.getFrom(), this.request.jsonObject);
                    break;
                case "do":
                    parseDo(request);
                    break;
                default:
                    reply = content;
                    reply.put("success", false);
                    reply.put("error_message", "method not supported");
                    communicationManager.sendMessage(reply, this.request.getFrom(), request.jsonObject);
                    break;
            }
        }
        // resolves get type requests
        private JSONObject parseGet(Request request){
            String request_target = request.getRequestLine().getString("request_target");
            JSONObject reply = new JSONObject();
            reply.put("success", true);
            reply.put("method", "sending");    // inverse to get
            reply.put("request_target", request_target);
            reply.put("error_message", "");
            final Context context = manager.ref().context();
            switch (request_target.toLowerCase()){
                case "game":
                    Game game = communicationManager.manager.ref().context().game();
                    reply.put("result", game.toJSON());
                    break;
                case "simple_board":
                    JSONObject simpleBoard = new JSONObject();
                    simpleBoard.put("graph", context.board().graph());
                    reply.put("result", simpleBoard);
                case "board":	// ME-TODO get better board rep, with actual board descrition of current status
                    reply.put("result", context.board().toJSON());
                    break;
                case "state":
                    reply.put("result", context.state().toJSON());
                    break;
                case "equipment":
                    reply.put("result", context.game().equipment().toJSON());
                    break;
                case "containers":
                    JSONArray c = new JSONArray();
                    System.out.println("container amount: "+ communicationManager.manager.ref().context().game().equipment().containers().length);
                    for (Container container: communicationManager.manager.ref().context().game().equipment().containers()){
                        c.put(container.toJSON());
                    }
                    reply.put("result", c);
                    break;
                case "legal_moves":
                    final Moves legal = context.game().moves(context);
                    JSONArray legalMoves = new JSONArray();
                    for (int i = 0; i < legal.moves().size(); i++) {
//                        legalMoves.put(legal.moves().get(i).getMoveWithConsequences(context).toJSON());
                        legalMoves.put(legal.moves().get(i).getMoveWithConsequences(context).toBourrytoFormat());
                    }
                    reply.put("result", legalMoves);
                    break;
                case "simple_legal_moves":
                    final Moves simple_legal = context.game().moves(context);
                    JSONArray simple_legalMoves = new JSONArray();
                    for (int i = 0; i < simple_legal.moves().size(); i++) {
                        simple_legalMoves.put(simple_legal.moves().get(i).getMoveWithConsequences(context).toBourrytoFormat());
                    }
                    reply.put("result", simple_legalMoves);
                    break;
                case "legal_moves_json":
                    final Moves legalJson = context.game().moves(context);
                    JSONArray legalMovesJSON = new JSONArray();
                    for (int i = 0; i < legalJson.moves().size(); i++) {
                        legalMovesJSON.put(legalJson.moves().get(i).getMoveWithConsequences(context).toJSON());
                    }
                    reply.put("result", legalMovesJSON);
                    break;
                case "mover":
                    reply.put("result", manager.ref().context().state().mover());
                    break;
                case "unset_players":
                    JSONArray unsetPlayers = new JSONArray();
                    int playerNumber = this.communicationManager.manager.ref().context().players().size();
                    for (int i = 1; i < playerNumber; i++) {
                        if (this.communicationManager.playerPortMapper.get(i) == null){
                            unsetPlayers.put(i);
                        }
                    }
                    reply.put("result", unsetPlayers);
                    break;
                case "trial_format":
                    reply.put("result", Move.getTrialFormat());
                    break;
                case "required_move_keys":
                    reply.put("result", Action.requiredComparisonKeys());
                    break;
                case "action_types":
                    reply.put("result", new JSONArray(ActionType.class.getEnumConstants()));
                    break;
                case "raw_game_config_file_content":
                    reply.put("result", manager().ref().context().game().description().raw());
                    break;
                case "winners":
                    reply.put("result", new JSONArray(manager().ref().context().winners().toArray()));
                    break;
                case "is_over":
                    reply.put("result", manager().ref().context().game().moves(manager().ref().context()).moves().isEmpty());
                    break;
                default:
                    reply.put("success", false);
                    reply.put("result", "");
                    reply.put("error_message", new JSONArray().put("unsupported method"));
                    //reply = "unsupported method";
                    break;
            }
            return reply;
        }

        // resolves executing requests
        private void parseDo(Request request) {
            System.out.println("doing (executing)");
            // BOURRYTO - TODO: add confirmation message for all messages
            String request_target = request.getRequestLine().getString("request_target");
            final Context context = manager.ref().context();
            String errorMessage = "";
            String text = "";
            boolean success = true;
            switch (request_target.toLowerCase()) {
                case "restart_game":
                    communicationManager.manager.getPlayerInterface().restartGame();
                    break;
                case "add_text_to_status_panel":
                    try {
                        text = request.getRequestLine().getString("text");
                    } catch (JSONException je){
                        success = false;
                        errorMessage = "'text' variable is missing from request.";
                        break;
                    }
                    communicationManager.manager.getPlayerInterface().addTextToStatusPanel("\n"+text);
                    break;
                case "set_temporary_message":
                    try {
                        text = request.getRequestLine().getString("text");
                    } catch (JSONException je){
                        success = false;
                        errorMessage = "'text' variable is missing from request.";
                        break;
                    }
                    communicationManager.manager.getPlayerInterface().setTemporaryMessage(text);
                    break;
                case "load_game_from_name":
                    // BOURRYTO - TODO: works and loads, but message isnt added to message panel
                    // this assumes we all work with the same database, should for now always be true but maybe not forever depending on added features
                    try {
                        text = request.getRequestLine().getString("name");
                    }  catch (JSONException je){
                        success = false;
                        errorMessage = "'name' variable is missing from request.";
                        break;
                    }
                    // BOURRYTO - LATER: add game options support
                    // currently not supporting gameoptions, we pass an empty list. this could be solved by sending game options along with the name
                    try {
                        Apps.getApp(communicationManager.manager).loadGameFromName(text, new ArrayList<>(), false);
                    } catch (IllegalArgumentException ie){
                        success = false;
                        errorMessage = "No Game with this name.";
                        break;
                    }
                    catch(Exception e){
                        communicationManager.logger.warning(e.getMessage());
                        success = false;
                        errorMessage = "Exception while trying to load the game. Error Message="+e.getMessage();
                        break;
                    }
                    if (!this.communicationManager.manager.ref().context().game().name().equalsIgnoreCase(text)) {
                        errorMessage = "Failed loading game "+text+".";
                        communicationManager.logger.warning(errorMessage);
                        success = false;
                        break;
                    }
                    communicationManager.resetPlayers();
                    communicationManager.manager.getPlayerInterface().addTextToStatusPanel("\nLoaded Game from name via request. Current Game: " + this.communicationManager.manager.ref().context().game().name());
                    break;
                // BOURRYTO - TODO : ISSUES WITH CAPTURING MOVES NOT BEING ABLE TO APPLY
                case "move_from_string":
                    // asserting its players turn
                    int senderPlayer;
                    int mover = context.state().mover();
                    /* not checking player index for now as it annoyes me that all is burning
                    if (!message.getContent().keySet().contains("password") || !message.getContent().get("password").equals(CommunicationManager.adminPassword)) {
                        try {
                            senderPlayer = communicationManager.portPlayerMapper.get(message.senderPort);
                        } catch (NullPointerException nullPointerException) {
                            errorMessage = ("No Player set for this PORT, set player number first and try again");
                            success = false;
                            break;
                        }
                        if (mover != senderPlayer) {  // mover is 1 for the user
                            errorMessage = ("It's not your turn. You are Player number " + senderPlayer + " and it's currently Player number " + mover + " 's Turn.");
                            success = false;
                            break;
                        }
                    }

                     */
                    Move foundMove = null;
                    String moveString = "";
                    try {
                        moveString = request.getRequestLine().getString("move").replace(" ", "");
                    } catch (JSONException je){
                        success = false;
                        errorMessage = "'move' variable is missing from request.";
                        break;
                    }
                    this.communicationManager.logger.info("got following move: " + moveString);
                    // currently never accepting index moves for simplicity
                    boolean acceptingIndexMoves = false;
                    if (acceptingIndexMoves) {
                        //System.out.println("Move is:" + move);
                        try {
                            int moveIndex = Integer.parseInt(moveString.replaceAll(" ", ""));
                            //System.out.println("is Number");
                            if (context.game().moves(context).moves().size() > moveIndex) {
                                foundMove = context.game().moves(context).moves().get(Integer.parseInt(moveString));
                            }
                            else {
                                errorMessage =("Move index out of bounds for this state");
                                success = false;
                                break;
                            }
                        } catch (NumberFormatException e) {
                            System.out.println(e.getMessage());
                            errorMessage ="Move index number must be a positive integer";
                            success = false;
                            break;
                        }
                    }
                    if (foundMove == null) {
                        // from PlayerDesktop.app.menu.MainMenuFunctions under remote > select move from string:
                        final FastArrayList<Move> substringMatchingMoves = new FastArrayList<>();
                        for (final Move m : context.game().moves(context).moves()) {
                            // Check for exact match first
                            this.communicationManager.logger.info("possible move formats:\n\t"+
                                    m.toTrialFormat(context) + "\n\t" +
                                    m.toTurnFormat(context, true) + "\n\t" +
                                    m.toTurnFormat(context, false) + "\n\t" +
                                    m.toMoveFormat(context, true) + "\n\t" +
                                    m.toMoveFormat(context, false) + "\n\t" +
                                    m.toString().equals(moveString));
                            if (m.toTrialFormat(context).equals(moveString)
                                    || m.toTurnFormat(context, true).equals(moveString)
                                    || m.toTurnFormat(context, false).equals(moveString)
                                    || m.toMoveFormat(context, true).equals(moveString)
                                    || m.toMoveFormat(context, false).equals(moveString)
                                    || m.toString().equals(moveString)) {
                                foundMove = m;
                            } else {
                                // Check for substring match
                                if (m.toTrialFormat(context).contains(moveString))
                                    substringMatchingMoves.add(m);
                                else if (m.toTurnFormat(context, true).contains(moveString))
                                    substringMatchingMoves.add(m);
                                else if (m.toTurnFormat(context, false).contains(moveString))
                                    substringMatchingMoves.add(m);
                                else if (m.toMoveFormat(context, true).contains(moveString))
                                    substringMatchingMoves.add(m);
                                else if (m.toMoveFormat(context, false).contains(moveString))
                                    substringMatchingMoves.add(m);
                                else if (m.toString().contains(moveString))
                                    substringMatchingMoves.add(m);
                            }
                        }
                        if (foundMove == null) {
                            if (substringMatchingMoves.size() == 1) {
                                foundMove = substringMatchingMoves.get(0);
                            } else if (substringMatchingMoves.size() > 1) {
                                errorMessage = "Multiple Mathcing Moves. Clarify which move out of these moves: " + substringMatchingMoves;
                                success = false;
                                break;
                            } else {
                                errorMessage = "No matching move found.";
                                success = false;
                                break;
                            }
                        }
                    }
                    if (foundMove != null) {
                        manager().ref().applyNetworkMoveToGame(manager(), foundMove);
                        if (!notifyPens.isEmpty() && notifyPens.get(context.state().mover()) != null) {
                            notifyPens.get(context.state().mover()).notifyOnMove();
                        }
                    }
                    else {
                        errorMessage = "Unclear Error, Issues finding matching move.";
                        success = false;
                        break;
                    }
                    break;
                case "move":
                    // asserting its players turn
                    mover = context.state().mover();
                    // not checking player index for now as it annoyes me that all is burning
                    boolean isAdmin = false;
                    try{
                        isAdmin = request.getRequestLine().get("password").equals(CommunicationManager.adminPassword);
                    } catch (JSONException ignored) {}
                    if (!isAdmin) {
                        try {
                            senderPlayer = communicationManager.portPlayerMapper.get(request.senderPort);
                        } catch (NullPointerException nullPointerException) {
                            errorMessage = ("No Player set for this PORT, set player number first and try again");
                            success = false;
                            break;
                        }
                        if (mover != senderPlayer) {  // mover is 1 for the user
                            errorMessage = ("It's not your turn. You are Player number " + senderPlayer + " and it's currently Player number " + mover + " 's Turn.");
                            success = false;
                            break;
                        }
                    }
                    foundMove = null;
                    JSONObject incoming_move = null;
                    try {
                        incoming_move= request.getRequestLine().getJSONObject("move");
                    } catch (JSONException je){
                        success = false;
                        errorMessage = "'move' variable is missing from request.";
                        break;
                    }

                    if(!incoming_move.keySet().contains("what")){
                        errorMessage = "Move parameter is missing 'what' key.";
                        success = false;
                        break;
                    }if(!incoming_move.keySet().contains("to")){
                        errorMessage = "Move parameter is missing 'to' key.";
                        success = false;
                        break;
                    }if(!incoming_move.keySet().contains("from")){
                        errorMessage = "Move parameter is missing 'from' key.";
                        success = false;
                        break;
                    }if(!incoming_move.keySet().contains("actionType")){
                        errorMessage = "Move parameter is missing 'actionType' key.";
                        success = false;
                        break;
                    }

                    if(incoming_move.keySet().contains("what")){
                        // todo - this only applies to add move, need to change logic when moving to chess
                        if(incoming_move.isNull("what")){errorMessage = "what needs to be an integer Value!"; success = false;break;}
                        int what;
                        try {
                            what = incoming_move.getInt("what");
                        } catch (JSONException e){
                            errorMessage = "'what' needs to be an Integer."; success = false;break;
                        }
                        // later todo important - this only works for t3, chess will have more what, we need to differenciate later. this is only a temporary solution!
//                        if(what == 0){
//                            errorMessage = "what with the value 0 is reserved for the game, it can't be used by players.";
//                            success = false;
//                            break;
//                        }
//                        if(what != mover){
//                            errorMessage = "There are no legal moves with 'what'="+Integer.toString(what)+". Check if you have used the right value for 'what'.";
//                            success = false;
//                            break;
//                        }
                    }
                    this.communicationManager.logger.info("got following move: " + incoming_move.toString());
                    // if the move containes multiple actions (chess swith or something probably, this needs to be considered and changed
                    for (final Move m : context.game().moves(context).moves()) {
                        this.communicationManager.logger.info("found move: " + m.toString());
                        this.communicationManager.logger.info("move from:" + m.from() + " to:" + m.to()+" what:" + m.what()+" actionType:" + m.actionType().toString());
                        //this.communicationManager.logger.info("move from:" + incoming_move.getInt("from") + " to:" + incoming_move.getInt("to")+" what:" + incoming_move.getInt("what")+" actionType:" + incoming_move.getString("actionType"));
                        try {
                            if (m.sameEnough(incoming_move, true)) {
                                this.communicationManager.logger.info("found same move");
                                foundMove = m;
                                break;
                            }
                        } catch (Exception e) {
                            print("Error matching keys");
                            errorMessage = e.getMessage();
                            success = false;
                            break;
                        }
                    }
                    if (foundMove != null) {
                        manager().ref().applyNetworkMoveToGame(manager(), foundMove);
                        if (!notifyPens.isEmpty() && notifyPens.get(context.state().mover()) != null) {
                            notifyPens.get(context.state().mover()).notifyOnMove();
                        }
                    }
                    else {
                        errorMessage = "No Move matching your move.";
                        success = false;
                        break;
                    }
                    break;
                case "reset_players":
                    this.communicationManager.resetPlayers();
                    this.communicationManager.logger.info("players are resetted " +this.communicationManager.portPlayerMapper+" ,"+this.communicationManager.playerPortMapper);
                    communicationManager.manager.getPlayerInterface().addTextToStatusPanel("\nPlayers where reset. Mapper: " + this.communicationManager.playerPortMapper.toString());
                    break;
                case "set_player":

                    this.communicationManager.logger.info("setting_player with current players: " + this.communicationManager.portPlayerMapper.toString() + ", " + this.communicationManager.playerPortMapper.toString());
                    isAdmin = false;
                    try{
                        isAdmin = request.getRequestLine().get("password").equals(CommunicationManager.adminPassword);
                        this.communicationManager.logger.info("is admin: "+ isAdmin);
                    } catch (JSONException ignored) {}
                    int playerNumber;
                    try {
                        playerNumber = request.getRequestLine().getInt("number");
                    } catch (JSONException je){
                        errorMessage = "'number' variable is missing from request.";
                        success = false;
                        break;
                    }
                    if (playerNumber == 0){
                        success = false;
                        errorMessage =("Player Number 0 is reserved for the game. Try getting unset players, and choose one of those.");
                        break;
                    }
                    boolean adminSuccess = false;
                    if (isAdmin) {
                        try{
                            int port_to_set = request.getRequestLine().getInt("port");
                            this.communicationManager.logger.info("Got port: "+port_to_set);
                            Integer oldPlayerNumber = this.communicationManager.portPlayerMapper.get(port_to_set);
                            this.communicationManager.logger.info("Got old player number: "+oldPlayerNumber);
                            try {
                                this.communicationManager.playerPortMapper.remove(oldPlayerNumber);
                                this.communicationManager.logger.info("removed old player number form mapping");
                                this.communicationManager.portPlayerMapper.remove(port_to_set);
                                this.communicationManager.logger.info("removed old port number form mapping");
                            } catch (NullPointerException ignored){}
                            this.communicationManager.addPlayer(port_to_set, playerNumber);
                            this.communicationManager.logger.info("added player");
                            this.communicationManager.logger.info("injected setting_player now players: " + this.communicationManager.portPlayerMapper.toString() + ", " + this.communicationManager.playerPortMapper.toString());
                            communicationManager.manager.getPlayerInterface().addTextToStatusPanel("Injected \nPlayer "+ playerNumber+ " was set. Mapper: " + this.communicationManager.playerPortMapper.toString());
                            this.communicationManager.logger.info("added text to status panel");
                            adminSuccess = true;
                        } catch (NullPointerException | JSONException nullPointerException) {
                            this.communicationManager.logger.info("No Port was set when trying to set player as inject.");
                        }
                    }
                    if (!adminSuccess) {
                        //System.out.println("playerid: " + playerID);
                        // test wether the user already set a player. its only possible if no moves have been made to avoid
                        // llm just switching players and playing against itself

                        if (this.communicationManager.manager.ref().context().trial().numberRealMoves() > 0) {
                            success = false;
                            this.communicationManager.logger.info("Number of moves made: "+this.communicationManager.manager.ref().context().trial().numMoves());
                            errorMessage = "Can't set player after moves have been made alrady. Try restarting the Game or starting a new one.";
                            break;
                        }
                        try {
                            if (this.communicationManager.playerPortMapper.get(playerNumber) != null) {
                                success = false;
                                errorMessage = "This Player index is already assigned. Try getting unset player indices.";
                                break;
                            }
                        } catch (NullPointerException ignored) {
                        }

                        int maxPlayers = this.communicationManager.manager().ref().context().players().size();
                        if (playerNumber > maxPlayers) {
                            success = false;
                            errorMessage = ("This Player number does not exists. There are only " + maxPlayers + " players in this gaem.");
                            break;
                        }
                        try {
                            Integer oldPlayerNumber = this.communicationManager.portPlayerMapper.get(request.senderPort);
                            this.communicationManager.playerPortMapper.remove(oldPlayerNumber);
                            this.communicationManager.portPlayerMapper.remove(request.senderPort);
                        } catch (NullPointerException ignored) {
                        }
                        this.communicationManager.addPlayer(request.senderPort, playerNumber);
                        this.communicationManager.logger.info("setting_player now players: " + this.communicationManager.portPlayerMapper.toString() + ", " + this.communicationManager.playerPortMapper.toString());
                        communicationManager.manager.getPlayerInterface().addTextToStatusPanel("\nPlayer " + playerNumber + " was set. Mapper: " + this.communicationManager.playerPortMapper.toString());
                    }
                    break;
                case "notify":
                    int notifyMover;
                    try {
                        notifyMover = request.getRequestLine().getInt("index");
                    } catch (JSONException je){
                        errorMessage = "'index' variable missing from request.";
                        success = false;
                        break;
                    }
                    setNotify(notifyMover, request.senderPort);
                    break;
                default:
                    success = false;
                    errorMessage ="Unknown request_target '"+request_target+ "'";
                    break;
            }
            JSONObject reply = new JSONObject();
            reply.put("success", success);
            reply.put("method", "did"); // inverse to do
            reply.put("request_target", request_target);
            reply.put("error_message", errorMessage);
            sendMessage(reply, request.getFrom(), request.jsonObject);
        }

        public void print(String text){
            System.out.println("[" + port + "] " + text);
        }
    }


    public Integer getPort(){return port;}
}
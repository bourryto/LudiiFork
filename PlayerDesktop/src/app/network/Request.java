package app.network;

import org.json.JSONArray;
import org.json.JSONObject;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public class Request {
    String message;
    JSONObject jsonObject;
    int senderPort;

    public Request(String message){
        this.message = message;
        this.jsonObject = new JSONObject(message);
        this.senderPort = this.getFrom().port;
    }

//    public Request(JSONObject jsonObject){
//        this.jsonObject = jsonObject;
//        this.message = this.jsonObject.toString();
//        this.senderPort = this.getFrom().port;
//    }

    public Request(JSONObject request_line, Address recipientPort, Address senderPort, JSONObject replyTo){
        jsonObject = new JSONObject();
        jsonObject.put("from", senderPort.toJSONObject());
        jsonObject.put("to", recipientPort.toJSONObject());
        jsonObject.put("request_line", request_line);
        jsonObject.put("replyTo", replyTo);
        JSONObject timestamp = new JSONObject();
        timestamp.put("standard", "UNIX");
        timestamp.put("format", "milliseconds");
        timestamp.put("time", Instant.now().toEpochMilli());
        jsonObject.put("timestamp", timestamp);
    }

//    public Request(JSONObject content, Address recipientPort, Address senderPort){
//        new Request(content, recipientPort, senderPort, new JSONObject());
//    }
//
//    public Request(Address recipientPort, Address senderPort, String[] parts){
//        new Request(createMessageContent(parts), recipientPort, senderPort, new JSONObject());
//    }

    public static JSONObject createMessageContent(String[] parts){
        return createMessageContent(Arrays.asList(parts));
    }
    public static JSONObject createMessageContent(List<Object> parts){
        return createMessageContent((ArrayList<Object>)parts);
    }
    public static JSONObject createMessageContent(ArrayList<Object> parts){
        if (parts.size() == 0){
            return new JSONObject().put("method", "").put("request_target", "");
        } else if(parts.size() == 1){
            return new JSONObject().put("method", parts.get(0)).put("request_target", "");
        }
        return new JSONObject().put("method", parts.get(0))
                .put("request_target", new JSONArray(parts.subList(1, parts.size())));
    }

    // old version of content structure
    public static JSONObject createMessageContentOld(String[] parts){
        System.out.println("Requests.java - createMessageContentOld() got called!!!! This uses old terms and will likely cause parsing issues.");
        if (parts.length == 0){
            return new JSONObject().put("command", "").put("option", "");
        } else if(parts.length == 1){
            return new JSONObject().put("command", parts[0]).put("option", "");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", parts[0]);
        if(parts.length == 2){
            jsonObject.put("option", parts[1]);
        } else{
            jsonObject.put("option", createMessageContent(Arrays.copyOfRange(parts, 1, parts.length)));
        }
        return jsonObject;
    }



    public Address getFrom(){

        try {
            return new Address(this.jsonObject.getJSONObject("from").getString("ip"),
                    this.jsonObject.getJSONObject("from").getInt("port"));
        }
        catch (Exception e){
            System.out.println("Request.java - \"From\" Port of request is not int parsable, is: \"" + this.jsonObject.get("from") + "\"");
        }
        return new Address("", -1);
    }

    public Address getTo(){
        try {
            return new Address(this.jsonObject.getJSONObject("to").getString("ip"),
                    this.jsonObject.getJSONObject("to").getInt("port"));
        }
        catch (Exception e){
            System.out.println("Request.java - \"To\" Port of request is not int parsable, is: \"" + this.jsonObject.get("to") + "\"");
        }
        return new Address("", -1);
    }

    public int getTimestamp(){
        try {
            return this.jsonObject.getJSONObject("timestamp").getInt("time");
        }
        catch (Exception e){
            System.out.println("Request.java - \"timestamp\" of request is not int parsable");
        }
        return -1;
    }
    public JSONObject getReplyTo(){
        try {
            return this.jsonObject.getJSONObject("replyTo");
        }
        catch (Exception e){
            System.out.println("Request.java - \"replyTo\" of request is not working\n->\t"+this.jsonObject.toString());
        }
        return new JSONObject();
    }
    public JSONObject getRequestLine(){
        try {
            return this.jsonObject.getJSONObject("request_line");
        }
        catch (Exception e){
            System.out.println("Request.java - \"getRequestLine\" of request is not working\n->\t"+this.jsonObject.toString());
        }
        return new JSONObject();
    }

    @Override
    public String toString() {
        return this.jsonObject.toString();
    }
    public String toString(int indent) {
        return this.jsonObject.toString(indent);
    }
    //jsonObject.put("Kategory Key", new JSONObject().put("Subkey", "Subvalue"));
}

/*
public class Message{
    String message;
    JSONObject jsonObject;
    int senderPort;

    public Message(String message){
        this.message = message;
        this.jsonObject = new JSONObject(message);
        this.senderPort = this.getFrom().port;
    }

    public Message(JSONObject jsonObject){
        this.jsonObject = jsonObject;
        this.message = this.jsonObject.toString();
        this.senderPort = this.getFrom().port;
    }

    public Message (JSONObject content, Address recipientPort, Address senderPort, JSONObject replyTo){
        jsonObject = new JSONObject();
        jsonObject.put("from", senderPort.toJSONObject());
        jsonObject.put("to", recipientPort.toJSONObject());
        jsonObject.put("content", content);
        jsonObject.put("replyTo", replyTo);
        JSONObject timestamp = new JSONObject();
        timestamp.put("standard", "UNIX");
        timestamp.put("format", "milliseconds");
        timestamp.put("time", Instant.now().toEpochMilli());
        jsonObject.put("timestamp", timestamp);
    }

    public Message(JSONObject content, Address recipientPort, Address senderPort){
        new Message(content, recipientPort, senderPort, new JSONObject());
    }

    public Message(Address recipientPort, Address senderPort, String[] parts){
        new Message(createMessageContent(parts), recipientPort, senderPort, new JSONObject());
    }

    public static JSONObject createMessageContent(String[] parts){
        return createMessageContent(Arrays.asList(parts));
    }
    public static JSONObject createMessageContent(List<Object> parts){
        return createMessageContent((ArrayList<Object>)parts);
    }
    public static JSONObject createMessageContent(ArrayList<Object> parts){
        if (parts.size() == 0){
            return new JSONObject().put("command", "").put("options", new JSONArray());
        } else if(parts.size() == 1){
            return new JSONObject().put("command", parts.get(0)).put("options", new JSONArray());
        }
        return new JSONObject().put("command", parts.get(0))
                                                .put("options", new JSONArray(parts.subList(1, parts.size())));
    }

    // old version of content structure
    public static JSONObject createMessageContentOld(String[] parts){
        if (parts.length == 0){
            return new JSONObject().put("command", "").put("option", "");
        } else if(parts.length == 1){
            return new JSONObject().put("command", parts[0]).put("option", "");
        }
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("command", parts[0]);
        if(parts.length == 2){
            jsonObject.put("option", parts[1]);
        } else{
            jsonObject.put("option", createMessageContent(Arrays.copyOfRange(parts, 1, parts.length)));
        }
        return jsonObject;
    }



    public Address getFrom(){

        try {
            return new Address(this.jsonObject.getJSONObject("from").getString("ip"),
                    this.jsonObject.getJSONObject("from").getInt("port"));
        }
        catch (Exception e){
            System.out.println("\"From\" Port of message is not int parsable, is: \"" + this.jsonObject.get("from") + "\"");
        }
        return new Address("", -1);
    }

    public Address getTo(){
        try {
            return new Address(this.jsonObject.getJSONObject("to").getString("ip"),
                    this.jsonObject.getJSONObject("to").getInt("port"));
        }
        catch (Exception e){
            System.out.println("\"To\" Port of message is not int parsable, is: \"" + this.jsonObject.get("to") + "\"");
        }
        return new Address("", -1);
    }

    public int getTimestamp(){
        try {
            return this.jsonObject.getJSONObject("timestamp").getInt("time");
        }
        catch (Exception e){
            System.out.println("\"timestamp\" of message is not int parsable");
        }
        return -1;
    }
    public JSONObject getReplyTo(){
        try {
            return this.jsonObject.getJSONObject("replyTo");
        }
        catch (Exception e){
            System.out.println("\"replyTo\" of message is not working");
        }
        return new JSONObject();
    }
    public JSONObject getContent(){
        try {
            return this.jsonObject.getJSONObject("content");
        }
        catch (Exception e){
            System.out.println("\"replyTo\" of message is not working");
        }
        return new JSONObject();
    }

    @Override
    public String toString() {
        return this.jsonObject.toString();
    }
    public String toString(int indent) {
        return this.jsonObject.toString(indent);
    }
    //jsonObject.put("Kategory Key", new JSONObject().put("Subkey", "Subvalue"));
}
*/
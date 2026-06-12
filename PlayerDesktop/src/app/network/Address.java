package app.network;

import org.json.JSONObject;

public class Address{
    String ip;
    int port;

    public Address(String ip, int port){
        this.ip = ip;
        this.port = port;
    }

    public Address(int port){
        this.port = port;
        this.ip = "127.0.0.1";
    }

    public JSONObject toJSONObject(){
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("ip", ip);
        jsonObject.put("port", port);
        return jsonObject;
    }
}
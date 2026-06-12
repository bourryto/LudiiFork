package app;

import manager.Manager;

import java.util.HashMap;

public class Apps {
    private static HashMap<Integer, App> list = new HashMap<Integer, App>();

    public static void addApp(App app){
        list.put(app.getID(), app);
    }

    public static App getFromID(Integer id){
        return list.get(id);
    }

    public static App getFromPort(Integer port)throws NullPointerException{
        for(App app: list.values()){
            if(app.getPort() == port){return app;}
        }
        throw new NullPointerException("Could not find app with port: " + port);
    }

    public static App getApp(Manager manager){
        return Apps.getFromID(manager.getAppID());
    }

    public static int numberInstances(){return list.size();}
}

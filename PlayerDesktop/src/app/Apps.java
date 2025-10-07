package app;

import java.util.HashMap;

public class Apps {
    private static HashMap<Integer, App> list = new HashMap<Integer, App>();

    public static void addApp(App app){
        list.put(app.getID(), app);
    }

    public static App getFromID(Integer id){
        return list.get(id);
    }
}

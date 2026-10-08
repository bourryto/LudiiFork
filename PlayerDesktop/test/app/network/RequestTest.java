package app.network;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class RequestTest {

    @Test
    void createMessageContentOld() {
        String[] message_components_4 = {"0", "1", "2", "3"};
        //System.out.println(Message.createMessageContentOld(text));
        JSONObject expectedJson_4 = new JSONObject().put("command", "0")
                .put("option", new JSONObject().put("command", "1")
                        .put("option", new JSONObject().put("command", "2").put("option", "3")));
        assertEquals(expectedJson_4.toString(), Request.createMessageContentOld(message_components_4).toString());
        assertTrue(expectedJson_4.similar(Request.createMessageContentOld(message_components_4)));

        String[] message_components_2 = {"0", "1"};
        JSONObject expectedJson_2 = new JSONObject().put("command", "0")
                .put("option", "1");
        assertEquals(expectedJson_2.toString(), Request.createMessageContentOld(message_components_2).toString());
        assertTrue(expectedJson_2.similar(Request.createMessageContentOld(message_components_2)));

        String[] message_components_1 = {"0"};
        JSONObject expectedJson_1 = new JSONObject().put("command", "0")
                .put("option", "");
        assertEquals(expectedJson_1.toString(), Request.createMessageContentOld(message_components_1).toString());
        assertTrue(expectedJson_1.similar(Request.createMessageContentOld(message_components_1)));

        String[] message_components_0 = {};
        JSONObject expectedJson_0 = new JSONObject().put("command", "")
                .put("option", "");
        assertEquals(expectedJson_0.toString(), Request.createMessageContentOld(message_components_0).toString());
        assertTrue(expectedJson_0.similar(Request.createMessageContentOld(message_components_0)));
    }

    @Test
    void createMessageContent() {
        ArrayList<Object> message_components_diffrent_types = new ArrayList<>();
        message_components_diffrent_types.add("String");
        message_components_diffrent_types.add(1);
        message_components_diffrent_types.add(2.5);
        message_components_diffrent_types.add(true);
        //System.out.println(Message.createMessageContent(text));
        JSONObject expectedJson_diffrent_types = new JSONObject().put("command", "String")
                .put("options", new JSONArray() .put(1)
                        .put(2.5)
                        .put(true));
        assertEquals(expectedJson_diffrent_types.toString(), Request.createMessageContent(message_components_diffrent_types).toString());
        assertTrue(expectedJson_diffrent_types.similar(Request.createMessageContent(message_components_diffrent_types)));
        // from object List
        ArrayList<Object> message_components_4 = new ArrayList<>();
        message_components_4.add(0);
        message_components_4.add(1);
        message_components_4.add(2);
        message_components_4.add(3);
        //System.out.println(Message.createMessageContent(text));
        JSONObject expectedJson_4 = new JSONObject().put("command", 0)
                .put("options", new JSONArray() .put(1)
                                                .put(2)
                                                .put(3));
        assertEquals(expectedJson_4.toString(), Request.createMessageContent(message_components_4).toString());
        assertTrue(expectedJson_4.similar(Request.createMessageContent(message_components_4)));

        ArrayList<Object> message_components_2 = new ArrayList<>();
        message_components_2.add(0);
        message_components_2.add(1);
        JSONObject expectedJson_2 = new JSONObject().put("command", 0)
                .put("options", new JSONArray() .put(1));
        assertEquals(expectedJson_2.toString(), Request.createMessageContent(message_components_2).toString());
        assertTrue(expectedJson_2.similar(Request.createMessageContent(message_components_2)));

        ArrayList<Object> message_components_1 = new ArrayList<>();
        message_components_1.add(0);
        JSONObject expectedJson_1 = new JSONObject().put("command", 0)
                .put("options", new JSONArray());

        assertEquals(expectedJson_1.toString(), Request.createMessageContent(message_components_1).toString());
        assertTrue(expectedJson_1.similar(Request.createMessageContent(message_components_1)));

        ArrayList<Object> message_components_0 = new ArrayList<>();
        JSONObject expectedJson_0 = new JSONObject().put("command", "")
                .put("options", new JSONArray());
        assertEquals(expectedJson_0.toString(), Request.createMessageContent(message_components_0).toString());
        assertTrue(expectedJson_0.similar(Request.createMessageContent(message_components_0)));
    }
}
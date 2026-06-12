package main;

import org.json.JSONObject;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;

class ExportUtilsTest {

    @Test
    void toJSON() {
        TestClass_Parent testClassParent = new TestClass_Parent();
        TestClass_Child testClassChild = new TestClass_Child();
        JSONObject expectedJSON_testClassChild = new JSONObject();
        expectedJSON_testClassChild.put("j", 2);
        expectedJSON_testClassChild.put("e", 2.2);
        expectedJSON_testClassChild.put("t", "test class child");

        //System.out.println("Expected: \n" + expectedJSON_testClassChild.toString());
        //System.out.println("Actual: \n" + ExportUtils.toJSON(testClassChild).toString());
        assertTrue(expectedJSON_testClassChild.similar(ExportUtils.toJSONWrapper(testClassChild)));
        assertEquals(expectedJSON_testClassChild.toString(), ExportUtils.toJSONWrapper(testClassChild).toString());
        System.out.println("Successfully tested simple class");


        JSONObject expectedJSON_testClassParent = new JSONObject();
        expectedJSON_testClassParent.put("i", 1);
        expectedJSON_testClassParent.put("d", 1.1);
        expectedJSON_testClassParent.put("s", "test class parent");
        expectedJSON_testClassParent.put("testClassChild", expectedJSON_testClassChild);
        //System.out.println("Expected: \n" + expectedJSON_testClassParent.toString());
        //System.out.println("Actual: \n" + ExportUtils.toJSON(testClassParent).toString());
        assertTrue(expectedJSON_testClassParent.similar(ExportUtils.toJSONWrapper(testClassParent)));
        assertEquals(expectedJSON_testClassParent.toString(), ExportUtils.toJSONWrapper(testClassParent).toString());
        System.out.println("Successfully tested Class with nested Class as attribute");

        TestClass_AllTypes testClassAllTypes = new TestClass_AllTypes();
        JSONObject expectedJSON_allTypes = new JSONObject();
        expectedJSON_allTypes.put("st", "Value st");
        expectedJSON_allTypes.put("bo", Boolean.valueOf(true));
        expectedJSON_allTypes.put("ch", Character.valueOf('a'));
        expectedJSON_allTypes.put("by", Byte.valueOf((byte)1));
        expectedJSON_allTypes.put("sh", Short.valueOf((short)1));
        expectedJSON_allTypes.put("in", Integer.valueOf(1));
        expectedJSON_allTypes.put("lo", 1L);
        expectedJSON_allTypes.put("fl", 1.0f);
        expectedJSON_allTypes.put("du", Double.valueOf(1.0));
        expectedJSON_allTypes.put("bo_", true);
        expectedJSON_allTypes.put("ch_", Character.valueOf('a'));       // json cant do primitive characters, it converts them to integers but thats not how it should work
        //System.out.println("expected char: " + expectedJSON_allTypes.get("ch_").getClass().getName());
        expectedJSON_allTypes.put("by_", Byte.valueOf((byte)1));            // cant do primitive
        expectedJSON_allTypes.put("sh_", Short.valueOf((short)1));          // cant do primitive
        expectedJSON_allTypes.put("in_", 1);
        expectedJSON_allTypes.put("lo_", 1L);
        expectedJSON_allTypes.put("fl_", 1.0f);
        expectedJSON_allTypes.put("du_", 1.0);
        JSONObject actualJSON_allTypes = ExportUtils.toJSONWrapper(testClassAllTypes);
        //System.out.println("Expected: \n" + expectedJSON_allTypes.toString());
        //System.out.println("Actual: \n" + actualJSON_allTypes.toString());
        assertEquals(expectedJSON_allTypes.toString(), actualJSON_allTypes.toString());
        assertTrue(expectedJSON_allTypes.similar(actualJSON_allTypes));
        System.out.println("Successfully tested Class with all diffrent Types of variables");
        /* if single pairs need to be examined
        for (String key: expectedJSON_allTypes.keySet()) {
            System.out.println("Key: " + key);
            System.out.println("\tExpected: " + expectedJSON_allTypes.get(key) + ", Type: " + expectedJSON_allTypes.get(key).getClass().getName());
            System.out.println("\tActual  : " + actualJSON_allTypes.get(key));
            assertTrue(expectedJSON_allTypes.get(key).equals(actualJSON_allTypes.get(key)));
        }
        System.out.println("\n---\n");
        for(Field field: testClassAllTypes.getClass().getDeclaredFields()){
            System.out.println("Field Name: " + field.getName() + "\nField Class: " + field.getType().getName());
        }

         */
    }

    public class TestClass_Parent {
        int i = 1;
        double d = 1.1;
        String s = "test class parent";
        TestClass_Child testClassChild = new TestClass_Child();
    }
    public class TestClass_Child {
        int j = 2;
        double e = 2.2;
        String t = "test class child";
    }
    public class TestClass_AllTypes {
        String st = "Value st";
        Boolean bo = true;
        Character ch = 'a';
        Byte by = (byte) 1;
        Short sh = (short) 1;
        Integer in = 1;
        Long lo = 1L;
        Float fl = 1.0f;
        Double du = 1.0;

        boolean bo_ = true;
        char ch_ = 'a';
        byte by_ = (byte) 1;
        short sh_ = (short) 1;
        int in_ = 1;
        long lo_ = 1L;
        float fl_ = 1.0f;
        double du_ = 1.0;
    }
}
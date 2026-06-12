package main;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;

import java.io.File;
import java.io.FileWriter;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.*;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

public class ExportUtils {
    private static final int maxPrintDepth = 2;
    private static final int maxRecursionDepth = 7;

    /*
    // TODO - FIX : doenst work with private attributes
    public static JSONObject toJSON(Object object) {
        System.out.println("[Test Output] " + "to json function on " + object.getClass() + " -from- ExportUtilsTest.java");
        if (isPrimitive(object.getClass())) {
            System.out.println("[Test Output] !!!!IsPrimitive true -from- ExportUtilsTest.java");
            return new JSONObject().put("idk", object.toString());
        }
        JSONObject json = new JSONObject();
        if (object.getClass().getDeclaredFields().length == 0) {
            System.out.println("[Test Output] " + "no fields" + " -from- ExportUtilsTest.java");
        }

        System.out.println("[Test Output] Fields -from- ExportUtilsTest.java");
        for (Field field : object.getClass().getDeclaredFields()) {
            System.out.println("[Test Output] " + field.getName() + " -from- ExportUtilsTest.java");
            if (field.getName().equals("this$0")){continue;}
            Object value;
            try {
                if (!isPrimitive(field.getType())) {
                    value = toJSON(field.get(object));
                } else {
                    value = field.get(object);
                }
            } catch (IllegalAccessException e) {
                value = "Error Fetching Value";
            }
            json.put(field.getName(), value);
        }
        return json;
    };

     */

    public static JSONObject toJSONWrapper(Object object){
        //System.out.println(object.getClass().getSimpleName());
        Object retObj = toJSON(object, false, false, false);
        if(retObj instanceof JSONObject){
             try {
                 String filename = "/home/leo/Uni/Bachelor Thesis/LudiiFork/DebuggingOutput/" + object.getClass().getSimpleName()  + "_" + Instant.now().toEpochMilli() + ".json";
                 File file = new File(filename);
                 if (!file.createNewFile()){
                     System.out.println("error creating file");
                 } else {
                     //System.out.println("file created");
                 }
                 FileWriter fileWriter = new FileWriter(filename);
                 fileWriter.write(((JSONObject) retObj).toString(4));
                 fileWriter.close();
                 //System.out.println("successfully wrote to the file");
             } catch (Exception e) {
                 System.out.println("error writing to json output to file: " + e.getMessage());
             }
            return (JSONObject)retObj;
        }
        return (new JSONObject()).put("status", "error");
    }
    public static Object toJSON(Object object, boolean doPrivate, boolean doProtected, boolean doGetter){
        return toJSON(object, doPrivate, doProtected, doGetter, 0);
    }
    public static Object toJSON(Object object, boolean doPrivate, boolean doProtected, boolean doGetter, int depth) {
        // recursive
        if (depth == maxRecursionDepth){
            return "max-depth reached";
        }
        if (object == null) {
            return new JSONObject();
        } else if (String.class.equals(object.getClass()) && object.equals("null")) {
            return new JSONObject();
        }
        if (isPrimitive(object.getClass()) || object.getClass().isEnum()) {
//            System.out.println("is prim");
            return object;
        }
//        System.out.println("-" + object.getClass().getSimpleName() + "-" + depth);
        if (object.getClass().isArray()){
            if (object.getClass().getComponentType().isPrimitive()){
                JSONArray jsonArray;
                switch (object.getClass().getComponentType().getSimpleName()) {
                    case "boolean":
                        boolean[] booleanArray = (boolean[]) object;
                        jsonArray = new JSONArray(booleanArray);
                        break;
                    case "char":
                        char[] charArray = (char[]) object;
                        jsonArray = new JSONArray(charArray);
                        break;
                    case "byte":
                        byte[] byteArray = (byte[]) object;
                        jsonArray = new JSONArray(byteArray);
                        break;
                    case "short":
                        short[] shortArray = (short[]) object;
                        jsonArray = new JSONArray(shortArray);
                        break;
                    case "int":
                        int[] intArray = (int[]) object;
                        jsonArray = new JSONArray(intArray);
                        break;
                    case "long":
                        long[] longArray = (long[]) object;
                        jsonArray = new JSONArray(longArray);
                        break;
                    case "float":
                        float[] floatArray = (float[]) object;
                        jsonArray = new JSONArray(floatArray);
                        break;
                    case "double":
                        double[] doubleArray = (double[]) object;
                        jsonArray = new JSONArray(doubleArray);
                        break;
                    default:
                        jsonArray = new JSONArray();
                        break;
                }
                return jsonArray;
            }
            JSONArray values = new JSONArray();
            List elements = Arrays.asList((Object[]) object);
            //System.out.println("List of object: " + object.toString() + ", is: " + elements);
            for (int i = 0; i < elements.size(); i++) {
                values.put(toJSON(elements.get(i), doPrivate, doProtected, doGetter, depth+1));
            }
            return values;
        } else if (Collection.class.isAssignableFrom(object.getClass())) {
            //System.out.println("is colection");
            if (List.class.isAssignableFrom(object.getClass())) {
                JSONArray values = new JSONArray();
                List<Object> elements = new ArrayList((List) object);
                for (Object element : elements) {
                    values.put(toJSON(element, doPrivate, doProtected, doGetter, depth + 1));
                }
                return values;
            } else if (Queue.class.isAssignableFrom(object.getClass())) {
                JSONArray values = new JSONArray();
                //System.out.println("Queue Object: " + object.toString());
                Queue elements = (Queue) object;
                //System.out.println("Queue Elements: " + elements.toString());
                for (Object element : elements) {
                    values.put(toJSON(element, doPrivate, doProtected, doGetter, depth+1));
                }
                return values;
            } else if (Set.class.isAssignableFrom(object.getClass())) {
                JSONArray values = new JSONArray();
                Set elements = (Set) object;
                for (Object element : elements) {
                    values.put(toJSON(element, doPrivate, doProtected, doGetter, depth+1));
                }
                return values;
            }
        } else if(Arrays.stream(object.getClass().getInterfaces()).collect(Collectors.toList()).contains(Iterable.class)){
            JSONArray values = new JSONArray();
            for (Object element : (Iterable<? extends Object>) object){
                values.put(toJSON(element, doPrivate, doProtected, doGetter, depth + 1));
            }
            return values;
        }    else if (Map.class.isAssignableFrom(object.getClass())) {
            //System.out.println("is map");
            JSONObject values = new JSONObject();
            Map elements = (Map) object;
            for (Object key : elements.keySet()) {
                values.put(key.toString(), toJSON(elements.get(key), doPrivate, doProtected, doGetter, depth+1));
            }
            return values;
        }else {
            //System.out.println("Object is not any of the pre checks, its: " + object.getClass() + " and is it instance of map? : " + object.getClass().isInstance(Map.class));
        }

        JSONObject jsonObject = new JSONObject();
        ArrayList<Field> fields = getFields(object.getClass());
        //printIndented("Found Fields:         ["+fields.stream().map(Field::getName).sorted().collect(Collectors.joining(", ")) + "]", depth+1);
        ArrayList<Field> addedFields = new ArrayList<>();
        for (Field field : fields){
            // skipping private or protected fields
            if (! Modifier.isPublic(field.getModifiers())) {
                continue;
            }
            try {
                //printIndented(field.getName() + ":", depth+1);
                jsonObject.put(field.getName(), toJSON(field.get(object), doPrivate, doProtected, doGetter, depth+1));
                //printIndented("|->" + jsonObject.get(field.getName()).toString(), depth+2);
                addedFields.add(field);
            } catch (IllegalAccessException e) {
                jsonObject.put(field.getName(), "error");
                e.printStackTrace();
            }

        }
        fields.removeAll(addedFields);
        List<String> fieldNames = fields.stream().map(Field::getName).map(String::toLowerCase).collect(Collectors.toList());
        //printIndented("Field Names:          ["+fieldNames.stream().sorted().collect(Collectors.joining(", "))+"]", depth+1);
        // for methods getter
        ArrayList<Method> methods = getMethods(object.getClass());
        //printIndented("Found Methods:        [" + methods.stream().map(Method::getName).sorted().collect(Collectors.joining(", ")) + "]", depth+1);
        // remove those who don't match a field and are public
        List<Method> reducedGetters = methods.stream().filter(m -> (fieldNames.contains(  m.getName().toLowerCase()) ||
                        (m.getName().toLowerCase().startsWith("get") && fieldNames.contains(m.getName().substring(3).toLowerCase()))))
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .collect(Collectors.toList());
        //printIndented("Reduced Methods:      [" + reducedGetters.stream().map(Method::getName).sorted().collect(Collectors.joining(", ")) + "]", depth+1);
        Set<String> reducedGettersNames = reducedGetters.stream().map(method -> method.getName().toLowerCase()).collect(Collectors.toSet());
        //printIndented("Reduced Method Names: [" + reducedGettersNames.stream().sorted().collect(Collectors.joining(", ")) + "]", depth+1);
        // removes duplicates if there is a getter with get prefix and one without
        reducedGetters = reducedGetters.stream().filter(m -> {if (m.getName().toLowerCase().startsWith("get") && reducedGettersNames.contains(m.getName().toLowerCase().substring(3))){return false;}return true;}).collect(Collectors.toList());
        //printIndented("Reduced Methods:      [" + reducedGetters.stream().map(Method::getName).sorted().collect(Collectors.joining(", ")) + "]", depth+1);
        //HashMap<String, Method> getters = (HashMap<String, Method>) reducedGetters.stream().collect(Collectors.toMap(method -> {if (method.getName().toLowerCase().startsWith("get")){return method.getName().substring(3,4).toLowerCase() + method.getName().substring(4);}return method.getName();}, method -> method));
        HashMap<String, Method> getters = new HashMap<>();
        for (Method method: reducedGetters){
            String name = method.getName();
            if (name.toLowerCase().startsWith("get")) {
                name = method.getName().substring(3, 4).toLowerCase() + method.getName().substring(4);
            }
            if (!getters.containsKey(name)) {
                getters.put(name, method);
            }
        }
        //printIndented("Getters:              [" + getters.keySet().stream().sorted().collect(Collectors.joining(", ")) + "]", depth+1);

        for (String key : getters.keySet()) {
            Method method = getters.get(key);
            if (method.getParameterCount() > 0){
                continue;
            }
            try {
                //printIndented(method.getName() + ":", depth+1);
                jsonObject.put(key, toJSON(method.invoke(object), doPrivate, doProtected, doGetter, depth+1));
            } catch (InvocationTargetException | IllegalAccessException e) {
                jsonObject.put(key, "error");
                System.out.println("! error while converting method '" + method.getName() + "' to JSON: " + e.getMessage());
                e.printStackTrace();
            }
        }
        return jsonObject;
    }

    public static ArrayList<Field> getFields(Class<?> clazz){
        if (clazz == null || clazz.equals(Object.class)) {
            return new ArrayList<>();
        }
        Set<Field> fieldSet = Arrays.stream(clazz.getDeclaredFields()).collect(Collectors.toSet());
        fieldSet.addAll(Arrays.stream(clazz.getFields()).collect(Collectors.toSet()));
        fieldSet.addAll(getFields(clazz.getSuperclass()));
        return fieldSet.stream().filter(field -> !field.getName().equals("this$0")).collect(Collectors.toCollection(ArrayList::new));
    }
    public static ArrayList<Method> getMethods(Class<?> clazz){
        Set<Method> methodSet = Arrays.stream(clazz.getDeclaredMethods()).collect(Collectors.toSet());
        methodSet.addAll(Arrays.stream(clazz.getMethods()).collect(Collectors.toSet()));
        return new ArrayList<>(methodSet);
    }

    private static boolean isPrimitive(Class<?> clazz) {

        boolean isPrim =  clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Boolean.class ||
                clazz == Character.class ||
                clazz == Byte.class ||
                clazz == Short.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Float.class ||
                clazz == Double.class;
        //System.out.println("class: " + clazz.getName() + " isPrimitive: " + isPrim);
        return isPrim;
    }


    public static void printIndented(String str, String context){printIndented(str, context, 1);}
    public static void printIndented(String str){printIndented(str,"", 1);}
    public static void printIndented(String str, int indent){printIndented(str, "", indent);}
    public static void printIndented(String str, String context, int indent){
        if (indent<=maxPrintDepth) {
            System.out.println(textIndented(str, context, indent));
        }
    }
    public static String textIndented(String str, String context){return textIndented(str, context, 1);}
    public static String textIndented(String str){return textIndented(str, "", 1);}
    public static String textIndented(String str, int indent){return textIndented(str, "",indent);}
    public static String textIndented(String str, String context, int indent) {
        if (context != null && context.length() > 0) {
            System.out.println(context);
        }
        String[] parts = str.split("\n");
        String tabbs = "";
        for (int i = 0; i < indent; i++) {
            tabbs += "\t";
        }
        final String finalTabbs = tabbs;
        String indentedString = Arrays.stream(parts).map(s -> finalTabbs + s + "\n").collect(Collectors.joining());
        if (indentedString.endsWith("\n")) {
            indentedString = indentedString.substring(0, indentedString.length() - 1);
        }
        return indentedString;
    }
}

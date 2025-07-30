package net.yadaframework.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class YadaUtilTest {
    private YadaUtil yadaUtil;

    @BeforeEach
    void setUp() {
        yadaUtil = new YadaUtil();
    }

    @Nested
    class JsonTests {
        private Map<String, Object> jsonObject;

        @BeforeEach
        void setUp() {
            jsonObject = yadaUtil.makeJsonObject();
        }

        @Test
        void makeJsonObject_CreatesEmptyMap() {
            Map<String, Object> result = yadaUtil.makeJsonObject();
            assertNotNull(result);
            assertTrue(result.isEmpty());
        }

        @Test
        void setAndGetJsonAttribute_SimpleValue() {
            yadaUtil.setJsonAttribute(jsonObject, "name", "John");
            assertEquals("John", yadaUtil.getJsonAttribute(jsonObject, "name"));
        }
        
        @Test
        void setAndGetJsonAttribute_IntegerValue() {
            yadaUtil.setJsonAttribute(jsonObject, "age", 33);
            Object result = yadaUtil.getJsonAttribute(jsonObject, "age");
            assertTrue(result instanceof Integer);
            assertEquals(Integer.valueOf(33), result);
        }
        
        @Test
        void setAndGetJsonAttribute_DoubleValue() {
            yadaUtil.setJsonAttribute(jsonObject, "weight", 63.3);
            Object result = yadaUtil.getJsonAttribute(jsonObject, "weight");
            assertTrue(result instanceof Double);
            assertEquals(63.3, (Double)result, 0.001);
        }

        @Test
        void setAndGetJsonAttribute_NestedValue() {
            yadaUtil.setJsonAttribute(jsonObject, "user.name", "John");
            yadaUtil.setJsonAttribute(jsonObject, "user.age", "30");
            
            assertEquals("John", yadaUtil.getJsonAttribute(jsonObject, "user.name"));
            assertEquals("30", yadaUtil.getJsonAttribute(jsonObject, "user.age"));
        }

        @Test
        void setAndGetJsonAttribute_ArrayValue() {
            yadaUtil.setJsonAttribute(jsonObject, "users[0].name", "John");
            yadaUtil.setJsonAttribute(jsonObject, "users[1].name", "Jane");
            
            assertEquals("John", yadaUtil.getJsonAttribute(jsonObject, "users[0].name"));
            assertEquals("Jane", yadaUtil.getJsonAttribute(jsonObject, "users[1].name"));
        }

        @Test
        void getJsonObject_ReturnsNestedObject() {
            yadaUtil.setJsonAttribute(jsonObject, "user.name", "John");
            yadaUtil.setJsonAttribute(jsonObject, "user.age", "30");
            
            Map<String, Object> user = yadaUtil.getJsonObject(jsonObject, "user");
            assertNotNull(user);
            assertEquals("John", user.get("name"));
            assertEquals("30", user.get("age"));
        }

        @Test
        void getJsonArray_ReturnsArray() {
            yadaUtil.setJsonAttribute(jsonObject, "users[0].name", "John");
            yadaUtil.setJsonAttribute(jsonObject, "users[1].name", "Jane");
            
            List<Object> users = yadaUtil.getJsonArray(jsonObject, "users");
            assertNotNull(users);
            assertEquals(2, users.size());
            assertEquals("John", ((Map<String,Object>)users.get(0)).get("name"));
            assertEquals("Jane", ((Map<String,Object>)users.get(1)).get("name"));
        }

        @Test
        void getJsonObject_WithArrayIndex() {
            yadaUtil.setJsonAttribute(jsonObject, "users[0].name", "John");
            yadaUtil.setJsonAttribute(jsonObject, "users[1].name", "Jane");
            
            Map<String, Object> user = yadaUtil.getJsonObject(jsonObject, "users", 1);
            assertNotNull(user);
            assertEquals("Jane", user.get("name"));
        }

        @Test
        void makeJsonObject_WithPath_CreatesNestedStructure() {
            Map<String, Object> nested = yadaUtil.makeJsonObject(jsonObject, "user.settings");
            assertNotNull(nested);
            assertTrue(nested.isEmpty());
            
            Map<String, Object> settings = yadaUtil.getJsonObject(jsonObject, "user.settings");
            assertNotNull(settings);
            assertTrue(settings.isEmpty());
        }

        @Test
        void makeJsonObject_WithArrayPath_ReturnsMapFromArray() {
        	 Map<String, Object> test = yadaUtil.makeJsonObject(yadaUtil.makeJsonObject(), "configurations[1]");
        	
            // Create a parent object with a configurations array
            yadaUtil.setJsonAttribute(jsonObject, "configurations[0].name", "Config1");
            yadaUtil.setJsonAttribute(jsonObject, "configurations[1].name", "Config2");
            
            // Test that nothing was added as it was there already
            Map<String, Object> cell1 = yadaUtil.makeJsonObject(jsonObject, "configurations[1]");
            assertNotNull(cell1);
            assertTrue(cell1 instanceof Map);
            //assertEquals("Config2", ((Map)((List<Object>)cell1.get("configurations")).get(1)).get("name"));
            assertEquals("Config2", cell1.get("name"));
            
            // Test making a new object in a new array index
            Map<String, Object> cell2 = yadaUtil.makeJsonObject(jsonObject, "configurations[2]");
            assertNotNull(cell2);
            assertTrue(cell2 instanceof Map);
            assertTrue(cell2.isEmpty());
            
            // Verify we can add properties to the new config
            yadaUtil.setJsonAttribute(jsonObject, "configurations[2].name", "Config3");
            assertEquals("Config3", yadaUtil.getJsonAttribute(jsonObject, "configurations[2].name"));
            
            // Verify all objects in array
            List<Object> configurations = yadaUtil.getJsonArray(jsonObject, "configurations");
            assertEquals(3, configurations.size());
            assertEquals("Config1", ((Map<String,Object>)configurations.get(0)).get("name"));
            assertEquals("Config2", ((Map<String,Object>)configurations.get(1)).get("name"));
            assertEquals("Config3", ((Map<String,Object>)configurations.get(2)).get("name"));
        }

        // Edge Cases
        @Test
        void setJsonAttribute_NonStringValue_ConvertsToString() {
            yadaUtil.setJsonAttribute(jsonObject, "number", 42);
            yadaUtil.setJsonAttribute(jsonObject, "boolean", true);
            assertEquals(42, yadaUtil.getJsonAttribute(jsonObject, "number"));
            assertEquals(true, yadaUtil.getJsonAttribute(jsonObject, "boolean"));
        }

        @Test
        void setJsonAttribute_DeepNesting_CreatesFullPath() {
            yadaUtil.setJsonAttribute(jsonObject, "a.very.deep.nested.path", "value");
            assertEquals("value", yadaUtil.getJsonAttribute(jsonObject, "a.very.deep.nested.path"));
            
            Map<String, Object> nested = yadaUtil.getJsonObject(jsonObject, "a.very.deep.nested");
            assertNotNull(nested);
            assertEquals("value", nested.get("path"));
        }

        @Test
        void setJsonAttribute_ArrayWithGaps_FillsWithEmptyObjects() {
            yadaUtil.setJsonAttribute(jsonObject, "array[5]", "value");
            List<Object> array = yadaUtil.getJsonArray(jsonObject, "array");
            assertEquals(6, array.size());
            for (int i = 0; i < 5; i++) {
                assertTrue(((Map<String,Object>)array.get(i)).isEmpty());
            }
            assertEquals("value", array.get(5));
        }

        // Error Conditions
        @Test
        void getJsonAttribute_NonexistentPath_ReturnsNull() {
            assertNull(yadaUtil.getJsonAttribute(jsonObject, "nonexistent.path"));
        }

//        @Test
//        void getJsonAttribute_NonexistentArrayIndex_ReturnsNull() {
//            yadaUtil.setJsonAttribute(jsonObject, "array[0]", "value");
//            assertNull(yadaUtil.getJsonAttribute(jsonObject, "array[1]"));
//        }

//        @Test
//        void getJsonAttribute_InvalidArrayIndex_ReturnsNull() {
//            yadaUtil.setJsonAttribute(jsonObject, "array[0]", "value");
//            assertNull(yadaUtil.getJsonAttribute(jsonObject, "array[invalid]"));
//        }

//        @Test
//        void getJsonObject_PathPointsToNonObject_ReturnsNull() {
//            yadaUtil.setJsonAttribute(jsonObject, "key", "value");
//            assertNull(yadaUtil.getJsonObject(jsonObject, "key.subkey"));
//        }

//        @Test
//        void getJsonArray_PathPointsToNonArray_ReturnsNull() {
//            yadaUtil.setJsonAttribute(jsonObject, "key", "value");
//            assertNull(yadaUtil.getJsonArray(jsonObject, "key"));
//        }

//        @ParameterizedTest
//        @ValueSource(strings = {
//            "user..name",          // Double dots
//            ".user.name",          // Starting with dot
//            "user.name.",          // Ending with dot
//            "[]",                  // Empty array brackets
//            "array[].name",        // Empty array index
//            "array[-1].name"       // Negative array index
//        })
//        void getJsonAttribute_InvalidPaths_ReturnsNull(String path) {
//            assertNull(yadaUtil.getJsonAttribute(jsonObject, path));
//        }

//        @Test
//        void setJsonAttribute_OverwriteObjectWithValue() {
//            // First create an object
//            yadaUtil.setJsonAttribute(jsonObject, "key.nested", "original");
//            // Then overwrite the parent with a value
//            yadaUtil.setJsonAttribute(jsonObject, "key", "overwritten");
//            
//            assertEquals("overwritten", yadaUtil.getJsonAttribute(jsonObject, "key"));
//            assertNull(yadaUtil.getJsonAttribute(jsonObject, "key.nested"));
//        }

//        @Test
//        void setJsonAttribute_OverwriteArrayWithObject() {
//            // First create an array
//            yadaUtil.setJsonAttribute(jsonObject, "key[0]", "value");
//            // Then overwrite with an object
//            yadaUtil.setJsonAttribute(jsonObject, "key.field", "object");
//            
//            assertNull(yadaUtil.getJsonArray(jsonObject, "key"));
//            assertEquals("object", yadaUtil.getJsonAttribute(jsonObject, "key.field"));
//        }
    }

    @Test
    void ensureSafeFilename_WithBlankInput_ReturnsNoname() {
        assertEquals("noname", yadaUtil.ensureSafeFilename(""));
        assertEquals("noname", yadaUtil.ensureSafeFilename("  "));
        assertEquals("noname", yadaUtil.ensureSafeFilename(null));
    }

    @Test
    void ensureSafeFilename_WithSpecialCharacters_ReturnsSafeString() {
        assertEquals("hello_world.txt", yadaUtil.ensureSafeFilename("hello world.txt"));
        assertEquals("hello_world.txt", yadaUtil.ensureSafeFilename("hello@world.txt"));
        assertEquals("hello_world.txt", yadaUtil.ensureSafeFilename("hello/world.txt"));
    }

    @Test
    void ensureSafeFilename_WithDiacritics_RemovesDiacritics() {
        assertEquals("ciao.txt", yadaUtil.ensureSafeFilename("cião.txt"));
        assertEquals("aeiou.txt", yadaUtil.ensureSafeFilename("àèìòù.txt"));
    }

    @Test
    void ensureSafeFilename_WithCase_RespectsCase() {
        String result = yadaUtil.ensureSafeFilename("Hello.TXT", false);
        assertEquals("Hello.TXT", result);
    }

    @Test
    void ensureSafeFilename_WithLowerCase_ConvertsToLowerCase() {
        String result = yadaUtil.ensureSafeFilename("Hello.TXT", true);
        assertEquals("hello.txt", result);
    }
}

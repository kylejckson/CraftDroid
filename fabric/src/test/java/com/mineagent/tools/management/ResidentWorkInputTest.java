package com.mineagent.tools.management;
import com.google.gson.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ResidentWorkInputTest {
    @Test void typedStepsAvoidStringEscapingAndRemainLegacyCompatible(){
        var args=JsonParser.parseString("{\"steps\":[{\"tool\":\"craft\",\"args\":{\"recipe_id\":\"minecraft:oak_planks\",\"count\":1}}]}").getAsJsonObject();
        var sequence=ResidentWorkTool.sequence(args);var legacy=new JsonObject();legacy.addProperty("steps_json",sequence);
        assertEquals(sequence,ResidentWorkTool.sequence(legacy));
        assertEquals("minecraft:oak_planks",JsonParser.parseString(sequence).getAsJsonArray().get(0).getAsJsonObject().getAsJsonObject("args").get("recipe_id").getAsString());
        args.addProperty("steps_json",sequence);assertThrows(IllegalArgumentException.class,()->ResidentWorkTool.sequence(args));
    }
    @Test void schemaTeachesActualConditionObjectsAndBoundsInsteadOfProse(){
        var schema=new Gson().toJsonTree(new ResidentWorkTool().parameterSchema()).getAsJsonObject();
        var steps=schema.getAsJsonObject("properties").getAsJsonObject("steps");
        assertEquals(24,steps.get("maxItems").getAsInt());
        var conditions=steps.getAsJsonObject("items").getAsJsonObject("properties").getAsJsonObject("expected_effects");
        assertEquals("object",conditions.getAsJsonObject("items").get("type").getAsString());
        var args=new JsonObject();args.add("steps",new JsonArray());assertThrows(IllegalArgumentException.class,()->ResidentWorkTool.sequence(args));
    }
}

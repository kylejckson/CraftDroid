package com.mineagent.engine.protection;
import org.junit.jupiter.api.Test;
import java.util.Set;
import static org.junit.jupiter.api.Assertions.*;
class RegionRulesTest {
    @Test void reversedCornersInclusiveEdgesDimensionsAndOverlapAreEnforced() {
        var rules=new RegionRules();
        rules.put(new RegionRules.Region("home","minecraft:overworld",10,70,10,0,60,0,Set.of(RegionRules.Action.BREAK)));
        assertFalse(rules.allows("minecraft:overworld",0,60,0,RegionRules.Action.BREAK));
        assertFalse(rules.allows("minecraft:overworld",10,70,10,RegionRules.Action.BREAK));
        assertTrue(rules.allows("minecraft:the_nether",0,60,0,RegionRules.Action.BREAK));
        assertTrue(rules.allows("minecraft:overworld",5,65,5,RegionRules.Action.ENTER));
        rules.put(new RegionRules.Region("private","minecraft:overworld",4,64,4,6,66,6,Set.of(RegionRules.Action.ENTER,RegionRules.Action.USE)));
        assertFalse(rules.allows("minecraft:overworld",5,65,5,RegionRules.Action.ENTER));
        rules.remove("private");assertTrue(rules.allows("minecraft:overworld",5,65,5,RegionRules.Action.ENTER));
        assertFalse(rules.allows("minecraft:overworld",5,65,5,RegionRules.Action.BREAK));
    }
    @Test void replacingZoneDoesNotDuplicateIt() {
        var r=new RegionRules();var zone=new RegionRules.Region("x","minecraft:overworld",0,0,0,1,1,1,Set.of(RegionRules.Action.PLACE));r.put(zone);r.put(zone);assertEquals(1,r.regions().size());
    }
    @Test void livePolicyRecallUsesNearestSameDimensionRegionsAndReflectsEdits(){
        var rules=new RegionRules();
        rules.put(new RegionRules.Region("boundary","minecraft:overworld",-30,-100,-30,-10,100,30,Set.of(RegionRules.Action.ENTER)));
        rules.put(new RegionRules.Region("orchard","minecraft:overworld",1,60,1,4,70,4,Set.of(RegionRules.Action.PLACE,RegionRules.Action.BREAK)));
        rules.put(new RegionRules.Region("other_dimension","minecraft:the_nether",0,60,0,4,70,4,Set.of(RegionRules.Action.ENTER)));
        rules.put(new RegionRules.Region("distant","minecraft:overworld",1000,60,1000,1004,70,1004,Set.of(RegionRules.Action.ENTER)));
        String context=rules.describeNearby("minecraft:overworld",0,64,0,32,8);
        assertTrue(context.contains("orchard x=1..4 y=60..70 z=1..4 denied=[BREAK, PLACE]"));
        assertTrue(context.indexOf("orchard")<context.indexOf("boundary"));
        assertFalse(context.contains("other_dimension"));assertFalse(context.contains("distant"));
        assertFalse(rules.describeNearby("minecraft:overworld",0,64,0,32,1).contains("boundary"));
        rules.remove("orchard");assertFalse(rules.describeNearby("minecraft:overworld",0,64,0,32,8).contains("orchard"));
        assertTrue(rules.describeNearby("minecraft:overworld",Integer.MAX_VALUE,0,Integer.MAX_VALUE,32,8).isEmpty());
    }
}

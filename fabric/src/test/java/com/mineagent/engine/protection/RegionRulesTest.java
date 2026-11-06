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
}

package com.mineagent.engine.resident;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class MemoryRecallTest {
    @Test void retrievesOlderRelevantPeopleAndPromisesWithoutAssigningRelationships(){
        UUID neighbor=UUID.randomUUID();var notes=new ArrayList<ResidentProfile.Memory>();
        var boundary=new ResidentProfile.Memory(neighbor,"promise","Stay away from the orchard near Rowan's house","conversation",1);
        notes.add(boundary);
        for(int i=0;i<40;i++)notes.add(new ResidentProfile.Memory(UUID.randomUUID(),"event","I watched clouds "+i,"self",i+2));
        assertTrue(MemoryRecall.select(notes,"orchard",Set.of(),3).contains(boundary));
        assertTrue(MemoryRecall.select(notes,"",Set.of(neighbor),3).contains(boundary));
        assertEquals(41,notes.size());assertEquals("promise",boundary.kind());
    }
    @Test void boundedRecallKeepsRecentExperiencesAvailable(){
        var old=new ResidentProfile.Memory(UUID.randomUUID(),"event","An old walk","self",1);
        var recent=new ResidentProfile.Memory(UUID.randomUUID(),"event","A recent walk","self",2);
        assertEquals(List.of(recent),MemoryRecall.select(List.of(old,recent),"",Set.of(),1));
    }
}

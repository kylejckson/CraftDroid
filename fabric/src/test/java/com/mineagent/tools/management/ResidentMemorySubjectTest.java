package com.mineagent.tools.management;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class ResidentMemorySubjectTest {
    @Test void selfReferencesCannotAccidentallyCreateAnotherIdentity(){
        var self=UUID.randomUUID();for(String value:new String[]{null,"","self","me","Luna",self.toString()})assertEquals(self,ResidentMemoryTool.resolveSubject(value,self,"Luna"));
        var other=UUID.randomUUID();assertEquals(other,ResidentMemoryTool.resolveSubject(other.toString(),self,"Luna"));
        assertThrows(IllegalArgumentException.class,()->ResidentMemoryTool.resolveSubject("CraftDroid",self,"Luna"));
    }
}

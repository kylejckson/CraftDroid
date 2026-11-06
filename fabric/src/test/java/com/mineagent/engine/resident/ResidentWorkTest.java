package com.mineagent.engine.resident;
import com.mineagent.engine.skill.*;
import com.mineagent.engine.world.SemanticWorldModel;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
class ResidentWorkTest {
    @Test void localWorkContinuesWithoutModelAndOnlySuccessfulProgramsBecomeSkills() {
        var library=new SkillLibrary();var calls=new ArrayList<String>();
        var runtime=new SkillRuntime(library,new SemanticWorldModel(),(id,tool,args)->{calls.add(tool);return new SkillRuntime.DispatchResult(true,false,true,"world verified");},snapshot->{});
        String program="[{\"tool\":\"craft\",\"args\":{}},{\"tool\":\"build\",\"args\":{}}]";
        assertTrue(runtime.startProposed("Build a workshop",program,1).accepted());assertEquals(0,library.size());
        for(int i=2;i<12;i++)runtime.tick(i);
        assertEquals(List.of("craft","build"),calls);assertEquals(SkillRuntime.Status.SUCCEEDED,runtime.snapshot().status());assertEquals(1,library.size());
    }
    @Test void failedWorkStopsAndCannotFabricateVerifiedExperience() {
        var library=new SkillLibrary();var calls=new ArrayList<String>();
        var runtime=new SkillRuntime(library,new SemanticWorldModel(),(id,tool,args)->{calls.add(tool);return new SkillRuntime.DispatchResult(false,false,false,"OP region denied");},snapshot->{});
        assertTrue(runtime.startProposed("Build", "[{\"tool\":\"build\"},{\"tool\":\"craft\"}]",1).accepted());
        for(int i=2;i<10;i++)runtime.tick(i);
        assertEquals(List.of("build"),calls);assertEquals(0,library.size());
        assertFalse(runtime.startProposed("Escape","[{\"tool\":\"run_command\"}]",20).accepted());
    }
}

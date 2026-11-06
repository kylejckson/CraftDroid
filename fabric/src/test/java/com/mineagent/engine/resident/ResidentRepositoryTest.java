package com.mineagent.engine.resident;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class ResidentRepositoryTest {
    @TempDir Path dir;
    private ResidentProfile profile(){return new ResidentProfile(UUID.randomUUID(),"Mira",null,"Likes workshops",false,"minecraft:overworld",1,64,2,"inventory snapshot","",List.of());}
    @Test void renameKeepsIdentityInventoryPersonalityAndAttributedRelationships() throws Exception {
        var p=profile();UUID other=UUID.randomUUID();
        p=p.remember(new ResidentProfile.Memory(other,"feelings","I admire their patience","personal opinion",10));
        var edited=p.edit("River",null,true,null,null);var repo=new ResidentRepository(dir.resolve("residents.json"));repo.save(List.of(edited));
        var restored=repo.load().getFirst();
        assertEquals(p.id(),restored.id());assertEquals("River",restored.name());assertEquals(p.persona(),restored.persona());
        assertEquals(p.body(),restored.body());assertEquals(other,restored.memories().getFirst().subject());
        assertEquals("personal opinion",restored.memories().getFirst().source());
    }
    @Test void boundedMemoriesCanReviseOpinionsWithoutEditingOthers() {
        var p=profile();UUID other=UUID.randomUUID();
        p=p.remember(new ResidentProfile.Memory(other,"opinion","I like them","self",1));
        p=p.remember(new ResidentProfile.Memory(other,"opinion","I distrust them after the argument","self",2));
        assertEquals(1,p.memories().size());
        for(int i=0;i<300;i++)p=p.remember(new ResidentProfile.Memory(other,"event"+i,"remembered experience","self",i));
        assertEquals(256,p.memories().size());
    }
    @Test void corruptedOrUnknownDataIsNeverSilentlyImported() throws Exception {
        Path file=dir.resolve("residents.json");Files.writeString(file,"{broken");var repo=new ResidentRepository(file);
        assertThrows(java.io.IOException.class,repo::load);assertEquals("{broken",Files.readString(file));
        Files.writeString(file,"{\"version\":99,\"residents\":[]}");assertThrows(java.io.IOException.class,repo::load);
    }
    @Test void duplicateIdentitiesAndUnboundedSettingsAreRejected() throws Exception {
        var p=profile();var repo=new ResidentRepository(dir.resolve("residents.json"));repo.save(List.of(p,p));
        assertThrows(java.io.IOException.class,repo::load);
        assertThrows(IllegalArgumentException.class,()->new ResidentSettings(1000,1,100,100000,"deepseek","deepseek-flash",""));
    }
}

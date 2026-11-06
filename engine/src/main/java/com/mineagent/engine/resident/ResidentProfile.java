package com.mineagent.engine.resident;

import com.mineagent.engine.persona.PersonaProfile;
import java.util.*;

/** Durable identity and personal history. Display names never serve as database keys. */
public record ResidentProfile(UUID id, String name, Persona persona, String biography,
                              boolean paused, String dimension, int x, int y, int z,
                              String body, String skinPlayer, List<Memory> memories) {
    public ResidentProfile {
        Objects.requireNonNull(id);
        if (name == null || !name.matches("[A-Za-z][A-Za-z0-9_]{0,31}"))
            throw new IllegalArgumentException("Name needs 1-32 letters, digits or underscores");
        persona = persona == null ? Persona.random() : persona;
        biography = bounded(biography, 4000);
        dimension = dimension == null ? "minecraft:overworld" : dimension;
        body = body == null ? "" : body;
        skinPlayer = skinPlayer == null ? "" : skinPlayer;
        memories = memories == null ? List.of() : memories.stream().filter(Objects::nonNull).toList();
        if (memories.size() > 256) memories = List.copyOf(memories.subList(memories.size()-256, memories.size()));
    }
    public record Persona(float openness, float conscientiousness, float extraversion,
                          float agreeableness, float neuroticism, List<String> traits) {
        public Persona {
            for (float v : new float[]{openness,conscientiousness,extraversion,agreeableness,neuroticism})
                if (!Float.isFinite(v) || v < 0 || v > 1) throw new IllegalArgumentException("Traits must be 0-1");
            traits = traits == null ? List.of() : traits.stream().limit(16).map(s -> bounded(s,120)).toList();
        }
        public static Persona random() {
            var p = PersonaProfile.random();
            return new Persona(p.openness(),p.conscientiousness(),p.extraversion(),p.agreeableness(),p.neuroticism(),List.of(p.traits()));
        }
        public PersonaProfile toProfile() { return new PersonaProfile(openness,conscientiousness,extraversion,agreeableness,neuroticism,traits.toArray(String[]::new)); }
    }
    /** Opinions are attributed assertions, not globally authoritative relationship states. */
    public record Memory(UUID subject, String kind, String text, String source, long tick) {
        public Memory { kind=bounded(kind,64);text=bounded(text,1000);source=bounded(source,120); }
    }
    public ResidentProfile remember(Memory memory) {
        var list = new ArrayList<>(memories);
        // Replace the resident's own current note for this subject/kind, retaining other experiences.
        list.removeIf(m -> Objects.equals(m.subject(),memory.subject()) && m.kind().equals(memory.kind()));
        list.add(memory);
        return new ResidentProfile(id,name,persona,biography,paused,dimension,x,y,z,body,skinPlayer,list);
    }
    public ResidentProfile edit(String newName, String bio, Boolean pause, Persona personality, String skin) {
        return new ResidentProfile(id,newName==null?name:newName,personality==null?persona:personality,
                bio==null?biography:bio,pause==null?paused:pause,dimension,x,y,z,body,skin==null?skinPlayer:skin,memories);
    }
    public ResidentProfile snapshot(String dim,int px,int py,int pz,String state) {
        return new ResidentProfile(id,name,persona,biography,paused,dim,px,py,pz,state,skinPlayer,memories);
    }
    static String bounded(String s,int max) { s=s==null?"":s.trim();return s.substring(0,Math.min(max,s.length())); }
}

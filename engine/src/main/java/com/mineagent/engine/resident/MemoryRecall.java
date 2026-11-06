package com.mineagent.engine.resident;
import java.util.*;
/** Retrieval keeps older relevant promises/relationships available instead of only appending recent notes. */
public final class MemoryRecall {
    private MemoryRecall(){}
    public static List<ResidentProfile.Memory> select(List<ResidentProfile.Memory> notes,String query,Set<UUID> nearby,int limit){
        var terms=Arrays.stream((query==null?"":query).toLowerCase(Locale.ROOT).split("[^a-z0-9_]+"))
                .filter(s->s.length()>2).limit(20).toList();
        var order=new ArrayList<Integer>();for(int i=0;i<notes.size();i++)order.add(i);
        order.sort(Comparator.<Integer>comparingDouble(i->{
            var m=notes.get(i);String content=(m.kind()+" "+m.text()).toLowerCase(Locale.ROOT);
            double score=(double)i/Math.max(1,notes.size());
            if(m.subject()!=null&&nearby.contains(m.subject()))score+=3;
            if(terms.stream().anyMatch(content::contains))score+=4;
            // Free-form text is never assigned a mutual relationship state. This only prioritizes recall.
            if(content.matches("(?s).*(promise|boundary|avoid|stay away|do not build|married|relationship|project|goal).*"))score+=1.5;
            return -score;
        }).thenComparing(Comparator.reverseOrder()));
        return order.stream().limit(Math.max(1,Math.min(32,limit))).map(notes::get).toList();
    }
}

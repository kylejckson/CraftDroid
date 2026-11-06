package com.mineagent.engine.protection;

import java.util.*;

/** Inclusive, dimension-specific cuboids. Overlap combines denials; no allow can override a deny. */
public final class RegionRules {
    public enum Action { ENTER, BREAK, PLACE, USE }
    public record Region(String name,String dimension,int minX,int minY,int minZ,int maxX,int maxY,int maxZ,Set<Action> denied) {
        public Region {
            if(name==null||!name.matches("[A-Za-z0-9_-]{1,48}")||dimension==null||dimension.length()>128)
                throw new IllegalArgumentException("Invalid region identity");
            int a=minX,b=minY,c=minZ;minX=Math.min(a,maxX);maxX=Math.max(a,maxX);
            minY=Math.min(b,maxY);maxY=Math.max(b,maxY);minZ=Math.min(c,maxZ);maxZ=Math.max(c,maxZ);
            denied=Set.copyOf(denied);if(denied.isEmpty())throw new IllegalArgumentException("Select at least one denied action");
        }
        public boolean contains(String dim,int x,int y,int z) {return dimension.equals(dim)&&x>=minX&&x<=maxX&&y>=minY&&y<=maxY&&z>=minZ&&z<=maxZ;}
    }
    private volatile List<Region> regions=List.of();
    public List<Region> regions(){return regions;}
    public synchronized void put(Region region){var list=new ArrayList<>(regions);list.removeIf(r->r.name().equals(region.name()));if(list.size()>=128)throw new IllegalArgumentException("Region limit reached");list.add(region);regions=List.copyOf(list);}
    public synchronized boolean remove(String name){var list=new ArrayList<>(regions);boolean removed=list.removeIf(r->r.name().equals(name));regions=List.copyOf(list);return removed;}
    public boolean allows(String dimension,int x,int y,int z,Action action){return regions.stream().noneMatch(r->r.denied().contains(action)&&r.contains(dimension,x,y,z));}

    /** Immutable policy metadata may be read by the model thread without touching the world. */
    public String describeNearby(String dimension,int x,int y,int z,int radius,int limit){
        var nearby=regions.stream().filter(r->r.dimension().equals(dimension)&&distance(r,x,y,z)<=Math.max(0,Math.min(128,radius)))
                .sorted(Comparator.comparingLong((Region r)->distance(r,x,y,z)).thenComparing(Region::name))
                .limit(Math.max(0,Math.min(8,limit))).toList();
        if(nearby.isEmpty())return "";
        var out=new StringBuilder("Nearby mandatory OP regions in "+dimension+" (inclusive coordinates; all regions remain executor-enforced):\n");
        for(var r:nearby)out.append(r.name()).append(" x=").append(r.minX()).append("..").append(r.maxX())
                .append(" y=").append(r.minY()).append("..").append(r.maxY()).append(" z=").append(r.minZ()).append("..").append(r.maxZ())
                .append(" denied=").append(r.denied().stream().map(Enum::name).sorted().toList()).append('\n');
        return out.toString();
    }
    private static long distance(Region r,int x,int y,int z){
        return Math.max(gap(x,r.minX(),r.maxX()),Math.max(gap(y,r.minY(),r.maxY()),gap(z,r.minZ(),r.maxZ())));
    }
    private static long gap(int value,int min,int max){return Math.max(0L,Math.max((long)min-value,(long)value-max));}
}

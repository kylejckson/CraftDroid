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
}

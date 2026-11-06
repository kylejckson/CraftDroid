package com.mineagent.engine.vision;

/** Immutable bounded world snapshot. Zero is air; opaque ARGB values are simplified materials. */
public record VoxelScene(int size,int[] colors,double eyeX,double eyeY,double eyeZ,
                         float yaw,float pitch,int skyColor,boolean raining) {
    public VoxelScene {
        if(size<4||size>32||colors.length!=size*size*size)throw new IllegalArgumentException("Invalid scene bounds");
        colors=colors.clone();
    }
    public int color(int x,int y,int z){return colors[(y*size+z)*size+x];}
    @Override public int[] colors(){return colors.clone();}
}

package com.mineagent.engine.vision;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;

/** Pure CPU perspective ray casting; no Minecraft client, GPU, external process or human camera. */
public final class SoftwareSceneRenderer {
    private SoftwareSceneRenderer() {}
    public static byte[] render(VoxelScene scene,int width,int height) throws IOException {
        if(width<32||width>320||height<32||height>240)throw new IllegalArgumentException("Image resolution out of bounds");
        var image=new BufferedImage(width,height,BufferedImage.TYPE_INT_RGB);
        double yaw=Math.toRadians(scene.yaw()),pitch=Math.toRadians(scene.pitch());
        double fx=-Math.sin(yaw)*Math.cos(pitch),fy=-Math.sin(pitch),fz=Math.cos(yaw)*Math.cos(pitch);
        double rx=Math.cos(yaw),rz=Math.sin(yaw);
        double ux=-Math.sin(yaw)*Math.sin(pitch),uy=Math.cos(pitch),uz=Math.cos(yaw)*Math.sin(pitch);
        double scale=Math.tan(Math.toRadians(70)/2),aspect=(double)width/height;
        for(int py=0;py<height;py++)for(int px=0;px<width;px++) {
            double u=(2*(px+.5)/width-1)*scale*aspect,v=(1-2*(py+.5)/height)*scale;
            double dx=fx+rx*u+ux*v,dy=fy+uy*v,dz=fz+rz*u+uz*v;
            double norm=Math.sqrt(dx*dx+dy*dy+dz*dz);dx/=norm;dy/=norm;dz/=norm;
            int color=trace(scene,dx,dy,dz);
            if(scene.raining()&&(px*13+py*7)%97==0)color=blend(color,0x8aa4b8,.35);
            image.setRGB(px,py,color);
        }
        var bytes=new ByteArrayOutputStream();if(!ImageIO.write(image,"png",bytes))throw new IOException("PNG encoder unavailable");return bytes.toByteArray();
    }
    private static int trace(VoxelScene s,double dx,double dy,double dz) {
        int x=(int)Math.floor(s.eyeX()),y=(int)Math.floor(s.eyeY()),z=(int)Math.floor(s.eyeZ());
        int sx=dx>=0?1:-1,sy=dy>=0?1:-1,sz=dz>=0?1:-1;
        double tx=boundary(s.eyeX(),dx),ty=boundary(s.eyeY(),dy),tz=boundary(s.eyeZ(),dz);
        double ax=Math.abs(1/dx),ay=Math.abs(1/dy),az=Math.abs(1/dz),distance=0;
        int face=1;
        for(int steps=0;steps<100;steps++) {
            if(x<0||y<0||z<0||x>=s.size()||y>=s.size()||z>=s.size())return s.skyColor();
            int c=s.color(x,y,z);
            if(c!=0) {
                double shade=face==1?1:face==0?.8:.65;
                int shaded=((int)(((c>>16)&255)*shade)<<16)|((int)(((c>>8)&255)*shade)<<8)|(int)((c&255)*shade);
                return blend(shaded,s.skyColor(),Math.min(.65,distance/48));
            }
            if(tx<=ty&&tx<=tz){distance=tx;tx+=ax;x+=sx;face=0;}
            else if(ty<=tz){distance=ty;ty+=ay;y+=sy;face=1;}
            else{distance=tz;tz+=az;z+=sz;face=2;}
        }
        return s.skyColor();
    }
    private static double boundary(double origin,double direction) {
        if(Math.abs(direction)<1e-12)return Double.POSITIVE_INFINITY;
        return ((direction>0?Math.floor(origin)+1:Math.floor(origin))-origin)/direction;
    }
    private static int blend(int a,int b,double t){int out=0;for(int shift:new int[]{0,8,16})out|=((int)(((a>>shift)&255)*(1-t)+((b>>shift)&255)*t))<<shift;return out;}
}

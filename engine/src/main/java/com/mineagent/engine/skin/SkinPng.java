package com.mineagent.engine.skin;

import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.ImageIO;
import java.util.*;

/** Canonical tiny PNGs strip metadata and reject oversized or malformed textures before decoding. */
public final class SkinPng {
    private SkinPng() {}
    public static byte[] canonical(byte[] bytes) throws IOException {
        if(bytes==null||bytes.length>131072)throw new IOException("Skin exceeds 128 KiB");
        try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw new IOException("Invalid PNG");
            var reader=readers.next();
            try {
                if(!reader.getFormatName().equalsIgnoreCase("png"))throw new IOException("Expected PNG");
                reader.setInput(input);int width=reader.getWidth(0),height=reader.getHeight(0);
                if(width!=64||(height!=64&&height!=32))throw new IOException("Skin must be 64x64 or 64x32");
                var image=reader.read(0);var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);
                if(out.size()>32768)throw new IOException("Canonical skin exceeds 32 KiB");return out.toByteArray();
            }finally{reader.dispose();}
        }
    }
    /** Reproducible procedural clothing, hair and face colors for each immutable identity. */
    public static byte[] generated(UUID id) throws IOException {
        Random r=new Random(id.getMostSignificantBits()^id.getLeastSignificantBits());
        int[] skin={0xfff1c7a5,0xffd59e78,0xffad7652,0xff805235,0xff573921};
        int flesh=skin[r.nextInt(skin.length)],shirt=0xff000000|java.awt.Color.HSBtoRGB(r.nextFloat(),.6f,.75f),pants=0xff000000|java.awt.Color.HSBtoRGB(r.nextFloat(),.5f,.35f);
        int hair=0xff000000|r.nextInt(0x776655);var image=new BufferedImage(64,64,BufferedImage.TYPE_INT_ARGB);
        var g=image.createGraphics();
        try {
            g.setColor(new java.awt.Color(flesh,true));g.fillRect(0,0,32,16);
            g.setColor(new java.awt.Color(shirt,true));g.fillRect(16,16,24,16);g.fillRect(40,16,16,16);g.fillRect(32,48,16,16);
            g.setColor(new java.awt.Color(pants,true));g.fillRect(0,16,16,16);g.fillRect(16,48,16,16);
            g.setColor(new java.awt.Color(flesh,true));g.fillRect(40,24,16,8);g.fillRect(32,56,16,8);
            g.setColor(new java.awt.Color(hair,true));g.fillRect(0,0,32,8);g.fillRect(8,8,8,2);g.fillRect(24,8,8,8);
            g.setColor(new java.awt.Color(0xffeeeeee,true));g.fillRect(9,11,2,1);g.fillRect(13,11,2,1);
            g.setColor(new java.awt.Color(0xff203c58,true));g.fillRect(10,11,1,1);g.fillRect(13,11,1,1);
            g.setColor(new java.awt.Color(0xff87594b,true));g.fillRect(11,14,2,1);
        }finally{g.dispose();}
        var out=new ByteArrayOutputStream();ImageIO.write(image,"png",out);return out.toByteArray();
    }
}

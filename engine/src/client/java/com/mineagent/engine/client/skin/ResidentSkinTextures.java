package com.mineagent.engine.client.skin;

import com.google.gson.JsonParser;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.resources.PlayerSkin;
import net.minecraft.resources.ResourceLocation;
import java.io.ByteArrayInputStream;
import java.util.*;

/** Cosmetic client texture receiver. Agent perception/rendering remains entirely server-side. */
public final class ResidentSkinTextures {
    private record Pending(long version,String[] parts) {}
    private static final Map<UUID,Pending> PENDING=new HashMap<>();
    private static final Map<UUID,PlayerSkin> SKINS=new HashMap<>();
    private ResidentSkinTextures() {}
    public static PlayerSkin get(UUID id){return SKINS.get(id);}
    public static void receive(UUID id,String json) {
        try {
            var p=JsonParser.parseString(json).getAsJsonObject();int index=p.get("part").getAsInt(),count=p.get("count").getAsInt();long version=p.get("version").getAsLong();String data=p.get("data").getAsString();
            if(count<1||count>16||index<0||index>=count||data.length()>3000||(!PENDING.containsKey(id)&&PENDING.size()>=32)||(!SKINS.containsKey(id)&&SKINS.size()>=32))return;
            var current=PENDING.get(id);
            if(current!=null&&current.version()>version)return;
            if(current==null||current.version()!=version||current.parts().length!=count){current=new Pending(version,new String[count]);PENDING.put(id,current);}
            current.parts()[index]=data;for(String part:current.parts())if(part==null)return;
            byte[] bytes=Base64.getDecoder().decode(String.join("",current.parts()));
            bytes=com.mineagent.engine.skin.SkinPng.canonical(bytes);
            var image=NativeImage.read(new ByteArrayInputStream(bytes));
            var location=ResourceLocation.fromNamespaceAndPath("mineagent","resident_skin/"+id);
            Minecraft.getInstance().getTextureManager().register(location,new DynamicTexture(image));
            SKINS.put(id,new PlayerSkin(location,null,null,null,PlayerSkin.Model.WIDE,false));PENDING.remove(id);
        }catch(Exception e){PENDING.remove(id);System.err.println("[CraftDroid] Invalid resident skin update rejected");}
    }
    public static void remove(UUID id){var skin=SKINS.remove(id);if(skin!=null)Minecraft.getInstance().getTextureManager().release(skin.texture());PENDING.remove(id);}
    public static void clear(){for(var skin:SKINS.values())Minecraft.getInstance().getTextureManager().release(skin.texture());SKINS.clear();PENDING.clear();}
}

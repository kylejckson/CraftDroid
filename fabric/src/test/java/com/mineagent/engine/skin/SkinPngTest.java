package com.mineagent.engine.skin;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.URI;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
class SkinPngTest {
    @Test void identitiesGetDistinctRepeatableValidSkins() throws Exception {
        UUID id=UUID.fromString("00000000-0000-0000-0000-000000000001");byte[] skin=SkinPng.generated(id);
        assertArrayEquals(skin,SkinPng.generated(id));assertFalse(java.util.Arrays.equals(skin,SkinPng.generated(UUID.randomUUID())));
        var image=ImageIO.read(new ByteArrayInputStream(SkinPng.canonical(skin)));assertEquals(64,image.getWidth());assertEquals(64,image.getHeight());
    }
    @Test void malformedHugeAndWrongDimensionImagesAreRejected() throws Exception {
        assertThrows(IOException.class,()->SkinPng.canonical(new byte[131073]));assertThrows(IOException.class,()->SkinPng.canonical(new byte[]{1,2}));
        var bytes=new ByteArrayOutputStream();ImageIO.write(new BufferedImage(100,100,BufferedImage.TYPE_INT_RGB),"png",bytes);
        assertThrows(IOException.class,()->SkinPng.canonical(bytes.toByteArray()));
    }
    @Test void remoteLinksCannotTargetLocalServices() {
        for(String url:java.util.List.of("http://example.com/a.png","https://127.0.0.1/a.png","https://[::1]/a.png","https://169.254.169.254/a.png","https://user:pass@example.com/a.png","https://example.com:8443/a.png"))
            assertThrows(IOException.class,()->ResidentSkins.validatePublicUrl(URI.create(url)),url);
    }
}

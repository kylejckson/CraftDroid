package com.mineagent.engine.vision;
import org.junit.jupiter.api.Test;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import static org.junit.jupiter.api.Assertions.*;
class SoftwareSceneRendererTest {
    @Test void rayCastSeesWallButCannotSeeHiddenRoomBehindIt() throws Exception {
        int n=12;int[] a=new int[n*n*n];int sky=0x88bbff;
        for(int y=0;y<n;y++)for(int x=0;x<n;x++)a[(y*n+7)*n+x]=0xffff0000;
        var scene=new VoxelScene(n,a,6.5,6.5,2.5,0,0,sky,false);
        byte[] first=SoftwareSceneRenderer.render(scene,96,64);
        var image=ImageIO.read(new ByteArrayInputStream(first));assertEquals(96,image.getWidth());
        int center=image.getRGB(48,32);assertTrue(((center>>16)&255)>((center>>8)&255));
        // A different hidden block cannot change the rendered picture.
        a[(6*n+9)*n+6]=0xff00ff00;
        assertArrayEquals(first,SoftwareSceneRenderer.render(new VoxelScene(n,a,6.5,6.5,2.5,0,0,sky,false),96,64));
    }
    @Test void turningChangesPerspectiveAndSnapshotsOwnTheirData() throws Exception {
        int n=8;int[] colors=new int[n*n*n];for(int y=0;y<n;y++)for(int x=0;x<n;x++)colors[(y*n+6)*n+x]=0xffff0000;
        var front=new VoxelScene(n,colors,4.5,4.5,2.5,0,0,0x88bbff,false);colors[0]=0xffffffff;
        assertEquals(0,front.color(0,0,0));
        assertFalse(java.util.Arrays.equals(SoftwareSceneRenderer.render(front,64,48),SoftwareSceneRenderer.render(new VoxelScene(n,colors,4.5,4.5,2.5,180,0,0x88bbff,false),64,48)));
        assertThrows(IllegalArgumentException.class,()->SoftwareSceneRenderer.render(front,8000,8000));
    }
}

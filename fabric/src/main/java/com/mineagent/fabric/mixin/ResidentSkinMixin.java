package com.mineagent.fabric.mixin;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.resources.PlayerSkin;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(AbstractClientPlayer.class)
public abstract class ResidentSkinMixin {
    @Inject(method="getSkin",at=@At("HEAD"),cancellable=true)
    private void craftdroidSkin(CallbackInfoReturnable<PlayerSkin> result) {
        var player=(AbstractClientPlayer)(Object)this;
        var skin=com.mineagent.engine.client.skin.ResidentSkinTextures.get(player.getUUID());
        if(skin!=null)result.setReturnValue(skin);
    }
}

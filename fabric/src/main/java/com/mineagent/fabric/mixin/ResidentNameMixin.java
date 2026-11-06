package com.mineagent.fabric.mixin;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Keep the immutable protocol login while exposing the resident's synchronized display name. */
@Mixin(Player.class)
public abstract class ResidentNameMixin {
    @Inject(method="getName",at=@At("HEAD"),cancellable=true)
    private void craftdroid$residentName(CallbackInfoReturnable<Component> result){
        var player=(Player)(Object)this;
        if(player.getCustomName()!=null&&player.getGameProfile().getName().matches("CD_[a-f0-9]{13}"))
            result.setReturnValue(player.getCustomName());
    }
}

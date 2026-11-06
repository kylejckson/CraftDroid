package com.mineagent.neoforge.mixin;
import com.mineagent.engine.protection.PropertyLedger;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.*;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Mixin(ServerPlayerGameMode.class)
public abstract class BlockBreakPropertyMixin {
    @Shadow @Final protected ServerPlayer player;
    @Inject(method="destroyBlock",at=@At("RETURN"))
    private void craftdroid$forgetBrokenBlock(BlockPos pos,CallbackInfoReturnable<Boolean> result){
        if(result.getReturnValue())PropertyLedger.forget(player.level(),pos);
    }
}

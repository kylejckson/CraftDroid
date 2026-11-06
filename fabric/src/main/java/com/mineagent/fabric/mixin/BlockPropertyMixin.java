package com.mineagent.fabric.mixin;

import com.mineagent.engine.MineAgentEngine;
import com.mineagent.engine.protection.PropertyLedger;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BlockItem.class)
public abstract class BlockPropertyMixin {
    @Inject(method="place",at=@At("HEAD"),cancellable=true)
    private void craftdroid$checkPlacement(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> result){
        if(context.getPlayer() instanceof ServerPlayer player&&MineAgentEngine.isCompanionPlayer(player.getUUID())){
            var pos=context.getClickedPos();var block=((BlockItem)(Object)this).getBlock();
            boolean allowed=PropertyLedger.mayPlace(player,pos);
            if(block instanceof net.minecraft.world.level.block.DoorBlock||block instanceof net.minecraft.world.level.block.DoublePlantBlock)
                allowed&=PropertyLedger.mayPlace(player,pos.above());
            if(block instanceof net.minecraft.world.level.block.BedBlock)
                allowed&=PropertyLedger.mayPlace(player,pos.relative(context.getHorizontalDirection()));
            if(!allowed)result.setReturnValue(InteractionResult.FAIL);
        }
    }
    @Inject(method="place",at=@At("RETURN"))
    private void craftdroid$rememberPlacement(BlockPlaceContext context,CallbackInfoReturnable<InteractionResult> result){
        if(result.getReturnValue().consumesAction()&&context.getPlayer() instanceof ServerPlayer player
                &&context.getLevel().getBlockState(context.getClickedPos()).is(((BlockItem)(Object)this).getBlock()))
            PropertyLedger.placed(player,context.getClickedPos());
    }
}

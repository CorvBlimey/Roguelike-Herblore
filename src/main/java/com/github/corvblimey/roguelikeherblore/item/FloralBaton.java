package com.github.corvblimey.roguelikeherblore.item;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class FloralBaton extends Item {

    public FloralBaton(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(final ItemUsageContext context) {
        final World world = context.getWorld();
        final BlockPos blockPos = context.getBlockPos();
        final BlockState targetState = world.getBlockState(blockPos);
        if (targetState.getBlock() instanceof ForageableBlock) {
            final ForageableBlock targetBlock = (ForageableBlock)targetState.getBlock();
            if (world instanceof ServerWorld) {
                toggleBearingIfAble(world.getBlockState(blockPos), (ServerWorld)world, blockPos);
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    public static void toggleBearingIfAble(BlockState state, final ServerWorld world, final BlockPos pos) {
        if (!state.get(ForageableBlock.FERTILE)){
            world.setBlockState(pos, state.with(ForageableBlock.BEARING, !state.get(ForageableBlock.BEARING)));
        }
    }
}
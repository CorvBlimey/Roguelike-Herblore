package com.github.corvblimey.roguelikeherblore.item;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.github.corvblimey.roguelikeherblore.feature.PlantClusterFeature;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class WildInoculant extends Item {

    public WildInoculant(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(final ItemUsageContext context) {
        final World world = context.getWorld();
        final BlockPos blockPos = context.getBlockPos();
        if (useOnInoculable(context.getStack(), world, blockPos)) {
            if (!world.isClient) {
                ((ServerWorld) world).spawnParticles(ParticleTypes.CRIT, blockPos.getX(), blockPos.getY()+1, blockPos.getZ(), 6, 0, 0, 0, 0);
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    public static boolean useOnInoculable(final ItemStack stack, final World world, final BlockPos pos) {
        if (world.isClient) {return false;}
        final BlockState targetState = world.getBlockState(pos);
        if (targetState.getBlock() instanceof ForageableBlock) {
            if (!targetState.get(ForageableBlock.FERTILE) || !targetState.get(ForageableBlock.BEARING)) {
                inoculate(world.getBlockState(pos), (ServerWorld)world, pos);
                stack.decrement(1);
                return true;
            }
        }
        return false;
    }

    public static void inoculate(BlockState state, final ServerWorld world, final BlockPos pos) {
        world.setBlockState(pos, state.with(ForageableBlock.FERTILE, true).with(ForageableBlock.BEARING, true));
    }
}
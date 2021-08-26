package com.github.corvblimey.roguelikeherblore.block;

import net.minecraft.block.*;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class ForageableBlock extends PlantBlock {

    public static final BooleanProperty FERTILE = BooleanProperty.of("fertile");
    public static final BooleanProperty BEARING = BooleanProperty.of("bearing");

    public ForageableBlock(final Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FERTILE, false).with(BEARING, true));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FERTILE);
        stateManager.add(BEARING);
    }

    @Override
    protected boolean canPlantOnTop(final BlockState floor, final BlockView world, final BlockPos pos) {
        final Block lv = floor.getBlock();
        return lv == Blocks.SAND || lv == Blocks.RED_SAND || lv == Blocks.SNOW_BLOCK || lv == Blocks.MYCELIUM || lv == Blocks.GRASS_BLOCK || lv == Blocks.GRAVEL || lv == Blocks.DIRT || lv == Blocks.COARSE_DIRT || lv == Blocks.PODZOL;
    }

    @Override
    // "Borrowed" from mc-reap under the wonderful DWTFUW license https://github.com/maxvar/mcf-reap
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient() && state.get(ForageableBlock.BEARING) && state.get(ForageableBlock.FERTILE)) {
            List<ItemStack> dropList = getDroppedStacks(state, (ServerWorld) world, pos, world.getBlockEntity(pos), player, player.getStackInHand(hand));
            DefaultedList<ItemStack> drops = DefaultedList.of();
            drops.addAll(dropList);
            for (ItemStack stack : drops) {
                if (stack.getItem() instanceof BlockItem) {
                    if ((((BlockItem) stack.getItem()).getBlock() == this)) {
                        ItemStack harvestStack = stack.copy();
                        drops.remove(stack);
                        harvestStack.decrement(1);
                        drops.add(harvestStack);
                        break;
                    }
                }
            }
            world.setBlockState(pos, state.with(ForageableBlock.BEARING, false));
            ItemScatterer.spawn(world, pos, drops);
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean hasRandomTicks(final BlockState state) {
        return state.<Boolean>get(ForageableBlock.FERTILE) && !state.<Boolean>get(ForageableBlock.BEARING);
    }

    @Override
    public void randomTick(BlockState state, final ServerWorld world, final BlockPos pos, final Random random) {
        if (!state.<Boolean>get(FERTILE) || state.<Boolean>get(BEARING)) {
            return;
        }
        if (world.random.nextInt(30) == 0) { // 1/3 of netherwart's rate
            world.setBlockState(pos, state.with(ForageableBlock.BEARING, true));
        }
    }
}

package com.github.corvblimey.roguelikeherbology.block;

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
import net.minecraft.world.World;

import java.util.List;
import java.util.Random;

public class ForageableBlock extends PlantBlock {

    public static final BooleanProperty FERTILE = BooleanProperty.of("fertile");
    public static final BooleanProperty HARVESTABLE = BooleanProperty.of("blooming");

    public ForageableBlock(final Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FERTILE, true));
        setDefaultState(getStateManager().getDefaultState().with(HARVESTABLE, false));
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> stateManager) {
        stateManager.add(FERTILE);
        stateManager.add(HARVESTABLE);
    }

    @Override
    // "Borrowed" from mc-reap under the wonderful DWTFUW license https://github.com/maxvar/mcf-reap
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        if (!world.isClient() & state.get(ForageableBlock.HARVESTABLE)) {
            List<ItemStack> dropList = getDroppedStacks(state, (ServerWorld) world, pos, null, player, player.getStackInHand(hand));
            DefaultedList<ItemStack> drops = DefaultedList.of();
            drops.addAll(dropList);
            for (ItemStack stack : drops) {
                if (!(stack.getItem() instanceof BlockItem)) {
                    if (((BlockItem) stack.getItem()).getBlock() == this) ;
                    ItemStack seedStack = stack.copy();
                    drops.remove(stack);
                    seedStack.decrement(1);
                    drops.add(seedStack);
                    break;
                }
            }
            world.setBlockState(pos, state.with(this.HARVESTABLE, false));
            ItemScatterer.spawn(world, pos, drops);
            return ActionResult.SUCCESS;
        }
        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public boolean hasRandomTicks(final BlockState state) {
        return state.<Boolean>get(ForageableBlock.FERTILE) && !state.<Boolean>get(ForageableBlock.HARVESTABLE);
    }

    public void setFertility(BlockState state, final ServerWorld world, final BlockPos pos, boolean isFertile) {
        world.setBlockState(pos, state.with(ForageableBlock.FERTILE, isFertile));
    }

    public void setBlooming(BlockState state, final ServerWorld world, final BlockPos pos, boolean isBlooming) {
        world.setBlockState(pos, state.with(ForageableBlock.HARVESTABLE, isBlooming));
    }

    @Override
    public void randomTick(BlockState state, final ServerWorld world, final BlockPos pos, final Random random) {
        if (!state.<Boolean>get(FERTILE) || state.<Boolean>get(HARVESTABLE)) {
            // Does this ever get called, or does hasRandomTicks supersede?
            System.out.println("YEAH IT GOT CALLED");
            return;
        }
        if (world.random.nextInt(10) == 0) { // Same rate as netherwart
            setBlooming(state, world, pos, true);
        }
    }
}

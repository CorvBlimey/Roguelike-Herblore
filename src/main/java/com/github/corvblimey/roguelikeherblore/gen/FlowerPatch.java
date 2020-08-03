package com.github.corvblimey.roguelikeherblore.gen;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.Heightmap;

import java.util.Random;

public abstract class FlowerPatch {

    private static final int genChance = 15; //The percent chance that any given call to spawn flowers will do so
    private static final int density = 12; // The rough percent chance of a block in a spawn area containing a plant
    private static final int bearingChance = 25; // The percent chance of a spawned plant being harvestable
    private static final int minRadius = 2;
    private static final int maxRadius = 5;
    private static final double chanceOfPlant = 1.0 / (100 - density);

    // Concepts lovingly borrowed from Plants https://github.com/Shadows-of-Fire/Plants
    public static void genFlowers(ChunkPos initialChunkPos, ServerWorld world, BlockState flowerToGen) {
        BlockPos pos = initialChunkPos.getCenterBlockPos();
        final int radius = world.getRandom().nextInt(maxRadius - minRadius) + minRadius;
        if (world.getRandom().nextInt(100) < genChance) {
            // Note that we may add or remove a few plants depending on the size of the circle, just for variety
            final int numToGen = (int) Math.pow(chanceOfPlant * (radius * 3.14), 2) + getIntInTwoSidedRange(world.getRandom(), radius - 1);
            final int numTriesAllowed = numToGen * 3;
            int successes = 0;
            for (int tries = 0; tries < numTriesAllowed; tries++) {
                if (successes > numToGen) {
                    break;
                }
                int xPos = pos.getX() + getIntInTwoSidedRange(world.getRandom(), radius);
                int zPos = pos.getZ() + getIntInTwoSidedRange(world.getRandom(), radius);
                // TODO: check difference between WORLD_SURFACE and WORLD_SURFACE_WG
                int yPos = world.getTopPosition(Heightmap.Type.WORLD_SURFACE, new BlockPos(xPos, 0, zPos)).getY();
                BlockPos targetPos = new BlockPos(xPos, yPos, zPos);
                if (groundSupportsPlantPlacement(world, targetPos)) {
                    boolean isBearing = world.random.nextInt(100) < bearingChance;
                    world.setBlockState(targetPos, flowerToGen.with(ForageableBlock.FERTILE, true).with(ForageableBlock.BEARING, isBearing));
                    successes++;
                }
            }
        }
    }

    public static boolean groundSupportsPlantPlacement(ServerWorld world, BlockPos pos) {
        Block groundMaterial = world.getBlockState(pos.down()).getBlock();
        System.out.println("Ground here is " + groundMaterial);
        return true;  //heehoo
    }

    public static int getIntInTwoSidedRange(Random random, int range) {
        /* Inclusive range, btw. So getIntInTwoSidedRange(rand, 4) could get you
         * -4, 4, or anything in between. Used for block offsets. */
        return random.nextInt(range * 2 + 1) - range;
    }
}
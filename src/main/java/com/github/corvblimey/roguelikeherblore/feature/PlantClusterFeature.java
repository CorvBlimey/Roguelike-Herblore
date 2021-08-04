package com.github.corvblimey.roguelikeherblore.feature;

import com.github.corvblimey.roguelikeherblore.RoguelikeHerblore;
import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.mojang.serialization.Codec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.StructureWorldAccess;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.chunk.ChunkGenerator;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.util.FeatureContext;

import java.util.ArrayList;
import java.util.Random;

public class PlantClusterFeature extends Feature<DefaultFeatureConfig> {
    public static final Identifier ID = RoguelikeHerblore.id("plant_cluster");

    public PlantClusterFeature(Codec<DefaultFeatureConfig> config) {
        super(config);
    }

    private static final int density = 14; // Rough percent chance of a block in a spawn area containing a plant
    // The percent chance of a spawned plant being harvestable. Note that nless this is set below 25, the first plant spawned is always harvestable.
    private static final int bearingChance = 30;
    // Length of a side of a spawning patch, sort of. We spawn in patchSize/2 blocks to either direction of a center, so the functional size is odd.
    // Additionally, jungle biomes get a +1 to effective patch size (but no extra plants) to help spawn around all the leaf blocks.
    private static final int minPatchSize = 2;
    private static final int maxPatchSize = 6;  // This would be a (6/2+1)*2=7-block square, 49 total blocks, 6 plants at density=14 (number's somewhat randomized)
    private static final double chanceOfPlant = density*0.01;

    public boolean generate(FeatureContext<DefaultFeatureConfig> context) {
        StructureWorldAccess world = context.getWorld();
        BlockPos pos = context.getOrigin();
        Random random = context.getRandom();
        BlockPos centerPos = pos.add(8, 0, 8);
        Biome.Category category = world.getBiome(centerPos).getCategory();
        // We don't spawn if we don't have any plants registered for the biome
        if (!RoguelikeHerblore.PLANT_SPAWN_BIOMES.containsKey(category)){
            return false;
        }
        // We also try not to overwhelm deserts, so we only spawn anything 2/5s of the time
        if(category == Biome.Category.DESERT && random.nextInt(5)<2){
            return true;
        }
        // Similarly, plains only get half the spawns
        if(category == Biome.Category.DESERT && random.nextInt(2)<1){
            return true;
        }
        // Note we add 1 because nextInt is right-exclusive, 0-inclusive and we want the other way round
        int patchSize = (random.nextInt(maxPatchSize - minPatchSize) + 1 + minPatchSize)/2;
        final BlockState flowerToGen = getFlowerToGen(random, category);
        // Get area of spawning square, calc # plants, add or remove a few for variety
        final int numToGen = (int)(Math.pow(2 * patchSize + 1, 2) * chanceOfPlant) + getIntInTwoSidedRange(random, patchSize/2);
        final int numTriesAllowed = numToGen * 6;  // Completely arbitrary but set it high-ish because jungles are a nightmare
        // Due to jungles being a nightmare, we allow a larger area to be checked for spawning (but not used in calculating numToGen)
        if(category == Biome.Category.JUNGLE){
            patchSize ++;
        }
        int successes = 0;
        for (int tries = 0; tries < numTriesAllowed; tries++) {
            if (successes > numToGen) {
                break;
            }
            int xPos = centerPos.getX() + getIntInTwoSidedRange(random, patchSize);
            int zPos = centerPos.getZ() + getIntInTwoSidedRange(random, patchSize);
            int yPos = world.getTopPosition(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, new BlockPos(xPos, 0, zPos)).getY();
            BlockPos targetPos = new BlockPos(xPos, yPos, zPos);
            boolean isBearing;
            if (groundSupportsPlantPlacement(world, targetPos, category)) {
                if (successes == 0 && bearingChance >= 25) {
                    isBearing = true;  // Guaranteed 1 harvestable per cluster to avoid frustration
                } else {
                    isBearing = random.nextInt(100) < bearingChance;
                }
                world.setBlockState(targetPos, flowerToGen.with(ForageableBlock.FERTILE, true).with(ForageableBlock.BEARING, isBearing), 0);
                successes++;
            }
        }
        return successes > 0;
    }

    public BlockState getFlowerToGen(Random random, Biome.Category category) {
        ArrayList<Block> availablePlants = RoguelikeHerblore.PLANT_SPAWN_BIOMES.get(category);
        return availablePlants.get(random.nextInt(availablePlants.size())).getDefaultState();
    }

    public static boolean groundSupportsPlantPlacement(StructureWorldAccess world, BlockPos pos, Biome.Category category) {
        BlockState groundState = world.getBlockState(pos.down());
        // Universally spawnable
        if(groundState.isOf(Blocks.GRASS_BLOCK)
                || groundState.isOf(Blocks.DIRT)
                || groundState.isOf(Blocks.COARSE_DIRT)
                || groundState.isOf(Blocks.PODZOL)
                || groundState.isOf(Blocks.SNOW_BLOCK)) {
            return true;
        } // Selectively spawnable
        else if(category == Biome.Category.BEACH || category == Biome.Category.DESERT || category == Biome.Category.MESA){
            return groundState.isOf(Blocks.SAND) || groundState.isOf(Blocks.RED_SAND);
        }
        return false;
    }

    public static int getIntInTwoSidedRange(Random random, int range) {
        /* Inclusive range, btw. So getIntInTwoSidedRange(rand, 4) could get you
         * -4, 4, or anything in between. Used for block offsets. */
        return random.nextInt(range * 2 + 1) - range;
    }
}

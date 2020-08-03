package com.github.corvblimey.roguelikeherblore;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.block.Block;
import net.minecraft.client.render.RenderLayer;

public class RoguelikeHerbloreClientInit implements ClientModInitializer {

    @Environment(EnvType.CLIENT)
    @Override
    public void onInitializeClient(){
        for(Block block : RoguelikeHerblore.getMippedBlocks()){
            BlockRenderLayerMap.INSTANCE.putBlock(block, RenderLayer.getCutoutMipped());
        }
    }
}

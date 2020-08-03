package com.github.corvblimey.roguelikeherblore;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.github.corvblimey.roguelikeherblore.item.ForageableFoodItem;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.BlockItem;
import net.minecraft.item.FoodComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.util.Identifier;
import net.minecraft.util.registry.Registry;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;

import static net.minecraft.util.math.MathHelper.ceil;

public class RoguelikeHerblore implements ModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "roguelikeherblore";
    public static final String MOD_NAME = "Roguelike Herblore";
    private static final int maxOffset = 100;
    private static final int defaultHunger = 1;
    private static final float defaultSaturation = 0.1f;
    private static final ArrayList<StatusEffectInstance> forageableEffects = new ArrayList<StatusEffectInstance>();
    /* Each edible points to an entry in effectOffsetRedirect, which itself points to
     * a position in forageableEffects. This lets use keep the plant list and effect list
     * sizes independent; we can add more plants without re-scrambling existing ones
     * (however, we CANNOT do the same for potion effects! That scrambles things!)
     */
    private static int[] effectOffsetRedirect;

    private static final Map<Item, Identifier> ITEMS = new LinkedHashMap<>();
    private static final Map<Block, Identifier> BLOCKS = new LinkedHashMap<>();
    private static final ItemGroup group = ItemGroup.DECORATIONS;
    // We increment this each time we add a new food item. It's how randomization works.
    // This means that the order of item creation matters!!! Use the python script!
    private static int offset = 0;

    // Ignore the "never used" warning, we do this for other folks' sake.
    public static final Block GORGEROOT_BLOCK = generateHarvestBlock("gorgeroot");
    public static final Item GORGEROOT_HARVEST_ITEM = generateHarvestFood("gorgeroot");
    public static final Block BULBFRUIT_BLOCK = generateHarvestBlock("bulbfruit");
    public static final Item BULBFRUIT_HARVEST_ITEM = generateHarvestFood("bulbfruit");
    public static final Block QUEENS_SCEPTER_BLOCK = generateHarvestBlock("queens_scepter");
    public static final Item QUEENS_SCEPTER_HARVEST_ITEM = generateHarvestFood("queens_scepter");
    public static final Block LURANA_BLOCK = generateHarvestBlock("lurana");
    public static final Item LURANA_HARVEST_ITEM = generateHarvestFood("lurana");
    public static final Block MOSS_CURL_BLOCK = generateHarvestBlock("moss_curl");
    public static final Item MOSS_CURL_HARVEST_ITEM = generateHarvestFood("moss_curl");
    public static final Block HONEYBLOOM_BLOCK = generateHarvestBlock("honeybloom");
    public static final Item HONEYBLOOM_HARVEST_ITEM = generateHarvestFood("honeybloom");
    public static final Block FAIRY_BUSH_BLOCK = generateHarvestBlock("fairy_bush");
    public static final Item FAIRY_BUSH_HARVEST_ITEM = generateHarvestFood("fairy_bush");
    public static final Block SHOREBERRY_BLOCK = generateHarvestBlock("shoreberry");
    public static final Item SHOREBERRY_HARVEST_ITEM = generateHarvestFood("shoreberry");
    public static final Block BUTTONCUP_BLOCK = generateHarvestBlock("buttoncup");
    public static final Item BUTTONCUP_HARVEST_ITEM = generateHarvestFood("buttoncup");
    public static final Block PYGMY_CACTUS_BLOCK = generateHarvestBlock("pygmy_cactus");
    public static final Item PYGMY_CACTUS_HARVEST_ITEM = generateHarvestFood("pygmy_cactus");
    public static final Block OOZECAP_BLOCK = generateHarvestBlock("oozecap");
    public static final Item OOZECAP_HARVEST_ITEM = generateHarvestFood("oozecap");
    public static final Block RYMEFLOWER_BLOCK = generateHarvestBlock("rymeflower");
    public static final Item RYMEFLOWER_HARVEST_ITEM = generateHarvestFood("rymeflower");

    public static final Block[] MIPPED_BLOCKS = {GORGEROOT_BLOCK, BULBFRUIT_BLOCK, QUEENS_SCEPTER_BLOCK, LURANA_BLOCK, MOSS_CURL_BLOCK, HONEYBLOOM_BLOCK, FAIRY_BUSH_BLOCK, SHOREBERRY_BLOCK, BUTTONCUP_BLOCK, PYGMY_CACTUS_BLOCK, OOZECAP_BLOCK, RYMEFLOWER_BLOCK};

    @Override
    public void onInitialize() {
        log(Level.INFO, "Initializing");
        forageableEffects.add(new StatusEffectInstance(StatusEffects.ABSORPTION, 1200, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.SLOWNESS, 100, 6));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.INSTANT_HEALTH, 1, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.REGENERATION, 450, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.POISON, 300, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 800, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, 800, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.NAUSEA, 200, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.HEALTH_BOOST, 600, -2));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.SPEED, 1200, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.SPEED, 300, 3));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.HUNGER, 200, 3));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 300, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.SLOW_FALLING, 300, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.JUMP_BOOST, 300, 2));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.REGENERATION, 200, 2));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.WATER_BREATHING, 300, 1));
        forageableEffects.add(new StatusEffectInstance(StatusEffects.RESISTANCE, 300, 2));

        ServerWorldEvents.LOAD.register((server, world) -> {
            // Check first so we don't do this each time someone goes to the Nether/etc.
            if(this.effectOffsetRedirect == null) { setScrambledEffects(world.getSeed());}
        });
        registerAll();
    }

    public static Block[] getMippedBlocks(){
        return MIPPED_BLOCKS;
    }

    private static Item generateHarvestFood(String source_plant, int hunger, float saturation) {
        Item harvest_food =  new ForageableFoodItem(new Item.Settings().group(group).food(new FoodComponent.Builder().hunger(hunger).saturationModifier(saturation).build())).withOffset(offset);
        ITEMS.put(harvest_food, new Identifier(MOD_ID, source_plant+"_harvest_item"));
        offset ++;
        return harvest_food;
    }

    private static Item generateHarvestFood(String source_plant){ return generateHarvestFood(source_plant, defaultHunger, defaultSaturation);}

    private static Block generateHarvestBlock(String source_plant) {
        Block harvest_block = new ForageableBlock(FabricBlockSettings.of(Material.PLANT).noCollision().nonOpaque());
        BLOCKS.put(harvest_block, new Identifier(MOD_ID, source_plant));
        Item harvest_block_item = new BlockItem(harvest_block, new Item.Settings().group(group));
        ITEMS.put(harvest_block_item, new Identifier(MOD_ID, source_plant));
        return harvest_block;
    }

    private static void registerAll() {
        ITEMS.keySet().forEach(item -> Registry.register(Registry.ITEM, ITEMS.get(item), item));
        BLOCKS.keySet().forEach(block -> Registry.register(Registry.BLOCK, BLOCKS.get(block), block));
    }

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }

    public static StatusEffectInstance getForageableEffect(int offset){
        System.out.println("AAAAAAAAAAAAAAAAAAAA"+offset);
        System.out.println("And also: "+effectOffsetRedirect[offset]);
        return forageableEffects.get(effectOffsetRedirect[offset]);
    }

    /* To keep the lengths of the plant and effect lists independent, we "tile"
     * the randomization of the list to meet (or exceed; I'm lazy) the offset.
     * The end result is that if you have 5 effects and a max offset of 12, you get
     * ceil(12/5)*5 effects, and each of the 5 is repeated at least floor(12/5)
     * times. This should help prevent people from getting worlds with nothing but
     * Nausea plants.
     */
    private void setScrambledEffects(long worldSeed){
        int repetitions = ceil((float) maxOffset /forageableEffects.size());
        effectOffsetRedirect = new int[forageableEffects.size()*repetitions];
        for(int i = 0; i < repetitions; i++){
            final int[] effectOffsets = IntStream.rangeClosed(0, forageableEffects.size()-1).toArray();
            scrambleEffectsPerSeed(effectOffsets, worldSeed+i);
            System.arraycopy(effectOffsets, 0, effectOffsetRedirect, i * forageableEffects.size(), forageableEffects.size());
        }
    }

    private static void scrambleEffectsPerSeed(int[] array, long seed)
    {
        int index, temp;
        Random random = new Random(seed);
        for (int i = array.length - 1; i > 0; i--)
        {
            index = random.nextInt(i + 1);
            temp = array[index];
            array[index] = array[i];
            array[i] = temp;
        }
    }
}
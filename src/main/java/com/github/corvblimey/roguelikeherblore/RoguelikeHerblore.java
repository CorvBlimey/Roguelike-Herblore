package com.github.corvblimey.roguelikeherblore;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.github.corvblimey.roguelikeherblore.item.FloralBaton;
import com.github.corvblimey.roguelikeherblore.item.ForageableFoodItem;
import com.github.corvblimey.roguelikeherblore.item.WildInoculant;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.minecraft.block.Block;
import net.minecraft.block.Material;
import net.minecraft.entity.effect.StatusEffect;
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
    private static final ArrayList<StatusEffectGen> forageableEffects = new ArrayList<StatusEffectGen>();
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

    public static final Item WILD_INOCULANT = new WildInoculant(new Item.Settings().group(group));
    public static final Item FLORAL_BATON = new FloralBaton(new Item.Settings().group(group));

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

    public static class StatusEffectGen{
        private final StatusEffect effect;
        private final int tickDuration;
        private final int intensity;
        public StatusEffectGen(StatusEffect effect, int tickDuration, int intensity){
            this.effect = effect;
            this.tickDuration = tickDuration;
            this.intensity = intensity;
        }

        public StatusEffectInstance genEffect(){
            return new StatusEffectInstance(this.effect, this.tickDuration, this.intensity);
        }
    }
    
    @Override
    public void onInitialize() {
        log(Level.INFO, "Initializing");
        forageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 300, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 300, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 450, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 1200, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 300, 2));
        // A few effects are made more common by repetition because hacks
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 300, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOWNESS, 100, 5));
        forageableEffects.add(new StatusEffectGen(StatusEffects.INSTANT_HEALTH, 1, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 100, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NIGHT_VISION, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NAUSEA, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HEALTH_BOOST, 600, -1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 200, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 300, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.WATER_BREATHING, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOW_FALLING, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.JUMP_BOOST, 300, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.WATER_BREATHING, 300, 0));

        ServerWorldEvents.LOAD.register((server, world) -> {
            // It'll also fire off when someone loads the Nether/etc...is  there some way of filtering that?
            // Because of course we also want it to work properly if someone loads a different overworld instance.
            setScrambledEffects(world.getSeed());
        });
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "wild_inoculant"), WILD_INOCULANT);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "floral_baton"), FLORAL_BATON);
        registerHarvestables();
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

    private static void registerHarvestables() {
        ITEMS.keySet().forEach(item -> Registry.register(Registry.ITEM, ITEMS.get(item), item));
        BLOCKS.keySet().forEach(block -> Registry.register(Registry.BLOCK, BLOCKS.get(block), block));
    }

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }

    public static StatusEffectInstance getForageableEffect(int offset){
        return forageableEffects.get(effectOffsetRedirect[offset]).genEffect();
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
package com.github.corvblimey.roguelikeherblore;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.github.corvblimey.roguelikeherblore.feature.PlantClusterFeature;
import com.github.corvblimey.roguelikeherblore.item.FloralBaton;
import com.github.corvblimey.roguelikeherblore.item.ForageableFoodItem;
import com.github.corvblimey.roguelikeherblore.item.PotentForageableFoodItem;
import com.github.corvblimey.roguelikeherblore.item.WildInoculant;
import net.fabricmc.api.ModInitializer;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.fabricmc.fabric.api.object.builder.v1.block.FabricBlockSettings;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
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
import net.minecraft.util.registry.BuiltinRegistries;
import net.minecraft.util.registry.Registry;
import net.minecraft.util.registry.RegistryKey;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.gen.GenerationStep;
import net.minecraft.world.gen.decorator.ChanceDecoratorConfig;
import net.minecraft.world.gen.decorator.Decorator;
import net.minecraft.world.gen.feature.ConfiguredFeature;
import net.minecraft.world.gen.feature.DefaultFeatureConfig;
import net.minecraft.world.gen.feature.Feature;
import net.minecraft.world.gen.feature.FeatureConfig;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.stream.IntStream;

import static net.minecraft.util.math.MathHelper.ceil;

public class RoguelikeHerblore implements ModInitializer {

    public static Logger LOGGER = LogManager.getLogger();

    public static final String MOD_ID = "roguelikeherblore";
    public static final String MOD_NAME = "Roguelike Herblore";

    private static final int maxOffset = 100;
    private static final int rarity = 16;  // Try to spawn flower patch once per X chunks
    private static final int defaultHunger = 1;
    private static final float defaultSaturation = 0.1f;
    private static final ArrayList<StatusEffectGen> forageableEffects = new ArrayList<StatusEffectGen>();
    private static final ArrayList<StatusEffectGen> potentForageableEffects = new ArrayList<StatusEffectGen>();
    /* Each edible points to an entry in effectOffsetRedirect, which itself points to
     * a position in forageableEffects. This lets use keep the plant list and effect list
     * sizes independent; we can add more plants without re-scrambling existing ones
     * (however, we CANNOT do the same for potion effects! That scrambles things!)
     */
    private static int[] effectOffsetRedirect;
    private static int[] potentEffectOffsetRedirect;

    private static final Map<Item, Identifier> ITEMS = new LinkedHashMap<>();
    private static final Map<Block, Identifier> BLOCKS = new LinkedHashMap<>();
    public static final Map<Biome.Category, ArrayList<Block>> PLANT_SPAWN_BIOMES = new HashMap<>();

    private static final ItemGroup group = ItemGroup.DECORATIONS;
    // We increment this each time we add a new food item. It's how randomization works.
    // This means that the order of item creation matters!!! Use the python script!
    private static int offset = 0;
    private static int potentOffset = 0;

    public static final Item WILD_INOCULANT = new WildInoculant(new Item.Settings().group(group));
    public static final Item FLORAL_BATON = new FloralBaton(new Item.Settings().group(group));

    // Ignore the "never used" warning, we have to make the calls anyways and I think it's clearer to do it here
    // Also, I decided late that some plants spawn in additional biomes, so the "bonus plants" document that. See onInitialize.

    // Call order matters.

    // beach, river
    public static final Block SHOREBERRY_BLOCK = generateHarvestBlock("shoreberry", Biome.Category.BEACH);
    public static final Harvestables SHOREBERRY_HARVEST_ITEMS = generateHarvestFood("shoreberry");
    // Unique to desert
    public static final Block PYGMY_CACTUS_BLOCK = generateHarvestBlock("pygmy_cactus", Biome.Category.DESERT);
    public static final Harvestables PYGMY_CACTUS_HARVEST_ITEMS = generateHarvestFood("pygmy_cactus");
    // Desert and beach
    public static final Block FIREWORK_YUCCA_BLOCK = generateHarvestBlock("firework_yucca", Biome.Category.DESERT);
    public static final Harvestables FIREWORK_YUCCA_HARVEST_ITEMS = generateHarvestFood("firework_yucca");
    // Desert and swamp
    public static final Block RASPTHORN_BLOCK = generateHarvestBlock("raspthorn", Biome.Category.DESERT);
    public static final Harvestables RASPTHORN_HARVEST_ITEMS = generateHarvestFood("raspthorn");
    // Unique to Extreme Hills
    public static final Block FAIRY_BUSH_BLOCK = generateHarvestBlock("fairy_bush", Biome.Category.EXTREME_HILLS);
    public static final Harvestables FAIRY_BUSH_HARVEST_ITEMS = generateHarvestFood("fairy_bush");
    // Unique to Extreme Hills
    public static final Block QUEENS_SCEPTER_BLOCK = generateHarvestBlock("queens_scepter", Biome.Category.EXTREME_HILLS);
    public static final Harvestables QUEENS_SCEPTER_HARVEST_ITEMS = generateHarvestFood("queens_scepter");
    // Forest, jungle
    public static final Block NECTAR_TRUMPET_BLOCK = generateHarvestBlock("nectar_trumpet", Biome.Category.FOREST);
    public static final Harvestables NECTAR_TRUMPET_HARVEST_ITEMS = generateHarvestFood("nectar_trumpet");
    // Unique to icy
    public static final Block RIMEFLOWER_BLOCK = generateHarvestBlock("rimeflower", Biome.Category.ICY);
    public static final Harvestables RIMEFLOWER_HARVEST_ITEMS = generateHarvestFood("rimeflower");
    // Unique to jungle
    public static final Block LURANA_BLOCK = generateHarvestBlock("lurana", Biome.Category.JUNGLE);
    public static final Harvestables LURANA_HARVEST_ITEMS = generateHarvestFood("lurana");
    // plains, mesa
    public static final Block GORGEROOT_BLOCK = generateHarvestBlock("gorgeroot", Biome.Category.PLAINS);
    public static final Harvestables GORGEROOT_HARVEST_ITEMS = generateHarvestFood("gorgeroot");
    // Unique to plains
    public static final Block HONEYBLOOM_BLOCK = generateHarvestBlock("honeybloom", Biome.Category.PLAINS);
    public static final Harvestables HONEYBLOOM_HARVEST_ITEMS = generateHarvestFood("honeybloom");
    // Plains, savannah
    public static final Block BREEZEGRASS_BLOCK = generateHarvestBlock("breezegrass", Biome.Category.PLAINS);
    public static final Harvestables BREEZEGRASS_HARVEST_ITEMS = generateHarvestFood("breezegrass");
    // River, forest
    public static final Block BULBFRUIT_BLOCK = generateHarvestBlock("bulbfruit", Biome.Category.RIVER);
    public static final Harvestables BULBFRUIT_HARVEST_ITEMS = generateHarvestFood("bulbfruit");
    // Forest, extreme hills
    public static final Block RUFFLEAF_BLOCK = generateHarvestBlock("ruffleaf", Biome.Category.EXTREME_HILLS);
    public static final Harvestables RUFFLEAF_HARVEST_ITEMS = generateHarvestFood("ruffleaf");
    // Savannah, jungle
    public static final Block BUTTONCUP_BLOCK = generateHarvestBlock("buttoncup", Biome.Category.SAVANNA);
    public static final Harvestables BUTTONCUP_HARVEST_ITEMS = generateHarvestFood("buttoncup");
    // Unique to savannah
    public static final Block MUPINO_BLOCK = generateHarvestBlock("mupino", Biome.Category.SAVANNA);
    public static final Harvestables MUPINO_HARVEST_ITEMS = generateHarvestFood("mupino");
    // Swamp, jungle
    public static final Block OOZECAP_BLOCK = generateHarvestBlock("oozecap", Biome.Category.SWAMP);
    public static final Harvestables OOZECAP_HARVEST_ITEMS = generateHarvestFood("oozecap");
    // Unique to swamp
    public static final Block GRIMEBERRY_BLOCK = generateHarvestBlock("grimeberry", Biome.Category.SWAMP);
    public static final Harvestables GRIMEBERRY_HARVEST_ITEMS = generateHarvestFood("grimeberry");
    // Unique to taiga
    public static final Block MOSS_CURL_BLOCK = generateHarvestBlock("moss_curl", Biome.Category.TAIGA);
    public static final Harvestables MOSS_CURL_HARVEST_ITEM = generateHarvestFood("moss_curl");
    // Taiga, forest
    public static final Block YELLOWTHROAT_CROCUS_BLOCK = generateHarvestBlock("yellowthroat_crocus", Biome.Category.TAIGA);
    public static final Harvestables YELLOWTHROAT_CROCUS_HARVEST_ITEMS = generateHarvestFood("yellowthroat_crocus");
    // Taiga, tundra
    public static final Block TYNNIA_BLOCK = generateHarvestBlock("tynnia", Biome.Category.TAIGA);
    public static final Harvestables TYNNIA_HARVEST_ITEMS = generateHarvestFood("tynnia");



    public static final Block[] MIPPED_BLOCKS = {GORGEROOT_BLOCK, BULBFRUIT_BLOCK, QUEENS_SCEPTER_BLOCK, LURANA_BLOCK, MOSS_CURL_BLOCK, HONEYBLOOM_BLOCK, FAIRY_BUSH_BLOCK, SHOREBERRY_BLOCK, BUTTONCUP_BLOCK, PYGMY_CACTUS_BLOCK, OOZECAP_BLOCK, RIMEFLOWER_BLOCK, GRIMEBERRY_BLOCK, FIREWORK_YUCCA_BLOCK, NECTAR_TRUMPET_BLOCK, BREEZEGRASS_BLOCK, YELLOWTHROAT_CROCUS_BLOCK, RASPTHORN_BLOCK, MUPINO_BLOCK, RUFFLEAF_BLOCK, TYNNIA_BLOCK};

    public static final Feature<DefaultFeatureConfig> PLANT_CLUSTER = new PlantClusterFeature(DefaultFeatureConfig.CODEC);
    // The ChanceDecoratorConfig is "once per X chunks"
    public static final ConfiguredFeature<?, ?> PLANT_CLUSTER_CONFIGURED = PLANT_CLUSTER.configure(FeatureConfig.DEFAULT)
            .decorate(Decorator.CHANCE.configure(new ChanceDecoratorConfig(rarity)));

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
        // Remember to try to keep parity with the number of plants!
        // The mod won't break, but if there's more plants than effects, the "extra" plant effects are randomized from this list.
        // Since I'm not currently writing a world save file, if the effect list is then lengthened, the plant's effect
        // will change in the world (because it no longer had to loop around, so it has a different offset now).
        forageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NAUSEA, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 600, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 400, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 1200, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 300, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOWNESS, 80, 5));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 600, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 100, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NIGHT_VISION, 600, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NAUSEA, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.WATER_BREATHING, 600, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOW_FALLING, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.JUMP_BOOST, 600, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 200, 3));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HASTE, 1200, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.BLINDNESS, 200, 2));

        potentForageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 1200, 2));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.DOLPHINS_GRACE, 600, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 200, 3));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 600, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 600, 2));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 200, 2));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.NIGHT_VISION, 1800, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.WATER_BREATHING, 1800, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.WITHER, 160, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.HASTE, 600, 2));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.INSTANT_HEALTH, 1, 2));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.JUMP_BOOST, 200, 3));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.LUCK, 1200, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 900, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.WITHER, 160, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.STRENGTH, 1200, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.STRENGTH, 600, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 200, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.HASTE, 600, 1));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.INVISIBILITY, 1200, 0));
        potentForageableEffects.add(new StatusEffectGen(StatusEffects.WITHER, 160, 1));

        ServerWorldEvents.LOAD.register((server, world) -> {
            // It'll also fire off when someone loads the Nether/etc...is  there some way of filtering that?
            // Because of course we also want it to work properly if someone loads a different overworld instance.
            // No idea how those are implemented/if they get a "new" seed (I imagine it'd be derived from the old, but still)
            effectOffsetRedirect = setScrambledEffects(forageableEffects, world.getSeed(), 0);
            // Offset is used to avoid random seed overlap, so we just need it high enough we'd never reasonably have an effect list
            // of that length
            potentEffectOffsetRedirect = setScrambledEffects(potentForageableEffects, world.getSeed(), 500);
        });
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "wild_inoculant"), WILD_INOCULANT);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "floral_baton"), FLORAL_BATON);
        registerHarvestables();
        registerAndConfigureFeatures();
        // Beach: 2 (Unique plants: 0)
        addAdditionalHarvestBiome(FIREWORK_YUCCA_BLOCK, Biome.Category.BEACH);
        // Desert: 3 (1)
        // Extreme Hills: 3 (2)
        // Forest: 4 (0)
        addAdditionalHarvestBiome(RUFFLEAF_BLOCK, Biome.Category.FOREST);
        addAdditionalHarvestBiome(BULBFRUIT_BLOCK, Biome.Category.FOREST);
        addAdditionalHarvestBiome(YELLOWTHROAT_CROCUS_BLOCK, Biome.Category.FOREST);
        // Icy: 2 (1)
        addAdditionalHarvestBiome(TYNNIA_BLOCK, Biome.Category.ICY);
        // Jungle: 4 (1)
        addAdditionalHarvestBiome(NECTAR_TRUMPET_BLOCK, Biome.Category.JUNGLE);
        addAdditionalHarvestBiome(OOZECAP_BLOCK, Biome.Category.JUNGLE);
        addAdditionalHarvestBiome(BUTTONCUP_BLOCK, Biome.Category.JUNGLE);
        // Mesa: 1 (0)
        addAdditionalHarvestBiome(GORGEROOT_BLOCK, Biome.Category.MESA);
        // Plains: 3 (1)
        // River: 2 (0)
        addAdditionalHarvestBiome(SHOREBERRY_BLOCK, Biome.Category.RIVER);
        // Savanna: 3 (1)
        addAdditionalHarvestBiome(BREEZEGRASS_BLOCK, Biome.Category.SAVANNA);
        // Swamp: 3 (1)
        addAdditionalHarvestBiome(RASPTHORN_BLOCK, Biome.Category.SWAMP);
        // Taiga: 3 (1)
        // Mushroom: 0
        // Nether: 0
        // TheEnd: 0
    }

    public static Block[] getMippedBlocks(){
        return MIPPED_BLOCKS;
    }

    public static class Harvestables {
        Item standardItem;
        Item potentItem;

        public Harvestables(Item standardItem, Item potentItem){
            this.standardItem = standardItem;
            this.potentItem = potentItem;
        }
    }

    private static Harvestables generateHarvestFood(String source_plant, int hunger, float saturation) {
        Item harvest_food =  new ForageableFoodItem(new Item.Settings().group(group).food(new FoodComponent.Builder().hunger(hunger).saturationModifier(saturation).build())).withOffset(offset);
        ITEMS.put(harvest_food, new Identifier(MOD_ID, source_plant+"_harvest_item"));
        Item potent_harvest_food =  new PotentForageableFoodItem(new Item.Settings().group(group).food(new FoodComponent.Builder().hunger(hunger).saturationModifier(saturation).build())).withOffset(offset);
        ITEMS.put(potent_harvest_food, new Identifier(MOD_ID, source_plant+"_harvest_item_potent"));
        offset ++;
        return new Harvestables(harvest_food, potent_harvest_food);
    }

    private static Harvestables generateHarvestFood(String sourcePlant){ return generateHarvestFood(sourcePlant, defaultHunger, defaultSaturation);}

    private static Block generateHarvestBlock(String sourcePlant, Biome.Category spawnBiome) {
        Block harvest_block = new ForageableBlock(FabricBlockSettings.of(Material.PLANT).noCollision().nonOpaque());
        BLOCKS.put(harvest_block, new Identifier(MOD_ID, sourcePlant));
        Item harvest_block_item = new BlockItem(harvest_block, new Item.Settings().group(group));
        ITEMS.put(harvest_block_item, new Identifier(MOD_ID, sourcePlant));
        // The computeIfAbsent ?seems to? be my key to an offbrand defaultDict.
        PLANT_SPAWN_BIOMES.computeIfAbsent(spawnBiome, k -> new ArrayList<>());
        PLANT_SPAWN_BIOMES.get(spawnBiome).add(harvest_block);
        return harvest_block;
    }

    private static void addAdditionalHarvestBiome(Block harvestablePlant, Biome.Category spawnBiome) {
        PLANT_SPAWN_BIOMES.computeIfAbsent(spawnBiome, k -> new ArrayList<>());
        PLANT_SPAWN_BIOMES.get(spawnBiome).add(harvestablePlant);
    }

    private static void registerHarvestables() {
        ITEMS.keySet().forEach(item -> Registry.register(Registry.ITEM, ITEMS.get(item), item));
        BLOCKS.keySet().forEach(block -> Registry.register(Registry.BLOCK, BLOCKS.get(block), block));
    }

    public static void registerAndConfigureFeatures() {
        Registry.register(Registry.FEATURE, PlantClusterFeature.ID, PLANT_CLUSTER);
        RegistryKey<ConfiguredFeature<?, ?>> plantCluster = RegistryKey.of(Registry.CONFIGURED_FEATURE_KEY,
                new Identifier(MOD_ID, "plant_cluster"));
        Registry.register(BuiltinRegistries.CONFIGURED_FEATURE, plantCluster.getValue(), PLANT_CLUSTER_CONFIGURED);
        BiomeModifications.addFeature(BiomeSelectors.all(), GenerationStep.Feature.VEGETAL_DECORATION, plantCluster);
    }

    public static void log(Level level, String message){
        LOGGER.log(level, "["+MOD_NAME+"] " + message);
    }

    public static StatusEffectInstance getForageableEffect(int offset){
        return forageableEffects.get(effectOffsetRedirect[offset]).genEffect();
    }

    public static StatusEffectInstance getPotentForageableEffect(int offset){
        return potentForageableEffects.get(potentEffectOffsetRedirect[offset]).genEffect();
    }

    /* To keep the lengths of the plant and effect lists independent, we "tile"
     * the randomization of the list to meet (or exceed; I'm lazy) the offset.
     * The end result is that if you have 5 effects and a max offset of 12, you get
     * ceil(12/5)*5 effects, and each of the 5 is repeated at least floor(12/5)
     * times. This should help prevent people from getting worlds with nothing but
     * Nausea plants.
     */
    private int[] setScrambledEffects(ArrayList<StatusEffectGen> effectList, long worldSeed, int seedOffset){
        int repetitions = ceil((float) maxOffset /effectList.size());
        int[] outputArray = new int[effectList.size()*repetitions];
        for(int i = 0; i < repetitions; i++){
            final int[] effectOffsets = IntStream.rangeClosed(0, effectList.size()-1).toArray();
            scrambleEffectsPerSeed(effectOffsets, worldSeed+i+seedOffset);
            System.arraycopy(effectOffsets, 0, outputArray, i * effectList.size(), effectList.size());
        }
        return outputArray;
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

    public static Identifier id(String name) {
        return new Identifier(MOD_ID, name);
    }
}
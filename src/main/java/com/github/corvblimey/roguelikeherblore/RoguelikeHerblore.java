package com.github.corvblimey.roguelikeherblore;

import com.github.corvblimey.roguelikeherblore.block.ForageableBlock;
import com.github.corvblimey.roguelikeherblore.feature.PlantClusterFeature;
import com.github.corvblimey.roguelikeherblore.item.FloralBaton;
import com.github.corvblimey.roguelikeherblore.item.ForageableFoodItem;
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
    /* Each edible points to an entry in effectOffsetRedirect, which itself points to
     * a position in forageableEffects. This lets use keep the plant list and effect list
     * sizes independent; we can add more plants without re-scrambling existing ones
     * (however, we CANNOT do the same for potion effects! That scrambles things!)
     */
    private static int[] effectOffsetRedirect;

    private static final Map<Item, Identifier> ITEMS = new LinkedHashMap<>();
    private static final Map<Block, Identifier> BLOCKS = new LinkedHashMap<>();
    public static final Map<Biome.Category, ArrayList<Block>> PLANT_SPAWN_BIOMES = new HashMap<>();

    private static final ItemGroup group = ItemGroup.DECORATIONS;
    // We increment this each time we add a new food item. It's how randomization works.
    // This means that the order of item creation matters!!! Use the python script!
    private static int offset = 0;

    public static final Item WILD_INOCULANT = new WildInoculant(new Item.Settings().group(group));
    public static final Item FLORAL_BATON = new FloralBaton(new Item.Settings().group(group));

    // Ignore the "never used" warning, we have to make the calls anyways and I think it's clearer to do it here
    // Also, I decided late that some plants spawn in additional biomes, so the "bonus plants" document that. See onInitialize.

    // Unique to beach
    public static final Block SHOREBERRY_BLOCK = generateHarvestBlock("shoreberry", Biome.Category.BEACH);
    public static final Item SHOREBERRY_HARVEST_ITEM = generateHarvestFood("shoreberry");
    // Unique to desert
    public static final Block PYGMY_CACTUS_BLOCK = generateHarvestBlock("pygmy_cactus", Biome.Category.DESERT);
    public static final Item PYGMY_CACTUS_HARVEST_ITEM = generateHarvestFood("pygmy_cactus");
    // Desert and beach
    public static final Block FIREWORK_YUCCA_BLOCK = generateHarvestBlock("firework_yucca", Biome.Category.DESERT);
    public static final Item FIREWORK_YUCCA_HARVEST_ITEM = generateHarvestFood("firework_yucca");
    // Desert and swamp
    public static final Block RASPTHORN_BLOCK = generateHarvestBlock("raspthorn", Biome.Category.DESERT);
    public static final Item RASPTHORN_HARVEST_ITEM = generateHarvestFood("raspthorn");
    // Unique to Extreme Hills
    public static final Block FAIRY_BUSH_BLOCK = generateHarvestBlock("fairy_bush", Biome.Category.EXTREME_HILLS);
    public static final Item FAIRY_BUSH_HARVEST_ITEM = generateHarvestFood("fairy_bush");
    // Unique to Extreme Hills
    public static final Block QUEENS_SCEPTER_BLOCK = generateHarvestBlock("queens_scepter", Biome.Category.EXTREME_HILLS);
    public static final Item QUEENS_SCEPTER_HARVEST_ITEM = generateHarvestFood("queens_scepter");
    // Forest, jungle
    public static final Block NECTAR_TRUMPET_BLOCK = generateHarvestBlock("nectar_trumpet", Biome.Category.FOREST);
    public static final Item NECTAR_TRUMPET_HARVEST_ITEM = generateHarvestFood("nectar_trumpet");
    // Unique to icy
    public static final Block RIMEFLOWER_BLOCK = generateHarvestBlock("rimeflower", Biome.Category.ICY);
    public static final Item RIMEFLOWER_HARVEST_ITEM = generateHarvestFood("rimeflower");
    // Unique to jungle
    public static final Block LURANA_BLOCK = generateHarvestBlock("lurana", Biome.Category.JUNGLE);
    public static final Item LURANA_HARVEST_ITEM = generateHarvestFood("lurana");
    // Unique to plains
    public static final Block GORGEROOT_BLOCK = generateHarvestBlock("gorgeroot", Biome.Category.PLAINS);
    public static final Item GORGEROOT_HARVEST_ITEM = generateHarvestFood("gorgeroot");
    // Plains, forest
    public static final Block HONEYBLOOM_BLOCK = generateHarvestBlock("honeybloom", Biome.Category.PLAINS);
    public static final Item HONEYBLOOM_HARVEST_ITEM = generateHarvestFood("honeybloom");
    // Plains, savannah
    public static final Block BREEZEGRASS_BLOCK = generateHarvestBlock("breezegrass", Biome.Category.PLAINS);
    public static final Item BREEZEGRASS_HARVEST_ITEM = generateHarvestFood("breezegrass");
    // River, forest
    public static final Block BULBFRUIT_BLOCK = generateHarvestBlock("bulbfruit", Biome.Category.RIVER);
    public static final Item BULBFRUIT_HARVEST_ITEM = generateHarvestFood("bulbfruit");
    // River, extreme hills
    public static final Block RUFFLEAF_BLOCK = generateHarvestBlock("ruffleaf", Biome.Category.RIVER);
    public static final Item RUFFLEAF_HARVEST_ITEM = generateHarvestFood("ruffleaf");
    // Savannah, jungle
    public static final Block BUTTONCUP_BLOCK = generateHarvestBlock("buttoncup", Biome.Category.SAVANNA);
    public static final Item BUTTONCUP_HARVEST_ITEM = generateHarvestFood("buttoncup");
    // Unique to savannah
    public static final Block MUPINO_BLOCK = generateHarvestBlock("mupino", Biome.Category.SAVANNA);
    public static final Item MUPINO_HARVEST_ITEM = generateHarvestFood("mupino");
    // Swamp, jungle
    public static final Block OOZECAP_BLOCK = generateHarvestBlock("oozecap", Biome.Category.SWAMP);
    public static final Item OOZECAP_HARVEST_ITEM = generateHarvestFood("oozecap");
    // Unique to swamp
    public static final Block GRIMEBERRY_BLOCK = generateHarvestBlock("grimeberry", Biome.Category.SWAMP);
    public static final Item GRIMEBERRY_HARVEST_ITEM = generateHarvestFood("grimeberry");
    // Unique to taiga
    public static final Block MOSS_CURL_BLOCK = generateHarvestBlock("moss_curl", Biome.Category.TAIGA);
    public static final Item MOSS_CURL_HARVEST_ITEM = generateHarvestFood("moss_curl");
    // Taiga, forest
    public static final Block YELLOWTHROAT_CROCUS_BLOCK = generateHarvestBlock("yellowthroat_crocus", Biome.Category.TAIGA);
    public static final Item YELLOWTHROAT_CROCUS_HARVEST_ITEM = generateHarvestFood("yellowthroat_crocus");
    // Taiga, tundra
    public static final Block TYNNIA_BLOCK = generateHarvestBlock("tynnia", Biome.Category.TAIGA);
    public static final Item TYNNIA_HARVEST_ITEM = generateHarvestFood("tynnia");


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
        forageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.ABSORPTION, 300, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.RESISTANCE, 300, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.REGENERATION, 450, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 1200, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SPEED, 300, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOWNESS, 100, 5));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.POISON, 100, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NIGHT_VISION, 800, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.NAUSEA, 200, 1));
        forageableEffects.add(new StatusEffectGen(StatusEffects.HUNGER, 200, 2));
        forageableEffects.add(new StatusEffectGen(StatusEffects.WATER_BREATHING, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.SLOW_FALLING, 300, 0));
        forageableEffects.add(new StatusEffectGen(StatusEffects.JUMP_BOOST, 300, 1));

        ServerWorldEvents.LOAD.register((server, world) -> {
            // It'll also fire off when someone loads the Nether/etc...is  there some way of filtering that?
            // Because of course we also want it to work properly if someone loads a different overworld instance.
            setScrambledEffects(world.getSeed());
        });
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "wild_inoculant"), WILD_INOCULANT);
        Registry.register(Registry.ITEM, new Identifier(MOD_ID, "floral_baton"), FLORAL_BATON);
        registerHarvestables();
        registerAndConfigureFeatures();
        // Beach: 2 (Unique: 1)
        addAdditionalHarvestBiome(FIREWORK_YUCCA_BLOCK, Biome.Category.BEACH);
        // Desert: 3 (1)
        // Extreme Hills: 3 (2)
        addAdditionalHarvestBiome(RUFFLEAF_BLOCK, Biome.Category.EXTREME_HILLS);
        // Forest: 4 (0)
        addAdditionalHarvestBiome(BULBFRUIT_BLOCK, Biome.Category.FOREST);
        addAdditionalHarvestBiome(HONEYBLOOM_BLOCK, Biome.Category.FOREST);
        addAdditionalHarvestBiome(YELLOWTHROAT_CROCUS_BLOCK, Biome.Category.FOREST);
        // Icy: 2 (1)
        addAdditionalHarvestBiome(TYNNIA_BLOCK, Biome.Category.ICY);
        // Jungle: 4 (1)
        addAdditionalHarvestBiome(NECTAR_TRUMPET_BLOCK, Biome.Category.JUNGLE);
        addAdditionalHarvestBiome(OOZECAP_BLOCK, Biome.Category.JUNGLE);
        addAdditionalHarvestBiome(BUTTONCUP_BLOCK, Biome.Category.JUNGLE);
        // Plains: 3 (1)
        // River: 2 (0)
        // Savanna: 3 (1)
        addAdditionalHarvestBiome(BREEZEGRASS_BLOCK, Biome.Category.SAVANNA);
        // Swamp: 3 (1)
        addAdditionalHarvestBiome(RASPTHORN_BLOCK, Biome.Category.SWAMP);
        // Taiga: 3 (1)
        // Mushroom: 0
        // Mesa: 0
        // Nether: 0
        // TheEnd: 0
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

    private static Item generateHarvestFood(String sourcePlant){ return generateHarvestFood(sourcePlant, defaultHunger, defaultSaturation);}

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
        RegistryKey<ConfiguredFeature<?, ?>> plantCluster = RegistryKey.of(Registry.CONFIGURED_FEATURE_WORLDGEN,
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

    public static Identifier id(String name) {
        return new Identifier(MOD_ID, name);
    }
}
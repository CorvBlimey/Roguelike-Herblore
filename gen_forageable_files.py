"""
Generation script for most of the files in the mod.

All the plants are essentially just "rerolls", so I use this to generate the
near-identical jsons and block/item declarations. All you have to do is add
plant names into NAMES below, then run the script:

$ python3 gen_forageable_files.py

It does enforce naming. The images associated with the new plant have to be
named [<plantname>.png, <plantname>_harvested.png, <plantname>_harvest_item.png]
for the harvestable, harvested, and thing-you-get-when-harvested pics
respectively.
"""

import sys
import os
import json

NAMES = ["gorgeroot","bulbfruit","queens_scepter","lurana", "moss_curl",
         "honeybloom", "fairy_bush", "shoreberry", "buttoncup",
         "pygmy_cactus", "oozecap", "rimeflower", "grimeberry",
         "firework_yucca", "breezegrass", "nectar_trumpet", "yellowthroat_crocus",
         "tynnia", "raspthorn", "ruffleaf", "mupino"]
MOD_NAME = "roguelikeherblore"
# Tag used for items that can be used in the inoculant/other recipes
CRAFTING_TAG = "forageable_plants"

def main():
    core_path = os.path.join(os.getcwd(), "src/main/resources")
    data_path = os.path.join(core_path, "data", MOD_NAME)
    asset_path = os.path.join(core_path, "assets", MOD_NAME)
    required_dirs = {"blockstates": os.path.join(asset_path, "blockstates"),
                     "block": os.path.join(asset_path, "models/block"),
                     "item": os.path.join(asset_path, "models/item"),
                     "loot_tables": os.path.join(data_path, "loot_tables/blocks"),
                     "item_tags": os.path.join(data_path, "tags/items")}
    for dir in required_dirs.values():
        if not os.path.exists(dir):
            os.makedirs(dir)
    for name in NAMES:
        blockstate = {
              "variants": {
                "bearing=true": { "model": f"{MOD_NAME}:block/{name}"},
                "bearing=false": { "model": f"{MOD_NAME}:block/{name}_harvested" }
              }}
        block = {"parent": "minecraft:block/cross",
                 "textures": {"cross": f"{MOD_NAME}:block/{name}"}}
        block_harvested = {"parent": "minecraft:block/cross",
                           "textures": {"cross": f"{MOD_NAME}:block/{name}_harvested"}}
        block_item= {"parent": f"minecraft:item/generated",
                    "textures": {"layer0": f"{MOD_NAME}:block/{name}"}}
        harvest_item={"parent": "item/generated",
                      "textures": {"layer0": f"{MOD_NAME}:item/{name}_harvest_item"}}
        loot_table={"type": "minecraft:block",
                    "pools": [{"rolls": 1.0,
                               "entries": [{"type": "minecraft:item",
                                            "conditions": [{"condition": "minecraft:block_state_property",
                                            "block": f"{MOD_NAME}:{name}",
                                            "properties": {"bearing": "true"}}],
                                            "name": f"{MOD_NAME}:{name}_harvest_item"}]},
                              {"rolls": 1.0,
                               "entries": [{"type": "minecraft:item",
                                            "name":  f"{MOD_NAME}:{name}"}]}]}
        for content, dir, target in zip(
            [blockstate, block, block_harvested, block_item, harvest_item, loot_table],
            ["blockstates", "block","block", "item", "item", "loot_tables"],
            [f"{name}.json", f"{name}.json", f"{name}_harvested.json", f"{name}.json", f"{name}_harvest_item.json", f"{name}.json"]):
            with open(os.path.join(required_dirs[dir], target), "w") as outfile:
                json.dump(content, outfile)
        print(f"public static final Block {name.upper()}_BLOCK = generateHarvestBlock(\"{name}\");")
        print(f"public static final Item {name.upper()}_HARVEST_ITEM = generateHarvestFood(\"{name}\");")
    with open(os.path.join(required_dirs["item_tags"], CRAFTING_TAG+".json"), "w") as item_tags_file:
        item_tags = {"replace": False,
                      "values": [f"{MOD_NAME}:{name}" for name in NAMES]}
        json.dump(item_tags, item_tags_file)

        print("\n\n")
        for name in NAMES:
            clean_name = name.replace("_", " ").title() 
            print(f'"block.{MOD_NAME}.{name}": "{clean_name}",')
            print(f'"block.{MOD_NAME}.{name}_harvested": "Harvested {clean_name}",')

if __name__ == "__main__":
    main()

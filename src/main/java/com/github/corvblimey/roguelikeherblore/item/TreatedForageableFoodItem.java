package com.github.corvblimey.roguelikeherblore.item;

import com.github.corvblimey.roguelikeherblore.RoguelikeHerblore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class TreatedForageableFoodItem extends ForageableFoodItem {

    public TreatedForageableFoodItem(final Settings settings) {
        super(settings);
    }

    public StatusEffectInstance getStatusEffect() {
        return RoguelikeHerblore.getTreatedForageableEffect(this.offset);
    }

    public boolean hasGlint(ItemStack stack) {
        return true;
    }
}


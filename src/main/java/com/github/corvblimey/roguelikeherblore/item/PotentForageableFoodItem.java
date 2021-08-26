package com.github.corvblimey.roguelikeherblore.item;

import com.github.corvblimey.roguelikeherblore.RoguelikeHerblore;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class PotentForageableFoodItem extends ForageableFoodItem {

    public PotentForageableFoodItem(final Settings settings) {
        super(settings);
    }

    public boolean hasGlint(ItemStack stack) {
        return true;
    }

    @Override
    public ItemStack finishUsing(final ItemStack stack, final World world, final LivingEntity user) {
        final ItemStack remainingStack = super.finishUsing(stack, world, user);
        user.addStatusEffect(RoguelikeHerblore.getForageableEffect(this.offset));
        user.addStatusEffect(RoguelikeHerblore.getPotentForageableEffect(this.offset));
        return remainingStack;
    }
}


package com.github.corvblimey.roguelikeherbology.item;

import com.github.corvblimey.roguelikeherbology.RoguelikeHerbology;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

public class ForageableFoodItem extends Item {

    private int offset;  // Used to calculate the potion effect

    public ForageableFoodItem(final Settings settings) {
        super(settings);
    }

    public ForageableFoodItem withOffset(int offset) {
        this.offset = offset;
        return this;
    }

    public StatusEffectInstance getStatusEffect() {
        return RoguelikeHerbology.getForageableEffect(this.offset);
    }

    @Override
    public ItemStack finishUsing(final ItemStack stack, final World world, final LivingEntity user) {
        final ItemStack remainingStack = super.finishUsing(stack, world, user);
        user.addStatusEffect(getStatusEffect());
        return remainingStack;
    }
}


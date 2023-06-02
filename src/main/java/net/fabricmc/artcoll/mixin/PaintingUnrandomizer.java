package net.fabricmc.artcoll.mixin;

import net.fabricmc.artcoll.ArtCollector;
import net.fabricmc.artcoll.StackCreator;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;


import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(PaintingEntity.class)
public class PaintingUnrandomizer {
	@Redirect(
		method = "onBreak",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;dropItem(Lnet/minecraft/item/ItemConvertible;)Lnet/minecraft/entity/ItemEntity;"))
	private ItemEntity replaceDroppedItem(PaintingEntity painting, ItemConvertible itemType) {
			ArtCollector.LOGGER.debug("Painting drop is being unrandomized!");
			if (itemType != Items.PAINTING)
			{
				ArtCollector.LOGGER.error("Unexpected painting drop type: ", itemType);
				return painting.dropItem(itemType);
			}
			else 
			{
				String variant = painting.writeNbt(new NbtCompound()).getString("variant");
				return painting.dropStack(StackCreator.Specific(variant));
			}
	}
}

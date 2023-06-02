package net.fabricmc.artcoll;

import net.minecraft.util.math.random.Random;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;

public class StackCreator
{
	public static ItemStack	Random(Random random){
		PaintingVariant variant;
		String id;
		int i;

		i = random.nextBetween(0, Registries.PAINTING_VARIANT.size());
		variant = Registries.PAINTING_VARIANT.get(i);
		id = Registries.PAINTING_VARIANT.getId(variant).toString();
		ArtCollector.LOGGER.info("Random variant:", id);

		return Specific(id);
	}

	public static ItemStack	Specific(String variantId){
		ItemStack stack = new ItemStack(Items.PAINTING);

		NbtCompound nbt = stack.getOrCreateNbt();
		nbt.put("EntityTag", new NbtCompound());
		nbt = nbt.getCompound("EntityTag");
		nbt.putString("variant", variantId);

		return stack;
	}

}

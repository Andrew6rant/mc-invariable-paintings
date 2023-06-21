package tk.estecka.invarpaint.mixin;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.AbstractDecorationEntity;
import net.minecraft.entity.decoration.painting.PaintingEntity;
import net.minecraft.entity.decoration.painting.PaintingVariant;
import net.minecraft.item.DecorationItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.registry.Registries;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.text.TextContent;
import net.minecraft.text.TranslatableTextContent;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import tk.estecka.invarpaint.InvariablePaintings;
import tk.estecka.invarpaint.PaintStackCreator;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

@Mixin(DecorationItem.class)
public class DecorationItemMixin extends Item {

    @Mutable
    @Final
    @Shadow
    private final EntityType<? extends AbstractDecorationEntity> entityType;

    private ItemStack itemStack;

    public DecorationItemMixin(Settings settings, EntityType<? extends AbstractDecorationEntity> entityType) {
        super(settings);
        this.entityType = entityType;
    }


    @Inject(
            method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;placePainting(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Ljava/util/Optional;",
                    shift = At.Shift.BEFORE
            )
    )
    private void captureItemStack(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        itemStack = context.getStack();
    }

        @Redirect(
            method = "useOnBlock(Lnet/minecraft/item/ItemUsageContext;)Lnet/minecraft/util/ActionResult;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/entity/decoration/painting/PaintingEntity;placePainting(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/util/math/Direction;)Ljava/util/Optional;"
            )
    )
    private Optional<PaintingEntity> filterPlacedPainting(World world, BlockPos pos, Direction facing) {
        String variantId = PaintStackCreator.GetVariantId(this.itemStack);
        PaintingVariant itemVariant = (variantId==null) ? null : Registries.PAINTING_VARIANT.get(new Identifier(variantId));

        if (itemVariant != null) {
            PaintingEntity entity = new PaintingEntity(world, pos, facing, Registries.PAINTING_VARIANT.getEntry(itemVariant));
            if (entity.canStayAttached())
                return Optional.of(entity);
            else
                return Optional.empty();
        }
        else {
            if (variantId != null)
                InvariablePaintings.LOGGER.warn("Unknown painting id: {}", variantId);
            return PaintingEntity.placePainting(world, pos, facing);
        }
    }

    @Inject(
            method = "appendTooltip(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Ljava/util/List;Lnet/minecraft/client/item/TooltipContext;)V",
            at = @At(
                    value = "TAIL"
            )
    )
    public void condenseTooltip(ItemStack stack, World world, List<Text> tooltip, TooltipContext context, CallbackInfo ci) {
        if (this.entityType == EntityType.PAINTING) {
            MutableText mutableText = Text.empty();
            AtomicBoolean randomPainting = new AtomicBoolean(true);
            tooltip.removeIf(text -> {
                TextContent textContent = text.getContent();
                if (textContent instanceof TranslatableTextContent) {
                    String key = ((TranslatableTextContent) textContent).getKey();
                    boolean isPainting = key.contains("painting");
                    if (isPainting) {
                        if (key.contains("title")) {
                            randomPainting.set(false); // Painting has title and thus is not random
                            // do not append mutableText because the title is now in the getName function
                        } else {
                            mutableText.append(text);
                            mutableText.append(Text.literal(" "));
                        }
                    }
                    return isPainting; // remove all vanilla painting tooltips
                }
                return false;
            });
            if (randomPainting.get()) {
                tooltip.add(Text.translatable("painting.random").formatted(Formatting.GRAY));
            } else {
                mutableText.getSiblings().remove(mutableText.getSiblings().size()-1); // remove trailing space
                tooltip.add(mutableText);
            }
        }
    }

    @Override
    public Text getName(ItemStack stack) {
        if (this.entityType == EntityType.PAINTING) {
            String variantId = PaintStackCreator.GetVariantId(stack);
            if (variantId != null) {
                return Text.translatable(this.getTranslationKey(stack)) // I could just use translatable variables,
                    .append(Text.literal(" (")                    // but this way is compatible with other languages
                    .append(Text.translatable("painting."+variantId.replace(":",".")+".title")
                    .append(")")).formatted(Formatting.YELLOW));
            }
        }
        return super.getName();
    }
}

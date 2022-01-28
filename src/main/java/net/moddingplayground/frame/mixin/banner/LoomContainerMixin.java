package net.moddingplayground.frame.mixin.banner;

import net.minecraft.block.entity.BannerBlockEntity;
import net.minecraft.block.entity.BannerPattern;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.DyeItem;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.screen.LoomScreenHandler;
import net.minecraft.screen.Property;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.DyeColor;
import net.moddingplayground.frame.api.banner.FrameBannerPattern;
import net.moddingplayground.frame.api.banner.FrameBannerPatternProvider;
import net.moddingplayground.frame.api.banner.PatternLimitModifier;
import net.moddingplayground.frame.impl.banner.FrameBannerPatternAccess;
import net.moddingplayground.frame.impl.banner.FrameBannerPatternsInternal;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LoomScreenHandler.class)
public abstract class LoomContainerMixin extends ScreenHandler {
    @Shadow @Final Property selectedPattern;
    @Shadow @Final Slot bannerSlot;
    @Shadow @Final Slot dyeSlot;
    @Shadow @Final private Slot patternSlot;
    @Shadow @Final private Slot outputSlot;

    @Unique private PlayerEntity player;

    private LoomContainerMixin(@Nullable ScreenHandlerType<?> type, int syncId) {
        super(type, syncId);
    }

    @Shadow
    private native void updateOutputSlot();

    /**
     * Saves the player entity for computing the banner pattern limit.
     */
    @Inject(
        method = "<init>(ILnet/minecraft/entity/player/PlayerInventory;Lnet/minecraft/screen/ScreenHandlerContext;)V",
        at = @At("RETURN")
    )
    private void frameSavePlayer(int capacity, PlayerInventory inventory, ScreenHandlerContext ctx, CallbackInfo ci) {
        player = inventory.player;
    }

    /**
     * When the player clicks on a square that contains a Frame banner pattern,
     * store the negative of the index clicked. This number is
     * -(frameBannerPatternIndex + 1 + BannerPattern.LOOM_APPLICABLE_COUNT).
     */
    @Inject(method = "onButtonClick", at = @At("HEAD"), cancellable = true)
    private void selectFrameBannerPatternOnClick(PlayerEntity entity, int clicked, CallbackInfoReturnable<Boolean> cir) {
        int vanillaCount = BannerPattern.LOOM_APPLICABLE_COUNT;
        if (clicked > vanillaCount && clicked - (1 + vanillaCount) < FrameBannerPatternsInternal.dyePatternCount()) {
            selectedPattern.set(-clicked);
            this.updateOutputSlot();
            cir.setReturnValue(true);
        }
    }

    @Unique
    private int patternLimit;

    /**
     * Computes and saves the banner pattern limit for this player to be
     * used server side.
     */
    @Inject(method = "onContentChanged", at = @At("HEAD"))
    private void invokePatternLimitEvent(CallbackInfo ci) {
        patternLimit = PatternLimitModifier.EVENT.invoker().computePatternLimit(6, player);
    }

    /**
     * Trigger the then branch if the selected pattern is a Frame banner pattern.
     * We make the condition `selectedPattern.get() < BannerPattern.COUNT - 5`
     * true in this case.
     */
    @Redirect(
        method = "onContentChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/screen/Property;get()I"
        )
    )
    private int addFrameBannerPatternCondition(Property self) {
        int res = self.get();
        return res < 0 ? 1 : res;
    }

    @ModifyConstant(method = "onContentChanged", constant = @Constant(intValue = 6))
    private int disarmVanillaPatternLimitCheck(int limit) {
        return Integer.MAX_VALUE;
    }

    @ModifyVariable(
        method = "onContentChanged",
        at = @At(value = "LOAD", ordinal = 0),
        ordinal = 0
    )
    private boolean addFrameBannerPatternsToFullCond(boolean original) {
        ItemStack banner = this.bannerSlot.getStack();
        return original || BannerBlockEntity.getPatternCount(banner) >= patternLimit;
    }

    /**
     * Set the Frame banner pattern when a Frame banner pattern item is placed in the loom.
     * This injection is at the beginning of the then block after the check
     * for a loom state that should display an item.
     * Relevant bytecode:
     * 110: aload         4
     * [injection point]
     * 112: invokevirtual #182                // Method net/minecraft/item/ItemStack.isEmpty:()Z
     * 115: ifne          217
     * 118: aload         4
     * 120: invokevirtual #196                // Method net/minecraft/item/ItemStack.getItem:()Lnet/minecraft/item/Item;
     * 123: instanceof    #198                // class net/minecraft/item/BannerPatternItem
     */
    @Inject(
        method = "onContentChanged",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;isEmpty()Z",
            ordinal = 4
        )
    )
    private void updateFrameContentChanged(CallbackInfo ci) {
        ItemStack banner = this.bannerSlot.getStack();
        ItemStack patternStack = this.patternSlot.getStack();

        // only run for special banner patterns
        if (!patternStack.isEmpty() && patternStack.getItem() instanceof FrameBannerPatternProvider provider) {
            boolean overfull = BannerBlockEntity.getPatternCount(banner) >= patternLimit;

            if (!overfull) {
                FrameBannerPattern pattern = provider.getPattern();
                this.selectedPattern.set(-FrameBannerPatternsInternal.indexOf(pattern) - (1 + BannerPattern.LOOM_APPLICABLE_COUNT));
            } else {
                this.selectedPattern.set(0);
            }
        } else if (-this.selectedPattern.get() - (1 + BannerPattern.LOOM_APPLICABLE_COUNT) >= FrameBannerPatternsInternal.dyePatternCount()) {
            // reset special pattern on removal
            this.selectedPattern.set(0);
            this.outputSlot.setStack(ItemStack.EMPTY);
        }
    }

    /**
     * When the output slot is updated, add the pattern to the
     * output banner.
     */
    @Inject(method = "updateOutputSlot", at = @At("HEAD"))
    private void addFrameBannerPatternToOutput(CallbackInfo ci) {
        ItemStack bannerStack = this.bannerSlot.getStack();
        ItemStack dyeStack = this.dyeSlot.getStack();

        if (this.selectedPattern.get() < 0 && !bannerStack.isEmpty() && !dyeStack.isEmpty()) {
            int rawId = -this.selectedPattern.get() - (1 + BannerPattern.LOOM_APPLICABLE_COUNT);

            if (rawId < FrameBannerPatternsInternal.size()) {
                FrameBannerPattern pattern = FrameBannerPatternsInternal.get(rawId);
                DyeColor color = ((DyeItem) dyeStack.getItem()).getColor();
                ItemStack output = bannerStack.copy();
                output.setCount(1);
                NbtCompound beTag = output.getOrCreateSubNbt("BlockEntityTag");
                NbtList framePatterns;

                if (beTag.contains(FrameBannerPatternAccess.NBT_KEY, NbtElement.LIST_TYPE)) {
                    framePatterns = beTag.getList(FrameBannerPatternAccess.NBT_KEY, NbtElement.COMPOUND_TYPE);
                } else {
                    framePatterns = new NbtList();
                    beTag.put(FrameBannerPatternAccess.NBT_KEY, framePatterns);
                }

                int vanillaPatternCount = beTag.getList("Patterns", 10).size();
                NbtCompound patternTag = new NbtCompound();
                patternTag.putString("Pattern", pattern.getId().toString());
                patternTag.putInt("Color", color.getId());
                patternTag.putInt("Index", vanillaPatternCount);
                framePatterns.add(patternTag);

                if (!ItemStack.areEqual(output, this.outputSlot.getStack())) {
                    this.outputSlot.setStack(output);
                }
            }
        }
    }

    /**
     * Attempts transfer of a Frame banner
     * pattern item into the loom's pattern slot.
     * (The vanilla code only attempts this on vanilla banner pattern items)
     * The injection point targets the first instruction in the if-else ladder.
     * Relevant bytecode:
     * 130: getstatic     #188                // Field net/minecraft/item/ItemStack.EMPTY:Lnet/minecraft/item/ItemStack;
     * 133: areturn
     * --- basic block boundary ---
     * [injection point]
     * 134: aload         5
     * 136: invokevirtual #196                // Method net/minecraft/item/ItemStack.getItem:()Lnet/minecraft/item/Item;
     * 139: instanceof    #275                // class net/minecraft/item/BannerItem
     * 142: ifeq          175
     */
    @Inject(
        method = "transferSlot",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/item/ItemStack;getItem()Lnet/minecraft/item/Item;",
            ordinal = 0,
            shift = At.Shift.BEFORE
        ),
        cancellable = true
    )
    private void attemptFrameBannerPatternItemTransfer(PlayerEntity player, int slotIdx, CallbackInfoReturnable<ItemStack> cir) {
        ItemStack stack = this.slots.get(slotIdx).getStack();

        if (stack.getItem() instanceof FrameBannerPatternProvider) {
            if (!this.insertItem(stack, this.patternSlot.id, this.patternSlot.id + 1, false)) {
                cir.setReturnValue(ItemStack.EMPTY);
            }
        }
    }
}

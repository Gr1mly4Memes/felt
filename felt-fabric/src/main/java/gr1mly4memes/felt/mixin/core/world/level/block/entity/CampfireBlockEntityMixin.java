package gr1mly4memes.felt.mixin.core.world.level.block.entity;

import gr1mly4memes.felt.bridge.core.world.item.crafting.RecipeHolderBridge;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felttools.mixin.Local;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.event.block.BlockCookEvent;
import org.bukkit.event.block.CampfireStartEvent;
import org.bukkit.inventory.CampfireRecipe;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.Optional;

@Mixin(CampfireBlockEntity.class)
public abstract class CampfireBlockEntityMixin extends BlockEntityMixin implements gr1mly4memes.felt.bridge.core.world.level.block.entity.CampfireControlBridge {
    @org.spongepowered.asm.mixin.Unique private final boolean[] felt$stoppedCooking = new boolean[4];
    @Override public boolean[] bridge$stoppedCooking() { return this.felt$stoppedCooking; }
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = "cookTick",
        constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 1, ordinal = 0),
        slice = @org.spongepowered.asm.mixin.injection.Slice(
            from = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I", ordinal = 0),
            to = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I", ordinal = 1)), require = 1)
    private static int felt$pauseCookingIncrement(int increment,
            @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) CampfireBlockEntity entity,
            @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) int slot) {
        return ((gr1mly4memes.felt.bridge.core.world.level.block.entity.CampfireControlBridge)entity).bridge$stoppedCooking()[slot] ? 0 : increment;
    }
    @Inject(method = "saveAdditional", at = @At("RETURN"))
    private void felt$saveCookingState(net.minecraft.world.level.storage.ValueOutput output, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        byte[] state = new byte[4];
        for (int slot = 0; slot < state.length; ++slot) state[slot] = (byte)(this.felt$stoppedCooking[slot] ? 1 : 0);
        output.store("Paper.StopCooking", com.mojang.serialization.Codec.BYTE_BUFFER, java.nio.ByteBuffer.wrap(state));
    }
    @Inject(method = "loadAdditional", at = @At("RETURN"))
    private void felt$loadCookingState(net.minecraft.world.level.storage.ValueInput input, org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci) {
        java.util.Arrays.fill(this.felt$stoppedCooking, false);
        input.read("Paper.StopCooking", com.mojang.serialization.Codec.BYTE_BUFFER).ifPresent(state -> {
            for (int slot = 0; slot < this.felt$stoppedCooking.length && state.hasRemaining(); ++slot) this.felt$stoppedCooking[slot] = state.get() == 1;
        });
    }

    // @formatter:off
    @Shadow @Final public int[] cookingTime;
    // @formatter:on

    @Decorate(method = "cookTick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void felt$cookEvent(Level instance, double x, double y, double z, ItemStack resultStack,
                                           ServerLevel level, BlockPos blockPos, BlockState blockState, CampfireBlockEntity campfireBlockEntity,
                                           @Local(ordinal = 0) ItemStack sourceStack) throws Throwable {
        CraftItemStack source = CraftItemStack.asCraftMirror(sourceStack);
        org.bukkit.inventory.ItemStack result = CraftItemStack.asBukkitCopy(resultStack);

        BlockCookEvent blockCookEvent = new BlockCookEvent(CraftBlock.at(level, blockPos), source, result);
        Bukkit.getPluginManager().callEvent(blockCookEvent);

        if (blockCookEvent.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }

        result = blockCookEvent.getResult();
        resultStack = CraftItemStack.asNMSCopy(result);
        DecorationOps.callsite().invoke(instance, x, y, z, resultStack);
    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingTime:[I", opcode = org.objectweb.asm.Opcodes.GETFIELD),
        require = 1, expect = 1, allow = 1)
    private void felt$validateStartRecipe(ServerLevel serverLevel, LivingEntity entity, ItemStack stack,
            CallbackInfoReturnable<Boolean> cir, int i, ItemStack oldStack, Optional<RecipeHolder<CampfireCookingRecipe>> recipe,
            @Share("felt$startRecipe") LocalRef<CampfireRecipe> prepared) {
        var converted = ((RecipeHolderBridge) (Object) recipe.orElseThrow()).bridge$toBukkitRecipe();
        if (!(converted instanceof CampfireRecipe campfire)) {
            throw new IllegalArgumentException("Campfire recipe cannot provide a valid Bukkit cooking result: " + recipe.get().id());
        }
        prepared.set(campfire);
    }

    @Inject(method = "placeFood", locals = LocalCapture.CAPTURE_FAILHARD,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/level/block/entity/CampfireBlockEntity;cookingProgress:[I"))
    private void felt$cookStart(ServerLevel serverLevel, LivingEntity entity, ItemStack stack, CallbackInfoReturnable<Boolean> cir, int i, ItemStack oldStack, Optional<RecipeHolder<CampfireCookingRecipe>> recipe,
            @Share("felt$startRecipe") LocalRef<CampfireRecipe> prepared) {
        var event = new CampfireStartEvent(CraftBlock.at(this.level, this.worldPosition), CraftItemStack.asCraftMirror(stack), prepared.get());
        Bukkit.getPluginManager().callEvent(event);
        this.cookingTime[i] = event.getTotalCookTime();
    }
}

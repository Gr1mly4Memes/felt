package gr1mly4memes.felt.mixin.core.world.inventory;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import gr1mly4memes.felt.mod.server.world.FeltExperience;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.entity.ExperienceOrb.SpawnReason;
import org.bukkit.event.block.BlockExpEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.inventory.GrindstoneMenu$4")
public abstract class GrindstoneExperienceMixin {
    @Unique private Player felt$grindstoneUser;

    @WrapOperation(method = "onTake", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/inventory/ContainerLevelAccess;execute(Ljava/util/function/BiConsumer;)V"))
    private void felt$scopeGrindstoneExperience(ContainerLevelAccess access, BiConsumer<Level, BlockPos> experience,
            Operation<Void> original, Player player, ItemStack result) {
        original.call(access, (BiConsumer<Level, BlockPos>) (level, pos) -> {
            Player previous = this.felt$grindstoneUser;
            this.felt$grindstoneUser = player;
            try {
                experience.accept(level, pos);
            } finally {
                this.felt$grindstoneUser = previous;
            }
        });
    }

    @WrapOperation(method = "lambda$onTake$0", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/ExperienceOrb;award(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/phys/Vec3;I)V"))
    private void felt$attributeGrindstoneExperience(ServerLevel level, Vec3 position, int amount, Operation<Void> original,
            Level world, BlockPos block) {
        var player = this.felt$grindstoneUser;
        var event = new BlockExpEvent(CraftBlock.at(world, block), amount);
        event.callEvent();
        FeltExperience.withAwardAttribution(SpawnReason.GRINDSTONE, player, null,
            () -> original.call(level, position, event.getExpToDrop()));
    }
}

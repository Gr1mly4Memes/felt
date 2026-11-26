package gr1mly4memes.felt.mixin.core.core.dispenser;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Cancellable;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felt.mod.server.block.DispenserBlockHooks;
import net.minecraft.core.Direction;
import net.minecraft.core.Position;
import net.minecraft.core.dispenser.BlockSource;
import net.minecraft.core.dispenser.ProjectileDispenseBehavior;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileItem;
import net.minecraft.world.level.Level;
import org.bukkit.craftbukkit.inventory.CraftItemStack;
import org.bukkit.craftbukkit.projectiles.CraftBlockProjectileSource;
import org.bukkit.util.Vector;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ProjectileDispenseBehavior.class)
public abstract class ProjectileDispenseBehaviorMixin {
    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/item/ProjectileItem;asProjectile(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/Position;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/core/Direction;)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile felt$selectProjectile(ProjectileItem item, Level level, Position position, ItemStack stack, Direction direction,
                                              Operation<Projectile> create, @Local(argsOnly = true) BlockSource source,
                                              @Share("projectileSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected,
                                              @Cancellable CallbackInfoReturnable<ItemStack> cir) {
        var selection = DispenserBlockHooks.selectDispensedItem(source, (ProjectileDispenseBehavior) (Object) this, stack,
            new Vector(direction.getStepX(), direction.getStepY(), direction.getStepZ()));
        if (!selection.proceed() || stack.isEmpty()) {
            cir.setReturnValue(stack);
            return null;
        }
        selected.set(selection);
        Projectile projectile = create.call(item, level, position, CraftItemStack.asNMSCopy(selection.event().getItem()), direction);
        ((EntityBridge) projectile).bridge$setProjectileSource(new CraftBlockProjectileSource(source.blockEntity()));
        return projectile;
    }

    @WrapOperation(method = "execute", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/world/entity/projectile/Projectile;spawnProjectileUsingShoot(Lnet/minecraft/world/entity/projectile/Projectile;Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/item/ItemStack;DDDFF)Lnet/minecraft/world/entity/projectile/Projectile;"))
    private Projectile felt$launchProjectile(Projectile projectile, ServerLevel level, ItemStack stack, double x, double y, double z,
                                              float power, float uncertainty, Operation<Projectile> launch,
                                              @Share("projectileSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected) {
        var velocity = selected.get().event().getVelocity();
        return launch.call(projectile, level, stack.copyWithCount(1), velocity.getX(), velocity.getY(), velocity.getZ(), power, uncertainty);
    }

    @WrapWithCondition(method = "execute", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;shrink(I)V"))
    private boolean felt$consumeProjectileItem(ItemStack stack, int count,
                                               @Share("projectileSelection") LocalRef<DispenserBlockHooks.DispenseSelection> selected) {
        return selected.get().consumeOriginal();
    }
}

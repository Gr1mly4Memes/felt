package gr1mly4memes.felt.mixin.core.world.entity.monster;

import gr1mly4memes.felt.bridge.core.world.entity.MobBridge;
import gr1mly4memes.felt.bridge.core.world.entity.npc.VillagerBridge;
import gr1mly4memes.felt.bridge.core.world.level.WorldBridge;
import gr1mly4memes.felt.mixin.core.world.entity.PathfinderMobMixin;
import gr1mly4memes.felt.mod.mixins.annotation.TransformAccess;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.NbtOps;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.monster.zombie.ZombieVillager;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.ServerLevelAccessor;
import org.bukkit.Bukkit;
import org.bukkit.entity.Zombie;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityCombustByEntityEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTransformEvent;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

@Mixin(net.minecraft.world.entity.monster.zombie.Zombie.class)
public abstract class ZombieMixin extends PathfinderMobMixin {

    @Decorate(method = "doHurtTarget", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void felt$entityCombust(Entity entity, float seconds) throws Throwable {
        EntityCombustByEntityEvent event = new EntityCombustByEntityEvent(this.getBukkitEntity(), entity.bridge$getBukkitEntity(), seconds);
        Bukkit.getPluginManager().callEvent(event);
        if (!event.isCancelled()) {
            DecorationOps.callsite().invoke(entity, (float) event.getDuration());
        }
    }

    @SuppressWarnings("unchecked")
    @Decorate(method = "convertVillagerToZombieVillager", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/npc/villager/Villager;convertTo(Lnet/minecraft/world/entity/EntityType;Lnet/minecraft/world/entity/ConversionParams;Lnet/minecraft/world/entity/ConversionParams$AfterConversion;)Lnet/minecraft/world/entity/Mob;"))
    private <T extends Mob> T felt$transform(Villager villagerEntity, EntityType<T> entityType, ConversionParams conversionParams, ConversionParams.AfterConversion afterConversion) throws Throwable {
        ((WorldBridge) villagerEntity.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.INFECTION);
        ((MobBridge) villagerEntity).bridge$pushTransformReason(EntityTransformEvent.TransformReason.INFECTION);
        T t = (T) DecorationOps.callsite().invoke(villagerEntity, entityType, conversionParams, afterConversion);
        if (t == null) {
            return (T) DecorationOps.cancel().invoke(false);
        }
        return t;
    }

    @Inject(method = "finalizeSpawn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ServerLevelAccessor;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z"))
    private void felt$mount(ServerLevelAccessor worldIn, DifficultyInstance difficultyIn, EntitySpawnReason reason, SpawnGroupData spawnDataIn, CallbackInfoReturnable<SpawnGroupData> cir) {
        ((WorldBridge) worldIn.getLevel()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.MOUNT);
    }

    @Decorate(method = "hurtServer", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/monster/zombie/Zombie;setTarget(Lnet/minecraft/world/entity/LivingEntity;)V"))
    private void felt$spawnWithReasonMod(net.minecraft.world.entity.monster.zombie.Zombie zombie, LivingEntity livingEntity) throws Throwable {
        ((WorldBridge) this.level()).bridge$pushAddEntityReason(CreatureSpawnEvent.SpawnReason.REINFORCEMENTS);
        if (livingEntity != null) {
            ((MobBridge) zombie).bridge$pushGoalTargetReason(EntityTargetEvent.TargetReason.REINFORCEMENT_TARGET, true);
        }
        DecorationOps.callsite().invoke(zombie, livingEntity);
    }

    @TransformAccess(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)
    private static ZombieVillager zombifyVillager(ServerLevel level, Villager villager, BlockPos blockPosition, boolean silent, CreatureSpawnEvent.SpawnReason spawnReason) {
        return ((VillagerBridge) villager).bridge$zombify(level, blockPosition, silent, spawnReason);
    }
}

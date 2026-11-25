package gr1mly4memes.felt.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent;
import gr1mly4memes.felt.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.cubemob.AbstractCubeMob$CubeMobRandomDirectionGoal")
public abstract class SlimeRandomDirectionGoalMixin {

    @Shadow @Final private AbstractCubeMob cubeMob;
    @Shadow private float chosenDegrees;

    @com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "canUse", at = @At("RETURN"))
    private boolean felt$wanderingEnabled(boolean canUse) {
        return canUse && ((gr1mly4memes.felt.bridge.core.world.entity.monster.CubeMobControlBridge)this.cubeMob).bridge$canWander();
    }

    @Decorate(method = "tick()V", inject = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/cubemob/AbstractCubeMob$CubeMobRandomDirectionGoal;chosenDegrees:F",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
        require = 1, expect = 1, allow = 1)
    private void felt$changeDirectionEvent() throws Throwable {
        // Paper keeps the random yaw/timer writes, even when this tick is vetoed.
        SlimeChangeDirectionEvent event = new SlimeChangeDirectionEvent(
            (org.bukkit.entity.AbstractCubeMob) ((EntityBridge) this.cubeMob).bridge$getBukkitEntity(), this.chosenDegrees);
        if (!((gr1mly4memes.felt.bridge.core.world.entity.monster.CubeMobControlBridge)this.cubeMob).bridge$canWander()) {
            DecorationOps.cancel().invoke();
            return;
        }
        if (!event.callEvent()) {
            DecorationOps.cancel().invoke();
            return;
        }
        this.chosenDegrees = event.getNewYaw();
    }
}

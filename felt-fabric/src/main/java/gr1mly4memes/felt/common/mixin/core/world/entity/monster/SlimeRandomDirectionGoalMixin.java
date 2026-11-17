package gr1mly4memes.felt.common.mixin.core.world.entity.monster;

import com.destroystokyo.paper.event.entity.SlimeChangeDirectionEvent;
import gr1mly4memes.felt.common.bridge.core.entity.EntityBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.world.entity.monster.Slime;
import org.bukkit.Bukkit;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(targets = "net.minecraft.world.entity.monster.Slime$SlimeRandomDirectionGoal")
public abstract class SlimeRandomDirectionGoalMixin {

    @Shadow @Final private Slime slime;
    @Shadow private float chosenDegrees;

    @Decorate(method = "tick()V", inject = true,
        at = @At(value = "FIELD", target = "Lnet/minecraft/world/entity/monster/Slime$SlimeRandomDirectionGoal;chosenDegrees:F",
            opcode = Opcodes.PUTFIELD, shift = At.Shift.AFTER),
        require = 1, expect = 1, allow = 1)
    private void arclight$changeDirectionEvent() throws Throwable {
        // Paper keeps the random yaw/timer writes, even when this tick is vetoed.
        SlimeChangeDirectionEvent event = new SlimeChangeDirectionEvent(
            (org.bukkit.entity.Slime) ((EntityBridge) this.slime).bridge$getBukkitEntity(), this.chosenDegrees);
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            DecorationOps.cancel().invoke();
            return;
        }
        this.chosenDegrees = event.getNewYaw();
    }
}

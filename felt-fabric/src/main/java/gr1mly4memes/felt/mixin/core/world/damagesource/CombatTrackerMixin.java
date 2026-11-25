package gr1mly4memes.felt.mixin.core.world.damagesource;

import gr1mly4memes.felt.bridge.core.world.damagesource.CombatEntryBridge;
import gr1mly4memes.felt.bridge.core.world.damagesource.CombatTrackerBridge;
import io.papermc.paper.world.damagesource.PaperCombatTrackerWrapper;
import io.papermc.paper.configuration.GlobalConfiguration;
import net.minecraft.network.chat.Component;
import net.minecraft.util.ArrayListDeque;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.damagesource.CombatTracker;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(CombatTracker.class)
public abstract class CombatTrackerMixin implements CombatTrackerBridge {

    @Shadow @Final @Mutable public List<CombatEntry> entries;
    @Shadow @Final public LivingEntity mob;
    @Shadow private int lastDamageTime;
    @Shadow private int combatStartTime;
    @Shadow private int combatEndTime;
    @Shadow public boolean inCombat;
    @Shadow public boolean takingDamage;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void felt$initializeHistory(LivingEntity entity, CallbackInfo ci) {
        this.entries = new ArrayListDeque<>();
    }

    @Inject(method = "recordDamage", at = @At(value = "INVOKE", target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void felt$trimNativeCombatHistory(DamageSource source, float damage, CallbackInfo ci) {
        this.felt$trimCombatHistory();
    }

    @Unique
    private void felt$trimCombatHistory() {
        var configuration = GlobalConfiguration.get();
        if (configuration != null && !this.entries.isEmpty() && configuration.misc.maxTrackingCombatEntries.enabled()) {
            int limit = Math.max(0, configuration.misc.maxTrackingCombatEntries.intValue());
            while (this.entries.size() > limit) {
                this.entries.removeFirst();
            }
        }
    }

    @Shadow
    private static boolean shouldEnterCombat(DamageSource source) {
        throw new AssertionError();
    }

    @Override
    @Invoker("getMostSignificantFall")
    public abstract @Nullable CombatEntry bridge$getMostSignificantFall();

    @Unique
    private PaperCombatTrackerWrapper paperCombatTracker; // Paper added

    @Unique
    private Component felt$emptyComnent;

    @Inject(method = "getDeathMessage", cancellable = true, at = @At("HEAD"))
    private void felt$useOverride(CallbackInfoReturnable<Component> cir) {
        if (!this.entries.isEmpty()) {
            var entry = this.entries.get(this.entries.size() - 1);
            var deathMessage = ((CombatEntryBridge) (Object) entry).bridge$getDeathMessage();
            if (deathMessage != null) {
                cir.setReturnValue(deathMessage);
            }
        } else {
            if (this.felt$emptyComnent != null) {
                cir.setReturnValue(this.felt$emptyComnent);
            }
        }
        this.felt$emptyComnent = null;
    }

    @Override
    public void bridge$setDeathMessage(Component component) {
        this.felt$emptyComnent = component;
        if (!this.entries.isEmpty()) {
            var entry = this.entries.getLast();
            ((CombatEntryBridge) (Object) entry).bridge$setDeathMessage(component);
        }
    }

    @Override
    public PaperCombatTrackerWrapper bridge$getPaperCombatTracker() {
        if (this.paperCombatTracker == null) {
            this.paperCombatTracker = new PaperCombatTrackerWrapper((CombatTracker) (Object) this);
        }
        return this.paperCombatTracker;
    }

    @Override
    public List<CombatEntry> bridge$getEntries() {
        return this.entries;
    }

    @Override
    public LivingEntity bridge$getMob() {
        return this.mob;
    }

    @Override
    public boolean bridge$isInCombat() {
        return this.inCombat;
    }

    @Override
    public boolean bridge$isTakingDamage() {
        return this.takingDamage;
    }

    @Override
    public void bridge$recordDamageAndCheckCombatState(CombatEntry entry) {
        // Native recordDamage tail: keep this entry's supplied fall attribution intact.
        this.felt$trimCombatHistory();
        this.entries.add(entry);
        this.lastDamageTime = this.mob.tickCount;
        this.takingDamage = true;
        if (!this.inCombat && this.mob.isAlive() && shouldEnterCombat(entry.source())) {
            this.inCombat = true;
            this.combatStartTime = this.mob.tickCount;
            this.combatEndTime = this.combatStartTime;
            this.mob.onEnterCombat();
        }
    }

    @Override
    public void bridge$resetCombatState() {
        // Native recheckStatus reset tail, without its timeout/takingDamage guard.
        boolean wasInCombat = this.inCombat;
        this.takingDamage = false;
        this.inCombat = false;
        this.combatEndTime = this.mob.tickCount;
        if (wasInCombat) {
            this.mob.onLeaveCombat();
        }
        this.entries.clear();
    }

    public void recordDamageAndCheckCombatState(net.minecraft.world.damagesource.CombatEntry entry) {
        this.bridge$recordDamageAndCheckCombatState(entry);
    }

    public void resetCombatState() {
        this.bridge$resetCombatState();
    }

}

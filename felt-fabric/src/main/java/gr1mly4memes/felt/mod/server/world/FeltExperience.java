package gr1mly4memes.felt.mod.server.world;

import gr1mly4memes.felt.bridge.core.world.entity.ExperienceOrbBridge;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.phys.Vec3;
import org.bukkit.entity.ExperienceOrb.SpawnReason;
import org.jetbrains.annotations.Nullable;

public final class FeltExperience {
    public record Attribution(SpawnReason reason, @Nullable UUID trigger, @Nullable UUID source) {
        public static final Attribution UNKNOWN = new Attribution(SpawnReason.UNKNOWN, null, null);

        public static Attribution of(SpawnReason reason, @Nullable Entity trigger, @Nullable Entity source) {
            return new Attribution(reason, trigger == null ? null : trigger.getUUID(), source == null ? null : source.getUUID());
        }
    }

    private static final ThreadLocal<Attribution> PENDING_AWARD = new ThreadLocal<>();

    private FeltExperience() {}

    public static ExperienceOrb withAttribution(ExperienceOrb orb, SpawnReason reason, @Nullable Entity trigger, @Nullable Entity source) {
        ((ExperienceOrbBridge) orb).bridge$setAttribution(Attribution.of(reason, trigger, source));
        return orb;
    }

    public static void award(ServerLevel level, Vec3 pos, Vec3 direction, int amount, SpawnReason reason,
                             @Nullable Entity trigger, @Nullable Entity source) {
        withAwardAttribution(reason, trigger, source, () -> ExperienceOrb.awardWithDirection(level, pos, direction, amount));
    }

    public static void withAwardAttribution(SpawnReason reason, @Nullable Entity trigger, @Nullable Entity source, Runnable award) {
        Attribution previous = PENDING_AWARD.get();
        PENDING_AWARD.set(Attribution.of(reason, trigger, source));
        try {
            award.run();
        } finally {
            if (previous == null) {
                PENDING_AWARD.remove();
            } else {
                PENDING_AWARD.set(previous);
            }
        }
    }

    public static @Nullable Attribution takeAwardAttribution() {
        Attribution attribution = PENDING_AWARD.get();
        PENDING_AWARD.remove();
        return attribution;
    }

    public static void withDefaultAwardAttribution(SpawnReason reason, Runnable award) {
        if (PENDING_AWARD.get() == null) {
            withAwardAttribution(reason, null, null, award);
        } else {
            award.run();
        }
    }
}

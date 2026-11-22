package gr1mly4memes.felt.mod.server.entity;

import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;
import org.bukkit.craftbukkit.CraftServer;
import org.bukkit.craftbukkit.entity.CraftAbstractSkeleton;
import org.bukkit.entity.Skeleton;
import org.jetbrains.annotations.NotNull;

public class FeltModAbstractSkeleton extends CraftAbstractSkeleton {
    public FeltModAbstractSkeleton(CraftServer server, AbstractSkeleton entity) {
        super(server, entity);
    }

    @NotNull
    @Override
    public Skeleton.SkeletonType getSkeletonType() {
        return Skeleton.SkeletonType.NORMAL;
    }

    @Override
    public void setSkeletonType(Skeleton.SkeletonType type) {
    }
}

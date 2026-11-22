package gr1mly4memes.felt.mixin.core.world.level.block;

import gr1mly4memes.felt.bridge.core.world.level.block.SculkSpreaderBridge;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSpreader;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.event.block.SculkBloomEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(SculkSpreader.class)
public abstract class SculkSpreaderMixin implements SculkSpreaderBridge {

    // @formatter:off
    @Shadow public abstract boolean isWorldGeneration();
    // @formatter:on

    private transient Level level; // Paper added

    @Override
    public void bridge$setLevel(Level level) {
        this.level = level;
    }

    @Inject(method = "addCursor", cancellable = true, at = @At(value = "INVOKE", remap = false, target = "Ljava/util/List;add(Ljava/lang/Object;)Z"))
    private void felt$bloomEvent(SculkSpreader.ChargeCursor cursor, CallbackInfo ci) {
        if (!isWorldGeneration() && this.level != null) {
            var bukkitBlock = CraftBlock.at(this.level, cursor.pos);
            var event = new SculkBloomEvent(bukkitBlock, cursor.getCharge());
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled()) {
                ci.cancel();
            }
            cursor.charge = event.getCharge();
        }
    }
}

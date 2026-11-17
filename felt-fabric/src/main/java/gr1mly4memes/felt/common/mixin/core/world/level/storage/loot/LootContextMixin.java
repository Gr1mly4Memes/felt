package gr1mly4memes.felt.common.mixin.core.world.level.storage.loot;

import gr1mly4memes.felt.common.bridge.core.world.level.storage.loot.LootContextBridge;
import net.minecraft.world.level.storage.loot.LootContext;
import org.spongepowered.asm.mixin.Mixin;

@Mixin(LootContext.class)
public class LootContextMixin implements LootContextBridge {
}

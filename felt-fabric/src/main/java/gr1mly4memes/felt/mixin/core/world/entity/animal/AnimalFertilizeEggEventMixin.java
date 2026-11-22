package gr1mly4memes.felt.mixin.core.world.entity.animal;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import gr1mly4memes.felt.bridge.core.world.entity.animal.AnimalFertilizeEggBridge;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.animal.frog.Frog;
import net.minecraft.world.entity.animal.sniffer.Sniffer;
import org.bukkit.craftbukkit.event.CraftEventFactory;
import org.spongepowered.asm.mixin.Mixin;

@Mixin({Frog.class, Sniffer.class})
public abstract class AnimalFertilizeEggEventMixin {

    @WrapMethod(method = "spawnChildFromBreeding(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/animal/Animal;)V")
    private void felt$fertilizeEgg(ServerLevel level, Animal partner, Operation<Void> original) {
        Animal animal = (Animal) (Object) this;
        var event = CraftEventFactory.callEntityFertilizeEggEvent(animal, partner);
        if (event.isCancelled()) {
            return;
        }

        AnimalFertilizeEggBridge bridge = (AnimalFertilizeEggBridge) animal;
        Integer previous = bridge.bridge$setFertilizeEggExperience(event.getExperience());
        try {
            // Keep each species' native finalization, pregnancy/egg, sound and insertion path.
            original.call(level, partner);
        } finally {
            bridge.bridge$setFertilizeEggExperience(previous);
        }
    }
}

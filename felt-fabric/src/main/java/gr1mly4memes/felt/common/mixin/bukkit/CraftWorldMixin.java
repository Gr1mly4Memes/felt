package gr1mly4memes.felt.common.mixin.bukkit;

import gr1mly4memes.felt.common.bridge.core.server.level.ServerLevelBridge;
import gr1mly4memes.felt.common.bridge.core.world.level.saveddata.WeatherDataBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import gr1mly4memes.felt.common.mod.server.world.BukkitWorldIdentity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.WeatherData;
import org.bukkit.craftbukkit.CraftWorld;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.nio.file.Path;

@Mixin(CraftWorld.class)
public abstract class CraftWorldMixin {

    @Decorate(method = "setStorm(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"), remap = false)
    private void arclight$stormScope(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of(this.world, org.bukkit.event.weather.WeatherChangeEvent.Cause.PLUGIN));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Decorate(method = "setThundering(Z)V", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"), remap = false)
    private void arclight$thunderScope(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of(this.world, org.bukkit.event.weather.WeatherChangeEvent.Cause.PLUGIN));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    // @formatter:off
    @Shadow @Final private ServerLevel world;
    // @formatter:on

    /**
     * @author IzzelAliz
     * @reason Resolve both Path and the World#getWorldFolder default through the
     * per-world storage bridge, preserving Bukkit identity for mod dimensions.
     */
    @Overwrite(remap = false)
    public Path getWorldPath() {
        return BukkitWorldIdentity.getWorldFolder(((ServerLevelBridge) this.world).bridge$getConvertable(), this.world.dimension());
    }
}

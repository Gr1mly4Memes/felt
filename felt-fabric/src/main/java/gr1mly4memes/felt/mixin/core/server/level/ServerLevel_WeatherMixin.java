package gr1mly4memes.felt.mixin.core.server.level;

import com.llamalad7.mixinextras.injector.v2.WrapWithCondition;
import gr1mly4memes.felt.bridge.core.world.level.saveddata.WeatherDataBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import net.minecraft.core.Holder;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.protocol.Packet;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;
import net.minecraft.world.level.saveddata.WeatherData;
import net.minecraft.world.level.storage.SavedDataStorage;
import net.minecraft.world.level.storage.WritableLevelData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Group;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ServerLevel.class)
public abstract class ServerLevel_WeatherMixin extends Level {

    @Shadow public abstract SavedDataStorage getDataStorage();

    protected ServerLevel_WeatherMixin(WritableLevelData levelData, ResourceKey<Level> dimension, RegistryAccess registryAccess, Holder<DimensionType> dimensionType, boolean isClientSide, boolean isDebug, long biomeZoomSeed, int maxChainedNeighborUpdates) {
        super(levelData, dimension, registryAccess, dimensionType, isClientSide, isDebug, biomeZoomSeed, maxChainedNeighborUpdates);
    }

    @Group(name = "felt$weatherBroadcasts", min = 6, max = 6)
    @WrapWithCondition(method = "advanceWeatherCycle()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;Lnet/minecraft/resources/ResourceKey;)V"), require = 0, allow = 6)
    private boolean felt$deferDimensionWeather(PlayerList list, Packet<?> packet, ResourceKey<Level> dimension) {
        return false;
    }

    // The Sift only narrows these sends to the ticking dimension (#2197).
    // Guard the redirected call too: the per-player delivery below already scopes it and honors overrides.
    @Group(name = "felt$weatherBroadcasts", min = 6, max = 6)
    @WrapWithCondition(method = "advanceWeatherCycle()V", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;broadcastAll(Lnet/minecraft/network/protocol/Packet;)V"), require = 0, allow = 4)
    private boolean felt$deferGlobalWeather(PlayerList list, Packet<?> packet) {
        return false;
    }

    @Inject(method = "advanceWeatherCycle()V", at = @At("RETURN"))
    private void felt$personalWeather(org.spongepowered.asm.mixin.injection.callback.CallbackInfo ci, @com.llamalad7.mixinextras.sugar.Local(ordinal = 0) boolean wasRaining) {
        ServerLevel world = (ServerLevel) (Object) this;
        for (var player : world.players()) {
            if (player.level() == world) {
                ((gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge) player).tickWeather();
            }
        }
        if (wasRaining != world.isRaining()) {
            for (var player : world.players()) {
                if (player.level() == world) {
                    ((gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge) player).setPlayerWeather(wasRaining ? org.bukkit.WeatherType.CLEAR : org.bukkit.WeatherType.DOWNFALL, false);
                }
            }
        }
        for (var player : world.players()) {
            if (player.level() == world) {
                ((gr1mly4memes.felt.bridge.core.server.level.ServerPlayerBridge) player).updateWeather(this.oRainLevel, this.rainLevel, this.oThunderLevel, this.thunderLevel);
            }
        }
    }
    private WeatherData weatherData; // Paper added

    private WeatherData felt$getWeatherData() {
        if (this.weatherData == null) {
            this.weatherData = this.getDataStorage().computeIfAbsent(WeatherData.TYPE);
            ((WeatherDataBridge) (Object) this.weatherData).bridge$setWorld((ServerLevel) (Object) this);
        }
        return this.weatherData;
    }

    @Inject(method = "getWeatherData", at = @At("HEAD"), cancellable = true)
    private void felt$worldWeather(CallbackInfoReturnable<WeatherData> cir) {
        cir.setReturnValue(this.felt$getWeatherData());
    }

    // The native constructor has assigned chunkSource before preparing weather.
    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;getWeatherData()Lnet/minecraft/world/level/saveddata/WeatherData;"))
    private WeatherData felt$prepareWorldWeather(MinecraftServer server) {
        return this.felt$getWeatherData();
    }
    @Decorate(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void felt$advanceRain(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.NATURAL));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Decorate(method = "advanceWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void felt$advanceThunder(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.NATURAL));
        try {
            DecorationOps.callsite().invoke(weather, value);
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Redirect(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRainTime(I)V"))
    private void felt$deferRainReset(WeatherData weather, int time) {
        // Apply only after the cancellable rain-state transition below.
    }

    @Redirect(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThunderTime(I)V"))
    private void felt$deferThunderReset(WeatherData weather, int time) {
        // Apply only after the cancellable thunder-state transition below.
    }

    @Decorate(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setRaining(Z)V"))
    private void felt$resetRain(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.SLEEP));
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isRaining()) {
                weather.setRainTime(0);
            }
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

    @Decorate(method = "resetWeatherCycle", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/WeatherData;setThundering(Z)V"))
    private void felt$resetThunder(WeatherData weather, boolean value) throws Throwable {
        var weatherBridge = (WeatherDataBridge) (Object) weather;
        var previous = weatherBridge.bridge$swapWeatherCause(com.mojang.datafixers.util.Pair.of((ServerLevel) (Object) this, org.bukkit.event.weather.WeatherChangeEvent.Cause.SLEEP));
        try {
            DecorationOps.callsite().invoke(weather, value);
            if (!weather.isThundering()) {
                weather.setThunderTime(0);
            }
        } finally {
            weatherBridge.bridge$swapWeatherCause(previous);
        }
    }

}

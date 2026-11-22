package gr1mly4memes.felt.mixin.core.server.network.config;

import gr1mly4memes.felt.bridge.core.server.network.config.PrepareSpawnTaskLocationEventBridge;
import gr1mly4memes.felttools.mixin.Decorate;
import gr1mly4memes.felttools.mixin.DecorationOps;
import io.papermc.paper.connection.PlayerConfigurationConnection;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.config.PrepareSpawnTask;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Objects;
import java.util.Optional;

@Mixin(PrepareSpawnTask.class)
public abstract class PrepareSpawnTaskLocationEventMixin implements PrepareSpawnTaskLocationEventBridge {

    @Unique private PlayerConfigurationConnection felt$spawnLocationConnection;
    @Unique private @Nullable ServerPlayer felt$spawnLocationPlayer;
    @Unique private Connection felt$spawnLocationNetwork;
    @Unique private boolean newPlayer; // Paper added

    @Override
    public void bridge$bindSpawnLocationConnection(PlayerConfigurationConnection connection, Connection network, @Nullable ServerPlayer player) {
        this.felt$spawnLocationConnection = Objects.requireNonNull(connection, "configuration connection");
        this.felt$spawnLocationPlayer = player;
        this.felt$spawnLocationNetwork = Objects.requireNonNull(network, "network connection");
    }

    @Override
    public PlayerConfigurationConnection bridge$getSpawnLocationConnection() {
        return Objects.requireNonNull(this.felt$spawnLocationConnection, "Unbound spawn-location task");
    }

    @Override
    public @Nullable ServerPlayer bridge$getSpawnLocationPlayer() {
        return this.felt$spawnLocationPlayer;
    }

    @Override
    public Connection bridge$getSpawnLocationNetwork() {
        return Objects.requireNonNull(this.felt$spawnLocationNetwork, "Unbound spawn-location network");
    }

    @Override
    public boolean bridge$isNewSpawnPlayer() {
        return this.newPlayer;
    }

    @Decorate(method = "start(Ljava/util/function/Consumer;)V", require = 1, expect = 1, allow = 1,
        at = @At(value = "INVOKE", target = "Lnet/minecraft/server/players/PlayerList;loadPlayerData(Lnet/minecraft/server/players/NameAndId;)Ljava/util/Optional;"))
    private Optional<CompoundTag> felt$captureNewSpawnPlayer(PlayerList list, NameAndId profile) throws Throwable {
        var data = (Optional<CompoundTag>) DecorationOps.callsite().invoke(list, profile);
        this.newPlayer = data.isEmpty();
        return data;
    }
}

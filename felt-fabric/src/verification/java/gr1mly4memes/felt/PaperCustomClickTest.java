package gr1mly4memes.felt;

import gr1mly4memes.felt.bridge.core.server.network.ServerCommonPacketListenerImplBridge;
import gr1mly4memes.felt.bridge.core.server.network.ServerConfigurationPacketListenerImplBridge;
import gr1mly4memes.felt.bridge.core.server.network.ServerGamePacketListenerImplBridge;
import io.papermc.paper.adventure.providers.ClickCallbackProviderImpl;
import io.papermc.paper.event.player.PlayerCustomClickEvent;
import java.util.Optional;
import java.util.UUID;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.event.ClickCallback;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.common.ServerboundCustomClickActionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;
import org.bukkit.plugin.Plugin;

final class PaperCustomClickTest {
    private static final String RESPONSE = "from the client";
    private final UUID adventure;
    private final UUID dialog = UUID.randomUUID();
    private Audience adventureAudience;
    private Audience dialogAudience;
    private int adventureUses;
    private int dialogUses;

    PaperCustomClickTest() {
        var options = ClickCallback.Options.builder().uses(1).build();
        this.adventure = ClickCallbackProviderImpl.ADVENTURE_CLICK_MANAGER.addCallback(audience -> {
            this.adventureAudience = audience;
            this.adventureUses++;
        }, options);
        ClickCallbackProviderImpl.DIALOG_CLICK_MANAGER.addCallback(this.dialog, (response, audience) -> {
            check(RESPONSE.equals(response.getText("answer")), "Dialog callback lost the clicked response");
            this.dialogAudience = audience;
            this.dialogUses++;
        }, options);
    }

    void verify(ServerLevel world, Plugin plugin) throws Exception {
        var player = PaperReconfigurationTest.createTestPlayer(world, "ClickFixture");
        var game = ((ServerGamePacketListenerImplBridge) player.connection).bridge$getGameConnection();
        var connection = ((ServerCommonPacketListenerImplBridge) player.connection).bridge$getConnection();
        var configurationListener = new ServerConfigurationPacketListenerImpl(world.getServer(), connection,
            CommonListenerCookie.createInitial(player.getGameProfile(), false));
        var configuration = ((ServerConfigurationPacketListenerImplBridge) configurationListener).bridge$getConfigurationConnection();
        var clickCount = new int[1];
        try (var listener = PaperEventHooksTest.listen(PlayerCustomClickEvent.getHandlerList(), PlayerCustomClickEvent.class, plugin, event -> {
            clickCount[0]++;
            switch (event.getIdentifier().asString()) {
                case "paper:click_callback" -> check(event.getCommonConnection() == game && event.getTag() != null,
                    "Adventure click lost its payload or play connection");
                case "paper:dialog_click_callback" -> check(event.getCommonConnection() == configuration && event.getTag() != null
                    && event.getDialogResponseView() != null && RESPONSE.equals(event.getDialogResponseView().getText("answer")),
                    "Dialog click lost its payload, response view or configuration connection");
                case "felt:empty_click" -> check(event.getCommonConnection() == configuration
                    && event.getTag() == null && event.getDialogResponseView() == null, "Empty click gained a payload");
                default -> throw new AssertionError("Unexpected click " + event.getIdentifier());
            }
        })) {
            var adventurePacket = packet("click_callback", this.adventure, false);
            player.connection.handleCustomClickAction(adventurePacket);
            player.connection.handleCustomClickAction(adventurePacket);
            var dialogPacket = packet("dialog_click_callback", this.dialog, true);
            configurationListener.handleCustomClickAction(dialogPacket);
            configurationListener.handleCustomClickAction(dialogPacket);
            configurationListener.handleCustomClickAction(new ServerboundCustomClickActionPacket(
                Identifier.fromNamespaceAndPath("felt", "empty_click"), Optional.empty()));
        }
        check(clickCount[0] == 5 && this.adventureUses == 1 && this.dialogUses == 1
            && this.adventureAudience == player.getBukkitEntity() && this.dialogAudience == configuration.getAudience(),
            "The tick loop did not register or consume both one-use Paper callbacks with their connection audiences");
        System.out.println("Paper custom click API: play and configuration events, dialog payloads and one-use callbacks passed");
    }

    private static ServerboundCustomClickActionPacket packet(String path, UUID id, boolean dialog) {
        var payload = new CompoundTag();
        payload.store(ClickCallbackProviderImpl.ID_KEY, UUIDUtil.CODEC, id);
        if (dialog) {
            payload.putString("answer", RESPONSE);
        }
        return new ServerboundCustomClickActionPacket(Identifier.fromNamespaceAndPath("paper", path), Optional.of(payload));
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

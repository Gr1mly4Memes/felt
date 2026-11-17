package gr1mly4memes.felt.fabric.mod.event;

import gr1mly4memes.felt.common.mod.server.event.EntityEventHandler;

public class EventHandlerRegistry {
    public static void register() {
        LivingDropsEvent.EVENT.register(EntityEventHandler::monitorLivingDrops);
        S2CPlayNConfigChannelHandler.register();
    }
}

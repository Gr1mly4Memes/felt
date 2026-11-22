package gr1mly4memes.felt.mod.event;

import gr1mly4memes.felt.mod.server.event.EntityEventHandler;

public class EventHandlerRegistry {
    public static void register() {
        LivingDropsEvent.EVENT.register(EntityEventHandler::monitorLivingDrops);
        S2CPlayNConfigChannelHandler.register();
    }
}

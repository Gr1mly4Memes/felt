package gr1mly4memes.felt.bridge.core.world.entity.projectile;

public interface FishingHookBridge {

    void bridge$resetTimeUntilLured();

    net.minecraft.world.InteractionHand bridge$getRetrieveHand();

    net.minecraft.world.InteractionHand bridge$swapRetrieveHand(net.minecraft.world.InteractionHand hand);

    default void resetTimeUntilLured() {
        this.bridge$resetTimeUntilLured();
    }

}

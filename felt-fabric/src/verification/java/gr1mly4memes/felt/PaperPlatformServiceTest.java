package gr1mly4memes.felt;

import ca.spottedleaf.moonrise.common.PlatformHooks;
import java.util.ServiceLoader;

/** Check the provider descriptor and linkage without initializing a Minecraft world. */
public final class PaperPlatformServiceTest {
    public static void main(String[] args) {
        var providers = ServiceLoader.load(PlatformHooks.class, PlatformHooks.class.getClassLoader())
            .stream().map(ServiceLoader.Provider::type).toList();
        if (providers.size() != 1 || !providers.getFirst().getName().equals("ca.spottedleaf.moonrise.paper.PaperHooks")) {
            throw new AssertionError("Expected exactly one Paper platform provider, got " + providers);
        }
        System.out.println("Paper platform service descriptor and provider linkage passed");
    }
}

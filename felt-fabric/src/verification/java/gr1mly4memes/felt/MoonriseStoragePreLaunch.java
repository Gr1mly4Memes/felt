package gr1mly4memes.felt;

import net.fabricmc.loader.api.entrypoint.PreLaunchEntrypoint;

/** Included only in the verification mod, never in Felt's production artifact. */
public final class MoonriseStoragePreLaunch implements PreLaunchEntrypoint {
    @Override public void onPreLaunch() {
        System.setProperty("felt.verify.storageMixins", "true");
        try {
            if (Boolean.getBoolean("felt.verify.world")) MoonriseWorldTest.main(new String[0]);
            else if (Boolean.getBoolean("felt.verify.tickets")) MoonriseTicketManagerTest.main(new String[0]);
            else MoonriseRegionFilesTest.main(new String[0]);
        } catch (Exception failure) {
            throw new IllegalStateException("Moonrise mixin verification failed", failure);
        }
    }
}

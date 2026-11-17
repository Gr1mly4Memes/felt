package gr1mly4memes.felt.common.bridge.core.server.bossevents;

import org.bukkit.boss.KeyedBossBar;

public interface CustomBossEventBridge {

    default KeyedBossBar getBukkitEntity() {
        return this.bridge$getBukkitEntity();
    }

    KeyedBossBar bridge$getBukkitEntity();
}

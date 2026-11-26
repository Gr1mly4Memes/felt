package gr1mly4memes.felt;

import io.papermc.paper.math.Position;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;

final class PaperWorldCreatorTest {
    private static final Position SPAWN = Position.block(32, -60, 48);
    private static final float YAW = 57.0F;
    private static final float PITCH = 19.0F;
    private static final long SEED = 402578L;

    static void verify() {
        String name = "felt_creator_" + UUID.randomUUID();
        var creator = new WorldCreator(name).seed(SEED).type(WorldType.FLAT).generateStructures(false).hardcore(true)
            .generatorSettings("""
                {"layers":[{"block":"minecraft:bedrock","height":1},{"block":"minecraft:gold_block","height":2}],
                "biome":"minecraft:plains"}
                """).forcedSpawnPosition(SPAWN, YAW, PITCH);
        World original = Objects.requireNonNull(Bukkit.createWorld(creator), "Could not create the Paper flat fixture");
        var uid = original.getUID();
        try {
            verifyWorld(original);
        } finally {
            check(Bukkit.unloadWorld(original, true), "Could not unload the Paper flat fixture");
        }
        var reopened = Objects.requireNonNull(Bukkit.createWorld(new WorldCreator(name).seed(-9L).type(WorldType.NORMAL)
            .forcedSpawnPosition(Position.block(80, -40, 80), 12.0F, 15.0F)), "Could not reopen the Paper flat fixture");
        try {
            check(reopened.getUID().equals(uid), "Reopened WorldCreator world changed its identity");
            verifyWorld(reopened);
        } finally {
            check(Bukkit.unloadWorld(reopened, true), "Could not unload the reopened Paper flat fixture");
        }
        System.out.println("Paper world creator API: flat settings, generated blocks, forced spawn and save/reopen passed");
    }

    private static void verifyWorld(World world) {
        check(world.getWorldType() == WorldType.FLAT && world.getSeed() == SEED && !world.canGenerateStructures() && world.isHardcore(),
            "WorldCreator lost the requested or saved generator, seed, structure or hardcore options");
        var spawn = world.getSpawnLocation();
        check(spawn.getBlockX() == SPAWN.blockX() && spawn.getBlockY() == SPAWN.blockY() && spawn.getBlockZ() == SPAWN.blockZ()
            && spawn.getYaw() == YAW && spawn.getPitch() == PITCH, "WorldCreator did not preserve its forced spawn and rotation");
        int floor = world.getMinHeight();
        check(world.getBlockAt(32, floor, 48).getType() == Material.BEDROCK
            && world.getBlockAt(32, floor + 1, 48).getType() == Material.GOLD_BLOCK
            && world.getBlockAt(32, floor + 2, 48).getType() == Material.GOLD_BLOCK,
            "WorldCreator did not use its custom flat layers in the generated chunk");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}

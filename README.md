# Felt

A Paper server implementation on Fabric.

![License](https://img.shields.io/github/license/Gr1mly4Memes/felt?style=flat-square)

## Installing

* Download the jar.  
* Launch with command `java -jar felt.jar nogui`. 
  * The `nogui` argument will disable the server control panel.

Read our document for more information.

## Building

This project builds directly against the editable `upstream/Paper` submodule at the commit pinned in `build-support/paper-sources.json`.

After cloning, initialize and fetch submodules:

```bash
git submodule update --init --recursive
```

If the submodule isn't registered in your clone yet, add it and check out the pinned commit:

```bash
git submodule add https://github.com/PaperMC/Paper.git upstream/Paper
git -C upstream/Paper checkout <commit from build-support/paper-sources.json>
git add upstream/Paper
```

Then build with:

```bash
./gradlew build
./gradlew collect
./gradlew fabricJar
```

### Editing Paper

Open the project from this repository root. The Paper API and server source directories under `upstream/Paper` are Gradle source roots, so they can be edited, indexed, and compiled directly in IntelliJ IDEA.

The files in `patches/` remain the reproducible record of the changes made to the pinned Paper revision:

```bash
# On a fresh, clean upstream/Paper checkout, apply the committed Felt changes once.
./gradlew applyPaperPatches

# After editing upstream/Paper, regenerate the per-file patches.
./gradlew rebuildPaperPatches

# Confirm the editable checkout and patch files describe the same code.
./gradlew checkPaperPatches
```

Do not commit changes inside the Paper submodule. Commit the regenerated files under `patches/` and the updated submodule pointer only when intentionally upgrading Paper.

### Paper compatibility

Paper-owned server classes, including anti-xray and `PaperBootstrap`, are compiled from `upstream/Paper`. Minecraft changes still require corresponding Felt mixins; compiling the Paper API alone does not guarantee feature parity.

Full Paper parity remains incomplete. `build-support/paper-parity-status.json` records the behaviors covered by the dedicated-world regression and the remaining audit work. Run `python tools/audit-paper-parity.py` to inventory the pinned upstream patches and Felt-added unsupported-operation sites in `build/paper-parity-audit.json`. An unsupported-operation site is a review candidate, and patch presence alone is not evidence of working behavior.

Per-player natural-spawn caps and cancellation backoff use Moonrise's tick-distance player map, including Paper's natural-only/count-all spawning setting. The Bukkit async chunk and teleport APIs use the native scheduler. Independent world send distance, MobGoals, world void damage, persistent entity spawn/origin metadata, freeze and visual-fire controls, daylight burning, peaceful despawn overrides, furnace cook-speed multipliers and beacon effect ranges are connected to their runtime implementations. Biome and terrain work runs on the worker already selected by Moonrise instead of dispatching to a second executor.

Lootable blocks and vehicles retain their item contents alongside refill tables, persist player loot history, honor replenishment settings and cancellation, and clear used shulker refill data from drops. Campfires support per-slot and all-slot cooking suspension, including saved state. These paths are exercised by the live-world regression rather than being API-only implementations.

The live-world checks also cover friction overrides and mob item pickup, player spawning influence, armor stand movement/ticking and equipment synchronization, falling block expiry and height limits, slime/magma cube wandering and attacks, witch drinking, ordinary/calibrated sculk sensor ranges, merchant discount flags and pricing, anvil enchantment limit bypass, and brewing view duration controls. Persistent settings are checked through native chunk save, unload and reload where applicable.

Gameplay regressions cover projectile and experience attribution, plugin-triggered potion splashes, villager trade counts, raid IDs and plugin data, dragon podiums and crystal-free respawns, and explosion source damage. They also exercise independent respawn world selection, plugin control of empty-server pausing, scheduled work during pauses, combat history eviction, and the villager demand setting.

Paper tag lifecycle handlers can edit tags before and after flattening during initial loading and datapack reload. The registry regression checks bound item and damage type tags, timeline track copying, and builder access to registered values before holder binding. A packet-capturing connection verifies the native configuration phase transition, legacy login guard, Paper event and return task.

The anti-xray integration uses Paper's engine on a detached packet snapshot, honors the bypass permission for chunk sends and refreshes, and reveals nearby blocks after block changes and mining actions. Snapshot preparation and neighbour capture run on the server thread; obfuscation runs on Minecraft's background executor. Each connection holds unready packets and all following writes/flushes until rewriting finishes, including bundled packets. Failed rewriting closes the connection without sending the original ores. The global palette can increase packet size.

The native Paper Moonrise backend owns chunk loading and generation, full-status promotion/demotion, entity and POI storage, Starlight lighting, autosave, unloading, and shutdown. Felt's mixins connect it to live Minecraft worlds while retaining Bukkit callbacks and the regular build and launcher. World and player view/simulation/send distance settings and delayed player-chunk unloading use the native loader. This integration does not establish complete Paper or third-party mod compatibility.

### Native Moonrise verification

`ChunkHolderManager` uses the tested `MoonriseTicketManager` for ticket identity, expiry, forced/keep-active counters, atomic operations, and concurrent propagation. The manager supplies the real scheduler lock and access radius; task dispatch happens after lock release. Storage uses the existing region-file caches with leases to protect active I/O from eviction, detached compression, external chunks, and header recovery. Lighting and scheduled-tick state are copied before background saves.

The normal build runs these checks:

- `:felt-fabric:testMoonriseSchedulingPrimitives` verifies priority ownership, cancellation, and unload ordering.
- `:felt-fabric:testMoonriseTicketPropagation` compares concurrent propagation with an independent distance oracle.
- `:felt-fabric:testMoonriseRegionFileIO` verifies read/save ordering, cancellation, retries, failures, and NBT isolation.
- `:felt-fabric:testMoonriseRegionFiles` and `:felt-fabric:runMoonriseStorageVerification` exercise actual temporary region files, eviction, recovery, and the installed storage mixins.
- `:felt-fabric:runMoonriseTicketVerification` bootstraps Minecraft and checks the real ticket bindings and scheduler status metadata.
- `:felt-fabric:runMoonriseWorldVerification` starts an isolated dedicated server, runs normal ticks, generates and promotes chunks, checks lighting, entity/POI lookup and natural-spawn mob counting, removes a holder, reloads saved data, and closes all three dimensions.
- `:felt-fabric:runMoonriseTerrainVerification` repeats the live-world checks with normal terrain generation.
- `:felt-fabric:verifyMoonriseDistribution` checks that every Leafpile runtime class is present in the packaged server implementation.

Run an individual check with `./gradlew <task>`. Verification worlds and mods stay under the build directories; the production jar does not include the verification mod. Deliberately injected storage failures and corruption-recovery messages are expected in the storage tests. Connected-client behavior and a separately installed Moonrise mod need separate compatibility testing.

Development launches use Felt's normal bootstrap transformations and strip marked compilation-only static methods before Mixin reads target metadata. Edit Paper-owned source in `upstream/Paper` and regenerate Felt patches as described above; Minecraft changes remain in Felt mixins.

Run `./gradlew :felt-fabric:testAsyncPacketGate` to verify asynchronous sending, ordering, failures, and disconnect cleanup. This check also runs during the normal build.

## Credits

Felt was originally forked from [Arclight](https://github.com/IzzelAliz/Arclight) by IzzelAliz. Huge thanks to the Arclight project and its contributors — Felt has since diverged significantly and is being developed as its own project.

## License

This project is licensed under [GPL v3](LICENSE).

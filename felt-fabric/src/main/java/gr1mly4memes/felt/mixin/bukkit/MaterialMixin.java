package gr1mly4memes.felt.mixin.bukkit;

import com.google.common.collect.ImmutableMap;
import gr1mly4memes.felt.bridge.bukkit.MaterialBridge;
import gr1mly4memes.felt.bridge.core.world.level.block.FireBlockBridge;
import gr1mly4memes.felt.mod.server.FeltServer;
import gr1mly4memes.felt.mod.config.MaterialPropertySpec;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FallingBlock;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.block.BlockState;
import org.bukkit.craftbukkit.block.CraftBlock;
import org.bukkit.craftbukkit.block.CraftBlockState;
import org.bukkit.craftbukkit.block.CraftBlockStates;
import org.bukkit.craftbukkit.inventory.CraftMetaArmorStand;
import org.bukkit.craftbukkit.inventory.CraftMetaBanner;
import org.bukkit.craftbukkit.inventory.CraftMetaBlockState;
import org.bukkit.craftbukkit.inventory.CraftMetaBook;
import org.bukkit.craftbukkit.inventory.CraftMetaBookSigned;
import org.bukkit.craftbukkit.inventory.CraftMetaCharge;
import org.bukkit.craftbukkit.inventory.CraftMetaCrossbow;
import org.bukkit.craftbukkit.inventory.CraftMetaEnchantedBook;
import org.bukkit.craftbukkit.inventory.CraftMetaFirework;
import org.bukkit.craftbukkit.inventory.CraftMetaItem;
import org.bukkit.craftbukkit.inventory.CraftMetaKnowledgeBook;
import org.bukkit.craftbukkit.inventory.CraftMetaLeatherArmor;
import org.bukkit.craftbukkit.inventory.CraftMetaMap;
import org.bukkit.craftbukkit.inventory.CraftMetaPotion;
import org.bukkit.craftbukkit.inventory.CraftMetaSkull;
import org.bukkit.craftbukkit.inventory.CraftMetaSpawnEgg;
import org.bukkit.craftbukkit.inventory.CraftMetaSuspiciousStew;
import org.bukkit.craftbukkit.inventory.CraftMetaTropicalFishBucket;
import org.bukkit.craftbukkit.util.CraftMagicNumbers;
import org.bukkit.craftbukkit.util.CraftNamespacedKey;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.material.MaterialData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.lang.reflect.Constructor;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

@Mixin(value = Material.class, remap = false)
public abstract class MaterialMixin implements MaterialBridge {

    // @formatter:off
    @Shadow @Mutable @Final private NamespacedKey key;
    @Shadow @Mutable @Final private Constructor<? extends MaterialData> ctor;
    @Shadow @Mutable @Final public Class<?> data;
    @Shadow public abstract boolean isBlock();
    // @formatter:on

    private static final Map<String, BiFunction<Material, CraftMetaItem, ItemMeta>> TYPES = ImmutableMap
        .<String, BiFunction<Material, CraftMetaItem, ItemMeta>>builder()
        .put("ARMOR_STAND", (a, b) -> b instanceof CraftMetaArmorStand ? b : new CraftMetaArmorStand(b))
        .put("BANNER", (a, b) -> b instanceof CraftMetaBanner ? b : new CraftMetaBanner(b))
        .put("TILE_ENTITY", (a, b) -> new CraftMetaBlockState(b, a))
        .put("BOOK", (a, b) -> b != null && b.getClass().equals(CraftMetaBook.class) ? b : new CraftMetaBook(b))
        .put("BOOK_SIGNED", (a, b) -> b instanceof CraftMetaBookSigned ? b : new CraftMetaBookSigned(b))
        .put("SKULL", (a, b) -> b instanceof CraftMetaSkull ? b : new CraftMetaSkull(b))
        .put("LEATHER_ARMOR", (a, b) -> b instanceof CraftMetaLeatherArmor ? b : new CraftMetaLeatherArmor(b))
        .put("MAP", (a, b) -> b instanceof CraftMetaMap ? b : new CraftMetaMap(b))
        .put("POTION", (a, b) -> b instanceof CraftMetaPotion ? b : new CraftMetaPotion(b))
        .put("SPAWN_EGG", (a, b) -> b instanceof CraftMetaSpawnEgg ? b : new CraftMetaSpawnEgg(b))
        .put("ENCHANTED", (a, b) -> b instanceof CraftMetaEnchantedBook ? b : new CraftMetaEnchantedBook(b))
        .put("FIREWORK", (a, b) -> b instanceof CraftMetaFirework ? b : new CraftMetaFirework(b))
        .put("FIREWORK_EFFECT", (a, b) -> b instanceof CraftMetaCharge ? b : new CraftMetaCharge(b))
        .put("KNOWLEDGE_BOOK", (a, b) -> b instanceof CraftMetaKnowledgeBook ? b : new CraftMetaKnowledgeBook(b))
        .put("TROPICAL_FISH_BUCKET", (a, b) -> b instanceof CraftMetaTropicalFishBucket ? b : new CraftMetaTropicalFishBucket(b))
        .put("CROSSBOW", (a, b) -> b instanceof CraftMetaCrossbow ? b : new CraftMetaCrossbow(b))
        .put("SUSPICIOUS_STEW", (a, b) -> b instanceof CraftMetaSuspiciousStew ? b : new CraftMetaSuspiciousStew(b))
        .put("UNSPECIFIC", (a, b) -> new CraftMetaItem(b))
        .put("NULL", (a, b) -> null)
        .build();

    private MaterialPropertySpec.MaterialType felt$type = MaterialPropertySpec.MaterialType.VANILLA;
    private MaterialPropertySpec felt$spec;
    private Identifier felt$location;
    private boolean felt$block = false, felt$item = false;

    @Override
    public void bridge$setBlock() {
        this.felt$block = true;
    }

    @Override
    public void bridge$setItem() {
        this.felt$item = true;
    }

    @Inject(method = "isBlock", cancellable = true, at = @At("HEAD"))
    private void felt$isBlock(CallbackInfoReturnable<Boolean> cir) {
        if (felt$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(felt$block);
        }
    }

    @Inject(method = "isItem", cancellable = true, at = @At("HEAD"))
    private void felt$isItem(CallbackInfoReturnable<Boolean> cir) {
        if (felt$type != MaterialPropertySpec.MaterialType.VANILLA) {
            cir.setReturnValue(felt$item);
        }
    }

    @Inject(method = "isEdible", cancellable = true, at = @At("HEAD"))
    private void felt$isEdible(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.edible);
        }
    }

    @Inject(method = "isRecord", cancellable = true, at = @At("HEAD"))
    private void felt$isRecord(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.record);
        }
    }

    @Inject(method = "isSolid", cancellable = true, at = @At("HEAD"))
    private void felt$isSolid(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.solid);
        }
    }

    @Inject(method = "isAir", cancellable = true, at = @At("HEAD"))
    private void felt$isAir(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.air);
        }
    }

    @Inject(method = "isTransparent", cancellable = true, at = @At("HEAD"))
    private void felt$isTransparent(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.transparent);
        }
    }

    @Inject(method = "isFlammable", cancellable = true, at = @At("HEAD"))
    private void felt$isFlammable(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.flammable);
        }
    }

    @Inject(method = "isBurnable", cancellable = true, at = @At("HEAD"))
    private void felt$isBurnable(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.burnable);
        }
    }

    @Inject(method = "isFuel", cancellable = true, at = @At("HEAD"))
    private void felt$isFuel(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.fuel);
        }
    }

    @Inject(method = "isOccluding", cancellable = true, at = @At("HEAD"))
    private void felt$isOccluding(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.occluding);
        }
    }

    @Inject(method = "hasGravity", cancellable = true, at = @At("HEAD"))
    private void felt$hasGravity(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.gravity);
        }
    }

    @Inject(method = "isInteractable", cancellable = true, at = @At("HEAD"))
    private void felt$isInteractable(CallbackInfoReturnable<Boolean> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.interactable);
        }
    }

    @Inject(method = "getHardness", cancellable = true, at = @At("HEAD"))
    private void felt$getHardness(CallbackInfoReturnable<Float> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.hardness);
        }
    }

    @Inject(method = "getBlastResistance", cancellable = true, at = @At("HEAD"))
    private void felt$getBlastResistance(CallbackInfoReturnable<Float> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.blastResistance);
        }
    }

    @Inject(method = "getMaxStackSize", cancellable = true, at = @At("HEAD"))
    private void felt$getMaxStackSize(CallbackInfoReturnable<Integer> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            cir.setReturnValue(felt$spec.maxStack);
        }
    }

    @Inject(method = "getMaxDurability", cancellable = true, at = @At("HEAD"))
    private void felt$getMaxDurability(CallbackInfoReturnable<Short> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            if (felt$spec.maxDurability != null) {
                cir.setReturnValue(felt$spec.maxDurability.shortValue());
            }
        }
    }

    @Inject(method = "getCraftingRemainingItem", cancellable = true, at = @At("HEAD"))
    private void felt$getCraftingRemainingItem(CallbackInfoReturnable<Material> cir) {
        if (felt$spec != null) {
            if (!felt$spec.isPresent) {
                felt$setupCommon();
            }
            if (felt$spec.craftingRemainingItem != null) {
                cir.setReturnValue(CraftMagicNumbers.getMaterial(BuiltInRegistries.ITEM.getValue(Identifier.parse(felt$spec.craftingRemainingItem))));
            }
        }
    }

    @Override
    public MaterialPropertySpec bridge$getSpec() {
        return felt$spec;
    }

    @Override
    public MaterialPropertySpec.MaterialType bridge$getType() {
        return felt$type;
    }

    private Function<CraftMetaItem, ItemMeta> felt$metaFunc;

    @Override
    public Function<CraftMetaItem, ItemMeta> bridge$itemMetaFactory() {
        if (felt$metaFunc == null && !felt$spec.isPresent) {
            felt$setupCommon();
        }
        return felt$metaFunc;
    }

    @Override
    public void bridge$setItemMetaFactory(Function<CraftMetaItem, ItemMeta> func) {
        this.felt$metaFunc = func;
    }

    private Function<CraftBlock, BlockState> felt$stateFunc;

    @Override
    public Function<CraftBlock, BlockState> bridge$blockStateFactory() {
        if (felt$stateFunc == null && !felt$spec.isPresent) {
            felt$setupCommon();
        }
        return felt$stateFunc;
    }

    @Override
    public void bridge$setBlockStateFactory(Function<CraftBlock, BlockState> func) {
        this.felt$stateFunc = func;
    }

    @Override
    public void bridge$setupBlock(Identifier key, MaterialPropertySpec spec) {
        this.felt$spec = spec.clone();
        felt$type = MaterialPropertySpec.MaterialType.MODS;
        felt$block = true;
        felt$setupCommonLazy(key);
    }

    @Override
    public void bridge$setupVanillaBlock(MaterialPropertySpec spec) {
        if (spec.isPresent) {
            this.felt$spec = spec.clone();
            this.setupBlockStateFunc();
        }
    }

    @Override
    public void bridge$setupItem(Identifier key, MaterialPropertySpec spec) {
        this.felt$spec = spec.clone();
        felt$type = MaterialPropertySpec.MaterialType.MODS;
        felt$item = true;
        felt$setupCommonLazy(key);
    }

    @Override
    public boolean bridge$shouldApplyStateFactory() {
        return this.felt$type != MaterialPropertySpec.MaterialType.VANILLA ||
            (this.felt$spec != null && this.felt$spec.blockStateClass != null);
    }

    @SuppressWarnings("unchecked")
    private void felt$setupCommonLazy(Identifier key) {
        this.key = CraftNamespacedKey.fromMinecraft(key);
        if (felt$spec.materialDataClass != null) {
            try {
                Class<?> data = Class.forName(felt$spec.materialDataClass);
                if (MaterialData.class.isAssignableFrom(data)) {
                    this.data = data;
                    this.ctor = (Constructor<? extends MaterialData>) data.getConstructor(Material.class, byte.class);
                }
            } catch (Exception e) {
                FeltServer.LOGGER.warn("Bad material data class {} for {}", felt$spec.materialDataClass, this);
                FeltServer.LOGGER.warn(e);
            }
        }
        felt$location = key;
    }

    private void felt$setupCommon() {
        Block block = BuiltInRegistries.BLOCK.getValue(felt$location);
        Item item = BuiltInRegistries.ITEM.getValue(felt$location);

        // Block properties
        if (felt$location.equals(MaterialBridge.AIR) || block != Blocks.AIR) {
            if (felt$spec.solid == null) {
                felt$spec.solid = block.defaultBlockState().canOcclude();
            }
            if (felt$spec.air == null) {
                felt$spec.air = block.defaultBlockState().isAir();
            }
            if (felt$spec.transparent == null) {
                felt$spec.transparent = block.defaultBlockState().useShapeForLightOcclusion();
            }
            if (felt$spec.flammable == null) {
                felt$spec.flammable = ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
            }
            if (felt$spec.burnable == null) {
                felt$spec.burnable = ((FireBlockBridge) Blocks.FIRE).bridge$canBurn(block);
            }
            if (felt$spec.hardness == null) {
                felt$spec.hardness = block.defaultBlockState().destroySpeed;
            }
            if (felt$spec.blastResistance == null) {
                felt$spec.blastResistance = block.getExplosionResistance();
            }
        }

        // Item properties
        if (felt$spec.maxStack == null) {
            felt$spec.maxStack = bridge$mod$getMaxStackSize(item);
        }
        if (felt$spec.maxDurability == null) {
            felt$spec.maxDurability = bridge$mod$getDurability(item);
        }
        if (felt$spec.edible == null) {
            felt$spec.edible = false;
        }
        if (felt$spec.record == null) {
            felt$spec.record = false;
        }
        if (felt$spec.fuel == null) {
            felt$spec.fuel = bridge$mod$isFuel(item);
        }
        if (felt$spec.occluding == null) {
            felt$spec.occluding = felt$spec.solid;
        }
        if (felt$spec.gravity == null) {
            felt$spec.gravity = block instanceof FallingBlock;
        }
        if (felt$spec.interactable == null) {
            felt$spec.interactable = true;
        }
        if (felt$spec.craftingRemainingItem == null) {
            // noinspection deprecation
            final var remaining = bridge$getCraftRemainingItem(item);
            felt$spec.craftingRemainingItem = remaining != null ? remaining.toString() : null;
        }
        if (felt$spec.itemMetaType == null) {
            felt$spec.itemMetaType = "UNSPECIFIC";
        }
        felt$spec.present();

        BiFunction<Material, CraftMetaItem, ItemMeta> function = TYPES.get(felt$spec.itemMetaType);
        if (function != null) {
            this.felt$metaFunc = meta -> function.apply((Material) (Object) this, meta);
        } else {
            this.felt$metaFunc = dynamicMetaCreator(felt$spec.itemMetaType);
        }
        this.setupBlockStateFunc();
    }

    private void setupBlockStateFunc() {
        if (felt$spec.blockStateClass != null && !felt$spec.blockStateClass.equalsIgnoreCase("auto")) {
            try {
                Class<?> cl = Class.forName(felt$spec.blockStateClass);
                if (!CraftBlockState.class.isAssignableFrom(cl)) {
                    FeltServer.LOGGER.warn("{} is not a subclass of {}", cl, CraftBlockState.class);
                } else {
                    for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                        if (constructor.getParameterTypes().length == 1
                            && org.bukkit.block.Block.class.isAssignableFrom(constructor.getParameterTypes()[0])) {
                            constructor.setAccessible(true);
                            this.felt$stateFunc = b -> {
                                try {
                                    return (BlockState) constructor.newInstance(b);
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            };
                        }
                    }
                }
            } catch (Exception e) {
                FeltServer.LOGGER.warn("{} provided blockStateClass {} is invalid: {}", this, felt$spec.blockStateClass, e);
            }
            if (this.felt$stateFunc == null) {
                FeltServer.LOGGER.warn("{} did not find a valid constructor in provided blockStateClass {}", this, felt$spec.blockStateClass);
            }
        }
        if (this.felt$stateFunc == null) {
            this.felt$stateFunc = CraftBlockStates::getBlockState;
        }
    }

    private Function<CraftMetaItem, ItemMeta> dynamicMetaCreator(String type) {
        Function<CraftMetaItem, ItemMeta> candidate = null;
        try {
            Class<?> cl = Class.forName(type);
            if (!CraftMetaItem.class.isAssignableFrom(cl)) {
                FeltServer.LOGGER.warn("{} is not a subclass of {}", cl, CraftMetaItem.class);
            } else {
                for (Constructor<?> constructor : cl.getDeclaredConstructors()) {
                Class<?>[] parameterTypes = constructor.getParameterTypes();
                if (parameterTypes.length == 1) {
                    if (parameterTypes[0] == Material.class) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                } else if (parameterTypes.length == 2) {
                    if (parameterTypes[0] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[1])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(this, meta);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    } else if (parameterTypes[1] == Material.class && CraftMetaItem.class.isAssignableFrom(parameterTypes[0])) {
                        constructor.setAccessible(true);
                        candidate = meta -> {
                            try {
                                return (ItemMeta) constructor.newInstance(meta, this);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        };
                        break;
                    }
                }
                }
            }
        } catch (Exception e) {
            FeltServer.LOGGER.warn("{} provided itemMetaType {} is invalid: {}", this, type, e);
        }
        if (candidate == null) {
            FeltServer.LOGGER.warn("{} did not find a valid constructor in provided itemMetaType {}", this, type);
            candidate = CraftMetaItem::new;
        }
        return candidate;
    }
}

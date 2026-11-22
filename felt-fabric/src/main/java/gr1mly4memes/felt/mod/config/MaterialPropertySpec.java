package gr1mly4memes.felt.mod.config;

public class MaterialPropertySpec implements Cloneable {

    public static final MaterialPropertySpec EMPTY = new MaterialPropertySpec().absent();

    public String materialDataClass;

    public Integer maxStack;

    public Integer maxDurability;

    public Boolean edible;

    public Boolean record;

    public Boolean solid;

    public Boolean air;

    public Boolean transparent;

    public Boolean flammable;

    public Boolean burnable;

    public Boolean fuel;

    public Boolean occluding;

    public Boolean gravity;

    public Boolean interactable;

    public Float hardness;

    public Float blastResistance;

    public String craftingRemainingItem;

    public String itemMetaType;

    public String blockStateClass;

    public boolean isPresent = true;

    public MaterialPropertySpec present() {
        this.isPresent = true;
        return this;
    }

    public MaterialPropertySpec absent() {
        this.isPresent = false;
        return this;
    }

    @Override
    public MaterialPropertySpec clone() {
        try {
            return (MaterialPropertySpec) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError(e);
        }
    }

    public enum MaterialType {
        VANILLA, MODS
    }
}

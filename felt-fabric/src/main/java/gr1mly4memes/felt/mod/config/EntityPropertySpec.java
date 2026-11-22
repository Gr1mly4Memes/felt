package gr1mly4memes.felt.mod.config;

public class EntityPropertySpec implements Cloneable {

    public static final EntityPropertySpec EMPTY = new EntityPropertySpec();

    public String entityClass;

    public String entityImplClass;

    @Override
    public EntityPropertySpec clone() {
        try {
            return (EntityPropertySpec) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
    }
}

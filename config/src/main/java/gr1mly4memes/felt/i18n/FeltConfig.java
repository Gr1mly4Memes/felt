package gr1mly4memes.felt.i18n;

import com.google.common.reflect.TypeToken;
import gr1mly4memes.felt.i18n.conf.ConfigSpec;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;
import ninja.leaping.configurate.objectmapping.ObjectMappingException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

public class FeltConfig {

    private static FeltConfig instance;

    private final CommentedConfigurationNode node;
    private final ConfigSpec spec;

    public FeltConfig(CommentedConfigurationNode node) throws ObjectMappingException {
        this.node = node;
        this.spec = this.node.getValue(TypeToken.of(ConfigSpec.class));
    }

    public CommentedConfigurationNode getNode() {
        return node;
    }

    public ConfigSpec getSpec() {
        return spec;
    }

    public ConfigurationNode get(String path) {
        return this.node.getNode((Object[]) path.split("\\."));
    }

    public static ConfigSpec spec() {
        return instance.spec;
    }

    private static void load() throws Exception {
        Path path = Paths.get("felt.conf");
        CommentedConfigurationNode node = HoconConfigurationLoader.builder().setSource(
            () -> new BufferedReader(new InputStreamReader(FeltConfig.class.getResourceAsStream("/META-INF/felt.conf"), StandardCharsets.UTF_8))
        ).build().load();
        HoconConfigurationLoader loader = HoconConfigurationLoader.builder().setPath(path).build();
        CommentedConfigurationNode cur = loader.load();
        cur.mergeValuesFrom(node);
        cur.removeChild("velocity");
        copyComments(cur, node);
        instance = new FeltConfig(cur);
        loader.save(cur);
    }

    private static void copyComments(CommentedConfigurationNode target, CommentedConfigurationNode source) {
        if (!target.getComment().isPresent() && source.getComment().isPresent()) {
            target.setComment(source.getComment().get());
        }
        if (target.hasMapChildren() && source.hasMapChildren()) {
            for (Map.Entry<Object, ? extends ConfigurationNode> entry : source.getChildrenMap().entrySet()) {
                if (entry.getValue() instanceof CommentedConfigurationNode sourceChild) {
                    copyComments((CommentedConfigurationNode) target.getNode(entry.getKey()), sourceChild);
                }
            }
        }
    }

    static {
        try {
            load();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}

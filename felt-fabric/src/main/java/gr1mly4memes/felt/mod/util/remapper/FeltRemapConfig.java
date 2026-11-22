package gr1mly4memes.felt.mod.util.remapper;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/*
 * Used to record transformation detail for specific ClassLoaders.
 */
public record FeltRemapConfig(boolean remap) {
    public static final FeltRemapConfig PLUGIN = new FeltRemapConfig(true);

    public FeltRemapConfig copy() {
        return new FeltRemapConfig(remap);
    }

    public int write(DataOutput output) throws IOException {
        output.writeBoolean(remap);
        return 1;
    }

    public static FeltRemapConfig read(DataInput input) throws IOException {
        return new FeltRemapConfig(input.readBoolean());
    }
}

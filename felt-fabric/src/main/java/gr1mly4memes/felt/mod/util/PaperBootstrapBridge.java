package gr1mly4memes.felt.mod.util;

import joptsimple.OptionDescriptor;
import joptsimple.OptionSet;
import joptsimple.OptionSpec;
import net.minecraft.server.Main;

/** Adapts Paper's parsed options to the Fabric-owned vanilla entry point. */
public final class PaperBootstrapBridge {
    private PaperBootstrapBridge() {}

    public static void boot(OptionSet options) {
        var arguments = new java.util.ArrayList<String>();
        for (OptionSpec<?> spec : new java.util.LinkedHashSet<>(options.specs())) {
            if (spec.options().isEmpty() || ((OptionDescriptor) spec).representsNonOptions()) continue;
            String name = spec.options().stream().max(java.util.Comparator.comparingInt(String::length)).orElseThrow();
            String flag = (name.length() == 1 ? "-" : "--") + name;
            var values = options.valuesOf(spec);
            if (values.isEmpty()) arguments.add(flag);
            for (Object value : values) {
                arguments.add(flag);
                arguments.add(value instanceof java.text.SimpleDateFormat format ? format.toPattern() : String.valueOf(value));
            }
        }
        arguments.add("--");
        for (Object argument : options.nonOptionArguments()) arguments.add(String.valueOf(argument));
        Main.main(arguments.toArray(String[]::new));
    }
}

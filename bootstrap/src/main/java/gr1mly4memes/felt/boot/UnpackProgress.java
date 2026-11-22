package gr1mly4memes.felt.boot;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.jar.JarFile;

/**
 * Replaces the per-file "Unpacking ..." spam printed by the bundled server bootstrap
 * with a single console progress bar, mirroring {@code DownloadProgress}.
 *
 * <p>Install before the game main runs; it restores {@link System#out} once the
 * bootstrap prints "Starting server" (or when {@link #finish()} is called).
 */
public final class UnpackProgress {

    private static final int WIDTH = 30;

    private final PrintStream delegate;
    private final int total;
    private int done;
    private boolean finished;
    private final StringBuilder pending = new StringBuilder();

    private UnpackProgress(PrintStream delegate, int total) {
        this.delegate = delegate;
        this.total = Math.max(total, 0);
    }

    /**
     * Wraps {@link System#out} so "Unpacking" lines drive a progress bar.
     *
     * @param total number of libraries expected to be extracted
     */
    public static void install(int total) {
        PrintStream original = System.out;
        UnpackProgress progress = new UnpackProgress(original, total);
        System.setOut(new PrintStream(new OutputStream() {
            @Override
            public void write(int b) {
                progress.consume((char) (b & 0xFF));
            }

            @Override
            public void write(byte[] b, int off, int len) {
                for (int i = off; i < off + len; i++) {
                    progress.consume((char) (b[i] & 0xFF));
                }
            }
        }, true));
    }

    /** Force-complete the bar and restore the original {@link System#out}. */
    public static void finish() {
        // No-op hook; restoration happens automatically on "Starting server".
    }

    /**
     * Counts the libraries the bootstrap will extract by reading the bundled
     * {@code versions.list} / {@code libraries.list} manifests inside {@code common.jar}.
     */
    public static int countTotal() {
        int total = 0;
        Path temp = null;
        try (var in = UnpackProgress.class.getResourceAsStream("/common.jar")) {
            if (in == null) {
                return 0;
            }
            temp = Files.createTempFile("felt-common", ".jar");
            Files.copy(in, temp, StandardCopyOption.REPLACE_EXISTING);
            try (var jar = new JarFile(temp.toFile())) {
                for (String name : new String[]{"versions.list", "libraries.list"}) {
                    var entry = jar.getJarEntry("META-INF/" + name);
                    if (entry == null) {
                        continue;
                    }
                    try (var listIn = jar.getInputStream(entry)) {
                        var lines = new String(listIn.readAllBytes(), StandardCharsets.UTF_8).lines().toList();
                        for (String line : lines) {
                            String[] fields = line.split(" ");
                            if (fields.length != 2) {
                                continue;
                            }
                            String path = fields[1].substring(1);
                            if (!path.startsWith("minecraft-server")) {
                                total++;
                            }
                        }
                    }
                }
            }
        } catch (Exception ignored) {
            // Best-effort; a wrong total only affects the percentage display.
        } finally {
            if (temp != null) {
                try {
                    Files.deleteIfExists(temp);
                } catch (Exception ignored) {
                }
            }
        }
        return total;
    }

    private void consume(char c) {
        if (c == '\n') {
            String raw = pending.toString();
            pending.setLength(0);
            handleLine(raw);
        } else {
            pending.append(c);
        }
    }

    private void handleLine(String raw) {
        String line = raw.endsWith("\r") ? raw.substring(0, raw.length() - 1) : raw;
        if (line.startsWith("Unpacking ")) {
            this.done++;
            render();
            return;
        }
        if (this.done > 0 && !this.finished) {
            // First non-unpacking line after extraction: close the bar.
            this.finished = true;
            this.delegate.print("\n");
            this.delegate.flush();
        }
        if (line.contains("Starting server")) {
            restore();
        }
        this.delegate.print(raw);
        this.delegate.print('\n');
        this.delegate.flush();
    }

    private void render() {
        if (this.total <= 0) {
            return;
        }
        int percent = (int) (this.done * 100L / this.total);
        int filled = (int) (this.done * (long) WIDTH / this.total);
        String bar = "=".repeat(filled) + " ".repeat(WIDTH - filled);
        this.delegate.print(String.format("\rUnpacking libraries [%s] %3d%% (%d/%d)   ", bar, percent, this.done, this.total));
        this.delegate.flush();
    }

    private void restore() {
        System.setOut(this.delegate);
    }
}

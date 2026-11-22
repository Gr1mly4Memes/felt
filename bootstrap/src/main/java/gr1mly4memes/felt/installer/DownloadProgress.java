package gr1mly4memes.felt.installer;

/**
 * Renders a console progress bar for parallel downloads. The total may grow
 * dynamically as nested dependency sets are discovered.
 */
public class DownloadProgress {

    private static final int WIDTH = 30;

    private int total;
    private int completed;

    public DownloadProgress(int initialTotal) {
        this.total = Math.max(initialTotal, 0);
    }

    public synchronized void addTotal(int count) {
        this.total += Math.max(count, 0);
        render();
    }

    public synchronized void complete() {
        this.completed++;
        render();
    }

    public synchronized void finish() {
        this.completed = this.total;
        render();
        System.out.print("\n");
        System.out.flush();
    }

    private void render() {
        if (this.total <= 0) {
            return;
        }
        int percent = (int) (this.completed * 100L / this.total);
        int filled = (int) (this.completed * (long) WIDTH / this.total);
        String bar = "=".repeat(filled) + " ".repeat(WIDTH - filled);
        System.out.print(String.format("\rDownloading libraries [%s] %3d%% (%d/%d)   ", bar, percent, this.completed, this.total));
        System.out.flush();
    }
}

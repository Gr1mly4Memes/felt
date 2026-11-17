package gr1mly4memes.felt.common.mod.util.thread;

public interface RunnableInPlace extends Runnable {

    static RunnableInPlace wrap(Runnable r) {
        if (r instanceof RunnableInPlace i) {
            return i;
        } else {
            return r::run;
        }
    }
}

package gr1mly4memes.felt.mod.server.world;

/** Separate holder: vanilla creates built-in ticket types during its own class initialization. */
public final class MoonriseTicketIds {
    private static final java.util.concurrent.atomic.AtomicLong NEXT = new java.util.concurrent.atomic.AtomicLong();
    private MoonriseTicketIds() {}
    public static long next() { return NEXT.getAndIncrement(); }
}

package rocks.gravili.notquests.paper.commands.framework;

/**
 * A short human-readable description of a command or argument. Mirrors Cloud's {@code Description}
 * so registration sites can swap with minimal changes. Used by the help menu and the action-bar
 * command-hint to label arguments.
 */
public final class NQDescription {
    static final NQDescription EMPTY = new NQDescription("");

    private final String text;

    private NQDescription(final String text) {
        this.text = text == null ? "" : text;
    }

    public static NQDescription of(final String text) {
        return new NQDescription(text);
    }

    public String textDescription() {
        return text;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }
}

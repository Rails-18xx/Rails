package rails.game.state;

/**
 * AutoChangeSets are ChangeSets that belong to no action directly
 */
class AutoChangeSet extends ChangeSet {

    private final boolean terminal;

    /**
     * @param terminal implies that this is the terminal of the stack
     */
    AutoChangeSet(boolean terminal) {
        this.terminal = terminal;
    }
    
    boolean isTerminal() {
        return terminal;
    }
    
    @Override
    public String toString() {
        return "AutoChangeSet";
    }
    
}

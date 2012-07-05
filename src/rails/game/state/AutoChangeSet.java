package rails.game.state;

/**
 * AutoChangeSets are ChangeSets that belong to no action directly
 */
final class AutoChangeSet extends ChangeSet {

    AutoChangeSet() {}
    
    @Override
    public String toString() {
        return "AutoChangeSet";
    }
    
}

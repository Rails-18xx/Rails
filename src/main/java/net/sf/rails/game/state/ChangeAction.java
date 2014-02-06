package net.sf.rails.game.state;

/**
 * A ChangeAction can be related to a ChangeSet
 */
public interface ChangeAction {
    
    public ChangeActionOwner getActionOwner();
    
}

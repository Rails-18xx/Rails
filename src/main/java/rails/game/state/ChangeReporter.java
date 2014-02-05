package rails.game.state;

public interface ChangeReporter {
    
    void updateOnClose(ChangeSet current);
    
    void informOnUndo();

    void informOnRedo();
    
    void update(ChangeSet changeSet);
}

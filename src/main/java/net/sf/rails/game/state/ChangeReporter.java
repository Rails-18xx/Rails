package net.sf.rails.game.state;

public interface ChangeReporter {
    
    public void init(ChangeStack stack);
    
    public void updateOnClose();
    
    public void informOnUndo();

    public void informOnRedo();
    
    public void updateAfterUndoRedo();
    
}

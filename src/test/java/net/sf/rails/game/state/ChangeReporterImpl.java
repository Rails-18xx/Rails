package net.sf.rails.game.state;

import net.sf.rails.game.state.ChangeReporter;
import net.sf.rails.game.state.ChangeStack;

public class ChangeReporterImpl implements ChangeReporter {

    @Override
    public void init(ChangeStack changeStack) {
        // do nothing
    }
    
    @Override
    public void updateOnClose() {
        // do nothing
    }

    @Override
    public void informOnUndo() {
        // do nothing
    }

    @Override
    public void informOnRedo() {
        // do nothing
    }
 
    @Override
    public void updateAfterUndoRedo() {
        // do nothing
    }

}

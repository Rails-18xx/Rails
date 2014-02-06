package net.sf.rails.game.state;

import net.sf.rails.game.state.ChangeReporter;
import net.sf.rails.game.state.ChangeSet;
import net.sf.rails.game.state.ChangeStack;

public class ChangeReporterImpl implements ChangeReporter {

    public void linkToChangeStack(ChangeStack changeStack) {
        // do nothing
    }

    public void updateOnClose(ChangeSet current) {
        // do nothing
    }
    public void informOnUndo() {
        // do nothing
    }
    public void informOnRedo() {
        // do nothing
    }
    public void update(ChangeSet changeSet) {
        // do nothing
    }

}

package rails.game.state;

import static org.fest.assertions.api.Fail.failBecauseExceptionWasNotThrown;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ChangeStackTest {
    
    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    private ChangeStack changeStack;
    
    private ChangeSet auto_1, auto_2, auto_3;
    private ChangeSet action_1, action_2, action_3;

    private ChangeSet startActionChangeSet() {
        return changeStack.startChangeSet(new ChangeSet(true, false));
    }
    
    @Before
    public void setUp() {
        root = Root.create();
        state = BooleanState.create(root, STATE_ID);
        changeStack = root.getStateManager().getChangeStack();
        auto_1 = changeStack.getCurrentChangeSet();
        action_1 = startActionChangeSet();
        auto_2 = changeStack.closeCurrentChangeSet();
        state.set(true);
        auto_3 = changeStack.closeCurrentChangeSet();
        action_2 = startActionChangeSet();
        state.set(false);
        action_3 = startActionChangeSet();
    }

    @Test
    public void testGetCurrentChangeSet() {
        assertSame(action_3, changeStack.getCurrentChangeSet());
        // on the stack there are auto_1, action_1, auto_2, action_2
        assertEquals(4, changeStack.sizeUndoStack());
    }

    @Test
    public void testCloseCurrentChangeSet() {
        changeStack.closeCurrentChangeSet();
        assertTrue(action_3.isClosed());
        assertSame(action_3, changeStack.getLastClosedChangeSet());
        // and now action_3 is added
        assertEquals(changeStack.sizeUndoStack(), 5);
    }

    @Test
    public void testUndo() {
        assertFalse(state.value());
        // undo action 3
        changeStack.undo();
        assertEquals(4, changeStack.sizeUndoStack());
        assertSame(action_2, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
        // undo action 2
        changeStack.undo();
        assertEquals(3, changeStack.sizeUndoStack());
        assertSame(auto_2, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertTrue(state.value());
        // undo auto_2 and action 1
        changeStack.undo();
        assertEquals(1, changeStack.sizeUndoStack());
        assertSame(auto_1, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
        // undo should fail now
        try{
            changeStack.undo();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (Exception e){
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        assertEquals(1, changeStack.sizeUndoStack());
        assertSame(auto_1, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
    }

    @Test
    public void testRedo() {
        // undo everything
        changeStack.undo();
        changeStack.undo();
        changeStack.undo();
        // the state until now was checked in testUndo
        // now redo action_1 and auto_2
        changeStack.redo();
        assertEquals(3, changeStack.sizeUndoStack());
        assertSame(auto_2, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertTrue(state.value());
        // redo action_2
        changeStack.redo();
        assertEquals(4, changeStack.sizeUndoStack());
        assertSame(action_2, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
        // redo action_3
        changeStack.redo();
        assertEquals(5, changeStack.sizeUndoStack());
        assertSame(action_3, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
        // then it should do anything
        changeStack.redo();
        assertEquals(5, changeStack.sizeUndoStack());
        assertSame(action_3, changeStack.getLastClosedChangeSet());
        assertFalse(changeStack.getCurrentChangeSet().isBlocking());
        assertFalse(state.value());
    }

}

package net.sf.rails.game.state;

import static org.fest.assertions.api.Fail.failBecauseExceptionWasNotThrown;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.ChangeAction;
import net.sf.rails.game.state.ChangeSet;
import net.sf.rails.game.state.ChangeStack;
import net.sf.rails.game.state.Root;

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
    private ChangeAction changeAction;
    
    private ChangeSet set_1, set_2, set_3;

    @Before
    public void setUp() {
        root = Root.create();
        changeStack = root.getStateManager().getChangeStack();
        changeAction = new ChangeActionImpl();

        // initial changeset
        state = BooleanState.create(root, STATE_ID, true);
        
        // next changeset
        StateTestUtils.close(root);
        set_1 = changeStack.getClosedChangeSet();
        state.set(false);
        
        // next changeset
        StateTestUtils.close(root);
        set_2 = changeStack.getClosedChangeSet();
        state.set(true);
        StateTestUtils.close(root);
        set_3 = changeStack.getClosedChangeSet();
    }

    @Test
    public void testGetCurrentChangeSet() {
        assertSame(set_3, changeStack.getClosedChangeSet());
        // on the stack are set2, set1 (thus index 2)
        assertEquals(3, changeStack.getCurrentIndex());
    }

    @Test
    public void testcloseAndNew() {
        changeStack.close(changeAction);
        assertSame(set_3, changeStack.getClosedChangeSet());
        // number on stack has not changed
        assertEquals(3, changeStack.getCurrentIndex());
    }

    private void testUndoAfterClose() {
        // check current state
        assertTrue(state.value());
        // undo set 3
        changeStack.undo();
        assertEquals(2, changeStack.getCurrentIndex());
        assertSame(set_2, changeStack.getClosedChangeSet());
        assertFalse(state.value());
        // undo set 2
        changeStack.undo();
        assertEquals(1, changeStack.getCurrentIndex());
        assertSame(set_1, changeStack.getClosedChangeSet());
        assertTrue(state.value());
        // undo set 1 => should fail
        try{
            changeStack.undo();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (Exception e){
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        assertEquals(1, changeStack.getCurrentIndex());
        assertSame(set_1, changeStack.getClosedChangeSet());
        assertTrue(state.value());
    }
    
    @Test
    public void testUndo() {
        changeStack.close(changeAction);
        testUndoAfterClose();
    }

    @Test
    public void testRedo() {
        // undo everything
        changeStack.close(changeAction);
        changeStack.undo();
        changeStack.undo();
        // the state until now was checked in testUndo
        
        // redo set_2
        changeStack.redo();
        assertEquals(2, changeStack.getCurrentIndex());
        assertSame(set_2, changeStack.getClosedChangeSet());
        assertFalse(state.value());

        // redo set_3
        changeStack.redo();
        assertEquals(3, changeStack.getCurrentIndex());
        assertSame(set_3, changeStack.getClosedChangeSet());
        assertTrue(state.value());

        // then it should do nothing
        try{
            changeStack.redo();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (Exception e){
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        assertEquals(3, changeStack.getCurrentIndex());
        assertSame(set_3, changeStack.getClosedChangeSet());
        assertTrue(state.value());
        
        // can we still undo?
        testUndoAfterClose();
    }

}

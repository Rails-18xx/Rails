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
    private ChangeAction changeAction;
    
    private ChangeSet set_1, set_2, set_3;

    @Before
    public void setUp() {
        root = Root.create(new ChangeReporterImpl());
        changeStack = root.getStateManager().getChangeStack();
        changeAction = new ChangeActionImpl();

        // initial changeset
        state = BooleanState.create(root, STATE_ID, true);
        
        // next changeset
        StateTestUtils.close(root);
        set_1 = changeStack.getPreviousChangeSet();
        state.set(false);
        
        // next changeset
        StateTestUtils.close(root);
        set_2 = changeStack.getPreviousChangeSet();
        state.set(true);
        StateTestUtils.close(root);
        set_3 = changeStack.getPreviousChangeSet();
    }

    @Test
    public void testGetCurrentChangeSet() {
        assertSame(set_3, changeStack.getPreviousChangeSet());
        // on the stack are set2, set1 (thus index 2)
        assertEquals(2, changeStack.getCurrentIndex());
    }

    @Test
    public void testcloseAndNew() {
        changeStack.close(changeAction);
        assertSame(set_3, changeStack.getPreviousChangeSet());
        // number on stack has not changed
        assertEquals(2, changeStack.getCurrentIndex());
    }

    private void testUndoAfterClose() {
        // check current state
        assertTrue(state.value());
        // undo set 3
        changeStack.undo();
        assertEquals(1, changeStack.getCurrentIndex());
        assertSame(set_2, changeStack.getPreviousChangeSet());
        assertFalse(state.value());
        // undo set 2
        changeStack.undo();
        assertEquals(0, changeStack.getCurrentIndex());
        assertSame(set_1, changeStack.getPreviousChangeSet());
        assertTrue(state.value());
        // undo set 1 => should fail
        try{
            changeStack.undo();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (Exception e){
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        assertEquals(0, changeStack.getCurrentIndex());
        assertSame(set_1, changeStack.getPreviousChangeSet());
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
        assertEquals(1, changeStack.getCurrentIndex());
        assertSame(set_2, changeStack.getPreviousChangeSet());
        assertFalse(state.value());

        // redo set_3
        changeStack.redo();
        assertEquals(2, changeStack.getCurrentIndex());
        assertSame(set_3, changeStack.getPreviousChangeSet());
        assertTrue(state.value());

        // then it should do nothing
        try{
            changeStack.redo();
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
        } catch (Exception e){
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        assertEquals(2, changeStack.getCurrentIndex());
        assertSame(set_3, changeStack.getPreviousChangeSet());
        assertTrue(state.value());
        
        // can we still undo?
        testUndoAfterClose();
    }

}

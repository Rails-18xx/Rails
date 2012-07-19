package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class BooleanStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 

    private Root root;
    private ChangeStack stack;
    private BooleanState stateDefault;
    private BooleanState stateInit;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stateDefault = BooleanState.create(root,  DEFAULT_ID);
        stateInit = BooleanState.create(root, INIT_ID, true);
        stack = root.getStateManager().getChangeStack();
    }

    @Test
    public void testValue() {
        assertFalse(stateDefault.value());
        assertTrue(stateInit.value());
    }
    
    @Test
    public void testSet() {
        stateDefault.set(true);
        assertTrue(stateDefault.value());
        stateInit.set(false);
        assertFalse(stateInit.value());
    }

    @Test
    public void testSetSameIgnored() {
        stateDefault.set(false);
        stateInit.set(true);
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(stateDefault, stateInit);
    }
    
    @Test
    public void testUndoRedo() {
        assertFalse(stateDefault.value());
        stateDefault.set(true);
        assertTrue(stateDefault.value());
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(stateDefault);
        stack.undo();
        assertFalse(stateDefault.value());
        stack.redo();
        assertTrue(stateDefault.value());
    }

}

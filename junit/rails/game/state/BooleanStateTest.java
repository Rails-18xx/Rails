package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class BooleanStateTest {

    private final static String STATE_ID = "Boolean";

    private Root root;
    private ChangeStack stack;
    private BooleanState state_default;
    private BooleanState state_init;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state_default = BooleanState.create(root,  STATE_ID);
        state_init = BooleanState.create(root, null, true);
        stack = root.getStateManager().getChangeStack();
    }

    @Test
    public void testValue() {
        assertFalse(state_default.value());
        assertTrue(state_init.value());
    }
    
    @Test
    public void testSet() {
        state_default.set(true);
        assertTrue(state_default.value());
        state_init.set(false);
        assertFalse(state_init.value());
    }

    @Test
    public void testUndoRedo() {
        assertFalse(state_default.value());
        state_default.set(true);
        assertTrue(state_default.value());
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(state_default);
        stack.undo();
        assertFalse(state_default.value());
        stack.redo();
        assertTrue(state_default.value());
    }

}

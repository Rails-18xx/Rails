package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class IntegerStateTest {

    private final static String STATE_ID = "Integer";
    private final static int INIT = 10;
    private final static int OTHER = -5;

    private Root root;
    private ChangeStack stack;
    private IntegerState state_default;
    private IntegerState state_init;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stack = root.getStateManager().getChangeStack();
        
        state_default = IntegerState.create(root, STATE_ID);
        state_init = IntegerState.create(root, null, INIT);
    }
    
    @Test
    public void testValue() {
        assertEquals(state_default.value(), 0);
        assertEquals(state_init.value(), INIT);
    }

    @Test
    public void testSet() {
        state_default.set(OTHER);
        assertEquals(state_default.value(), OTHER);
        state_init.set(0);
        assertEquals(state_init.value(), 0);
        
    }

    @Test
    public void testAdd() {
        state_default.add(OTHER);
        assertEquals(state_default.value(), OTHER);
        state_init.add(OTHER);
        assertEquals(state_init.value(), INIT + OTHER);
    }
    
    
    @Test
    public void testSetSameIgnored() {
        state_default.set(0);
        state_init.set((INIT));
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(state_default, state_init);
    }

    @Test
    public void testUndoRedo() {
        
        state_default.set(INIT);
        state_default.add(OTHER);
        
        state_init.add(OTHER);
        state_init.set(0);
        stack.closeCurrentChangeSet();
        
        assertEquals(state_default.value(), INIT+OTHER);
        assertEquals(state_init.value(), 0);

        stack.undo();
        assertEquals(state_default.value(), 0);
        assertEquals(state_init.value(), INIT);

        stack.redo();
        assertEquals(state_default.value(), INIT+OTHER);
        assertEquals(state_init.value(), 0);
    }

}

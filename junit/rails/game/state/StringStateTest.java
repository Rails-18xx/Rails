package rails.game.state;


import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Before;
import org.junit.Test;

public class StringStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    private final static String INIT = "INIT";
    private final static String OTHER = "OTHER";

    private Root root;
    private ChangeStack stack;
    private StringState state_default;
    private StringState state_init;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stack = root.getStateManager().getChangeStack();
        
        state_default = StringState.create(root, DEFAULT_ID);
        state_init = StringState.create(root, INIT_ID, INIT);
    }
    
    @Test
    public void testValue() {
        assertEquals(state_default.value(), "");
        assertEquals(state_init.value(), INIT);
    }

    @Test
    public void testSet() {
        state_default.set(OTHER);
        assertEquals(state_default.value(), OTHER);
        state_init.set("");
        assertEquals(state_init.value(), "");
        state_init.set(null);
        assertEquals(state_init.value(), null);
    }

    @Test
    public void testAppend() {
        state_default.append(OTHER, null);
        assertEquals(state_default.value(), OTHER);
        state_default.append(OTHER, "");
        assertEquals(state_default.value(), OTHER + OTHER);
        
        state_init.append(OTHER, ",");
        assertEquals(state_init.value(), INIT + "," + OTHER);
    }
    
    
    @Test
    public void testSetSameIgnored() {
        state_default.set("");
        state_init.set(null);
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(state_default);
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(state_init);

        state_default.set(null);
        state_init.set(null);
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(state_default);
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(state_init);
    }

    @Test
    public void testUndoRedo() {
        
        state_default.set(OTHER);
        state_default.append(OTHER, null);
        
        state_init.append(OTHER, "");
        state_init.set(null);
        stack.closeCurrentChangeSet();
        
        assertEquals(state_default.value(), OTHER+OTHER);
        assertNull(state_init.value());

        stack.undo();
        assertEquals(state_default.value(), "");
        assertEquals(state_init.value(), INIT);

        stack.redo();
        assertEquals(state_default.value(), OTHER+OTHER);
        assertNull(state_init.value());
    }


}

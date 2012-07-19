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
    private StringState stateDefault;
    private StringState stateInit;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stack = root.getStateManager().getChangeStack();
        
        stateDefault = StringState.create(root, DEFAULT_ID);
        stateInit = StringState.create(root, INIT_ID, INIT);
    }
    
    @Test
    public void testValue() {
        assertEquals(stateDefault.value(), "");
        assertEquals(stateInit.value(), INIT);
    }

    @Test
    public void testSet() {
        stateDefault.set(OTHER);
        assertEquals(stateDefault.value(), OTHER);
        stateInit.set("");
        assertEquals(stateInit.value(), "");
        stateInit.set(null);
        assertEquals(stateInit.value(), null);
    }

    @Test
    public void testAppend() {
        stateDefault.append(OTHER, null);
        assertEquals(stateDefault.value(), OTHER);
        stateDefault.append(OTHER, "");
        assertEquals(stateDefault.value(), OTHER + OTHER);
        
        stateInit.append(OTHER, ",");
        assertEquals(stateInit.value(), INIT + "," + OTHER);
    }
    
    
    @Test
    public void testSetSameIgnored() {
        stateDefault.set("");
        stateInit.set(null);
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(stateDefault);
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(stateInit);

        stateDefault.set(null);
        stateInit.set(null);
        stack.closeCurrentChangeSet();
        assertThat(stack.getLastClosedChangeSet().getStates()).contains(stateDefault);
        assertThat(stack.getLastClosedChangeSet().getStates()).doesNotContain(stateInit);
    }

    @Test
    public void testUndoRedo() {
        
        stateDefault.set(OTHER);
        stateDefault.append(OTHER, null);
        
        stateInit.append(OTHER, "");
        stateInit.set(null);
        stack.closeCurrentChangeSet();
        
        assertEquals(stateDefault.value(), OTHER+OTHER);
        assertNull(stateInit.value());

        stack.undo();
        assertEquals(stateDefault.value(), "");
        assertEquals(stateInit.value(), INIT);

        stack.redo();
        assertEquals(stateDefault.value(), OTHER+OTHER);
        assertNull(stateInit.value());
    }


}

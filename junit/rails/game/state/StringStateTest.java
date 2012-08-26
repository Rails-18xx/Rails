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
    
    private StringState stateDefault;
    private StringState stateInit;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        
        stateDefault = StringState.create(root, DEFAULT_ID);
        stateInit = StringState.create(root, INIT_ID, INIT);
    }
    
    @Test
    public void testValue() {
        assertEquals(stateDefault.value(), null);
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
        stateDefault.set(null);
        stateInit.set(null);
        StateTestUtils.close(root);
        assertThat(StateTestUtils.getLastClosedChangeSet(root).getObservableStates()).doesNotContain(stateDefault);
        assertThat(StateTestUtils.getLastClosedChangeSet(root).getObservableStates()).contains(stateInit);

        StateTestUtils.newChangeSet(root);
        stateDefault.set("");
        stateInit.set(null);
        StateTestUtils.close(root);
        assertThat(StateTestUtils.getLastClosedChangeSet(root).getObservableStates()).contains(stateDefault);
        assertThat(StateTestUtils.getLastClosedChangeSet(root).getObservableStates()).doesNotContain(stateInit);
    }

    @Test
    public void testUndoRedo() {
        
        stateDefault.set(OTHER);
        stateDefault.append(OTHER, null);
        
        stateInit.append(OTHER, "");
        stateInit.set(null);
        StateTestUtils.close(root);
        
        assertEquals(stateDefault.value(), OTHER+OTHER);
        assertNull(stateInit.value());

        StateTestUtils.undo(root);
        assertEquals(stateDefault.value(), null);
        assertEquals(stateInit.value(), INIT);

        StateTestUtils.redo(root);
        assertEquals(stateDefault.value(), OTHER+OTHER);
        assertNull(stateInit.value());
    }


}

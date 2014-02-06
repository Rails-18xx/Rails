package net.sf.rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import net.sf.rails.game.state.IntegerState;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

public class IntegerStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    private final static int INIT = 10;
    private final static int OTHER = -5;

    private Root root;
    
    private IntegerState stateDefault;
    private IntegerState stateInit;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        
        stateDefault = IntegerState.create(root, DEFAULT_ID);
        stateInit = IntegerState.create(root, INIT_ID, INIT);
    }
    
    @Test
    public void testValue() {
        assertEquals(stateDefault.value(), 0);
        assertEquals(stateInit.value(), INIT);
    }

    @Test
    public void testSet() {
        stateDefault.set(OTHER);
        assertEquals(stateDefault.value(), OTHER);
        stateInit.set(0);
        assertEquals(stateInit.value(), 0);
        
    }

    @Test
    public void testAdd() {
        stateDefault.add(OTHER);
        assertEquals(stateDefault.value(), OTHER);
        stateInit.add(OTHER);
        assertEquals(stateInit.value(), INIT + OTHER);
    }
    
    
    @Test
    public void testSetSameIgnored() {
        stateDefault.set(0);
        stateInit.set((INIT));
        StateTestUtils.close(root);
        assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault, stateInit);
    }

    @Test
    public void testUndoRedo() {
        
        stateDefault.set(INIT);
        stateDefault.add(OTHER);
        
        stateInit.add(OTHER);
        stateInit.set(0);
        StateTestUtils.close(root);
        
        assertEquals(stateDefault.value(), INIT+OTHER);
        assertEquals(stateInit.value(), 0);

        StateTestUtils.undo(root);
        assertEquals(stateDefault.value(), 0);
        assertEquals(stateInit.value(), INIT);

        StateTestUtils.redo(root);
        assertEquals(stateDefault.value(), INIT+OTHER);
        assertEquals(stateInit.value(), 0);
    }

}

package net.sf.rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import net.sf.rails.game.state.GenericState;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

public class GenericStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    private final static String ITEM_ID = "Item";
    private final static String ANOTHER_ID = "Another";

    private Root root;
    
    private GenericState<Item> stateDefault;
    private GenericState<Item> stateInit;
    
    private Item item, another_item;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        
        item = new AbstractItemImpl(root, ITEM_ID);
        another_item = new AbstractItemImpl(root, ANOTHER_ID);
        
        stateDefault = GenericState.create(root, DEFAULT_ID);
        stateInit = GenericState.create(root, INIT_ID, item);
    }
    
    @Test
    public void testValue() {
        assertNull(stateDefault.value());
        assertSame(item, stateInit.value());
    }
    
    @Test
    public void testSet() {
        stateDefault.set(item);
        assertSame(item, stateDefault.value());
        stateDefault.set(null);
        assertNull(stateDefault.value());
        stateInit.set(another_item);
        assertSame(another_item, stateInit.value());
    }
    
    @Test
    public void testSetSameIgnored() {
        stateDefault.set(null);
        stateInit.set(item);
        StateTestUtils.close(root);
        assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault, stateInit);
    }

    @Test
    public void testUndoRedo() {
        assertNull(stateDefault.value());
        assertSame(item, stateInit.value());

        stateDefault.set(item);
        stateInit.set(another_item);
        assertSame(item, stateDefault.value());
        assertSame(another_item, stateInit.value());

        StateTestUtils.close(root);
        // remark: stateInit is an internal (isObservable = false)
        assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).contains(stateDefault);
        
        StateTestUtils.undo(root);
        assertNull(stateDefault.value());
        assertSame(item, stateInit.value());

        StateTestUtils.redo(root);
        assertSame(item, stateDefault.value());
        assertSame(another_item, stateInit.value());
    }

}

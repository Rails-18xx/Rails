package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class ArrayListMultimapStateTest {

    private final static String STATE_ID = "State";
    private final static String ITEM_A_ID = "ItemA";
    private final static String ITEM_B_ID = "ItemB";
    private final static String ITEM_C_ID = "ItemC";

    private Root root;
    private ArrayListMultimapState<String, Item> state;
    private Item itemA, itemB, itemC;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state = ArrayListMultimapState.create(root, STATE_ID);
        itemA = OwnableItemImpl.create(root, ITEM_A_ID);
        itemB = OwnableItemImpl.create(root, ITEM_B_ID);
        itemC = OwnableItemImpl.create(root, ITEM_C_ID);
        StateTestUtils.close(root);
 }

    @Test
    public void testPut() {
        state.put(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA);
        state.put(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemA);
        state.put(ITEM_A_ID, itemB);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemA, itemB);
        // test undo
        StateTestUtils.closeAndUndo(root);
        assertTrue(state.isEmpty());
        // test redo
        StateTestUtils.redo(root);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemA, itemB);
    }

    @Test
    public void testRemove() {
        // initialize state
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_A_ID, itemB);
        state.put(ITEM_A_ID, itemA);
        StateTestUtils.close(root);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemB, itemA);
        // remove items
        state.remove(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemB, itemA);
        state.remove(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemB);
        // test undo
        StateTestUtils.closeAndUndo(root);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemB, itemA);
        // test redo
        StateTestUtils.redo(root);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemB);
    }

    @Test
    public void testGet() {
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_A_ID, itemB);
        state.put(ITEM_C_ID, itemC);
        assertThat(state.get(ITEM_A_ID)).containsExactly(itemA, itemB);
        assertThat(state.get(ITEM_C_ID)).containsExactly(itemC);
    }

    @Test
    public void testContainsEntry() {
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_C_ID, itemB);
        assertTrue(state.containsEntry(ITEM_A_ID, itemA));
        assertTrue(state.containsEntry(ITEM_C_ID, itemB));
        
        assertFalse(state.containsEntry(ITEM_B_ID, itemA));
        assertFalse(state.containsEntry(ITEM_A_ID, itemB));
        assertFalse(state.containsEntry(ITEM_B_ID, itemB));
        assertFalse(state.containsEntry(ITEM_C_ID, itemC));
    }

}

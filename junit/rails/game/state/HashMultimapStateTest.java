package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class HashMultimapStateTest {

    private final static String STATE_ID = "State";
    private final static String ITEM_A_ID = "ItemA";
    private final static String ITEM_B_ID = "ItemB";
    private final static String ITEM_C_ID = "ItemC";

    private Root root;
    private HashMultimapState<String, Item> state;
    private Item itemA, itemB, itemC;
    private List<Item> initContents;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state = HashMultimapState.create(root, STATE_ID);
        itemA = OwnableItemImpl.create(root, ITEM_A_ID);
        itemB = OwnableItemImpl.create(root, ITEM_B_ID);
        itemC = OwnableItemImpl.create(root, ITEM_C_ID);
        StateTestUtils.closeAndNew(root);
    }

    private void initState() {
        // initialize state
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_A_ID, itemB);
        state.put(ITEM_A_ID, itemC);
        state.put(ITEM_B_ID, itemB);
        state.put(ITEM_C_ID, itemC);
        StateTestUtils.closeAndNew(root);
        initContents = Lists.newArrayList(itemA, itemB, itemC, itemB, itemC);
    }

    @Test
    public void testPut() {
        state.put(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA);
        assertFalse(state.put(ITEM_A_ID, itemA)); // cannot add identical tuple
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA);
        state.put(ITEM_A_ID, itemB);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA, itemB);
        // test undo
        StateTestUtils.closeAndUndo(root);
        assertTrue(state.isEmpty());
        // test redo
        StateTestUtils.redo(root);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA, itemB);
    }

    @Test
    public void testRemove() {
        initState();
        // remove items
        state.remove(ITEM_A_ID, itemA);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemB, itemC);
        state.remove(ITEM_A_ID, itemC);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemB);
        // test undo
        StateTestUtils.closeAndUndo(root);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA, itemB, itemC);
        // test redo
        StateTestUtils.redo(root);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemB);
    }

    @Test
    public void testRemoveAll() {
        initState();
        Set<Item> removed = state.removeAll(ITEM_A_ID);
        assertThat(removed).containsOnly(itemA, itemB, itemC);
        assertThat(state.get(ITEM_A_ID)).isEmpty();
        // test undo
        StateTestUtils.closeAndUndo(root);
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA, itemB, itemC);
        // test redo
        StateTestUtils.redo(root);
        assertThat(state.get(ITEM_A_ID)).isEmpty();
    }
    
    @Test
    public void testGet() {
        initState();
        assertThat(state.get(ITEM_A_ID)).containsOnly(itemA, itemB, itemC);
        assertThat(state.get(ITEM_C_ID)).containsOnly(itemC);
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
    
    @Test
    public void testContainsKey() {
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_C_ID, itemB);
        assertTrue(state.containsKey(ITEM_A_ID));
        assertFalse(state.containsKey(ITEM_B_ID));
        assertTrue(state.containsKey(ITEM_C_ID));
    }

    @Test
    public void testContainsValue() {
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_C_ID, itemB);
        assertTrue(state.containsValue(itemA));
        assertTrue(state.containsValue(itemB));
        assertFalse(state.containsValue(itemC));
    }
    
    @Test
    public void testSize() {
        assertEquals(0, state.size());
        initState();
        assertEquals(5, state.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(state.isEmpty());
        initState();
        assertFalse(state.isEmpty());
    }
    
    @Test
    public void testKeySet() {
        state.put(ITEM_A_ID, itemA);
        state.put(ITEM_C_ID, itemB);
        assertThat(state.keySet()).containsOnly(ITEM_A_ID, ITEM_C_ID);
    }
    
    @Test
    public void testValues() {
        initState();
        assertThat(state.values()).containsAll(initContents);
    }
    
    private void assertTestIterator(Item thirdItem) {
        // no order is defined, so store them
        Set<Item> iterated = Sets.newHashSet();

        Iterator<Item> it = state.iterator();
        while (it.hasNext()) {
            iterated.add(it.next());
        }
        assertThat(iterated).containsAll(initContents);
        // iterator is an immutable copy, thus not changed by adding a new item
        state.put(ITEM_C_ID, itemA);
        assertFalse(it.hasNext());
        // remove the last added item
        state.remove(ITEM_C_ID, itemA);
    }
    
    @Test
    public void testIterator() {
        initState();
        Item thirdItem = AbstractItemImpl.create(root, "Third");
        assertTestIterator(thirdItem);
    }

    

}

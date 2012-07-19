package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;


import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class HashSetStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    
    private final static String ONE_ITEM_ID = "OneItem";
    private final static String ANOTHER_ITEM_ID = "AnotherItem";

    private Root root;
    private ChangeStack stack;
    private HashSetState<Item> state_default;
    private HashSetState<Item> state_init;
    
    private Item oneItem;
    private Item anotherItem;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stack = root.getStateManager().getChangeStack();
        
        oneItem = AbstractItemImpl.create(root, ONE_ITEM_ID);
        anotherItem = AbstractItemImpl.create(root, ANOTHER_ITEM_ID);
        
        state_default = HashSetState.create(root, DEFAULT_ID);
        state_init = HashSetState.create(root, INIT_ID, Sets.newHashSet(oneItem));
    }
    
    
    @Test
    public void testCreationWithList() {
        // checks if the set is created with a list, that it only contains non-unique elements
        HashSetState<Item> state = HashSetState.create(root, null, Lists.newArrayList(oneItem, oneItem));
        assertThat(state).containsOnly(oneItem);
        assertThat(state).hasSize(1);
    }
    
    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        stack.closeCurrentChangeSet();
        stack.undo();
        assertEquals(state_default.view(), Sets.newHashSet());
        assertEquals(state_init.view(), Sets.newHashSet(oneItem));
        stack.redo();
    }

    private void assertTestAdd() {
        assertThat(state_default).containsOnly(oneItem);
        assertThat(state_init).containsOnly(oneItem, anotherItem);
    }
    
    @Test
    public void testAdd() {
        state_default.add(oneItem);
        state_init.add(anotherItem);
        assertTestAdd();

        // check undo
        assertInitialStateAfterUndo();
        assertTestAdd();
    }

    @Test
    public void testRemove() {
        // remove a non-existing item
        assertFalse(state_default.remove(oneItem));

        // remove an existing item
        assertTrue(state_init.remove(oneItem));

        assertThat(state_init).doesNotContain(oneItem);
        
        // check undo
        assertInitialStateAfterUndo();
        // ... and the redo
        assertThat(state_init).doesNotContain(oneItem);
    }

    @Test
    public void testContains() {
        assertTrue(state_init.contains(oneItem));
        assertFalse(state_init.contains(anotherItem));
    }

    @Test
    public void testClear() {
        state_init.add(anotherItem);
        state_init.clear();
        assertTrue(state_init.isEmpty());
        // check undo and redo
        assertInitialStateAfterUndo();
        assertTrue(state_init.isEmpty());
    }

    @Test
    public void testView() {
        ImmutableSet<Item> list = ImmutableSet.of(oneItem);
        assertEquals(list, state_init.view());
    }

    @Test
    public void testSize() {
        assertEquals(0, state_default.size());
        assertEquals(1, state_init.size());
        state_init.add(anotherItem);
        assertEquals(2, state_init.size());
        state_init.add(oneItem);
        assertEquals(2, state_init.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(state_default.isEmpty());
        assertFalse(state_init.isEmpty());
    }

    private void assertTestIterator(Item thirdItem) {
        Iterator<Item> it = state_init.iterator();
        assertSame(oneItem,it.next());
        assertSame(anotherItem,it.next());
        // iterator is finished
        assertFalse(it.hasNext());
        // iterator is an immutable copy, thus not changed by adding a new item
        state_init.add(thirdItem);
        assertFalse(it.hasNext());
        // remove the last added item
        state_init.remove(thirdItem);
    }
    
    @Test
    public void testIterator() {
        state_init.add(anotherItem);
        Item thirdItem = AbstractItemImpl.create(root, "Third");
        assertTestIterator(thirdItem);
        // check initial state after undo
        assertInitialStateAfterUndo();
        // and check iterator after redo
        assertTestIterator(thirdItem);
    }
    
}

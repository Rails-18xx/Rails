package net.sf.rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Set;


import net.sf.rails.game.state.HashSetState;
import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

public class HashSetStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    private final static String OTHER_ID = "Other";
    
    private final static String ONE_ITEM_ID = "OneItem";
    private final static String ANOTHER_ITEM_ID = "AnotherItem";

    private Root root;
    
    private HashSetState<Item> stateDefault;
    private HashSetState<Item> stateInit;
    
    private Item oneItem;
    private Item anotherItem;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        
        oneItem = AbstractItemImpl.create(root, ONE_ITEM_ID);
        anotherItem = AbstractItemImpl.create(root, ANOTHER_ITEM_ID);
        
        stateDefault = HashSetState.create(root, DEFAULT_ID);
        stateInit = HashSetState.create(root, INIT_ID, Sets.newHashSet(oneItem));
    }
    
    
    @Test
    public void testCreationWithList() {
        // checks if the set is created with a list, that it only contains non-unique elements
        HashSetState<Item> state = HashSetState.create(root, OTHER_ID, Lists.newArrayList(oneItem, oneItem));
        assertThat(state).containsOnly(oneItem);
        assertThat(state).hasSize(1);
    }
    
    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        StateTestUtils.closeAndUndo(root);
        assertEquals(stateDefault.view(), Sets.newHashSet());
        assertEquals(stateInit.view(), Sets.newHashSet(oneItem));
        StateTestUtils.redo(root);
    }

    private void assertTestAdd() {
        assertThat(stateDefault).containsOnly(oneItem);
        assertThat(stateInit).containsOnly(oneItem, anotherItem);
    }
    
    @Test
    public void testAdd() {
        stateDefault.add(oneItem);
        stateInit.add(anotherItem);
        assertTestAdd();

        // check undo
        assertInitialStateAfterUndo();
        assertTestAdd();
    }

    @Test
    public void testRemove() {
        // remove a non-existing item
        assertFalse(stateDefault.remove(oneItem));

        // remove an existing item
        assertTrue(stateInit.remove(oneItem));

        assertThat(stateInit).doesNotContain(oneItem);
        
        // check undo
        assertInitialStateAfterUndo();
        // ... and the redo
        assertThat(stateInit).doesNotContain(oneItem);
    }

    @Test
    public void testContains() {
        assertTrue(stateInit.contains(oneItem));
        assertFalse(stateInit.contains(anotherItem));
    }

    @Test
    public void testClear() {
        stateInit.add(anotherItem);
        stateInit.clear();
        assertTrue(stateInit.isEmpty());
        // check undo and redo
        assertInitialStateAfterUndo();
        assertTrue(stateInit.isEmpty());
    }

    @Test
    public void testView() {
        ImmutableSet<Item> list = ImmutableSet.of(oneItem);
        assertEquals(list, stateInit.view());
    }

    @Test
    public void testSize() {
        assertEquals(0, stateDefault.size());
        assertEquals(1, stateInit.size());
        stateInit.add(anotherItem);
        assertEquals(2, stateInit.size());
        stateInit.add(oneItem);
        assertEquals(2, stateInit.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(stateDefault.isEmpty());
        assertFalse(stateInit.isEmpty());
    }

    private void assertTestIterator(Item thirdItem) {
        // no order is defined, so store them
        Set<Item> iterated = Sets.newHashSet();

        Iterator<Item> it = stateInit.iterator();
        iterated.add(it.next());
        iterated.add(it.next());
        
        assertThat(iterated).containsOnly(oneItem, anotherItem);
        // iterator is finished
        assertFalse(it.hasNext());
        // iterator is an immutable copy, thus not changed by adding a new item
        stateInit.add(thirdItem);
        assertFalse(it.hasNext());
        // remove the last added item
        stateInit.remove(thirdItem);
    }
    
    @Test
    public void testIterator() {
        stateInit.add(anotherItem);
     
        // create another test item
        Item thirdItem = AbstractItemImpl.create(root, "Third");
        
        assertTestIterator(thirdItem);
        // check initial state after undo
        assertInitialStateAfterUndo();
        // and check iterator after redo
        StateTestUtils.close(root); // test requires open changeset
        assertTestIterator(thirdItem);
    }
    
}

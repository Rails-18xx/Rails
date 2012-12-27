package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;


import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class ArrayListStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    
    private final static String ONE_ITEM_ID = "OneItem";
    private final static String ANOTHER_ITEM_ID = "AnotherItem";

    private Root root;
    
    private ArrayListState<Item> stateDefault;
    private ArrayListState<Item> stateInit;
    
    private Item oneItem;
    private Item anotherItem;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        oneItem = AbstractItemImpl.create(root, ONE_ITEM_ID);
        anotherItem = AbstractItemImpl.create(root, ANOTHER_ITEM_ID);
        
        stateDefault = ArrayListState.create(root, DEFAULT_ID);
        stateInit = ArrayListState.create(root, INIT_ID, Lists.newArrayList(oneItem));
    }

    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        StateTestUtils.closeAndUndo(root);
        assertEquals(stateDefault.view(), Lists.newArrayList());
        assertEquals(stateInit.view(), Lists.newArrayList(oneItem));
        StateTestUtils.redo(root);
    }

    private void assertTestAdd() {
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateDefault).containsOnly(oneItem);
        assertThat(stateInit).containsSequence(oneItem, anotherItem);
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
    public void testAddIndex() {
        stateInit.add(0, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem);
        stateInit.add(2, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem, anotherItem);
        stateInit.add(1, oneItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem, oneItem, anotherItem);
        
        // Check undo
        assertInitialStateAfterUndo();
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem, oneItem, anotherItem);
    }
    
    @Test
    public void testAddIndexFail() {
        // open new ChangeSet to test if it is still empty
        StateTestUtils.close(root);
        try {
            stateInit.add(2, anotherItem);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
        try {
            stateInit.add(-1, anotherItem);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
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
    public void testMove() {
        stateInit.add(0, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem);

        stateInit.move(oneItem, 0);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(oneItem, anotherItem);

        stateInit.move(oneItem, 1);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem);

        // try illegal move and check if nothing has changed
        try {
            stateInit.move(oneItem, 2);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem);
        
        
        // check undo
        assertInitialStateAfterUndo();
        // ... and the redo
        // TODO: replace with containsExactly, this does not work yet
        assertThat(stateInit).containsSequence(anotherItem, oneItem);
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
        ImmutableList<Item> list = ImmutableList.of(oneItem);
        assertEquals(list, stateInit.view());
    }

    @Test
    public void testSize() {
        assertEquals(0, stateDefault.size());
        assertEquals(1, stateInit.size());
        stateInit.add(anotherItem);
        stateInit.add(oneItem);
        assertEquals(3, stateInit.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(stateDefault.isEmpty());
        assertFalse(stateInit.isEmpty());
    }

    @Test
    public void testIndexOf() {
        stateInit.add(anotherItem);
        assertEquals(0, stateInit.indexOf(oneItem));
        assertEquals(1, stateInit.indexOf(anotherItem));
        // check if not included
        assertEquals(-1, stateDefault.indexOf(oneItem));
    }

    @Test
    public void testGet() {
        stateInit.add(anotherItem);
        assertSame(oneItem, stateInit.get(0));
        assertSame(anotherItem, stateInit.get(1));
        // check index out of bound
        try {
            stateInit.get(2);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    private void assertTestIterator() {
        Iterator<Item> it = stateInit.iterator();
        assertSame(oneItem,it.next());
        assertSame(anotherItem,it.next());
        // iterator is finished
        assertFalse(it.hasNext());
        // iterator is an immutable copy, thus not changed by adding a new item
        stateInit.add(oneItem);
        assertFalse(it.hasNext());
        // remove the last added item
        stateInit.remove(stateInit.size()-1);
    }
    
    @Test
    public void testIterator() {
        stateInit.add(anotherItem);
        assertTestIterator();
        // check initial state after undo
        assertInitialStateAfterUndo();
        // and check iterator after redo
        StateTestUtils.close(root);// requires open changeset
        assertTestIterator();
    }

}

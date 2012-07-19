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
    private ChangeStack stack;
    private ArrayListState<Item> state_default;
    private ArrayListState<Item> state_init;
    
    private Item oneItem;
    private Item anotherItem;
    

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        stack = root.getStateManager().getChangeStack();
        
        oneItem = AbstractItemImpl.create(root, ONE_ITEM_ID);
        anotherItem = AbstractItemImpl.create(root, ANOTHER_ITEM_ID);
        
        state_default = ArrayListState.create(root, DEFAULT_ID);
        state_init = ArrayListState.create(root, INIT_ID, Lists.newArrayList(oneItem));
    }

    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        stack.closeCurrentChangeSet();
        stack.undo();
        assertEquals(state_default.view(), Lists.newArrayList());
        assertEquals(state_init.view(), Lists.newArrayList(oneItem));
        stack.redo();
    }

    private void assertTestAdd() {
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_default).containsOnly(oneItem);
        assertThat(state_init).containsSequence(oneItem, anotherItem);
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
    public void testAddIndex() {
        state_init.add(0, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem);
        state_init.add(2, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem, anotherItem);
        state_init.add(1, oneItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem, oneItem, anotherItem);
        
        // Check undo
        assertInitialStateAfterUndo();
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem, oneItem, anotherItem);
    }
    
    @Test
    public void testAddIndexFail() {
        // open new ChangeSet to test if it is still empty
        StateTestUtils.startActionChangeSet(root);
        try {
            state_init.add(2, anotherItem);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
        try {
            state_init.add(-1, anotherItem);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
        // close and do the check
        stack.closeCurrentChangeSet();
        assertTrue(stack.getLastClosedChangeSet().isEmpty());
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
    public void testMove() {
        state_init.add(0, anotherItem);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem);

        state_init.move(oneItem, 0);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(oneItem, anotherItem);

        state_init.move(oneItem, 1);
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem);

        // try illegal move and check if nothing has changed
        try {
            state_init.move(oneItem, 2);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem);
        
        
        // check undo
        assertInitialStateAfterUndo();
        // ... and the redo
        // TODO: replace with containsExactly, this does not work yet
        assertThat(state_init).containsSequence(anotherItem, oneItem);
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
        ImmutableList<Item> list = ImmutableList.of(oneItem);
        assertEquals(list, state_init.view());
    }

    @Test
    public void testSize() {
        assertEquals(0, state_default.size());
        assertEquals(1, state_init.size());
        state_init.add(anotherItem);
        state_init.add(oneItem);
        assertEquals(3, state_init.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(state_default.isEmpty());
        assertFalse(state_init.isEmpty());
    }

    @Test
    public void testIndexOf() {
        state_init.add(anotherItem);
        assertEquals(0, state_init.indexOf(oneItem));
        assertEquals(1, state_init.indexOf(anotherItem));
        // check if not included
        assertEquals(-1, state_default.indexOf(oneItem));
    }

    @Test
    public void testGet() {
        state_init.add(anotherItem);
        assertSame(oneItem, state_init.get(0));
        assertSame(anotherItem, state_init.get(1));
        // check index out of bound
        try {
            state_init.get(2);
            failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
        }
    }

    private void assertTestIterator() {
        Iterator<Item> it = state_init.iterator();
        assertSame(oneItem,it.next());
        assertSame(anotherItem,it.next());
        // iterator is finished
        assertFalse(it.hasNext());
        // iterator is an immutable copy, thus not changed by adding a new item
        state_init.add(oneItem);
        assertFalse(it.hasNext());
        // remove the last added item
        state_init.remove(state_init.size()-1);
    }
    
    @Test
    public void testIterator() {
        state_init.add(anotherItem);
        assertTestIterator();
        // check initial state after undo
        assertInitialStateAfterUndo();
        // and check iterator after redo
        assertTestIterator();
    }

}

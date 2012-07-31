package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class HashMapStateTest {

    private final static String DEFAULT_ID = "Default";
    private final static String INIT_ID = "Init"; 
    
    private final static String FIRST_ITEM_ID = "FirstItem";
    private final static String NEW_FIRST_ITEM_ID = "NewFirstItem";
    private final static String SECOND_ITEM_ID = "SecondItem";
    private final static String THIRD_ITEM_ID = "ThirdItem";

    private Root root;
    
    private HashMapState<String, Item> state_default;
    private HashMapState<String, Item> stateInit; 
    private Map<String, Item> initMap, testMap;
    
    private Item firstItem, newFirstItem, secondItem, thirdItem;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        
        firstItem = AbstractItemImpl.create(root, FIRST_ITEM_ID);
        newFirstItem = AbstractItemImpl.create(root, NEW_FIRST_ITEM_ID);
        secondItem = AbstractItemImpl.create(root, SECOND_ITEM_ID);
        thirdItem = AbstractItemImpl.create(root, THIRD_ITEM_ID);
        
        state_default = HashMapState.create(root, DEFAULT_ID);
        
        // intialize stateInit with initMap and create testMap
        initMap = ImmutableMap.of(FIRST_ITEM_ID, firstItem);
        stateInit = HashMapState.create(root, INIT_ID, initMap);

        testMap = Maps.newHashMap();
        testMap.put(FIRST_ITEM_ID, newFirstItem);
        testMap.put(SECOND_ITEM_ID, secondItem);
    }

    
    @Test
    public void testCreate() {
        HashMapState<String, Item> state = HashMapState.create(root, "Test");
        assertThat(state.viewMap()).isEmpty();
    }

    @Test
    public void testCreateMapOfKV() {
        HashMapState<String, Item> state = HashMapState.create(root, "Test", testMap);
        assertEquals(state.viewMap(), testMap);
    }

    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        StateTestUtils.closeAndUndo(root);
        assertThat(state_default.viewMap()).isEmpty();
        assertEquals(stateInit.viewMap(), initMap);
        StateTestUtils.redo(root);
    }

    
    @Test
    public void testPut() {
        for (String key: testMap.keySet()) {
            state_default.put(key, testMap.get(key));
            stateInit.put(key, testMap.get(key));
        }
        assertEquals(state_default.viewMap(), testMap);
        assertEquals(stateInit.viewMap(), testMap);

        // check undo and redo
        assertInitialStateAfterUndo();
        assertEquals(state_default.viewMap(), testMap);
        assertEquals(stateInit.viewMap(), testMap);
    }

    // includes tests for viewMap
    @Test
    public void testPutAll() {
        stateInit.putAll(testMap);
        state_default.putAll(testMap);
        assertEquals(state_default.viewMap(), testMap);
        assertEquals(stateInit.viewMap(), testMap);

        // check undo and redo
        assertInitialStateAfterUndo();
        assertEquals(testMap, state_default.viewMap());
        assertEquals(testMap, stateInit.viewMap());
    }

    @Test
    public void testGet() {
        assertEquals(firstItem, stateInit.get(FIRST_ITEM_ID));
        assertNull(stateInit.get(SECOND_ITEM_ID));
    }

    @Test
    public void testRemove() {
        stateInit.remove(FIRST_ITEM_ID);
        assertNull(stateInit.get(FIRST_ITEM_ID));

        // check undo and redo
        assertInitialStateAfterUndo();
        assertNull(stateInit.get(FIRST_ITEM_ID));
    }

    @Test
    public void testContainsKey() {
        state_default.put(THIRD_ITEM_ID, thirdItem);
        assertTrue(state_default.containsKey(THIRD_ITEM_ID));

        assertTrue(stateInit.containsKey(FIRST_ITEM_ID));
        assertFalse(stateInit.containsKey(SECOND_ITEM_ID));
    }

    @Test
    public void testClear() {
        state_default.putAll(testMap);
        state_default.clear();
        assertTrue(state_default.isEmpty());
        
        // check undo and redo
        assertInitialStateAfterUndo();
        assertTrue(state_default.isEmpty());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(state_default.isEmpty());
        state_default.put(FIRST_ITEM_ID, firstItem);
        assertFalse(state_default.isEmpty());

        assertFalse(stateInit.isEmpty());
        stateInit.remove(FIRST_ITEM_ID);
        assertTrue(stateInit.isEmpty());
    }

    
    @Test
    public void testInitFromMap() {
        state_default.put(THIRD_ITEM_ID, thirdItem);
        state_default.initFromMap(testMap);
        assertEquals(testMap, state_default.viewMap());
        
        // check undo and redo
        assertInitialStateAfterUndo();
        assertEquals(ImmutableMap.copyOf(testMap), state_default.viewMap());
    }

    @Test
    public void testViewKeySet() {
        state_default.putAll(testMap);
        assertThat(state_default.viewKeySet()).containsAll(testMap.keySet());
    }

    @Test
    public void testViewValues() {
        state_default.putAll(testMap);
        assertThat(state_default.viewValues()).containsAll(testMap.values());
    }

}

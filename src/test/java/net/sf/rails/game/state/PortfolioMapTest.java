package net.sf.rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Set;

import net.sf.rails.game.state.Ownable;
import net.sf.rails.game.state.Owner;
import net.sf.rails.game.state.PortfolioMap;
import net.sf.rails.game.state.PortfolioSet;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.SetMultimap;
import com.google.common.collect.Sets;

public class PortfolioMapTest {

    private final static String PORTFOLIO_MAP_ID = "PortfolioMap";
    private final static String PORTFOLIO_SET_ID = "PortfolioSet";
    private final static String OWNER_MAP_ID = "OwnerMap";
    private final static String OWNER_SET_ID = "OwnerSet";
    private final static String ITEM_ID = "Item";
    private final static String ANOTHER_ITEM_ID = "AnotherItem";
    private final static String TYPE_ID = "Type";
    private final static String ANOTHER_TYPE_ID = "AnotherType";
    
    private Root root;
    private PortfolioMap<String, TypeOwnableItemImpl> portfolioMap;
    private PortfolioSet<TypeOwnableItemImpl> portfolioSet;
    private Owner ownerMap;
    private Owner ownerSet;
    private TypeOwnableItemImpl item;
    private TypeOwnableItemImpl anotherItem;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        ownerMap = OwnerImpl.create(root, OWNER_MAP_ID);
        ownerSet = OwnerImpl.create(root, OWNER_SET_ID);
        portfolioMap = PortfolioMap.create(ownerMap, PORTFOLIO_MAP_ID , TypeOwnableItemImpl.class);
        portfolioSet = PortfolioSet.create(ownerSet, PORTFOLIO_SET_ID , TypeOwnableItemImpl.class);
        item = TypeOwnableItemImpl.create(root, ITEM_ID, TYPE_ID);
        anotherItem = TypeOwnableItemImpl.create(root, ANOTHER_ITEM_ID, ANOTHER_TYPE_ID);
        portfolioSet.add(item);
        StateTestUtils.close(root);
    }

    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        StateTestUtils.closeAndUndo(root);
        assertTrue(portfolioSet.containsItem(item));
        assertSame(ownerSet, item.getOwner());
        StateTestUtils.redo(root);
    }
    
    @Test
        public void testAdd() {
            portfolioMap.add(item);
            assertSame(ownerMap, item.getOwner());
            assertTrue(portfolioMap.containsItem(item));
            assertFalse(portfolioSet.containsItem(item));
            // check undo
            assertInitialStateAfterUndo();
            // and redo
            assertTrue(portfolioMap.containsItem(item));
        }

    @Test
    public void testContainsItem() {
        assertFalse(portfolioMap.containsItem(item));
        item.moveTo(ownerMap);
        assertTrue(portfolioMap.containsItem(item));
    }

    @Test
    public void testItems() {
        assertThat(portfolioMap.items()).isEmpty();
        item.moveTo(ownerMap);
        assertThat(portfolioMap.items()).containsOnly(item);
        anotherItem.moveTo(ownerMap);
        assertThat(portfolioMap.items()).containsOnly(item, anotherItem);
    }

    @Test
    public void testSize() {
        assertEquals(0, portfolioMap.size());
        item.moveTo(ownerMap);
        assertEquals(1, portfolioMap.size());
        anotherItem.moveTo(ownerMap);
        assertEquals(2, portfolioMap.size());
    }

    @Test
    public void testIsEmpty() {
        assertTrue(portfolioMap.isEmpty());
        item.moveTo(ownerMap);
        assertFalse(portfolioMap.isEmpty());
    }

    @Test
    public void testContainsKey() {
        assertFalse(portfolioMap.containsKey(TYPE_ID));
        item.moveTo(ownerMap);
        assertTrue(portfolioMap.containsKey(TYPE_ID));
        item.moveTo(ownerSet);
        assertFalse(portfolioMap.containsKey(TYPE_ID));
    }

    @Test
    public void testGetItems() {
        assertThat(portfolioMap.items(TYPE_ID)).isEmpty();
        item.moveTo(ownerMap);
        assertThat(portfolioMap.items(TYPE_ID)).containsOnly(item);
    }

    @Test
    public void testView() {
        item.moveTo(ownerMap);
        SetMultimap<String, TypeOwnableItemImpl> view = portfolioMap.view();
        assertTrue(view.containsValue(item));
        // still holds true after removing item
        item.moveTo(ownerSet);
        assertTrue(view.containsValue(item));
        assertFalse(portfolioMap.containsItem(item));
    }   

    @Test
    public void testIterator() {
        item.moveTo(ownerMap);
        anotherItem.moveTo(ownerMap);
        
        // no order is defined, so store them
        Set<Ownable> iterated = Sets.newHashSet();
        

        Iterator<TypeOwnableItemImpl> it = portfolioMap.iterator();
        // and it still works even after removing items
        item.moveTo(ownerSet);
        
        iterated.add(it.next());
        iterated.add(it.next());
        
        assertThat(iterated).containsOnly(item, anotherItem);
        // iterator is finished
        assertFalse(it.hasNext());
    }

}

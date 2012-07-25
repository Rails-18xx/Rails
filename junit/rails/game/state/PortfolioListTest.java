package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import java.util.Iterator;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

public class PortfolioListTest {

    private final static String PORTFOLIO_A_ID = "PortfolioA";
    private final static String PORTFOLIO_B_ID = "PortfolioB";
    private final static String OWNER_A_ID = "OwnerA";
    private final static String OWNER_B_ID = "OwnerB";
    private final static String ITEM_ID = "Item";
    private final static String ANOTHER_ITEM_ID = "AnotherItem";
    
    private Root root;
    private PortfolioList<Ownable> portfolioA;
    private PortfolioList<Ownable> portfolioB;
    private Owner ownerA;
    private Owner ownerB;
    private Ownable item;
    private Ownable anotherItem;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        ownerA = OwnerImpl.create(root, OWNER_A_ID);
        ownerB = OwnerImpl.create(root, OWNER_B_ID);
        portfolioA = PortfolioList.create(ownerA, PORTFOLIO_A_ID , Ownable.class);
        portfolioB = PortfolioList.create(ownerB, PORTFOLIO_B_ID , Ownable.class);
        item = OwnableItemImpl.create(root, ITEM_ID);
        anotherItem = OwnableItemImpl.create(root, ANOTHER_ITEM_ID);
        portfolioA.moveInto(item);
        StateTestUtils.startActionChangeSet(root);
    }

    // helper function to check the initial state after undo
    // includes redo, so after returning the state should be unchanged
    private void assertInitialStateAfterUndo() {
        StateTestUtils.closeAndUndo(root);
        assertTrue(portfolioA.containsItem(item));
        assertSame(ownerA, item.getOwner());
        StateTestUtils.redo(root);
    }
    
    @Test
    public void testMoveInto() {
        // move item to B
        item.moveTo(ownerB);
        assertTrue(portfolioB.containsItem(item));
        assertSame(ownerB, item.getOwner());
        
        // undo check
        assertInitialStateAfterUndo();
        
        // redo check
        assertTrue(portfolioB.containsItem(item));
        assertSame(ownerB, item.getOwner());
    }

    @Test
    public void testContainsItem() {
        assertTrue(portfolioA.containsItem(item));
        assertFalse(portfolioB.containsItem(item));
    }

    @Test
    public void testItems() {
        assertThat(portfolioA.items()).containsOnly(item);
        anotherItem.moveTo(ownerA);
        assertThat(portfolioA.items()).containsOnly(item, anotherItem);
    }

    @Test
    public void testSize() {
        assertEquals(1, portfolioA.size());
        assertEquals(0, portfolioB.size());
        anotherItem.moveTo(ownerA);
        assertEquals(2, portfolioA.size());
        item.moveTo(ownerB);
        assertEquals(1, portfolioA.size());
        assertEquals(1, portfolioB.size());
    }

    @Test
    public void testIsEmpty() {
        assertFalse(portfolioA.isEmpty());
        assertTrue(portfolioB.isEmpty());
    }

    @Test
    public void testIterator() {
        anotherItem.moveTo(ownerA);
        
        // no order is defined, so store them
        Set<Ownable> iterated = Sets.newHashSet();

        Iterator<Ownable> it = portfolioA.iterator();
        iterated.add(it.next());
        iterated.add(it.next());
        
        assertThat(iterated).containsOnly(item, anotherItem);
        // iterator is finished
        assertFalse(it.hasNext());
    }

}

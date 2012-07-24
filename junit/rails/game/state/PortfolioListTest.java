package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class PortfolioListTest {

    private final static String PORTFOLIO_A_ID = "PortfolioA";
    private final static String PORTFOLIO_B_ID = "PortfolioB";
    private final static String OWNER_A_ID = "OwnerA";
    private final static String OWNER_B_ID = "OwnerB";
    private final static String ITEM_ID = "Item";
    
    private Root root;
    private PortfolioList<Ownable> portfolioA;
    private PortfolioList<Ownable> portfolioB;
    private Owner ownerA;
    private Owner ownerB;
    private Ownable item;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        ownerA = OwnerImpl.create(root, OWNER_A_ID);
        ownerB = OwnerImpl.create(root, OWNER_B_ID);
        portfolioA = PortfolioList.create(ownerA, PORTFOLIO_A_ID , Ownable.class);
        portfolioB = PortfolioList.create(ownerA, PORTFOLIO_B_ID , Ownable.class);
        item = OwnableItemImpl.create(root, ITEM_ID);
    }

    @Test
    public void testInitialAdd() {
        portfolioA.initialAdd(item);
        assertTrue(portfolioA.containsItem(item));
        assertSame(portfolioA, item.getOwner());
    }

    @Test
    public void testMoveInto() {
        fail("Not yet implemented");
    }

    @Test
    public void testContainsItem() {
        fail("Not yet implemented");
    }

    @Test
    public void testItems() {
        fail("Not yet implemented");
    }

    @Test
    public void testSize() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsEmpty() {
        fail("Not yet implemented");
    }

    @Test
    public void testChange() {
        fail("Not yet implemented");
    }

    @Test
    public void testIterator() {
        fail("Not yet implemented");
    }

}

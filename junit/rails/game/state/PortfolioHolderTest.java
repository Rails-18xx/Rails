package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class PortfolioHolderTest {
    
    private final static String PORTFOLIO_MAP_ID = "PortfolioMap";
    private final static String PORTFOLIO_SET_ID = "PortfolioSet";
    private final static String OWNER_ID = "Owner";
    private final static String HOLDER_ID = "Holder";
    
    private final static String ITEM_ID = "Item";
    private final static String TYPE_ID = "Type";
    
    private Root root;
    private Owner owner;
    private PortfolioHolder holder;
    private TypeOwnableItemImpl item;

    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        owner = OwnerImpl.create(root, OWNER_ID);
        holder = PortfolioHolderImpl.create(owner, HOLDER_ID);
        
        item = TypeOwnableItemImpl.create(root, ITEM_ID, TYPE_ID);
        StateTestUtils.startActionChangeSet(root);
    }
    
    @Test
    public void testPortfolioMap() {
        PortfolioMap<String, TypeOwnableItemImpl> portfolio = 
                PortfolioMap.create(holder, PORTFOLIO_MAP_ID , TypeOwnableItemImpl.class);
        item.moveTo(owner);
        assertTrue(portfolio.containsItem(item));
        assertSame(owner, item.getOwner());
    }
    
    @Test
    public void testPortfolioSet() {
        PortfolioSet<TypeOwnableItemImpl> portfolio = 
                PortfolioSet.create(holder, PORTFOLIO_SET_ID , TypeOwnableItemImpl.class);
        item.moveTo(owner);
        assertTrue(portfolio.containsItem(item));
        assertSame(owner, item.getOwner());
    }
}

package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class AbstractItemTest {

    private static final String MANAGER_ID = "manager";
    private static final String ITEM_ID = "item";
    private static final String ANOTHER_ID = "anotherItem";
   
    private Root root;
    private Manager manager;
    private Item item;
    private Item anotherItem;
    
    @Before
    public void setUp() {
        root = Root.create();
        manager = new ManagerImpl(root, MANAGER_ID);
        item = new AbstractItemImpl(manager, ITEM_ID);
        anotherItem = new AbstractItemImpl(item, ANOTHER_ID);
    }
    
    @Test
    public void testGetId() {
        assertEquals(item.getId(), ITEM_ID);
        assertEquals(anotherItem.getId(), ANOTHER_ID);
    }

    @Test
    public void testGetParent() {
        assertSame(item.getParent(), manager);
        assertSame(anotherItem.getParent(), item);
    }

    @Test
    public void testGetContext() {
        assertSame(item.getContext(), manager);
        assertSame(anotherItem.getContext(), manager);
    }

    @Test
    public void testGetRoot() {
        assertSame(item.getRoot(), root);
        assertSame(anotherItem.getRoot(), root);
    }

    @Test
    public void testGetURI() {
        assertEquals(item.getURI(), ITEM_ID);
        assertEquals(anotherItem.getURI(), ITEM_ID + Item.SEP + ANOTHER_ID);
    }

    @Test
    public void testGetFullURI() {
        assertEquals(item.getFullURI(), Item.SEP + MANAGER_ID + Item.SEP + ITEM_ID);
        assertEquals(anotherItem.getFullURI(), Item.SEP + MANAGER_ID+ Item.SEP + ITEM_ID + Item.SEP + ANOTHER_ID);
    }

}

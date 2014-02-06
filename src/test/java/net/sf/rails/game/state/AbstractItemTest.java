package net.sf.rails.game.state;

import static org.junit.Assert.*;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Manager;
import net.sf.rails.game.state.Root;

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
        assertEquals(ITEM_ID, item.getId());
        assertEquals(ANOTHER_ID, anotherItem.getId());
    }

    @Test
    public void testGetParent() {
        assertSame(manager, item.getParent());
        assertSame(item, anotherItem.getParent());
    }

    @Test
    public void testGetContext() {
        assertSame(manager, item.getContext());
        assertSame(manager, anotherItem.getContext());
    }

    @Test
    public void testGetRoot() {
        assertSame(root, item.getRoot());
        assertSame(root, anotherItem.getRoot());
    }

    @Test
    public void testGetURI() {
        assertEquals(ITEM_ID, item.getURI());
        assertEquals(ITEM_ID + Item.SEP + ANOTHER_ID, anotherItem.getURI());
    }

    @Test
    public void testGetFullURI() {
        assertEquals(Item.SEP + MANAGER_ID + Item.SEP + ITEM_ID, item.getFullURI());
        assertEquals(Item.SEP + MANAGER_ID+ Item.SEP + ITEM_ID + Item.SEP + ANOTHER_ID, anotherItem.getFullURI());
    }

}

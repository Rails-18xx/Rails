package net.sf.rails.game.state;

import static org.junit.Assert.*;

import net.sf.rails.game.state.Item;
import net.sf.rails.game.state.Manager;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;

public class ManagerTest {

    private static final String MANAGER_ID = "manager";
    private static final String ITEM_ID = "item";
    private static final String ANOTHER_ITEM_ID = "anotherItem";
    private static final String ANOTHER_MANAGER_ID = "anotherManager";

    private Root root;
    private Manager manager, anotherManager;
    private Item item, anotherItem;

    @Before
    public void setUp() {
        root = Root.create();
        item = new AbstractItemImpl(root, ITEM_ID);
        manager = new ManagerImpl(item, MANAGER_ID);
        anotherItem = new AbstractItemImpl(manager, ANOTHER_ITEM_ID);
        anotherManager = new ManagerImpl(manager, ANOTHER_MANAGER_ID);
    }

    @Test
    public void testGetId() {
        assertEquals(MANAGER_ID, manager.getId());
        assertEquals(ANOTHER_MANAGER_ID, anotherManager.getId());
    }

    @Test
    public void testGetParent() {
        assertSame(item, manager.getParent());
        assertSame(manager, anotherManager.getParent());
    }

    @Test
    public void testGetContext() {
        assertSame(root, manager.getContext());
        assertSame(manager, anotherManager.getContext());
    }

    @Test
    public void testGetURI() {
        assertEquals(ITEM_ID + Item.SEP + MANAGER_ID, manager.getURI());
        assertEquals(ANOTHER_MANAGER_ID, anotherManager.getURI());
    }

    @Test
    public void testGetFullURI() {
        assertEquals(Item.SEP + ITEM_ID + Item.SEP + MANAGER_ID, manager.getFullURI());
        assertEquals(Item.SEP + ITEM_ID + Item.SEP + MANAGER_ID + Item.SEP + ANOTHER_MANAGER_ID, anotherManager.getFullURI());
    }

    @Test
    public void testLocate() {
        // anotherItem is local
        assertSame(anotherItem, manager.locate(anotherItem.getURI()));
        assertSame(anotherItem, manager.locate(anotherItem.getFullURI()));
        
        // item is not local
        assertNull(manager.locate(item.getURI()));
        assertSame(item, manager.locate(item.getFullURI()));

        // manager is not local in itself, but in root
        assertNull(manager.locate(manager.getURI()));
        assertSame(manager, root.locate(manager.getURI()));
        assertSame(manager, manager.locate(manager.getFullURI()));

        // anotherManager is not local in itself, but in manager
        assertNull(anotherManager.locate(anotherManager.getURI()));
        assertSame(anotherManager, manager.locate(anotherManager.getURI()));
        assertSame(anotherManager, anotherManager.locate(anotherManager.getFullURI()));
    }

    @Test
    public void testGetRoot() {
        assertSame(root, manager.getRoot());
        assertSame(root, anotherManager.getRoot());
    }

}

package rails.game.state;

import static org.junit.Assert.*;

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
        assertEquals(manager.getId(), MANAGER_ID);
        assertEquals(anotherManager.getId(), ANOTHER_MANAGER_ID);
    }

    @Test
    public void testGetParent() {
        assertSame(manager.getParent(), item);
        assertSame(anotherManager.getParent(), manager);
    }

    @Test
    public void testGetContext() {
        System.out.println();
        assertSame(manager.getContext(), root);
        assertSame(anotherManager.getContext(), manager);
    }

    @Test
    public void testGetURI() {
        assertEquals(manager.getURI(), ITEM_ID + Item.SEP + MANAGER_ID);
        assertEquals(anotherManager.getURI(), ANOTHER_MANAGER_ID);
    }

    @Test
    public void testGetFullURI() {
        assertEquals(manager.getFullURI(), Item.SEP + ITEM_ID + Item.SEP + MANAGER_ID);
        assertEquals(anotherManager.getFullURI(), Item.SEP + ITEM_ID + Item.SEP + MANAGER_ID + Item.SEP + ANOTHER_MANAGER_ID);
    }

    @Test
    public void testLocate() {
        // anotherItem is local
        assertSame(manager.locate(anotherItem.getURI()), anotherItem);
        assertSame(manager.locate(anotherItem.getFullURI()), anotherItem);
        
        // item is not local
        assertNull(manager.locate(item.getURI()));
        assertSame(manager.locate(item.getFullURI()), item);

        // manager is not local in itself, but in root
        assertNull(manager.locate(manager.getURI()));
        assertSame(root.locate(manager.getURI()), manager);
        assertSame(manager.locate(manager.getFullURI()), manager);

        // anotherManager is not local in itself, but in manager
        assertNull(anotherManager.locate(anotherManager.getURI()));
        assertSame(manager.locate(anotherManager.getURI()), anotherManager);
        assertSame(anotherManager.locate(anotherManager.getFullURI()), anotherManager);
    }

    @Test
    public void testGetRoot() {
        assertSame(manager.getRoot(), root);
        assertSame(anotherManager.getRoot(), root);
    }

}

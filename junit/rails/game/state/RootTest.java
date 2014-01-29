package rails.game.state;

import static org.junit.Assert.*;
import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

public class RootTest {

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
        item = new AbstractItemImpl(root, ITEM_ID);
        anotherItem = new AbstractItemImpl(manager, ANOTHER_ID);
    }

    @Test
    public void testGetStateManager() {
        assertThat(root.getStateManager()).isInstanceOf(StateManager.class);
    }

    @Test(expected = UnsupportedOperationException.class)
    public void testGetParent() {
        root.getParent();
    }

    @Test
    public void testGetId() {
        assertEquals(Root.ID, root.getId());
    }

    @Test
    public void testGetContext() {
        assertSame(root, root.getContext());
    }

    @Test
    public void testGetRoot() {
        assertSame(root, root.getRoot());
    }

    @Test
    public void testGetURI() {
        assertSame(Root.ID, root.getURI());
    }

    @Test
    public void testGetFullURI() {
        assertSame(Root.ID, root.getFullURI());
    }

    @Test
    public void testLocate() {
        // item is local
        assertSame(item, root.locate(item.getURI()));
        assertSame(item, root.locate(item.getFullURI()));
        // manager is local
        assertSame(manager, root.locate(manager.getURI()));
        assertSame(manager, root.locate(manager.getFullURI()));
        // anotherItem is not local
        assertNull(root.locate(anotherItem.getURI()));
        assertSame(root.locate(anotherItem.getFullURI()), anotherItem);
        // root is local in root
        assertSame(root, root.locate(root.getURI()));
        assertSame(root, root.locate(root.getFullURI()));
        // and if item is removed it is not found anymore
        root.removeItem(item);
        assertNull(root.locate(item.getURI()));
        assertNull(root.locate(item.getFullURI()));
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testAddItemFail() {
        root.addItem(item);
    }

    public void testAddItemSuccess() {
        root.removeItem(item);
        root.addItem(item);
        assertSame(item, root.locate(item.getFullURI()));
    }
    
    public void testRemoveItemSuccess() {
        root.removeItem(item);
        root.locate(item.getFullURI());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testRemoveItemFail() {
        root.removeItem(item);
        root.removeItem(item);
    }

}

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
        assertEquals(root.getId(), Root.ID);
    }

    @Test
    public void testGetContext() {
        assertSame(root.getContext(), root);
    }

    @Test
    public void testGetRoot() {
        assertSame(root.getRoot(), root);
    }

    @Test
    public void testGetURI() {
        assertSame(root.getURI(), Root.ID);
    }

    @Test
    public void testGetFullURI() {
        assertSame(root.getFullURI(), Root.ID);
    }

    @Test
    public void testLocate() {
        assertSame(root.locate(item.getFullURI()), item);
        assertSame(root.locate(anotherItem.getFullURI()), anotherItem);
    }
    
    @Test
    public void testLocateFail() {
        root.removeItem(item);
        assertNull(root.locate(item.getFullURI()));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testAddItemFail() {
        root.addItem(item);
    }

    public void testAddItemSuccess() {
        root.removeItem(item);
        root.addItem(item);
        assertSame(root.locate(item.getFullURI()), item);
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

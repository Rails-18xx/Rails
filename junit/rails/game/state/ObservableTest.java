package rails.game.state;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ObservableTest {
    
    // Implementation for Testing only
    private class ObservableImpl extends Observable {
        ObservableImpl(Item parent, String id) {
            super(parent, id);
        }

        @Override
        public String toText() {
            return null;
        }
    }

    private final static String MANAGER_ID = "Manager";
    private final static String ITEM_ID = "Item";
    private final static String OBS_ID = "Observable";
    
    private Root root;
    private Manager manager;
    private Item item;
    private Observable observable, unobservable;
    @Mock private Observer observer;
    @Mock private Model model;
    
    @Before
    public void setUp() {
        root = Root.create();
        manager = new ManagerImpl(root, MANAGER_ID);
        item = new AbstractItemImpl(manager, ITEM_ID);
        observable = new ObservableImpl(item, OBS_ID);
        unobservable = new ObservableImpl(root, null);
    }
    

    @Test
    public void testObservers() {
        // add observer and test if contained
        observable.addObserver(observer);
        assertThat(observable.getObservers()).contains(observer);
        
        // remove observer and test if not contained
        assertTrue(observable.removeObserver(observer));
        assertThat(observable.getObservers()).doesNotContain(observer);
        
        // remove observer not contained anymore
        assertFalse(observable.removeObserver(observer));
        
        // Check failing on unobservable
        try {
            unobservable.addObserver(observer);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        try {
            unobservable.removeObserver(observer);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        try {
            unobservable.getObservers();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testModels() {
        // add observer and test if contained
        observable.addModel(model);
        assertThat(observable.getModels()).contains(model);
        
        // check update
        observable.updateModels();
        verify(model).update();
        
        // remove Model and test if not contained
        assertTrue(observable.removeModel(model));
        assertThat(observable.getModels()).doesNotContain(model);
        observable.updateModels();
        // still only called once(!)
        verify(model).update();
        
        // remove Model not contained anymore
        assertFalse(observable.removeModel(model));
        
        // Check failing on unobservable
        try {
            unobservable.addModel(model);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        try {
            unobservable.removeModel(model);
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        try {
            unobservable.getModels();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }

    }

    @Test
    public void testGetId() {
        assertEquals(OBS_ID, observable.getId());
        assertEquals(null, unobservable.getId());
    }

    @Test
    public void testGetParent() {
        assertSame(item, observable.getParent());
        assertSame(root, unobservable.getParent());
    }

    @Test
    public void testGetContext() {
        assertSame(manager, observable.getContext());
        assertSame(root, unobservable.getContext());
    }

    @Test
    public void testGetRoot() {
        assertSame(root, observable.getRoot());
        assertSame(root, unobservable.getRoot());
    }

    @Test
    public void testGetURI() {
        assertEquals(ITEM_ID + Item.SEP + OBS_ID, observable.getURI());
        assertSame(observable, observable.getContext().locate(observable.getURI()));
        try {
            unobservable.getURI();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void testGetFullURI() {
        assertEquals(Item.SEP + MANAGER_ID + Item.SEP + ITEM_ID + Item.SEP + OBS_ID, observable.getFullURI());
        assertSame(observable, observable.getContext().locate(observable.getURI()));
        assertSame(observable, observable.getRoot().locate(observable.getFullURI()));
        try {
            unobservable.getFullURI();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
    }

}

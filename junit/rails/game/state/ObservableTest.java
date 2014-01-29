package rails.game.state;

import static org.junit.Assert.*;
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
    private Observable observable;
    @Mock private Observer observer;
    @Mock private Model model;
    
    @Before
    public void setUp() {
        root = Root.create(new ChangeReporterImpl());
        manager = new ManagerImpl(root, MANAGER_ID);
        item = new AbstractItemImpl(manager, ITEM_ID);
        observable = new ObservableImpl(item, OBS_ID);
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
        
    }

    @Test
    public void testModels() {
        // add observer and test if contained
        observable.addModel(model);
        assertThat(observable.getModels()).contains(model);
      
        // remove Model and test if not contained
        assertTrue(observable.removeModel(model));
        assertThat(observable.getModels()).doesNotContain(model);
        
        // remove Model not contained anymore
        assertFalse(observable.removeModel(model));
    }

    @Test
    public void testGetId() {
        assertEquals(OBS_ID, observable.getId());
    }

    @Test
    public void testGetParent() {
        assertSame(item, observable.getParent());
    }

    @Test
    public void testGetContext() {
        assertSame(manager, observable.getContext());
    }

    @Test
    public void testGetRoot() {
        assertSame(root, observable.getRoot());
    }

    @Test
    public void testGetURI() {
        assertEquals(ITEM_ID + Item.SEP + OBS_ID, observable.getURI());
        assertSame(observable, observable.getContext().locate(observable.getURI()));
    }

    @Test
    public void testGetFullURI() {
        assertEquals(Item.SEP + MANAGER_ID + Item.SEP + ITEM_ID + Item.SEP + OBS_ID, observable.getFullURI());
        assertSame(observable, observable.getContext().locate(observable.getURI()));
        assertSame(observable, observable.getRoot().locate(observable.getFullURI()));
    }

}

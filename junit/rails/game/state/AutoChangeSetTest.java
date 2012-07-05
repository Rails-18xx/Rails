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
public class AutoChangeSetTest {

    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    @Mock Model model;
    private ChangeStack changeStack;
    private AutoChangeSet changeSet;
    
    
    @Before
    public void setUp() {
        root = Root.create();
        state = BooleanState.create(root, STATE_ID);
        state.addModel(model);
        changeStack = root.getStateManager().getChangeStack();
        changeSet = changeStack.startAutoChangeSet();
    }

    @Test
    public void testAutoChangeSet() {
        assertNotNull(changeSet);
    }
    
    @Test
    public void testAddChange() {
        assertTrue(changeSet.isEmpty());
        state.set(true);
        assertFalse(changeSet.isEmpty());
        verify(model).update();
    }

    @Test
    public void testClose() {
        assertFalse(changeSet.isClosed());
        state.set(true);
        changeSet.close();
        assertTrue(changeSet.isClosed());
        assertThat(changeSet.getStates()).contains(state);
    }

    @Test
    public void testUnAndReexecute() {
        assertFalse(state.booleanValue());
        state.set(true);
        assertTrue(state.booleanValue());
        changeSet.close();
        changeSet.unexecute();
        assertFalse(state.booleanValue());
        changeSet.reexecute();
        assertTrue(state.booleanValue());
    }
}

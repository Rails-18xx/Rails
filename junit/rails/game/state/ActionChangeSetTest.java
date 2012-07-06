package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rails.game.Player;
import rails.game.action.PossibleAction;

@RunWith(MockitoJUnitRunner.class)
public class ActionChangeSetTest {

    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    @Mock Model model;
    private ChangeStack changeStack;
    @Mock Player player;
    @Mock PossibleAction action;
    private ActionChangeSet changeSet;
    
    
    @Before
    public void setUp() {
        root = Root.create();
        state = BooleanState.create(root, STATE_ID);
        state.addModel(model);
        changeStack = root.getStateManager().getChangeStack();
        changeSet = changeStack.startActionChangeSet(player, action);
    }
    
    @Test
    public void testActionChangeSet() {
        assertNotNull(changeSet);
    }

    @Test
    public void testGetPlayer() {
        assertSame(player, changeSet.getPlayer());
    }

    @Test
    public void testGetAction() {
        assertSame(action, changeSet.getAction());
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
        assertFalse(state.value());
        state.set(true);
        assertTrue(state.value());
        changeSet.close();
        changeSet.unexecute();
        assertFalse(state.value());
        changeSet.reexecute();
        assertTrue(state.value());
    }

}

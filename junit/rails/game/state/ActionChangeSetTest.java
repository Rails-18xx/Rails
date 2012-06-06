package rails.game.state;

import static org.junit.Assert.*;
import static org.junit.matchers.JUnitMatchers.*;
import static org.mockito.Mockito.*;

import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import rails.game.Player;
import rails.game.action.PossibleAction;

@RunWith(MockitoJUnitRunner.class)
public class ActionChangeSetTest {

    @Mock
    private Player player;
    
    @Mock
    private PossibleAction action;
    
    @Mock
    private Change change;
    
    @Mock
    private State state;
    
    private ActionChangeSet changeSet;
    
    private ActionChangeSet closedSet;
    
    @Before
    public void setup() {
        when(change.getState()).thenReturn(state);
        changeSet = new ActionChangeSet(player, action);
        
        closedSet = new ActionChangeSet(player, action);
        closedSet.close();
    }
    
    @Test
    public void testIsUndoableByPlayer() {
        assertTrue(changeSet.isUndoableByPlayer(player));
        Player otherPlayer = mock(Player.class);
        assertFalse(changeSet.isUndoableByPlayer(otherPlayer));
    }

    @Test
    public void testActionChangeSet() {
        assertNotNull(changeSet);
    }

    @Test
    public void testGetPlayer() {
        assertSame(changeSet.getPlayer(), player);
    }

    @Test
    public void testGetAction() {
        assertSame(changeSet.getAction(), action);
    }

    @Test
    public void testToString() {
        assertNotNull(changeSet.toString());
    }

    @Test
    public void testAddChange() {
        changeSet.addChange(change);
        verify(change).execute();
        verify(state).updateModels();
    }
    
    @Test(expected=IllegalStateException.class)
    public void testAddChangeClosed() {
        Change change = mock(Change.class);
        closedSet.addChange(change);
    }

    @Test
    public void testClose() {
        changeSet.close();
        assertTrue(changeSet.isClosed());
    }

    @Test
    public void testReexecute() {
        changeSet.addChange(change);
        changeSet.close();
        changeSet.reexecute();
        verify(change, times(2)).execute();
        verify(state, times(2)).updateModels();
    }

    @Test(expected=IllegalStateException.class)
    public void testReexecuteOpen() {
        changeSet.reexecute();
    }

    @Test
    public void testUnexecute() {
        changeSet.addChange(change);
        changeSet.close();
        changeSet.unexecute();
        verify(change).execute();
        verify(change).undo();
        verify(state, times(2)).updateModels();
    }

    @Test(expected=IllegalStateException.class)
    public void testUnexecuteOpen() {
        changeSet.unexecute();
    }
    
    @Test
    public void testIsEmpty() {
        assertTrue(changeSet.isEmpty());
        changeSet.addChange(change);
        assertFalse(changeSet.isEmpty());
    }

    @Test
    public void testGetStates() {
        changeSet.addChange(change);
        changeSet.close();
        assertThat(changeSet.getStates(), hasItem(state));
    }

}

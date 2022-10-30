package net.sf.rails.game.state;

import static org.mockito.Mockito.*;
import static org.fest.assertions.api.Assertions.assertThat;

import net.sf.rails.game.state.BooleanState;
import net.sf.rails.game.state.Observer;
import net.sf.rails.game.state.Root;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ObserverTest {
    
    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    @Mock private Observer observer;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state = new BooleanState(root, STATE_ID, false);
        state.addObserver(observer);
    }
    
    @Test
    public void testUpdate() {
        assertThat(state.getObservers()).contains(observer);
        state.set(true);
        verify(observer, never()).update(state.toText());
        StateTestUtils.close(root);
        verify(observer).update(state.toText());
    }

}

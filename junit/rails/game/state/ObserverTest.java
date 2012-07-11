package rails.game.state;

import static org.mockito.Mockito.*;
import static org.fest.assertions.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ObserverTest {
    
    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    @Mock private Observer observer;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state = BooleanState.create(root, STATE_ID, false);
        state.addObserver(observer);
    }
    
    @Test
    public void testUpdate() {
        assertThat(state.getObservers()).contains(observer);
        state.set(true);
        verify(observer, never()).update(state.observerText());
        root.getStateManager().getChangeStack().closeCurrentChangeSet();
        verify(observer).update(state.observerText());
    }

}

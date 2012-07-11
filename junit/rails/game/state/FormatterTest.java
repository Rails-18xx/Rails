package rails.game.state;

import static org.mockito.Mockito.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class FormatterTest {
    
    private static final String YES = "yes";
    private static final String NO = "no";

    // This formatter doubles the text of the state
    private class FormatterImpl extends Formatter<BooleanState> {
        private final BooleanState state;
        private FormatterImpl(BooleanState state){
            super(state);
            this.state = state;
        }
        public String observerText() {
            if (state.value()) {
                return YES;
            } else {
                return NO;
            }
        }
    }
    
    private final static String STATE_ID = "State";
    
    private Root root;
    private BooleanState state;
    private Formatter<BooleanState> formatter;
    @Mock private Observer observer;

    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        state = BooleanState.create(root, STATE_ID);
        formatter = new FormatterImpl(state);
        formatter.addObserver(observer);
    }
    
    @Test
    public void testFormatValue() {
        state.set(true);
        root.getStateManager().getChangeStack().closeCurrentChangeSet();
        state.set(false);
        root.getStateManager().getChangeStack().closeCurrentChangeSet();

        InOrder inOrder = inOrder(observer); 
        inOrder.verify(observer).update(YES);
        inOrder.verify(observer).update(NO);
    }
}

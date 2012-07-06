package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class FormatterTest {

    // This formatter doubles the text of the state
    private class FormatterImpl implements Formatter<State> {
        public String formatValue(State state) {
            return state.toString() + state.toString();
        }
    }
    
    private final static String STATE_ID = "State";
    private final static String STATE_TEXT = "Test";
    
    private Root root;
    private State state;
    private Formatter<State> formatter;
    
    @Before
    public void setUp() {
        root = Root.create();
        state = new StateImpl(root, STATE_ID, STATE_TEXT);
        formatter = new FormatterImpl();
    }
    
    @Test
    public void testFormatValue() {
        assertEquals(STATE_TEXT, state.getText());
        state.setFormatter(formatter);
        assertEquals(STATE_TEXT + STATE_TEXT, state.getText());
        state.setFormatter(null);
        assertEquals(STATE_TEXT, state.getText());
    }

}

package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class StateTest {

    private final static String STATE_ID = "State";
    private final static String MODEL_ID = "Model";
    private final static String STATE_TEXT = "Test";
    
    private Root root;
    private State state;
    private State state_model;
    private State state_wo_id;
    private Model model;
    
    @Before
    public void setUp() {
        root = Root.create();
        state = new StateImpl(root, STATE_ID, null);
        model = new ModelImpl(root, MODEL_ID, null);
        state_model = new StateImpl(model, STATE_ID, null);
        state_wo_id = new StateImpl(model, null, STATE_TEXT);
    }
    
    @Test
    public void testState() {
        // check that model is linked by state_model
        assertThat(state_model.getModels()).contains(model);
        // but for the standard model only after explicit link
        assertThat(state.getModels()).doesNotContain(model);
        state.addModel(model);
        assertThat(state.getModels()).contains(model);
        
        // state_wo_id does not have any link
        try {
            state_wo_id.getModels();
        } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
        }
        
    }
    
    @Test
    public void testObserverText() {
        assertNull(state.observerText());
        assertEquals(STATE_TEXT, state_wo_id.observerText());
    }

}

package rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class StateTest {

    private final static String STATE_ID = "State";
    private final static String MODEL_ID = "Model";
    
    private Root root;
    private State state;
    private State state_model;
    private Model model;
    
    @Before
    public void setUp() {
        root = Root.create(new ChangeReporterImpl());
        state = StateImpl.create(root, STATE_ID, null);
        model = ModelImpl.create(root, MODEL_ID, null);
        state_model = StateImpl.create(model, STATE_ID, null);
    }
    
    @Test
    public void testState() {
        // check that model is linked by state_model
        assertThat(state_model.getModels()).contains(model);
        // but for the standard model only after explicit link
        assertThat(state.getModels()).doesNotContain(model);
        state.addModel(model);
        assertThat(state.getModels()).contains(model);
    }
    
    @Test
    public void testObserverText() {
        assertNull(state.toText());
    }

}

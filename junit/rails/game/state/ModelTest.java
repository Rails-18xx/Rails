package rails.game.state;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ModelTest {

    private static final String MODEL_ID = "Model";
    private static final String MODEL_TEXT_INIT = "Init";
    private static final String MODEL_TEXT_CHANGE = "Change";
    
    private Root root;
    private ModelImpl model;
    @Mock private Observer observer;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        model = ModelImpl.create(root, MODEL_ID, MODEL_TEXT_INIT);
        StateTestUtils.close(root);
        model.addObserver(observer);
    }
    
    @Test
    public void testModel() {
        assertEquals(MODEL_TEXT_INIT, model.toText());
        model.changeText(MODEL_TEXT_CHANGE);
        StateTestUtils.close(root);
        assertEquals(MODEL_TEXT_CHANGE, model.toText());
        verify(observer).update(MODEL_TEXT_CHANGE);
    }

}

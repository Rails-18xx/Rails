package rails.game.state;

import static org.junit.Assert.*;

import org.junit.Test;
import org.mockito.Mock;

public class ChangeStackTest {

    @Mock
    private StateManager sm;
    
    @Test
    public void testCreate() {
        ChangeStack stack = ChangeStack.create(sm);
        assertNotNull(stack);
    }


    @Test
    public void testIsEnabled() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAvailableChangeSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetAvailableChangeSetBoolean() {
        fail("Not yet implemented");
    }

    @Test
    public void testStart() {
        fail("Not yet implemented");
    }

    @Test
    public void testFinish() {
        fail("Not yet implemented");
    }

    @Test
    public void testCancel() {
        fail("Not yet implemented");
    }

    @Test
    public void testAddChange() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsUndoableByPlayer() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsUndoableByManager() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsOpen() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetIndex() {
        fail("Not yet implemented");
    }

    @Test
    public void testSize() {
        fail("Not yet implemented");
    }

    @Test
    public void testGetCurrentIndex() {
        fail("Not yet implemented");
    }

    @Test
    public void testGotoIndex() {
        fail("Not yet implemented");
    }

    @Test
    public void testIsRedoable() {
        fail("Not yet implemented");
    }

    @Test
    public void testRedoMoveSet() {
        fail("Not yet implemented");
    }

    @Test
    public void testUndo() {
        fail("Not yet implemented");
    }

    @Test
    public void testAdd() {
        fail("Not yet implemented");
    }

}

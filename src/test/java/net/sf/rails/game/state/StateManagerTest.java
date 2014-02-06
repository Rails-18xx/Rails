package net.sf.rails.game.state;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.fest.assertions.api.Assertions.failBecauseExceptionWasNotThrown;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Set;

import net.sf.rails.game.state.Model;
import net.sf.rails.game.state.Observer;
import net.sf.rails.game.state.Root;
import net.sf.rails.game.state.State;
import net.sf.rails.game.state.StateManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

@RunWith(MockitoJUnitRunner.class)
public class StateManagerTest {

    private final static List<String> ID = 
            ImmutableList.of("A1" , "A2" , "A3", "B1", "B2", "C1", "C2", "C3", "D", "E", "F");
    
    private Root root;
    private StateManager sm;
    
    @Mock private State state;
    @Mock private Observer observer;
    @Mock private Model model; 
    private ModelImpl m_A1, m_A2, m_A3, m_B1, m_B2, m_C1, m_C2, m_C3, m_D, m_E, m_F;
    @Mock private Observer o_A1, o_A2, o_A3, o_B1, o_B2, o_C1, o_C2, o_C3;
    
    @Before
    public void setUp() {
        root = StateTestUtils.setUpRoot();
        sm = root.getStateManager();
        
        // initialize the models
        m_A1 = ModelImpl.create(root, ID.get(0), ID.get(0));
        m_A2 = ModelImpl.create(root, ID.get(1), ID.get(1));
        m_A3 = ModelImpl.create(root, ID.get(2), ID.get(2));
        m_B1 = ModelImpl.create(root, ID.get(3), ID.get(3));
        m_B2 = ModelImpl.create(root, ID.get(4), ID.get(4));
        m_C1 = ModelImpl.create(root, ID.get(5), ID.get(5));
        m_C2 = ModelImpl.create(root, ID.get(6), ID.get(6));
        m_C3 = ModelImpl.create(root, ID.get(7), ID.get(7));
        m_D = ModelImpl.create(root, ID.get(8), ID.get(8));
        m_E = ModelImpl.create(root, ID.get(9), ID.get(9));
        m_F = ModelImpl.create(root, ID.get(10), ID.get(10));
        
        // define connections
        m_A1.addModel(m_B1);
        m_A1.addModel(m_B2);
        m_A1.addModel(m_C2);
        m_A2.addModel(m_B2);
        m_A3.addModel(m_C3);
        m_B1.addModel(m_C1);
        m_B2.addModel(m_C2);
        
        // D is special that it depends on the state in A3 directly
        m_A3.getState().addModel(m_D);
        m_D.addModel(m_A3);
        
        // E and F form a cycle
        m_E.addModel(m_F);
        m_F.addModel(m_E);
        
        // observers
        m_A1.addObserver(o_A1);
        m_A2.addObserver(o_A2);
        m_A3.addObserver(o_A3);
        m_B1.addObserver(o_B1);
        m_B2.addObserver(o_B2);
        m_C1.addObserver(o_C1);
        m_C2.addObserver(o_C2);
        m_C3.addObserver(o_C3);
  }

    @Test
    public void testRegisterState() {
        sm.registerState(state);
        assertThat(sm.getAllStates()).contains(state);
//        sm.deRegisterState(state);
//        assertThat(sm.getAllStates()).doesNotContain(state);
    }

    @Test
    public void testAddObserver() {
        sm.addObserver(observer, state);
        assertThat(sm.getObservers(state)).contains(observer);
        sm.removeObserver(observer, state);
        assertThat(sm.getObservers(state)).doesNotContain(observer);
    }

    @Test
    public void testAddModel() {
        sm.addModel(model, state);
        assertThat(sm.getModels(state)).contains(model);
        sm.removeModel(model, state);
        assertThat(sm.getModels(state)).doesNotContain(model);
    }
    
    private void checkOrderings(List<Model> updates) {
        for (Model m:updates) {
            for (Model dep_m: m.getModels()) {
                assertThat(updates.indexOf(dep_m)).isGreaterThan(updates.indexOf(m));
            }
        }
    }
    

    private void assertObservables(List<? extends Model> expected, Set<ModelImpl> updated) {
        // get all embedded states that are included
        Set<State> states = Sets.newHashSet();
        for (ModelImpl m:updated) {
            states.add(m.getState());
        }
        // get all observables that are updated
        List<Model> toUpdate = sm.getModelsToUpdate(states);
        // check that all non-states observables are the updated models and the expected models
        assertThat(toUpdate).containsAll(expected);
        // ... and does not have duplicates
        assertThat(toUpdate).doesNotHaveDuplicates();
        // ... and has the same size
        assertEquals(expected.size(), toUpdate.size());
        // and check ordering
        checkOrderings(toUpdate);
   }
    
    @Test
    public void testObservablesToUpdate() {
        // nothing <= nothing
        assertObservables(ImmutableList.<Model>of(),ImmutableSet.<ModelImpl>of());
        // A1, B1, B2, C1, C2 <= A1
        assertObservables(ImmutableList.of(m_A1, m_B1, m_B2, m_C1, m_C2),ImmutableSet.of(m_A1));
        // A2, B2, C2 <= A2
        assertObservables(ImmutableList.of(m_A2, m_B2, m_C2),ImmutableSet.of(m_A2));
        // A3, C3 <= A3
        assertObservables(ImmutableList.of(m_A3, m_C3, m_D),ImmutableSet.of(m_A3));
        // B1, C1 <= B1
        assertObservables(ImmutableList.of(m_B1, m_C1),ImmutableSet.of(m_B1));
        // B2, C2 <= B2
        assertObservables(ImmutableList.of(m_B2, m_C2),ImmutableSet.of(m_B2));
        // C1, C2 <= C1, C2
        assertObservables(ImmutableList.of(m_C1, m_C2),ImmutableSet.of(m_C1, m_C2));
        // Combinations:
        // A1, A2, B1, B2, C1, C2 <= A1, A2
        assertObservables(ImmutableList.of(m_A1, m_A2, m_B1, m_B2, m_C1, m_C2),ImmutableSet.of(m_A1, m_A2));
        // A1, A2, A3, B1, B2, C1, C2, C3 <= A1, A2, A3
        assertObservables(ImmutableList.of(m_A1, m_A2, m_A3, m_B1, m_B2, m_C1, m_C2, m_C3, m_D),ImmutableSet.of(m_A1, m_A2, m_A3));
        // A2, B1, B2, C1, C2 <= A2, B1
        assertObservables(ImmutableList.of(m_A2, m_B1, m_B2, m_C1, m_C2),ImmutableSet.of(m_A2, m_B1));
        
        try{
            assertObservables(ImmutableList.<Model>of(), ImmutableSet.of(m_E));
            failBecauseExceptionWasNotThrown(IllegalStateException.class);
          } catch (Exception e) {
            assertThat(e).isInstanceOf(IllegalStateException.class);
          }
    }

    @Test
    public void testUpdateObservers() {
        sm.updateObservers(ImmutableSet.of(m_A1.getState()));
        verify(o_A1).update(ID.get(0));
        verify(o_B1).update(ID.get(3));
        verify(o_B2).update(ID.get(4));
        verify(o_C1).update(ID.get(5));
        verify(o_C2).update(ID.get(6));
        verifyZeroInteractions(o_A2, o_A3, o_C3);
    }

    @Test
    public void testGetChangeStack() {
        assertNotNull(sm.getChangeStack());
    }

    @Test
    public void testGetPortfolioManager() {
        assertNotNull(sm.getPortfolioManager());
    }

}

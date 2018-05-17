using GameLib.Net.Game.State;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    [TestClass]
    public class StateManagerTest
    {
        private static List<string> ID = new List<string>()
            { "A1", "A2", "A3", "B1", "B2", "C1", "C2", "C3", "D", "E", "F" };

        private Root root;
        private StateManager sm;

        private GameState state;
        private IObserver observer = Mock.Of<IObserver>();
        private Model model;
        private ModelImpl m_A1, m_A2, m_A3, m_B1, m_B2, m_C1, m_C2, m_C3, m_D, m_E, m_F;
        private IObserver o_A1 = Mock.Of<IObserver>();
        private IObserver o_A2 = Mock.Of<IObserver>();
        private IObserver o_A3 = Mock.Of<IObserver>();
        private IObserver o_B1 = Mock.Of<IObserver>();
        private IObserver o_B2 = Mock.Of<IObserver>();
        private IObserver o_C1 = Mock.Of<IObserver>();
        private IObserver o_C2 = Mock.Of<IObserver>();
        private IObserver o_C3 = Mock.Of<IObserver>();

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            sm = root.StateManager;
            state = new Mock<GameState>(new object[] { root, "gamestatemock" }).Object;
            model = new Mock<Model>(new object[] { root, "modelmock" }).Object;

            // initialize the models
            m_A1 = ModelImpl.Create(root, ID[0], ID[0]);
            m_A2 = ModelImpl.Create(root, ID[1], ID[1]);
            m_A3 = ModelImpl.Create(root, ID[2], ID[2]);
            m_B1 = ModelImpl.Create(root, ID[3], ID[3]);
            m_B2 = ModelImpl.Create(root, ID[4], ID[4]);
            m_C1 = ModelImpl.Create(root, ID[5], ID[5]);
            m_C2 = ModelImpl.Create(root, ID[6], ID[6]);
            m_C3 = ModelImpl.Create(root, ID[7], ID[7]);
            m_D = ModelImpl.Create(root, ID[8], ID[8]);
            m_E = ModelImpl.Create(root, ID[9], ID[9]);
            m_F = ModelImpl.Create(root, ID[10], ID[10]);

            // define connections
            m_A1.AddModel(m_B1);
            m_A1.AddModel(m_B2);
            m_A1.AddModel(m_C2);
            m_A2.AddModel(m_B2);
            m_A3.AddModel(m_C3);
            m_B1.AddModel(m_C1);
            m_B2.AddModel(m_C2);

            // D is special that it depends on the state in A3 directly
            m_A3.GameState.AddModel(m_D);
            m_D.AddModel(m_A3);

            // E and F form a cycle
            m_E.AddModel(m_F);
            m_F.AddModel(m_E);

            // observers
            m_A1.AddObserver(o_A1);
            m_A2.AddObserver(o_A2);
            m_A3.AddObserver(o_A3);
            m_B1.AddObserver(o_B1);
            m_B2.AddObserver(o_B2);
            m_C1.AddObserver(o_C1);
            m_C2.AddObserver(o_C2);
            m_C3.AddObserver(o_C3);
        }

        [TestMethod]
        public void TestRegisterState()
        {
            sm.RegisterState(state);
            Assert.IsTrue(sm.GetAllStates().Contains(state));
        }

        [TestMethod]
        public void TestAddObserver()
        {
            sm.AddObserver(observer, state);
            Assert.IsTrue(sm.GetObservers(state).Contains(observer));
            sm.RemoveObserver(observer, state);
            Assert.IsTrue(!sm.GetObservers(state).Contains(observer));
        }

        [TestMethod]
        public void TestAddModel()
        {
            sm.AddModel(model, state);
            Assert.IsTrue(sm.GetModels(state).Contains(model));
            sm.RemoveModel(model, state);
            Assert.IsTrue(!sm.GetModels(state).Contains(model));
        }

        private void CheckOrderings(List<Model> updates)
        {
            foreach (Model m in updates)
            {
                foreach (Model dep_m in m.GetModels())
                {
                    Assert.IsTrue(updates.IndexOf(dep_m) > updates.IndexOf(m));
                }
            }
        }


        private void AssertObservables<T>(List<T> expected, ISet<ModelImpl> updated) where T : Model
        {
            // get all embedded states that are included
            HashSet<GameState> states = new HashSet<GameState>();
            foreach (ModelImpl m in updated)
            {
                states.Add(m.GameState);
            }
            // get all observables that are updated
            var toUpdate = new List<Model>(sm.GetModelsToUpdate(states));
            // check that all non-states observables are the updated models and the expected models
            Assert.IsTrue(StateTestUtils.ContainsAllItems(toUpdate, expected));
            // ... and does not have duplicates
            //assertThat(toUpdate).doesNotHaveDuplicates();
            CollectionAssert.AllItemsAreUnique(toUpdate);
            // ... and has the same size
            Assert.AreEqual(expected.Count, toUpdate.Count);
            // and check ordering
            CheckOrderings(toUpdate);
        }

        [TestMethod]
        public void TestObservablesToUpdate()
        {
            // nothing <= nothing
            AssertObservables(new List<Model>(), new HashSet<ModelImpl>());
            // A1, B1, B2, C1, C2 <= A1
            AssertObservables(new List<Model>() { m_A1, m_B1, m_B2, m_C1, m_C2 }, new HashSet<ModelImpl>() { m_A1 });
            // A2, B2, C2 <= A2
            AssertObservables(new List<Model>() { m_A2, m_B2, m_C2 }, new HashSet<ModelImpl>() { m_A2 });
            // A3, C3 <= A3
            AssertObservables(new List<Model>() { m_A3, m_C3, m_D }, new HashSet<ModelImpl>() { m_A3 });
            // B1, C1 <= B1
            AssertObservables(new List<Model>() { m_B1, m_C1 }, new HashSet<ModelImpl>() { m_B1 });
            // B2, C2 <= B2
            AssertObservables(new List<Model>() { m_B2, m_C2 }, new HashSet<ModelImpl>() { m_B2 });
            // C1, C2 <= C1, C2
            AssertObservables(new List<Model>() { m_C1, m_C2 }, new HashSet<ModelImpl>() { m_C1, m_C2 });
            // Combinations:
            // A1, A2, B1, B2, C1, C2 <= A1, A2
            AssertObservables(new List<Model>() { m_A1, m_A2, m_B1, m_B2, m_C1, m_C2 }, new HashSet<ModelImpl>() { m_A1, m_A2 });
            // A1, A2, A3, B1, B2, C1, C2, C3 <= A1, A2, A3
            AssertObservables(new List<Model>() { m_A1, m_A2, m_A3, m_B1, m_B2, m_C1, m_C2, m_C3, m_D }, new HashSet<ModelImpl>() { m_A1, m_A2, m_A3 });
            // A2, B1, B2, C1, C2 <= A2, B1
            AssertObservables(new List<Model>() { m_A2, m_B1, m_B2, m_C1, m_C2 }, new HashSet<ModelImpl>() { m_A2, m_B1 });

            Assert.ThrowsException<InvalidOperationException>(() => AssertObservables(new List<Model>(), new HashSet<ModelImpl>() { m_E }));
            //  try
            //  {
            //      assertObservables(ImmutableList.< Model > of(), ImmutableSet.of(m_E));
            //      failBecauseExceptionWasNotThrown(IllegalStateException.class);
            //} catch (Exception e) {
            //  assertThat(e).isInstanceOf(IllegalStateException.class);
            //}
        }

        [TestMethod]
        public void TestUpdateObservers()
        {
            sm.UpdateObservers(new HashSet<GameState>() { m_A1.GameState });

            //verify(o_A1).update(ID.get(0));
            Mock.Get(o_A1).Verify(o_A1 => o_A1.Update(ID[0]), Times.Once());
            //verify(o_B1).update(ID.get(3));
            Mock.Get(o_B1).Verify(o_B1 => o_B1.Update(ID[3]), Times.Once());
            //verify(o_B2).update(ID.get(4));
            Mock.Get(o_B2).Verify(o_B2 => o_B2.Update(ID[4]), Times.Once());
            //verify(o_C1).update(ID.get(5));
            Mock.Get(o_C1).Verify(o_C1 => o_C1.Update(ID[5]), Times.Once());
            //verify(o_C2).update(ID.get(6));
            Mock.Get(o_C2).Verify(o_C2 => o_C2.Update(ID[6]), Times.Once());

            Mock.Get(o_A2).VerifyNoOtherCalls();
            Mock.Get(o_A3).VerifyNoOtherCalls();
            Mock.Get(o_C3).VerifyNoOtherCalls();
            //verifyZeroInteractions(o_A2, o_A3, o_C3);
        }

        [TestMethod]
        public void TestGetChangeStack()
        {
            Assert.IsNotNull(sm.ChangeStack);
        }

        [TestMethod]
        public void TestGetPortfolioManager()
        {
            Assert.IsNotNull(sm.PortfolioManager);
        }

    }
}

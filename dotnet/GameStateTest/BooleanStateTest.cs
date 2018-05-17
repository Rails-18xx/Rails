using GameLib.Net.Game.State;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    [TestClass]
    public class BooleanStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";

        private Root root;

        private BooleanState stateDefault;
        private BooleanState stateInit;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            stateDefault = BooleanState.Create(root, DEFAULT_ID);
            stateInit = BooleanState.Create(root, INIT_ID, true);

        }

        [TestMethod]
        public void TestValue()
        {
            Assert.IsFalse(stateDefault.Value);
            Assert.IsTrue(stateInit.Value);
        }

        [TestMethod]
        public void TestSet()
        {
            stateDefault.Set(true);
            Assert.IsTrue(stateDefault.Value);
            stateInit.Set(false);
            Assert.IsFalse(stateInit.Value);
        }

        [TestMethod]
        public void TestSetSameIgnored()
        {
            stateDefault.Set(false);
            stateInit.Set(true);
            StateTestUtils.Close(root);
            var a = StateTestUtils.GetPreviousChangeSet(root).GetStates();
            Assert.IsTrue(!(a.Contains(stateDefault) || a.Contains(stateInit)));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault, stateInit);
        }

        [TestMethod]
        public void TestUndoRedo()
        {
            Assert.IsFalse(stateDefault.Value);
            stateDefault.Set(true);
            Assert.IsTrue(stateDefault.Value);
            StateTestUtils.Close(root);
            Assert.IsTrue(StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateDefault));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).contains(stateDefault);
            StateTestUtils.Undo(root);
            Assert.IsFalse(stateDefault.Value);
            StateTestUtils.Redo(root);
            Assert.IsTrue(stateDefault.Value);
        }
    }
}

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
    public class StringStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";
        private const string INIT = "INIT";
        private const string OTHER = "OTHER";

        private Root root;

        private StringState stateDefault;
        private StringState stateInit;


        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();

            stateDefault = StringState.Create(root, DEFAULT_ID);
            stateInit = StringState.Create(root, INIT_ID, INIT);
        }

        [TestMethod]
        public void TestValue()
        {
            Assert.AreEqual(stateDefault.Value, null);
            Assert.AreEqual(stateInit.Value, INIT);
        }

        [TestMethod]
        public void TestSet()
        {
            stateDefault.Set(OTHER);
            Assert.AreEqual(stateDefault.Value, OTHER);
            stateInit.Set("");
            Assert.AreEqual(stateInit.Value, "");
            stateInit.Set(null);
            Assert.AreEqual(stateInit.Value, null);
        }

        [TestMethod]
        public void TestAppend()
        {
            stateDefault.Append(OTHER, null);
            Assert.AreEqual(stateDefault.Value, OTHER);
            stateDefault.Append(OTHER, "");
            Assert.AreEqual(stateDefault.Value, OTHER + OTHER);

            stateInit.Append(OTHER, ",");
            Assert.AreEqual(stateInit.Value, INIT + "," + OTHER);
        }


        [TestMethod]
        public void TestSetSameIgnored()
        {
            stateDefault.Set(null);
            stateInit.Set(null);
            StateTestUtils.Close(root);
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault);
            Assert.IsTrue(!StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateDefault));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).contains(stateInit);
            Assert.IsTrue(StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateInit));

            StateTestUtils.Close(root);
            stateDefault.Set("");
            stateInit.Set(null);
            StateTestUtils.Close(root);
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).contains(stateDefault);
            Assert.IsTrue(StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateDefault));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateInit);
            Assert.IsTrue(!StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateInit));
        }

        [TestMethod]
        public void TestUndoRedo()
        {

            stateDefault.Set(OTHER);
            stateDefault.Append(OTHER, null);

            stateInit.Append(OTHER, "");
            stateInit.Set(null);
            StateTestUtils.Close(root);

            Assert.AreEqual(stateDefault.Value, OTHER + OTHER);
            Assert.IsNull(stateInit.Value);

            StateTestUtils.Undo(root);
            Assert.AreEqual(stateDefault.Value, null);
            Assert.AreEqual(stateInit.Value, INIT);

            StateTestUtils.Redo(root);
            Assert.AreEqual(stateDefault.Value, OTHER + OTHER);
            Assert.IsNull(stateInit.Value);
        }

    }
}

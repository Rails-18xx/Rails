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
    public class IntegerStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";
        private const int INIT = 10;
        private const int OTHER = -5;

        private Root root;

        private IntegerState stateDefault;
        private IntegerState stateInit;


        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();


            stateDefault = IntegerState.Create(root, DEFAULT_ID);
            stateInit = IntegerState.Create(root, INIT_ID, INIT);
        }

        [TestMethod]
        public void TestValue()
        {
            Assert.AreEqual(stateDefault.Value, 0);
            Assert.AreEqual(stateInit.Value, INIT);
        }

        [TestMethod]
        public void TestSet()
        {
            stateDefault.Set(OTHER);
            Assert.AreEqual(stateDefault.Value, OTHER);
            stateInit.Set(0);
            Assert.AreEqual(stateInit.Value, 0);
        }

        [TestMethod]
        public void TestAdd()
        {
            stateDefault.Add(OTHER);
            Assert.AreEqual(stateDefault.Value, OTHER);
            stateInit.Add(OTHER);
            Assert.AreEqual(stateInit.Value, INIT + OTHER);
        }


        [TestMethod]
        public void TestSetSameIgnored()
        {
            stateDefault.Set(0);
            stateInit.Set((INIT));
            StateTestUtils.Close(root);
            var a = StateTestUtils.GetPreviousChangeSet(root).GetStates();
            Assert.IsTrue(!(a.Contains(stateDefault) || a.Contains(stateInit)));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault, stateInit);
        }

        [TestMethod]
        public void TestUndoRedo()
        {

            stateDefault.Set(INIT);
            stateDefault.Add(OTHER);

            stateInit.Add(OTHER);
            stateInit.Set(0);
            StateTestUtils.Close(root);

            Assert.AreEqual(stateDefault.Value, INIT + OTHER);
            Assert.AreEqual(stateInit.Value, 0);

            StateTestUtils.Undo(root);
            Assert.AreEqual(stateDefault.Value, 0);
            Assert.AreEqual(stateInit.Value, INIT);

            StateTestUtils.Redo(root);
            Assert.AreEqual(stateDefault.Value, INIT + OTHER);
            Assert.AreEqual(stateInit.Value, 0);
        }
    }
}

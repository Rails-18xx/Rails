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
    public class GenericStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";
        private const string ITEM_ID = "Item";
        private const string ANOTHER_ID = "Another";

        private Root root;

        private GenericState<IItem> stateDefault;
        private GenericState<IItem> stateInit;

        private IItem item, another_item;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();


            item = new AbstractItemImpl(root, ITEM_ID);
            another_item = new AbstractItemImpl(root, ANOTHER_ID);

            stateDefault = GenericState<IItem>.Create(root, DEFAULT_ID);
            stateInit = GenericState<IItem>.Create(root, INIT_ID, item);
        }

        [TestMethod]
        public void TestValue()
        {
            Assert.IsNull(stateDefault.Value);
            Assert.AreSame(item, stateInit.Value);
        }

        [TestMethod]
        public void TestSet()
        {
            stateDefault.Set(item);
            Assert.AreSame(item, stateDefault.Value);
            stateDefault.Set(null);
            Assert.IsNull(stateDefault.Value);
            stateInit.Set(another_item);
            Assert.AreSame(another_item, stateInit.Value);
        }

        [TestMethod]
        public void TestSetSameIgnored()
        {
            stateDefault.Set(null);
            stateInit.Set(item);
            StateTestUtils.Close(root);
            var a = StateTestUtils.GetPreviousChangeSet(root).GetStates();
            Assert.IsTrue(!(a.Contains(stateDefault) || a.Contains(stateInit)));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).doesNotContain(stateDefault, stateInit);
        }

        [TestMethod]
        public void TestUndoRedo()
        {
            Assert.IsNull(stateDefault.Value);
            Assert.AreSame(item, stateInit.Value);

            stateDefault.Set(item);
            stateInit.Set(another_item);
            Assert.AreSame(item, stateDefault.Value);
            Assert.AreSame(another_item, stateInit.Value);

            StateTestUtils.Close(root);
            // remark: stateInit is an internal (isObservable = false)
            Assert.IsTrue(StateTestUtils.GetPreviousChangeSet(root).GetStates().Contains(stateDefault));
            //assertThat(StateTestUtils.getPreviousChangeSet(root).getStates()).contains(stateDefault);

            StateTestUtils.Undo(root);
            Assert.IsNull(stateDefault.Value);
            Assert.AreSame(item, stateInit.Value);

            StateTestUtils.Redo(root);
            Assert.AreSame(item, stateDefault.Value);
            Assert.AreSame(another_item, stateInit.Value);
        }

    }
}

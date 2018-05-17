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
    public class ChangeStackTest
    {
        private const string STATE_ID = "State";

        private Root root;
        private BooleanState state;
        private ChangeStack changeStack;
        private IChangeAction changeAction;

        private ChangeSet set_1, set_2, set_3;

        [TestInitialize]
        public void SetUp()
        {
            root = Root.Create();
            changeStack = root.StateManager.ChangeStack;
            changeAction = new ChangeActionImpl();

            // initial changeset
            state = BooleanState.Create(root, STATE_ID, true);

            // next changeset
            StateTestUtils.Close(root);
            set_1 = changeStack.GetClosedChangeSet();
            state.Set(false);

            // next changeset
            StateTestUtils.Close(root);
            set_2 = changeStack.GetClosedChangeSet();
            state.Set(true);
            StateTestUtils.Close(root);
            set_3 = changeStack.GetClosedChangeSet();
        }

        [TestMethod]
        public void TestGetCurrentChangeSet()
        {
            Assert.AreSame(set_3, changeStack.GetClosedChangeSet());
            // on the stack are set2, set1 (thus index 2)
            Assert.AreEqual(3, changeStack.CurrentIndex);
        }

        [TestMethod]
        public void TestcloseAndNew()
        {
            changeStack.Close(changeAction);
            Assert.AreSame(set_3, changeStack.GetClosedChangeSet());
            // number on stack has not changed
            Assert.AreEqual(3, changeStack.CurrentIndex);
        }

        private void TestUndoAfterClose()
        {
            // check current state
            Assert.IsTrue(state.Value);
            // undo set 3
            changeStack.Undo();
            Assert.AreEqual(2, changeStack.CurrentIndex);
            Assert.AreSame(set_2, changeStack.GetClosedChangeSet());
            Assert.IsFalse(state.Value);
            // undo set 2
            changeStack.Undo();
            Assert.AreEqual(1, changeStack.CurrentIndex);
            Assert.AreSame(set_1, changeStack.GetClosedChangeSet());
            Assert.IsTrue(state.Value);
            // undo set 1 => should fail
            Assert.ThrowsException<InvalidOperationException>(() => changeStack.Undo());

            //    try
            //    {
            //        changeStack.Undo();
            //        failBecauseExceptionWasNotThrown(IllegalStateException.class);
            //} catch (Exception e){
            //    assertThat(e).isInstanceOf(IllegalStateException.class);
            //}
            Assert.AreEqual(1, changeStack.CurrentIndex);
            Assert.AreSame(set_1, changeStack.GetClosedChangeSet());
            Assert.IsTrue(state.Value);
        }

        [TestMethod]
        public void TestUndo()
        {
            changeStack.Close(changeAction);
            TestUndoAfterClose();
        }

        [TestMethod]
        public void TestRedo()
        {
            // undo everything
            changeStack.Close(changeAction);
            changeStack.Undo();
            changeStack.Undo();
            // the state until now was checked in testUndo

            // redo set_2
            changeStack.Redo();
            Assert.AreEqual(2, changeStack.CurrentIndex);
            Assert.AreSame(set_2, changeStack.GetClosedChangeSet());
            Assert.IsFalse(state.Value);

            // redo set_3
            changeStack.Redo();
            Assert.AreEqual(3, changeStack.CurrentIndex);
            Assert.AreSame(set_3, changeStack.GetClosedChangeSet());
            Assert.IsTrue(state.Value);

            Assert.ThrowsException<InvalidOperationException>(() => changeStack.Redo());
            // then it should do nothing
            //        try
            //{
            //    changeStack.Redo();
            //    failBecauseExceptionWasNotThrown(IllegalStateException.class);
            //    } catch (Exception e){
            //        assertThat(e).isInstanceOf(IllegalStateException.class);
            //    }
            Assert.AreEqual(3, changeStack.CurrentIndex);
            Assert.AreSame(set_3, changeStack.GetClosedChangeSet());
            Assert.IsTrue(state.Value);

            // can we still undo?
            TestUndoAfterClose();
        }

    }
}

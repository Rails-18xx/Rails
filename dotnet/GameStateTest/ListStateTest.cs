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
    public class ListStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";

        private const string ONE_ITEM_ID = "OneItem";
        private const string ANOTHER_ITEM_ID = "AnotherItem";

        private Root root;

        private ListState<IItem> stateDefault;
        private ListState<IItem> stateInit;

        private List<IItem> twoItemList;
        private List<IItem> oneItemList;
        private List<IItem> noItemList;

        private IItem oneItem;
        private IItem anotherItem;


        [TestInitialize]
        public void Initialize()
        {
            root = StateTestUtils.SetUpRoot();

            oneItem = AbstractItemImpl.Create(root, ONE_ITEM_ID);
            anotherItem = AbstractItemImpl.Create(root, ANOTHER_ITEM_ID);

            stateDefault = ListState<IItem>.Create(root, DEFAULT_ID);
            stateInit = ListState<IItem>.Create(root, INIT_ID, new List<IItem>() { oneItem });

            twoItemList = new List<IItem>() { oneItem, anotherItem };
            oneItemList = new List<IItem>() { oneItem };
            noItemList = new List<IItem>();
        }

        private List<IItem> GetViewList(ListState<IItem> state)
        {
            return new List<IItem>(state.View());
        }

        // helper function to check the initial state after undo
        // includes redo, so after returning the state should be unchanged
        private void AssertInitialStateAfterUndo()
        {
            StateTestUtils.CloseAndUndo(root);
            CollectionAssert.AreEqual(GetViewList(stateDefault), noItemList);
            //Assert.AreEqual(stateDefault.View(), new List<IItem>());
            CollectionAssert.AreEqual(GetViewList(stateInit), oneItemList);
            //Assert.AreEqual(stateInit.view(), Lists.newArrayList(oneItem));
            StateTestUtils.Redo(root);
        }

        private void AssertTestAdd()
        {
            // TODO: replace with containsExactly, this does not work yet
            Assert.IsTrue(stateDefault.Count == 1 && stateDefault.Contains(oneItem));
            //assertThat(stateDefault).containsOnly(oneItem);

            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, twoItemList));
            //assertThat(stateInit).containsSequence(oneItem, anotherItem);
        }

        [TestMethod]
        public void TestAdd()
        {
            stateDefault.Add(oneItem);
            stateInit.Add(anotherItem);
            AssertTestAdd();

            // check undo
            AssertInitialStateAfterUndo();
            AssertTestAdd();
        }

        [TestMethod]
        public void TestAddIndex()
        {
            stateInit.Add(0, anotherItem);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem);

            stateInit.Add(2, anotherItem);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem, anotherItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem, anotherItem);

            stateInit.Add(1, oneItem);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem, oneItem, anotherItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem, oneItem, anotherItem);

            // Check undo
            AssertInitialStateAfterUndo();
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem, oneItem, anotherItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem, oneItem, anotherItem);
        }

        [TestMethod]
        [ExpectedException(typeof(IndexOutOfRangeException))]
        public void TestAddIndexFailPastEnd()
        {
            // open new ChangeSet to test if it is still empty
            StateTestUtils.Close(root);
            stateInit.Add(2, anotherItem);
            //    try
            //    {
            //        stateInit.Add(2, anotherItem);
            //        failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
            //} catch (Exception e) {
            //    assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
            //}
            //try {
            //    stateInit.add(-1, anotherItem);
            //    failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
            //} catch (Exception e) {
            //    assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
            //}
        }

        [TestMethod]
        [ExpectedException(typeof(IndexOutOfRangeException))]
        public void TestAddIndexNegative()
        {
            StateTestUtils.Close(root);
            stateInit.Add(-1, anotherItem);
        }

        [TestMethod]
        public void TestRemove()
        {
            // remove a non-existing item
            Assert.IsFalse(stateDefault.Remove(oneItem));

            // remove an existing item
            Assert.IsTrue(stateInit.Remove(oneItem));

            Assert.IsFalse(stateInit.Contains(oneItem));
            //assertThat(stateInit).doesNotContain(oneItem);

            // check undo
            AssertInitialStateAfterUndo();
            // ... and the redo
            Assert.IsFalse(stateInit.Contains(oneItem));
            //assertThat(stateInit).doesNotContain(oneItem);
        }

        [TestMethod]
        public void TestMove()
        {
            stateInit.Add(0, anotherItem);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem);

            stateInit.Move(oneItem, 0);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { oneItem, anotherItem }));
            //assertThat(stateInit).containsSequence(oneItem, anotherItem);

            stateInit.Move(oneItem, 1);
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem);

            // try illegal move and check if nothing has changed
            //stateInit.Move(oneItem, 2);
            Assert.ThrowsException<IndexOutOfRangeException>(() => stateInit.Move(oneItem, 2));

            //    try
            //    {
            //stateInit.Move(oneItem, 2);
            //failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
            //} catch (Exception e) {
            //    assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
            //}

            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem);


            // check undo
            AssertInitialStateAfterUndo();
            // ... and the redo
            // TODO: replace with containsExactly, this does not work yet
            Assert.AreNotEqual(-1, GetViewList(stateInit).SubListIndex(0, new List<IItem>() { anotherItem, oneItem }));
            //assertThat(stateInit).containsSequence(anotherItem, oneItem);
        }

        [TestMethod]
        public void TestSetTo()
        {
            stateInit.SetTo(stateDefault.View());
            Assert.IsTrue(GetViewList(stateInit).SequenceEqual(stateDefault.View()));
            //Assert.AreEqual(stateInit.view(), stateDefault.view());

            stateDefault.Add(anotherItem);
            stateInit.SetTo(stateDefault.View());
            Assert.IsTrue(GetViewList(stateInit).SequenceEqual(stateDefault.View()));
            //Assert.AreEqual(stateInit.view(), stateDefault.view());

            stateDefault.Add(oneItem);
            stateDefault.Remove(anotherItem);
            stateInit.SetTo(stateDefault.View());
            Assert.IsTrue(GetViewList(stateInit).SequenceEqual(stateDefault.View()));
            //Assert.AreEqual(stateInit.view(), stateDefault.view());

            stateDefault.Add(oneItem);
            stateDefault.Add(oneItem);
            stateInit.SetTo(stateDefault.View());
            Assert.IsTrue(GetViewList(stateInit).SequenceEqual(stateDefault.View()));
            //Assert.AreEqual(stateInit.view(), stateDefault.view());

            AssertInitialStateAfterUndo();

            // and the redo
            Assert.IsTrue(GetViewList(stateInit).SequenceEqual(stateDefault.View()));
            //Assert.AreEqual(stateInit.view(), stateDefault.view());
        }

        [TestMethod]
        public void TestContains()
        {
            Assert.IsTrue(stateInit.Contains(oneItem));
            Assert.IsFalse(stateInit.Contains(anotherItem));
        }

        [TestMethod]
        public void TestClear()
        {
            stateInit.Add(anotherItem);
            stateInit.Clear();
            Assert.IsTrue(stateInit.IsEmpty);
            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsTrue(stateInit.IsEmpty);
        }

        [TestMethod]
        public void TestView()
        {
            //ImmutableList<Item> list = ImmutableList.of(oneItem);
            Assert.IsTrue(oneItemList.SequenceEqual(stateInit.View()));
            //Assert.AreEqual(list, stateInit.view());
        }

        [TestMethod]
        public void TestCount()
        {
            Assert.AreEqual(0, stateDefault.Count);
            Assert.AreEqual(1, stateInit.Count);
            stateInit.Add(anotherItem);
            stateInit.Add(oneItem);
            Assert.AreEqual(3, stateInit.Count);
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsTrue(stateDefault.IsEmpty);
            Assert.IsFalse(stateInit.IsEmpty);
        }

        [TestMethod]
        public void TestIndexOf()
        {
            stateInit.Add(anotherItem);
            Assert.AreEqual(0, stateInit.IndexOf(oneItem));
            Assert.AreEqual(1, stateInit.IndexOf(anotherItem));
            // check if not included
            Assert.AreEqual(-1, stateDefault.IndexOf(oneItem));
        }

        [TestMethod]
        public void TestGet()
        {
            stateInit.Add(anotherItem);
            Assert.AreSame(oneItem, stateInit.Get(0));
            //assertSame(oneItem, stateInit.get(0));
            Assert.AreSame(anotherItem, stateInit.Get(1));
            //assertSame(anotherItem, stateInit.get(1));
            // check index out of bound
            Assert.ThrowsException<ArgumentOutOfRangeException>(() => stateInit.Get(2));
            //        try
            //{
            //    stateInit.get(2);
            //    failBecauseExceptionWasNotThrown(IndexOutOfBoundsException.class);
            //    } catch (Exception e) {
            //        assertThat(e).isInstanceOf(IndexOutOfBoundsException.class);
            //    }
        }

        private void AssertTestIterator()
        {
            IEnumerator<IItem> it = stateInit.GetEnumerator();
            it.MoveNext();
            Assert.AreSame(oneItem, it.Current);
            //assertSame(oneItem, it.next());
            it.MoveNext();
            Assert.AreSame(anotherItem, it.Current);
            //assertSame(anotherItem, it.next());
            // iterator is finished
            Assert.IsFalse(it.MoveNext());
            // iterator is an immutable copy, thus not changed by adding a new item
            stateInit.Add(oneItem);
            Assert.IsFalse(it.MoveNext());
            // remove the last added item
            stateInit.Remove(stateInit.Count - 1);
        }

        [TestMethod]
        public void TestIterator()
        {
            stateInit.Add(anotherItem);
            AssertTestIterator();
            // check initial state after undo
            AssertInitialStateAfterUndo();
            // and check iterator after redo
            StateTestUtils.Close(root);// requires open changeset
            AssertTestIterator();
        }

    }
}

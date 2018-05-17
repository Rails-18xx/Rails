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
    public class HashSetState
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";
        private const string OTHER_ID = "Other";

        private const string ONE_ITEM_ID = "OneItem";
        private const string ANOTHER_ITEM_ID = "AnotherItem";

        private Root root;

        private HashSetState<IItem> stateDefault;
        private HashSetState<IItem> stateInit;

        private IItem oneItem;
        private IItem anotherItem;


        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();


            oneItem = AbstractItemImpl.Create(root, ONE_ITEM_ID);
            anotherItem = AbstractItemImpl.Create(root, ANOTHER_ITEM_ID);

            stateDefault = HashSetState<IItem>.Create(root, DEFAULT_ID);
            stateInit = HashSetState<IItem>.Create(root, INIT_ID, new HashSet<IItem>() { (oneItem) });
        }


        [TestMethod]
        public void TestCreationWithList()
        {
            // checks if the set is created with a list, that it only contains non-unique elements
            HashSetState<IItem> state = HashSetState<IItem>.Create(root, OTHER_ID, new List<IItem>() { oneItem, oneItem });
            Assert.IsTrue(state.View().Contains(oneItem));
            Assert.AreEqual(1, state.Count);
            //assertThat(state).containsOnly(oneItem);
            //assertThat(state).hasSize(1);
        }

        // helper function to check the initial state after undo
        // includes redo, so after returning the state should be unchanged
        private void AssertInitialStateAfterUndo()
        {
            StateTestUtils.CloseAndUndo(root);
            Assert.IsTrue(StateTestUtils.CollectionEquals(stateDefault.View(), new HashSet<IItem>()));
            //Assert.AreEqual(stateDefault.View(), Sets.newHashSet());
            Assert.IsTrue(StateTestUtils.CollectionEquals(stateInit.View(), new HashSet<IItem>() { oneItem }));
            //Assert.AreEqual(stateInit.view(), Sets.newHashSet(oneItem));
            StateTestUtils.Redo(root);
        }

        private void AssertTestAdd()
        {
            Assert.IsTrue(stateDefault.Contains(oneItem) && stateDefault.Count == 1);
            //assertThat(stateDefault).containsOnly(oneItem);
            Assert.IsTrue(stateInit.Contains(oneItem) && stateInit.Contains(anotherItem) && stateInit.Count == 2);
            //assertThat(stateInit).containsOnly(oneItem, anotherItem);
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
        public void TestRemove()
        {
            // remove a non-existing item
            Assert.IsFalse(stateDefault.Remove(oneItem));

            // remove an existing item
            Assert.IsTrue(stateInit.Remove(oneItem));

            Assert.IsTrue(!stateInit.Contains(oneItem));
            //assertThat(stateInit).doesNotContain(oneItem);

            // check undo
            AssertInitialStateAfterUndo();
            // ... and the redo
            Assert.IsTrue(!stateInit.Contains(oneItem));
            //assertThat(stateInit).doesNotContain(oneItem);
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
            //ImmutableSet<IItem> list = ImmutableSet.of(oneItem);
            //Assert.AreEqual(list, stateInit.view());
            Assert.IsTrue(StateTestUtils.CollectionEquals(new List<IItem>() { oneItem }, stateInit.View()));
        }

        [TestMethod]
        public void TestSize()
        {
            Assert.AreEqual(0, stateDefault.Count);
            Assert.AreEqual(1, stateInit.Count);
            stateInit.Add(anotherItem);
            Assert.AreEqual(2, stateInit.Count);
            stateInit.Add(oneItem);
            Assert.AreEqual(2, stateInit.Count);
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsTrue(stateDefault.IsEmpty);
            Assert.IsFalse(stateInit.IsEmpty);
        }

        private void AssertTestIterator(IItem thirdItem)
        {
            // no order is defined, so store them
            HashSet<IItem> iterated = new HashSet<IItem>();

            IEnumerator<IItem> it = stateInit.GetEnumerator();
            it.MoveNext();
            iterated.Add(it.Current);
            it.MoveNext();
            iterated.Add(it.Current);

            Assert.IsTrue(iterated.Count == 2 && iterated.Contains(oneItem) && iterated.Contains(anotherItem));
            //assertThat(iterated).containsOnly(oneItem, anotherItem);
            // iterator is finished
            Assert.IsFalse(it.MoveNext());
            // iterator is an immutable copy, thus not changed by adding a new item
            stateInit.Add(thirdItem);
            Assert.IsFalse(it.MoveNext());
            // remove the last added item
            stateInit.Remove(thirdItem);
        }

        [TestMethod]
        public void TestIterator()
        {
            stateInit.Add(anotherItem);

            // create another test item
            IItem thirdItem = AbstractItemImpl.Create(root, "Third");

            AssertTestIterator(thirdItem);
            // check initial state after undo
            AssertInitialStateAfterUndo();
            // and check iterator after redo
            StateTestUtils.Close(root); // test requires open changeset
            AssertTestIterator(thirdItem);
        }
    }
}

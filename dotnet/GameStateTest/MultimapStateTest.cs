using System;
using System.Collections.Generic;
using System.Linq;
using GameLib.Net.Game.State;
using Microsoft.VisualStudio.TestTools.UnitTesting;

namespace GameStateTest
{
    [TestClass]
    public class MultimapStateTest
    {
        private const string STATE_ID = "State";
        private const string ITEM_A_ID = "ItemA";
        private const string ITEM_B_ID = "ItemB";
        private const string ITEM_C_ID = "ItemC";

        private Root root;
        private HashMultimapState<string, IItem> state;
        private IItem itemA, itemB, itemC;
        private List<IItem> initContents;

        //public TestContext TestContext { get; set; }

        //[TestMethod]
        //public void TestMethod1()
        //{
        //}

        //[ClassInitialize]
        //public static void ClassInitialize(TestContext testContext)
        //{
        //    Context = testContext;
        //}

        [TestInitialize]
        public void TestInitialize()
        {
            root = StateTestUtils.SetUpRoot();
            state = HashMultimapState<string, IItem>.Create(root, STATE_ID);
            itemA = OwnableItemImpl.Create(root, ITEM_A_ID);
            itemB = OwnableItemImpl.Create(root, ITEM_B_ID);
            itemC = OwnableItemImpl.Create(root, ITEM_C_ID);
            StateTestUtils.Close(root);
        }

        private void InitState()
        {
            // initialize state
            state.Put(ITEM_A_ID, itemA);
            state.Put(ITEM_A_ID, itemB);
            state.Put(ITEM_A_ID, itemC);
            state.Put(ITEM_B_ID, itemB);
            state.Put(ITEM_C_ID, itemC);
            StateTestUtils.Close(root);
            initContents = new List<IItem>() { itemA, itemB, itemC, itemB, itemC };
        }

        [TestMethod]
        public void TestPut()
        {
            state.Put(ITEM_A_ID, itemA);

            var a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 1 && a.Contains(itemA));

            Assert.IsFalse(state.Put(ITEM_A_ID, itemA)); // cannot add identical tuple

            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 1 && a.Contains(itemA));

            state.Put(ITEM_A_ID, itemB);
            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 2 && a.Contains(itemA) && a.Contains(itemB));

            // test undo
            StateTestUtils.CloseAndUndo(root);
            Assert.IsTrue(state.IsEmpty);

            // test redo
            StateTestUtils.Redo(root);
            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 2 && a.Contains(itemA) && a.Contains(itemB));
        }

        [TestMethod]
        public void TestRemove()
        {
            InitState();
            // remove items
            state.Remove(ITEM_A_ID, itemA);

            var a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 2 && a.Contains(itemB) && a.Contains(itemC));

            state.Remove(ITEM_A_ID, itemC);

            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 1 && a.Contains(itemB));

            // test undo
            StateTestUtils.CloseAndUndo(root);
            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 3 && a.Contains(itemA) && a.Contains(itemB) && a.Contains(itemC));

            // test redo
            StateTestUtils.Redo(root);
            a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 1 && a.Contains(itemB));
        }

        [TestMethod]
        public void TestRemoveAll()
        {
            InitState();

            ISet<IItem> removed = state.RemoveAll(ITEM_A_ID);
            Assert.IsTrue(removed.Count == 3 && removed.Contains(itemA) && removed.Contains(itemB) && removed.Contains(itemC));

            Assert.IsTrue(state.Get(ITEM_A_ID).Count == 0);

            // test undo
            StateTestUtils.CloseAndUndo(root);
            var a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 3 && a.Contains(itemA) && a.Contains(itemB) && a.Contains(itemC));

            // test redo
            StateTestUtils.Redo(root);
            Assert.IsTrue(state.Get(ITEM_A_ID).Count == 0);
        }

        [TestMethod]
        public void TestGet()
        {
            InitState();
            var a = state.Get(ITEM_A_ID);
            Assert.IsTrue(a.Count == 3 && a.Contains(itemA) && a.Contains(itemB) && a.Contains(itemC));

            a = state.Get(ITEM_C_ID);
            Assert.IsTrue(a.Count == 1 && a.Contains(itemC));
        }

        [TestMethod]
        public void TestContainsEntry()
        {
            state.Put(ITEM_A_ID, itemA);
            state.Put(ITEM_C_ID, itemB);
            Assert.IsTrue(state.ContainsEntry(ITEM_A_ID, itemA));
            Assert.IsTrue(state.ContainsEntry(ITEM_C_ID, itemB));

            Assert.IsFalse(state.ContainsEntry(ITEM_B_ID, itemA));
            Assert.IsFalse(state.ContainsEntry(ITEM_A_ID, itemB));
            Assert.IsFalse(state.ContainsEntry(ITEM_B_ID, itemB));
            Assert.IsFalse(state.ContainsEntry(ITEM_C_ID, itemC));
        }

        [TestMethod]
        public void TestContainsKey()
        {
            state.Put(ITEM_A_ID, itemA);
            state.Put(ITEM_C_ID, itemB);
            Assert.IsTrue(state.ContainsKey(ITEM_A_ID));
            Assert.IsFalse(state.ContainsKey(ITEM_B_ID));
            Assert.IsTrue(state.ContainsKey(ITEM_C_ID));
        }

        [TestMethod]
        public void TestContainsValue()
        {
            state.Put(ITEM_A_ID, itemA);
            state.Put(ITEM_C_ID, itemB);
            Assert.IsTrue(state.ContainsValue(itemA));
            Assert.IsTrue(state.ContainsValue(itemB));
            Assert.IsFalse(state.ContainsValue(itemC));
        }

        [TestMethod]
        public void TestSize()
        {
            Assert.AreEqual(0, state.Count);
            InitState();
            Assert.AreEqual(5, state.Count);
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsTrue(state.IsEmpty);
            InitState();
            Assert.IsFalse(state.IsEmpty);
        }

        [TestMethod]
        public void TestKeySet()
        {
            state.Put(ITEM_A_ID, itemA);
            state.Put(ITEM_C_ID, itemB);
            var a = state.KeySet();
            Assert.IsTrue(a.Count == 2 && a.Contains(ITEM_A_ID) && a.Contains(ITEM_C_ID));
        }

        [TestMethod]
        public void TestValues()
        {
            InitState();
            Assert.IsTrue(!state.Values().Except(initContents).Any());
        }

        private void AssertTestIterator(IItem thirdItem)
        {
            // no order is defined, so store them
            HashSet<IItem> iterated = new HashSet<IItem>();

            IEnumerator<IItem> it = state.GetEnumerator();
            while (it.MoveNext())
            {
                iterated.Add(it.Current);
            }
            Assert.IsTrue(!iterated.Except(initContents).Any());

            // iterator is an immutable copy, thus not changed by adding a new item
            state.Put(ITEM_C_ID, itemA);
            Assert.IsFalse(it.MoveNext());
            // remove the last added item
            state.Remove(ITEM_C_ID, itemA);
        }

        [TestMethod]
        public void TestIterator()
        {
            InitState();
            IItem thirdItem = AbstractItemImpl.Create(root, "Third");
            AssertTestIterator(thirdItem);
        }


    }
}

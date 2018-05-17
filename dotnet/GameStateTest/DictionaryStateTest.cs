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
    public class DictionaryStateTest
    {
        private const string DEFAULT_ID = "Default";
        private const string INIT_ID = "Init";

        private const string FIRST_ITEM_ID = "FirstItem";
        private const string NEW_FIRST_ITEM_ID = "NewFirstItem";
        private const string SECOND_ITEM_ID = "SecondItem";
        private const string THIRD_ITEM_ID = "ThirdItem";

        private Root root;

        private DictionaryState<string, IItem> state_default;
        private DictionaryState<string, IItem> stateInit;
        private Dictionary<string, IItem> initMap, testMap;

        private IItem firstItem, newFirstItem, secondItem, thirdItem;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();

            firstItem = AbstractItemImpl.Create(root, FIRST_ITEM_ID);
            newFirstItem = AbstractItemImpl.Create(root, NEW_FIRST_ITEM_ID);
            secondItem = AbstractItemImpl.Create(root, SECOND_ITEM_ID);
            thirdItem = AbstractItemImpl.Create(root, THIRD_ITEM_ID);

            state_default = DictionaryState<string, IItem>.Create(root, DEFAULT_ID);

            // initialize stateInit with initMap and create testMap
            initMap = new Dictionary<string, IItem>() { { FIRST_ITEM_ID, firstItem } };// ImmutableMap.of(FIRST_ITEM_ID, firstItem);
            stateInit = DictionaryState<string, IItem>.Create(root, INIT_ID, initMap);

            testMap = new Dictionary<string, IItem>();
            testMap[FIRST_ITEM_ID] = newFirstItem;
            testMap[SECOND_ITEM_ID] = secondItem;
        }


        [TestMethod]
        public void TestCreate()
        {
            DictionaryState<string, IItem> state = DictionaryState<string, IItem>.Create(root, "Test");
            Assert.IsTrue(state.View().Count == 0);
        }

        [TestMethod]
        public void TestCreateMapOfKV()
        {
            DictionaryState<string, IItem> state = DictionaryState<string, IItem>.Create(root, "Test", testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state.View(), testMap));
            //Assert.AreEqual(state.View(), testMap);
        }

        // helper function to check the initial state after undo
        // includes redo, so after returning the state should be unchanged
        private void AssertInitialStateAfterUndo()
        {
            StateTestUtils.CloseAndUndo(root);
            Assert.IsTrue(state_default.View().Count == 0);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(stateInit.View(), initMap));
            //Assert.AreEqual(stateInit.View(), initMap);
            StateTestUtils.Redo(root);
        }


        [TestMethod]
        public void TestPut()
        {
            foreach (string key in testMap.Keys)
            {
                state_default.Put(key, testMap[key]);
                stateInit.Put(key, testMap[key]);
            }
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(state_default.View(), testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(stateInit.View(), testMap));
            //Assert.AreEqual(stateInit.View(), testMap);

            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(state_default.View(), testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(stateInit.View(), testMap));
            //Assert.AreEqual(stateInit.View(), testMap);
        }

        // includes tests for viewMap
        [TestMethod]
        public void TestPutAll()
        {
            stateInit.PutAll(testMap);
            state_default.PutAll(testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(state_default.View(), testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(stateInit.View(), testMap));
            //Assert.AreEqual(stateInit.View(), testMap);

            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(testMap, state_default.View());
            Assert.IsTrue(StateTestUtils.DictionaryEquals(stateInit.View(), testMap));
            //Assert.AreEqual(testMap, stateInit.View());
        }

        [TestMethod]
        public void TestGet()
        {
            Assert.AreEqual(firstItem, stateInit.Get(FIRST_ITEM_ID));
            Assert.IsNull(stateInit.Get(SECOND_ITEM_ID));
        }

        [TestMethod]
        public void TestRemove()
        {
            stateInit.Remove(FIRST_ITEM_ID);
            Assert.IsNull(stateInit.Get(FIRST_ITEM_ID));

            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsNull(stateInit.Get(FIRST_ITEM_ID));
        }

        [TestMethod]
        public void TestContainsKey()
        {
            state_default.Put(THIRD_ITEM_ID, thirdItem);
            Assert.IsTrue(state_default.ContainsKey(THIRD_ITEM_ID));

            Assert.IsTrue(stateInit.ContainsKey(FIRST_ITEM_ID));
            Assert.IsFalse(stateInit.ContainsKey(SECOND_ITEM_ID));
        }

        [TestMethod]
        public void TestClear()
        {
            state_default.PutAll(testMap);
            state_default.Clear();
            Assert.IsTrue(state_default.IsEmpty());

            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsTrue(state_default.IsEmpty());
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsTrue(state_default.IsEmpty());
            state_default.Put(FIRST_ITEM_ID, firstItem);
            Assert.IsFalse(state_default.IsEmpty());

            Assert.IsFalse(stateInit.IsEmpty());
            stateInit.Remove(FIRST_ITEM_ID);
            Assert.IsTrue(stateInit.IsEmpty());
        }


        [TestMethod]
        public void TestInitFromMap()
        {
            state_default.Put(THIRD_ITEM_ID, thirdItem);
            state_default.InitFromMap(testMap);
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(testMap, state_default.View());

            // check undo and redo
            AssertInitialStateAfterUndo();
            Assert.IsTrue(StateTestUtils.DictionaryEquals(state_default.View(), testMap));
            //Assert.AreEqual(testMap, state_default.View());
        }

        [TestMethod]
        public void TestViewKeySet()
        {
            state_default.PutAll(testMap);
            CollectionAssert.IsSubsetOf(testMap.Keys, new List<string>(state_default.ViewKeys()));
            //assertThat(state_default.viewKeySet()).containsAll(testMap.keySet());
        }

        [TestMethod]
        public void TestViewValues()
        {
            state_default.PutAll(testMap);
            CollectionAssert.IsSubsetOf(testMap.Values, new List<IItem>(state_default.ViewValues()));
            //assertThat(state_default.viewValues()).containsAll(testMap.values());
        }

    }
}

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
    public class PortfolioMapTest
    {
        private const string PORTFOLIO_MAP_ID = "PortfolioMap";
        private const string PORTFOLIO_SET_ID = "PortfolioSet";
        private const string OWNER_MAP_ID = "OwnerMap";
        private const string OWNER_SET_ID = "OwnerSet";
        private const string ITEM_ID = "Item";
        private const string ANOTHER_ITEM_ID = "AnotherItem";
        private const string TYPE_ID = "Type";
        private const string ANOTHER_TYPE_ID = "AnotherType";

        private Root root;
        private PortfolioMap<string, TypeOwnableItemImpl> portfolioMap;
        private PortfolioSet<TypeOwnableItemImpl> portfolioSet;
        private IOwner ownerMap;
        private IOwner ownerSet;
        private TypeOwnableItemImpl item;
        private TypeOwnableItemImpl anotherItem;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            ownerMap = OwnerImpl.Create(root, OWNER_MAP_ID);
            ownerSet = OwnerImpl.Create(root, OWNER_SET_ID);
            portfolioMap = PortfolioMap<string, TypeOwnableItemImpl>.Create(ownerMap, PORTFOLIO_MAP_ID);
            portfolioSet = PortfolioSet<TypeOwnableItemImpl>.Create(ownerSet, PORTFOLIO_SET_ID);
            item = TypeOwnableItemImpl.Create(root, ITEM_ID, TYPE_ID);
            anotherItem = TypeOwnableItemImpl.Create(root, ANOTHER_ITEM_ID, ANOTHER_TYPE_ID);
            portfolioSet.Add(item);
            StateTestUtils.Close(root);
        }

        // helper function to check the initial state after undo
        // includes redo, so after returning the state should be unchanged
        private void AssertInitialStateAfterUndo()
        {
            StateTestUtils.CloseAndUndo(root);
            Assert.IsTrue(portfolioSet.ContainsItem(item));
            Assert.AreSame(ownerSet, item.Owner);
            StateTestUtils.Redo(root);
        }

        [TestMethod]
        public void TestAdd()
        {
            portfolioMap.Add(item);
            Assert.AreSame(ownerMap, item.Owner);
            Assert.IsTrue(portfolioMap.ContainsItem(item));
            Assert.IsFalse(portfolioSet.ContainsItem(item));
            // check undo
            AssertInitialStateAfterUndo();
            // and redo
            Assert.IsTrue(portfolioMap.ContainsItem(item));
        }

        [TestMethod]
        public void TestContainsItem()
        {
            Assert.IsFalse(portfolioMap.ContainsItem(item));
            item.MoveTo(ownerMap);
            Assert.IsTrue(portfolioMap.ContainsItem(item));
        }

        [TestMethod]
        public void TestItems()
        {
            Assert.IsTrue(portfolioMap.Items.Count == 0);
            item.MoveTo(ownerMap);
            Assert.IsTrue(StateTestUtils.ContainsOnly(portfolioMap.Items, item));
            anotherItem.MoveTo(ownerMap);
            Assert.IsTrue(portfolioMap.Items.Count == 2 && portfolioMap.Items.Contains(item) && portfolioMap.Contains(anotherItem));
        }

        [TestMethod]
        public void TestSize()
        {
            Assert.AreEqual(0, portfolioMap.Count);
            item.MoveTo(ownerMap);
            Assert.AreEqual(1, portfolioMap.Count);
            anotherItem.MoveTo(ownerMap);
            Assert.AreEqual(2, portfolioMap.Count);
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsTrue(portfolioMap.IsEmpty);
            item.MoveTo(ownerMap);
            Assert.IsFalse(portfolioMap.IsEmpty);
        }

        [TestMethod]
        public void testContainsKey()
        {
            Assert.IsFalse(portfolioMap.ContainsKey(TYPE_ID));
            item.MoveTo(ownerMap);
            Assert.IsTrue(portfolioMap.ContainsKey(TYPE_ID));
            item.MoveTo(ownerSet);
            Assert.IsFalse(portfolioMap.ContainsKey(TYPE_ID));
        }

        [TestMethod]
        public void TestGetItems()
        {
            //assertThat(portfolioMap.items(TYPE_ID)).isEmpty();
            Assert.IsTrue(portfolioMap.GetItems(TYPE_ID).Count == 0);
            item.MoveTo(ownerMap);
            //assertThat(portfolioMap.items(TYPE_ID)).containsOnly(item);
            Assert.IsTrue(StateTestUtils.ContainsOnly(portfolioMap.GetItems(TYPE_ID), item));
        }

        [TestMethod]
        public void TestView()
        {
            item.MoveTo(ownerMap);
            //SetMultimap<string, TypeOwnableItemImpl> view = portfolioMap.view();
            var view = portfolioMap.View();
            Assert.IsTrue(view.ContainsValue(item));
            // still holds true after removing item
            item.MoveTo(ownerSet);
            Assert.IsTrue(view.ContainsValue(item));
            Assert.IsFalse(portfolioMap.ContainsItem(item));
        }

        [TestMethod]
        public void TestIterator()
        {
            item.MoveTo(ownerMap);
            anotherItem.MoveTo(ownerMap);

            // no order is defined, so store them
            HashSet<IOwnable> iterated = new HashSet<IOwnable>();


            var it = portfolioMap.GetEnumerator();
            // and it still works even after removing items
            item.MoveTo(ownerSet);
            it.MoveNext();
            iterated.Add(it.Current);
            it.MoveNext();
            iterated.Add(it.Current);

            Assert.IsTrue(iterated.Count == 2 && iterated.Contains(item) && iterated.Contains(anotherItem));
            // iterator is finished
            Assert.IsFalse(it.MoveNext());
        }
    }
}

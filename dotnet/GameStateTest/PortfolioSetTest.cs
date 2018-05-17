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
    public class PortfolioSetTest
    {
        private const string PORTFOLIO_A_ID = "PortfolioA";
        private const string PORTFOLIO_B_ID = "PortfolioB";
        private const string OWNER_A_ID = "OwnerA";
        private const string OWNER_B_ID = "OwnerB";
        private const string ITEM_ID = "Item";
        private const string ANOTHER_ITEM_ID = "AnotherItem";

        private Root root;
        private PortfolioSet<IOwnable> portfolioA;
        private PortfolioSet<IOwnable> portfolioB;
        private IOwner ownerA;
        private IOwner ownerB;
        private IOwnable item;
        private IOwnable anotherItem;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            ownerA = OwnerImpl.Create(root, OWNER_A_ID);
            ownerB = OwnerImpl.Create(root, OWNER_B_ID);
            portfolioA = PortfolioSet<IOwnable>.Create(ownerA, PORTFOLIO_A_ID);
            portfolioB = PortfolioSet<IOwnable>.Create(ownerB, PORTFOLIO_B_ID);
            item = OwnableItemImpl.Create(root, ITEM_ID);
            anotherItem = OwnableItemImpl.Create(root, ANOTHER_ITEM_ID);
            portfolioA.Add(item);
            StateTestUtils.Close(root);
        }

        // helper function to check the initial state after undo
        // includes redo, so after returning the state should be unchanged
        private void AssertInitialStateAfterUndo()
        {
            StateTestUtils.CloseAndUndo(root);
            Assert.IsTrue(portfolioA.ContainsItem(item));
            Assert.AreSame(ownerA, item.Owner);
            StateTestUtils.Redo(root);
        }

        [TestMethod]
        public void TestAdd()
        {
            // move item to B
            item.MoveTo(ownerB);
            Assert.IsTrue(portfolioB.ContainsItem(item));
            Assert.AreSame(ownerB, item.Owner);

            // undo check
            AssertInitialStateAfterUndo();

            // redo check
            Assert.IsTrue(portfolioB.ContainsItem(item));
            Assert.AreSame(ownerB, item.Owner);
        }

        [TestMethod]
        public void TestContainsItem()
        {
            Assert.IsTrue(portfolioA.ContainsItem(item));
            Assert.IsFalse(portfolioB.ContainsItem(item));
        }

        [TestMethod]
        public void TestItems()
        {
            Assert.IsTrue(StateTestUtils.ContainsOnly(portfolioA.Items, item));
            anotherItem.MoveTo(ownerA);
            var items = portfolioA.Items;
            Assert.IsTrue(portfolioA.Items.Count == 2 && portfolioA.Items.Contains(item) && portfolioA.Contains(anotherItem));
            // and the view is unchanged after changing the portfolio
            anotherItem.MoveTo(ownerB);
            Assert.IsTrue(items.Contains(anotherItem));
            Assert.IsFalse(portfolioA.ContainsItem(anotherItem));
        }

        [TestMethod]
        public void TestSize()
        {
            Assert.AreEqual(1, portfolioA.Count);
            Assert.AreEqual(0, portfolioB.Count);
            anotherItem.MoveTo(ownerA);
            Assert.AreEqual(2, portfolioA.Count);
            item.MoveTo(ownerB);
            Assert.AreEqual(1, portfolioA.Count);
            Assert.AreEqual(1, portfolioB.Count);
        }

        [TestMethod]
        public void TestIsEmpty()
        {
            Assert.IsFalse(portfolioA.IsEmpty);
            Assert.IsTrue(portfolioB.IsEmpty);
        }

        [TestMethod]
        public void TestIterator()
        {
            anotherItem.MoveTo(ownerA);

            // no order is defined, so store them
            HashSet<IOwnable> iterated = new HashSet<IOwnable>();

            var it = portfolioA.GetEnumerator();
            // and it still works even after removing items
            anotherItem.MoveTo(ownerB);
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

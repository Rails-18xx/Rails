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
    public class WalletBagTest
    {
        private const string WALLET_A_ID = "WalletA";
        private const string WALLET_B_ID = "WalletB";

        private const string OWNER_A_ID = "OwnerA";
        private const string OWNER_B_ID = "OwnerB";
        private const string ITEM_ID = "Item";
        private const int AMOUNT = 10;

        private Root root;
        private WalletBag<ICountable> walletA;
        private WalletBag<ICountable> walletB;
        private IOwner ownerA;
        private IOwner ownerB;
        private ICountable item;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            ownerA = OwnerImpl.Create(root, OWNER_A_ID);
            ownerB = OwnerImpl.Create(root, OWNER_B_ID);
            item = CountableItemImpl.Create(root, ITEM_ID);
            walletA = WalletBag<ICountable>.Create(ownerA, WALLET_A_ID, item);
            walletB = WalletBag<ICountable>.Create(ownerB, WALLET_B_ID, item);
            StateTestUtils.Close(root);
        }

        [TestMethod]
        public void TestWalletBag()
        {
            Assert.AreEqual(0, walletA.Value());
            item.Move(ownerA, AMOUNT, ownerB);
            Assert.AreEqual(-AMOUNT, walletA.Value());
            Assert.AreEqual(AMOUNT, walletB.Value());

            StateTestUtils.CloseAndUndo(root);
            Assert.AreEqual(0, walletA.Value());
            Assert.AreEqual(0, walletB.Value());

            StateTestUtils.Redo(root);
            Assert.AreEqual(-AMOUNT, walletA.Value());
            Assert.AreEqual(AMOUNT, walletB.Value());
        }

    }
}

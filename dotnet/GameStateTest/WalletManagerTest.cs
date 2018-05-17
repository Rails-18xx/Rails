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
    public class WalletManagerTest
    {
        private const string WALLET_A_ID = "WalletA";
        private const string WALLET_B_ID = "WalletB";
        private const string ITEM_ID = "Item";
        private const string OWNER_A_ID = "OwnerA";
        private const string OWNER_B_ID = "OwnerB";
        private const string OWNER_C_ID = "OwnerC";

        private Root root;
        private WalletManager wm;
        private CountableItemImpl item;
        private WalletBag<ICountable> walletA, walletB;
        private IOwner ownerA, ownerB, ownerC;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            wm = root.StateManager.WalletManager;
            item = CountableItemImpl.Create(root, ITEM_ID);
            ownerA = OwnerImpl.Create(root, OWNER_A_ID);
            ownerB = OwnerImpl.Create(root, OWNER_B_ID);
            ownerC = OwnerImpl.Create(root, OWNER_C_ID);
            walletA = WalletBag<ICountable>.Create(ownerA, WALLET_A_ID, item);
            walletB = WalletBag<ICountable>.Create(ownerB, WALLET_B_ID, item);
            StateTestUtils.Close(root);
        }

        [TestMethod]
        public void TestWMKey()
        {
            // create various keys
            WalletManager.WMKey<ICountable> keyA1, keyA2, keyB;
            WalletManager.WMKey<CountableItem<ICountable>> keyADiff;
            keyA1 = wm.CreateWMKey<ICountable>(typeof(ICountable), ownerA);
            keyA2 = wm.CreateWMKey<ICountable>(typeof(ICountable), ownerA);
            keyADiff = wm.CreateWMKey<CountableItem<ICountable>>(typeof(CountableItem<ICountable>), ownerA);
            keyB = wm.CreateWMKey<ICountable>(typeof(ICountable), ownerB);

            // check that the two A keys are identical...
            Assert.IsTrue(keyA1.Equals(keyA2));
            Assert.IsTrue(keyA2.Equals(keyA1));

            // ... the other differ
            Assert.IsFalse(keyA1.Equals(keyADiff));
            Assert.IsFalse(keyADiff.Equals(keyA1));
            Assert.IsFalse(keyA1.Equals(keyB));
            Assert.IsFalse(keyB.Equals(keyA1));

            // check hashcodes
            Assert.IsTrue(keyA1.GetHashCode() == keyA2.GetHashCode());
            Assert.IsFalse(keyA1.GetHashCode() == keyADiff.GetHashCode());
            Assert.IsFalse(keyA1.GetHashCode() == keyB.GetHashCode());
        }

        [TestMethod]
        public void TestGetUnknownOwner()
        {
            Assert.IsNotNull(wm.UnknownOwner);
        }

        [TestMethod]
        public void TestRemoveWallet()
        {
            wm.RemoveWallet(walletA);
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), ownerA));

            // undo and redo check
            StateTestUtils.CloseAndUndo(root);
            Assert.AreSame(walletA, wm.GetWallet<ICountable>(typeof(ICountable), ownerA));
            StateTestUtils.Redo(root);
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), ownerA));
        }

        [TestMethod]
        public void TestAddWallet()
        {
            // remove first to prepare
            wm.RemoveWallet(walletA);
            StateTestUtils.Close(root);
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), ownerA));

            // then add
            wm.AddWallet(walletA);
            Assert.AreSame(walletA, wm.GetWallet<ICountable>(typeof(ICountable), ownerA));

            // undo and redo check
            StateTestUtils.CloseAndUndo(root);
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), ownerA));

            // redo check
            StateTestUtils.Redo(root);
            Assert.AreSame(walletA, wm.GetWallet<ICountable>(typeof(ICountable), ownerA));
        }

        [TestMethod]
        public void TestGetWallet()
        {
            Assert.AreSame(walletA, wm.GetWallet<ICountable>(typeof(ICountable), ownerA));
            Assert.AreSame(walletB, wm.GetWallet<ICountable>(typeof(ICountable), ownerB));
            Assert.IsNull(wm.GetWallet<CountableItem<ICountable>>(typeof(CountableItem<ICountable>), ownerA));
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), ownerC));
            Assert.IsNull(wm.GetWallet<ICountable>(typeof(ICountable), wm.UnknownOwner));
        }
    }
}

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
    public class PortfolioManagerTest
    {
        private const string PORTFOLIO_A_ID = "PortfolioA";
        private const string PORTFOLIO_B_ID = "PortfolioB";
        private const string OWNER_A_ID = "OwnerA";
        private const string OWNER_B_ID = "OwnerB";
        private const string OWNER_C_ID = "OwnerC";

        private Root root;
        private PortfolioManager pm;
        private PortfolioSet<IOwnable> portfolioA, portfolioB;
        private IOwner ownerA, ownerB, ownerC;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            pm = root.StateManager.PortfolioManager;
            ownerA = OwnerImpl.Create(root, OWNER_A_ID);
            ownerB = OwnerImpl.Create(root, OWNER_B_ID);
            ownerC = OwnerImpl.Create(root, OWNER_C_ID);
            portfolioA = PortfolioSet<IOwnable>.Create(ownerA, PORTFOLIO_A_ID);
            portfolioB = PortfolioSet<IOwnable>.Create(ownerB, PORTFOLIO_B_ID);
            StateTestUtils.Close(root);
        }

        [TestMethod]
        public void TestPMKey()
        {
            // create various keys
            PortfolioManager.PMKey keyA1, keyA2, keyADiff, keyB;
            keyA1 = pm.CreatePMKey<IOwnable>(ownerA);
            keyA2 = pm.CreatePMKey<IOwnable>(ownerA);
            keyADiff = pm.CreatePMKey<OwnableItem<IOwnable>>(ownerA);
            keyB = pm.CreatePMKey<IOwnable>(ownerB);

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
        public void TestGetUnkownOwner()
        {
            Assert.IsNotNull(pm.UnknownOwner);
        }

        [TestMethod]
        public void TestRemovePortfolio()
        {
            pm.RemovePortfolio(portfolioA);
            Assert.IsNull(pm.GetPortfolio<IOwnable>(ownerA));

            // undo and redo check
            StateTestUtils.CloseAndUndo(root);
            Assert.AreSame(portfolioA, pm.GetPortfolio<IOwnable>(ownerA));
            StateTestUtils.Redo(root);
            Assert.IsNull(pm.GetPortfolio<IOwnable>(ownerA));
        }

        [TestMethod]
        public void TestAddPortfolio()
        {
            // remove first to prepare
            pm.RemovePortfolio(portfolioA);
            StateTestUtils.Close(root);
            Assert.IsNull(pm.GetPortfolio<IOwnable>(ownerA));

            // then add
            pm.AddPortfolio(portfolioA);
            Assert.AreSame(portfolioA, pm.GetPortfolio<IOwnable>(ownerA));

            // undo and redo check
            StateTestUtils.CloseAndUndo(root);
            Assert.IsNull(pm.GetPortfolio<IOwnable>(ownerA));

            // redo check
            StateTestUtils.Redo(root);
            Assert.AreSame(portfolioA, pm.GetPortfolio<IOwnable>(ownerA));
        }

        [TestMethod]
        public void TestGetPortfolio()
        {
            Assert.AreSame(portfolioA, pm.GetPortfolio<IOwnable>(ownerA));
            Assert.AreSame(portfolioB, pm.GetPortfolio<IOwnable>(ownerB));
            Assert.IsNull(pm.GetPortfolio<OwnableItem<IOwnable>>(ownerA));
            Assert.IsNull(pm.GetPortfolio<IOwnable>(ownerC));
            Assert.IsNull(pm.GetPortfolio<IOwnable>(pm.UnknownOwner));
        }

    }
}

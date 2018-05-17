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
    public class ManagerTest
    {
        private const string MANAGER_ID = "manager";
        private const string ITEM_ID = "item";
        private const string ANOTHER_ITEM_ID = "anotherItem";
        private const string ANOTHER_MANAGER_ID = "anotherManager";

        private Root root;
        private Manager manager, anotherManager;
        private IItem item, anotherItem;

        [TestInitialize]
        public void SetUp()
        {
            root = Root.Create();
            item = new AbstractItemImpl(root, ITEM_ID);
            manager = new ManagerImpl(item, MANAGER_ID);
            anotherItem = new AbstractItemImpl(manager, ANOTHER_ITEM_ID);
            anotherManager = new ManagerImpl(manager, ANOTHER_MANAGER_ID);
        }

        [TestMethod]
        public void TestGetId()
        {
            Assert.AreEqual(MANAGER_ID, manager.Id);
            Assert.AreEqual(ANOTHER_MANAGER_ID, anotherManager.Id);
        }

        [TestMethod]
        public void TestGetParent()
        {
            Assert.AreSame(item, manager.Parent);
            Assert.AreSame(manager, anotherManager.Parent);
        }

        [TestMethod]
        public void TestGetContext()
        {
            Assert.AreSame(root, manager.GetContext);
            Assert.AreSame(manager, anotherManager.GetContext);
        }

        [TestMethod]
        public void TestGetURI()
        {
            Assert.AreEqual(ITEM_ID + IItemConsts.SEP + MANAGER_ID, manager.URI);
            Assert.AreEqual(ANOTHER_MANAGER_ID, anotherManager.URI);
        }

        [TestMethod]
        public void TestGetFullURI()
        {
            Assert.AreEqual(IItemConsts.SEP + ITEM_ID + IItemConsts.SEP + MANAGER_ID, manager.FullURI);
            Assert.AreEqual(IItemConsts.SEP + ITEM_ID + IItemConsts.SEP + MANAGER_ID + IItemConsts.SEP + ANOTHER_MANAGER_ID, anotherManager.FullURI);
        }

        [TestMethod]
        public void TestLocate()
        {
            // anotherItem is local
            Assert.AreSame(anotherItem, manager.Locate(anotherItem.URI));
            Assert.AreSame(anotherItem, manager.Locate(anotherItem.FullURI));

            // item is not local
            Assert.IsNull(manager.Locate(item.URI));
            Assert.AreSame(item, manager.Locate(item.FullURI));

            // manager is not local in itself, but in root
            Assert.IsNull(manager.Locate(manager.URI));
            Assert.AreSame(manager, root.Locate(manager.URI));
            Assert.AreSame(manager, manager.Locate(manager.FullURI));

            // anotherManager is not local in itself, but in manager
            Assert.IsNull(anotherManager.Locate(anotherManager.URI));
            Assert.AreSame(anotherManager, manager.Locate(anotherManager.URI));
            Assert.AreSame(anotherManager, anotherManager.Locate(anotherManager.FullURI));
        }

        [TestMethod]
        public void testGetRoot()
        {
            Assert.AreSame(root, manager.GetRoot);
            Assert.AreSame(root, anotherManager.GetRoot);
        }

    }
}

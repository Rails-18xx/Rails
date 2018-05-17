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
    public class RootTest
    {
        private const string MANAGER_ID = "manager";
        private const string ITEM_ID = "item";
        private const string ANOTHER_ID = "anotherItem";

        private Root root;
        private Manager manager;
        private IItem item;
        private IItem anotherItem;

        [TestInitialize]
        public void SetUp()
        {
            root = Root.Create();
            manager = new ManagerImpl(root, MANAGER_ID);
            item = new AbstractItemImpl(root, ITEM_ID);
            anotherItem = new AbstractItemImpl(manager, ANOTHER_ID);
        }

        [TestMethod]
        public void TestGetStateManager()
        {
            Assert.IsTrue(root.StateManager is StateManager);
        }

        [TestMethod] //(expected = UnsupportedOperationException.class)
        [ExpectedException(typeof(NotSupportedException))]
        public void TestGetParent()
        {
            var r = root.Parent;
        }

        [TestMethod]
        public void TestGetId()
        {
            Assert.AreEqual(Root.ID, root.Id);
        }

        [TestMethod]
        public void TestGetContext()
        {
            Assert.AreSame(root, root.GetContext);
        }

        [TestMethod]
        public void TestGetRoot()
        {
            Assert.AreSame(root, root.GetRoot);
        }

        [TestMethod]
        public void TestGetURI()
        {
            Assert.AreSame(Root.ID, root.URI);
        }

        [TestMethod]
        public void TestGetFullURI()
        {
            Assert.AreSame(Root.ID, root.FullURI);
        }

        [TestMethod]
        public void TestLocate()
        {
            // item is local
            Assert.AreSame(item, root.Locate(item.URI));
            Assert.AreSame(item, root.Locate(item.FullURI));
            // manager is local
            Assert.AreSame(manager, root.Locate(manager.URI));
            Assert.AreSame(manager, root.Locate(manager.FullURI));
            // anotherItem is not local
            Assert.IsNull(root.Locate(anotherItem.URI));
            Assert.AreSame(root.Locate(anotherItem.FullURI), anotherItem);
            // root is local in root
            Assert.AreSame(root, root.Locate(root.URI));
            Assert.AreSame(root, root.Locate(root.FullURI));
            // and if item is removed it is not found anymore
            root.RemoveItem(item);
            Assert.IsNull(root.Locate(item.URI));
            Assert.IsNull(root.Locate(item.FullURI));
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void TestAddItemFail()
        {
            root.AddItem(item);
        }

        public void TestAddItemSuccess()
        {
            root.RemoveItem(item);
            root.AddItem(item);
            Assert.AreSame(item, root.Locate(item.FullURI));
        }

        public void TestRemoveItemSuccess()
        {
            root.RemoveItem(item);
            root.Locate(item.FullURI);
        }

        [TestMethod]
        [ExpectedException(typeof(ArgumentException))]
        public void TestRemoveItemFail()
        {
            root.RemoveItem(item);
            root.RemoveItem(item);
        }
    }
}

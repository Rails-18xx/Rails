using GameLib.Net.Game.State;
using Microsoft.VisualStudio.TestTools.UnitTesting;
using Moq;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameStateTest
{
    [TestClass]
    public class ObservableTest
    {
        // Implementation for Testing only
        private class ObservableImpl : Observable
        {
            public ObservableImpl(IItem parent, string id) : base(parent, id)
            {

            }

            override public string ToText()
            {
                return null;
            }
        }

        private const string MANAGER_ID = "Manager";
        private const string ITEM_ID = "IItem";
        private const string OBS_ID = "Observable";

        private Root root;
        private Manager manager;
        private IItem item;
        private Observable observable;
        private IObserver observer;
        private Model model;

        [TestInitialize]
        public void SetUp()
        {
            root = Root.Create();
            manager = new ManagerImpl(root, MANAGER_ID);
            item = new AbstractItemImpl(manager, ITEM_ID);
            observable = new ObservableImpl(item, OBS_ID);
        }


        [TestMethod]
        public void TestObservers()
        {
            observer = new Mock<IObserver>().Object;
            // add observer and test if contained
            observable.AddObserver(observer);
            Assert.IsTrue(observable.GetObservers().Contains(observer));

            // remove observer and test if not contained
            Assert.IsTrue(observable.RemoveObserver(observer));
            Assert.IsTrue(!(observable.GetObservers().Contains(observer)));

            // remove observer not contained anymore
            Assert.IsFalse(observable.RemoveObserver(observer));
        }

        [TestMethod]
        public void TestModels()
        {
            model = new Mock<Model>(new object[] { root, "model"}).Object;
            // add observer and test if contained
            observable.AddModel(model);
            Assert.IsTrue(observable.GetModels().Contains(model));

            // remove Model and test if not contained
            Assert.IsTrue(observable.RemoveModel(model));
            Assert.IsTrue(!observable.GetModels().Contains(model));

            // remove Model not contained anymore
            Assert.IsFalse(observable.RemoveModel(model));
        }

        [TestMethod]
        public void TestGetId()
        {
            Assert.AreEqual(OBS_ID, observable.Id);
        }

        [TestMethod]
        public void TestGetParent()
        {
            Assert.AreSame(item, observable.Parent);
        }

        [TestMethod]
        public void TestGetContext()
        {
            Assert.AreSame(manager, observable.GetContext);
        }

        [TestMethod]
        public void TestGetRoot()
        {
            Assert.AreSame(root, observable.GetRoot);
        }

        [TestMethod]
        public void TestGetURI()
        {
            Assert.AreEqual(ITEM_ID + IItemConsts.SEP + OBS_ID, observable.URI);
            Assert.AreSame(observable, observable.GetContext.Locate(observable.URI));
        }

        [TestMethod]
        public void TestGetFullURI()
        {
            Assert.AreEqual(IItemConsts.SEP + MANAGER_ID + IItemConsts.SEP + ITEM_ID + IItemConsts.SEP + OBS_ID, observable.FullURI);
            Assert.AreSame(observable, observable.GetContext.Locate(observable.URI));
            Assert.AreSame(observable, observable.GetRoot.Locate(observable.FullURI));
        }
    }
}

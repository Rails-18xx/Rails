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
    public class ModelTest
    {
        private const string MODEL_ID = "Model";
        private const string MODEL_TEXT_INIT = "Init";
        private const string MODEL_TEXT_CHANGE = "Change";

        private Root root;
        private ModelImpl model;
        private IObserver observer;

        [TestInitialize]
        public void SetUp()
        {
            root = StateTestUtils.SetUpRoot();
            model = ModelImpl.Create(root, MODEL_ID, MODEL_TEXT_INIT);
            StateTestUtils.Close(root);
            var mock = new Mock<IObserver>();
            observer = mock.Object;
            model.AddObserver(observer);
        }

        [TestMethod]
        public void TestModel()
        {
            Assert.AreEqual(MODEL_TEXT_INIT, model.ToText());
            model.ChangeText(MODEL_TEXT_CHANGE);
            StateTestUtils.Close(root);
            Assert.AreEqual(MODEL_TEXT_CHANGE, model.ToText());
            //verify(observer).update(MODEL_TEXT_CHANGE);
            Mock.Get(observer).Verify(observer => observer.Update(MODEL_TEXT_CHANGE), Times.Once());
        }
    }
}

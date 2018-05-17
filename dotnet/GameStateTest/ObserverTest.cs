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
    public class ObserverTest
    {
        private const string STATE_ID = "State";

        private Root root;
        private BooleanState state;
        private IObserver observer;

        [TestInitialize]
        public void SetUp()
        {
            observer = Mock.Of<IObserver>();
            root = StateTestUtils.SetUpRoot();
            state = BooleanState.Create(root, STATE_ID, false);
            state.AddObserver(observer);
        }

        [TestMethod]
        public void TestUpdate()
        {
            Assert.IsTrue(state.GetObservers().Contains(observer));
            state.Set(true);
            Mock.Get(observer).Verify(observer => observer.Update(state.ToText()), Times.Never());
            StateTestUtils.Close(root);
            Mock.Get(observer).Verify(observer => observer.Update(state.ToText()), Times.Once());
        }
    }
}

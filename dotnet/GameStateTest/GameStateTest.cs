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
    public class GameStateTest
    {
        private const string STATE_ID = "State";
        private const string MODEL_ID = "Model";

        private Root root;
        private GameState state;
        private GameState state_model;
        private Model model;

        [TestInitialize]
        public void SetUp()
        {
            root = Root.Create();
            state = StateImpl.Create(root, STATE_ID, null);
            model = ModelImpl.Create(root, MODEL_ID, null);
            state_model = StateImpl.Create(model, STATE_ID, null);
        }

        [TestMethod]
        public void TestState()
        {
            // check that model is linked by state_model
            Assert.IsTrue(state_model.GetModels().Contains(model));
            // but for the standard model only after explicit link
            Assert.IsTrue(!state.GetModels().Contains(model));
            state.AddModel(model);
            Assert.IsTrue(state.GetModels().Contains(model));
        }

        [TestMethod]
        public void TestObserverText()
        {
            Assert.IsNull(state.ToText());
        }

    }
}

using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
 * GameState is an abstract generic class
 * that defines the base layer of objects that contain game state.
 * 
 * All State(s) are Item(s) themselves.
 * 
 * It allows to add a Formatter to change the String output dynamically.
 * 
 * 
 * GameStates get register with the StateManager after initialization
 */
namespace GameLib.Net.Game.State
{
    abstract public class GameState : Observable
    {
        protected GameState(IItem parent, string id) : base(parent, id)
        {
            // register if not StateManager itself is the parent
            if (!(parent is StateManager))
            {
                this.StateManager.RegisterState(this);
            }
            // check if parent is a model and add as dependent model
            if (parent is Model)
            {
                AddModel((Model)parent);
            }
        }

        public void InformTriggers(Change change)
        {
            this.StateManager.InformTriggers(this, change);
        }
    }
}

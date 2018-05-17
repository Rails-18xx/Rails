using System;

/**
 * Base Class for all Change Objects
 * 
 * Replaces previous move interface
 */
namespace GameLib.Net.Game.State
{
    abstract public class Change 
    {
        protected void Init(GameState state)
        {
            state.StateManager.ChangeStack.AddChange(this);
        }

        public abstract void Execute();
        public abstract void Undo();
        public abstract GameState GameState { get; }
        public T GetGameState<T>() where T : GameState
        {
            return (T)GameState;
        }
    }
}

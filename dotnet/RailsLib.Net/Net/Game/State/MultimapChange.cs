using System;


namespace GameLib.Net.Game.State
{
    public sealed class MultimapChange<K, V> : Change
    {
        private readonly MultimapState<K, V> state;
        private readonly K key;
        private readonly V value;
        private readonly bool addToMap;

        public MultimapChange(MultimapState<K, V> state, K key, V value, bool addToMap)
        {
            this.state = state;
            this.key = key;
            this.value = value;
            this.addToMap = addToMap;
            base.Init(state);
        }

        public override void Execute()
        {
            state.Change(key, value, addToMap);
        }

        public override void Undo()
        {
            state.Change(key, value, !addToMap);
        }

        //override public MultimapState<K, V> GetState()
        //{
        //    return state;
        //}
        override public GameState GameState
        {
            get
            {
                return state;
            }
        }

        override public string ToString()
        {
            if (addToMap)
            {
                return "Change for " + state + ": Add key = " + key + " with value " + value;
            }
            else
            {
                return "Change for " + state + ": Remove key = " + key + " with value " + value;
            }
        }
    }
}

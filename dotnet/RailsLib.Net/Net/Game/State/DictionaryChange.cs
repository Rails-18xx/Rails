using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
/**
 * Change associated with DictionaryState
 */
namespace GameLib.Net.Game.State
{
    public sealed class DictionaryChange<K, V> : Change
    {
        private MapState<K, V> state;
        private K key;
        private V newValue;
        private bool remove;
        private V oldValue;
        private bool existed;

        /**
         * Put element into map
         */
        public DictionaryChange(MapState<K, V> state, K key, V value)
        {
            this.state = state;
            this.key = key;
            newValue = value;
            remove = false;
            oldValue = state.Get(key);
            existed = state.ContainsKey(key);
            base.Init(state);
        }

        /**
         * Remove element from map
         */
        public DictionaryChange(MapState<K, V> state, K key)
        {
            this.state = state;
            this.key = key;
            newValue = default(V);
            remove = true;
            oldValue = state.Get(key);
            existed = true;
            base.Init(state);
        }

        override public void Execute()
        {
            state.Change(key, newValue, remove);
        }

        override public void Undo()
        {
            state.Change(key, oldValue, !existed);
        }

        override public /*MapState<K, V>*/ GameState GameState
        {
            get
            {
                return state;
            }
        }

        override public string ToString()
        {
            if (!remove)
            {
                if (existed)
                {
                    return "Change for " + state + ": For key=" + key + " replace value " + oldValue + " by " + newValue;
                }
                else
                {
                    return "Change for " + state + ": Add key=" + key + " with value " + newValue;
                }
            }
            else
            {
                return "Change for " + state + ": Remove key=" + key + " with value " + newValue;
            }
        }

    }
}

using System;

/**
 * Generic State wrapper
 * @author freystef
 *
 * @param <E> class to wrap
 */
namespace GameLib.Net.Game.State
{
    public sealed class GenericState<T> : GameState
    {
        private T item;

        private GenericState(IItem parent, string id, T o) : base(parent, id)
        {
            this.item = o;
        }

        /** 
         * {@inheritDoc}
         * Creates an empty GenericState
         */
        public static GenericState<T> Create(IItem parent, string id)
        {
            return new GenericState<T>(parent, id, default(T));
        }

        /**
         * @param object initial object contained
         */
        public static GenericState<T> Create(IItem parent, string id, T o)
        {
            return new GenericState<T>(parent, id, o);
        }

        public void Set(T o)
        {
            if (o == null)
            {
                if (this.item != null)
                {
                    new GenericStateChange<T>(this, o);
                }
            }
            else if (!o.Equals(item))
            {
                new GenericStateChange<T>(this, o);
            }
        }

        public T Value
        {
            get
            {
                return this.item;
            }
        }

        /**
         * For observable objects it returns toText(), for others toString()
         * If GenericState is set to null returns empty string
         */
        override public string ToText()
        {
            if (item == null)
            {
                return "";
            }

            // can't use pattern matching (item is Observable o) in C# 7.0, it's an error for generics
            Observable o = item as Observable;
            if (o != null)
            {
                return o.ToText();
            }
            else
            {
                return item.ToString();
            }
        }

        public void Change(T o)
        {
            this.item = o;
        }
    }
}

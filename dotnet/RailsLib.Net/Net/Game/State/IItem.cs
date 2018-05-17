using System;


/**
 * An item is defined by two (final) attributes:
 * 
 * Id: A string identifier, which should be unique inside the context used.
 * Parent: The parent of the item in the item hierarchy
 * 
 * 
 * Implied attributes are
 * Context: The nearest context in the item hierarchy
 * URI: From the nearest context
 * FullURI: From the root context
 */
namespace GameLib.Net.Game.State
{
    public class IItemConsts
    {
        public static char SEP = '/';
    }
    public interface IItem
    {
        string Id { get; }

        IItem Parent { get; }

        Context GetContext { get; }

        Root GetRoot { get; }

        /** 
         * @return a string which allows to identify the item in the Context
         */
        string URI { get; }

        /**
         * @return a string which allows to locate the item from the Root
         */
        string FullURI { get; }

        /**
         * @return a string used for display
         */
        string ToText();
    }

    public interface IItem<out T, out U> : IItem
    {
        new T Parent { get; }
        new U GetRoot { get; }
    }
}

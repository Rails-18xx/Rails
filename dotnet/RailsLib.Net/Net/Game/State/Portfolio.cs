using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.State
{
    public interface IPortfolio
    {
        Type PortfolioType { get; }
        IOwner PortfolioOwner { get; }
    }

    public static class Portfolio
    {
        /**
        * Moves all items of an iterable object to a new owner
        * @param newOwner
        */
        public static void MoveAll<U>(IEnumerable<U> items, IOwner newOwner)
            where U : IOwnable
        {
            foreach (U item in items)
            {
                item.MoveTo(newOwner);
            }
        }

        /**
         * Moves all items of a specific type from one owner to the other
         */
        public static void MoveAll<U>(IOwner owner, IOwner newOwner)
            where U : IOwnable
        {
            // get the portfolio
            Portfolio<U> pf = owner.GetRoot.StateManager.PortfolioManager.GetPortfolio<U>(newOwner);
            // and move items
            pf.MoveAll(newOwner);
        }
    }

    abstract public class Portfolio<T> : Model, IPortfolio, IEnumerable<T>
        where T : IOwnable
    {
        private Type type;

        /**
         * Creation of a portfolio
         * @param parent owner of the portfolio
         * @param id identifier of the portfolio
         * @param type type of items stored in the portfolio
         */
        protected Portfolio(IOwner parent, string id) : base(parent, id)
        {
            this.type = typeof(T);
            PortfolioManager.AddPortfolio<T>(this);
        }
        /**
         * @return the owner of the portfolio
         */
        new public IOwner Parent
        {
            get
            {
                return (IOwner)base.Parent;
            }
        }

        public Type PortfolioType
        {
            get
            {
                return type;
            }
        }

        // delayed due to initialization issues
        // TODO: Check is this still true?
        protected PortfolioManager PortfolioManager
        {
            get
            {
                return StateManager.PortfolioManager;
            }
        }

        /**
         * Add a new item to the portfolio and removes the item 
         * from the previous containing portfolio
         * 
         * @param item to add to the portfolio
         * @return false if the portfolio already contains the item, otherwise true
         */
        public abstract bool Add(T item);

        /**
         * @param item that is checked if it is in the portfolio
         * @return true if contained, false otherwise
         */
        public abstract bool ContainsItem(T item);

        /**
         * @return all items contained in the portfolio
         */
        public abstract IReadOnlyCollection<T> Items { get; }

        /**
         * @return size of portfolio
         */
        public abstract int Count { get; }

        /**
         * @return true if portfolio is empty
         */
        public abstract bool IsEmpty { get; }

        public IOwner PortfolioOwner
        {
            get
            {
                return Parent;
            }
        }

        public abstract void Include(T item);

        public abstract void Exclude(T item);

        /**
         * Moves all items of the portfolio to the new owner
         * @param newOwner
         */
        public void MoveAll(IOwner newOwner)
        {
            foreach (T item in Items)
            {
                item.MoveTo(newOwner);
            }
        }

        /**
         * Moves all items of an iterable object to a new owner
         * @param newOwner
         */
        public static void MoveAll<U>(IEnumerable<U> items, IOwner newOwner)
            where U : IOwnable
        {
            Portfolio.MoveAll(items, newOwner);
        }

        /**
         * Moves all items of a specific type from one owner to the other
         */
        public static void MoveAll<U>(IOwner owner, IOwner newOwner)
            where U : IOwnable
        {
            Portfolio.MoveAll<U>(owner, newOwner);
        }

        public IEnumerator<T> GetEnumerator()
        {
            return new List<T>(Items).GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return new List<T>(Items).GetEnumerator();
        }
    }
}

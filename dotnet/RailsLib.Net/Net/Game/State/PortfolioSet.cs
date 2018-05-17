using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

/**
 * PortfolioSet is an implementation of a Portfolio that is based on a SortedSet (TreeSet)

 * @param <T> the type of Ownable (items) stored inside the portfolio
 */

namespace GameLib.Net.Game.State
{
    public class PortfolioSet<T> : Portfolio<T> where T : IOwnable
    {
        private SortedSetState<T> portfolio;

        private PortfolioSet(IOwner parent, string id) : base(parent, id)
        {
            portfolio = SortedSetState<T>.Create(this, "set");
        }

        public static PortfolioSet<T> Create(IOwner parent, string id)
        {
            return new PortfolioSet<T>(parent, id);
        }

        override public bool Add(T item)
        {
            if (portfolio.Contains(item)) return false;
            item.MoveTo(Parent);
            return true;
        }

        override public bool ContainsItem(T item)
        {
            return portfolio.Contains(item);
        }

        override public IReadOnlyCollection<T> Items
        {
            get
            {
                return new ReadOnlyCollection<T>(new List<T>(portfolio)); //ImmutableSortedSet.copyOf(portfolio);
            }
        }

        override public int Count
        {
            get
            {
                return portfolio.Count;
            }
        }

        override public bool IsEmpty
        {
            get
            {
                return portfolio.IsEmpty;
            }
        }

        //public Iterator<T> iterator()
        //{
        //    return ImmutableSet.copyOf(portfolio).iterator();
        //}

        override public string ToText()
        {
            return portfolio.ToString();
        }

        override public void Include(T item)
        {
            portfolio.Add(item);
        }

        override public void Exclude(T item)
        {
            portfolio.Remove(item);
        }
    }
}

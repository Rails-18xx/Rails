using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

/**
 * PortfolioMap is an implementation of a portfolio based on a SortedMultimap
 *
 * @param <K> type of the keys that are used to structure the portfolio
 * @param <T> type of Ownable (items) stored inside the portfolio
 * Remark: T has to extend Typable<K> to inform the portfolio about its type 
 */

namespace GameLib.Net.Game.State
{
    public class PortfolioMap<K, T> : Portfolio<T>
        where K : IComparable
        where T : IOwnable, IComparable, ITypable<K>
    {
        private OrderedMultimapState<K, T> portfolio;// = OrderedMultimapState<K, T>.Create(this, "map");

        private PortfolioMap(IOwner parent, string id) : base(parent, id)
        {
            //super(parent, id, type);
            portfolio = OrderedMultimapState<K, T>.Create(this, "map");
        }

        public static PortfolioMap<K, T> Create(IOwner parent, string id)//, Class<T> type)
        {
            return new PortfolioMap<K, T>(parent, id);//, type);
        }

     override public bool Add(T item)
        {
            if (portfolio.ContainsValue(item)) return false;
            item.MoveTo(Parent);
            return true;
        }

        
    override public bool ContainsItem(T item)
        {
            return portfolio.ContainsValue(item);
        }

        
    override public IReadOnlyCollection<T> Items
        {
            get
            {
                //return ImmutableSortedSet.copyOf(portfolio.values());
                return portfolio.Values();
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
                return portfolio.Count == 0;
            }
        }

        /**
         * @param key that is checked if there are items stored for
         * @return true if there a items stored under that key, false otherwise
         */
        public bool ContainsKey(K key)
        {
            return portfolio.ContainsKey(key);
        }

        /**
         * Returns the set of keys, each appearing once 
         * @return collection of distinct keys
         */
        public IReadOnlyCollection<K> Keys
        {
            get
            {
                //return ImmutableSortedSet.copyOf(portfolio.keySet());
                return portfolio.KeySet();
            }
        }

        /**
         * @param key that defines the specific for which the portfolio members get returned
         * @return all items for the key contained in the portfolio
         */
        public IReadOnlyCollection<T> GetItems(K key)
        {
            return (IReadOnlyCollection<T>)portfolio.Get(key); //new ReadOnlyCollection<T>(portfolio[key]);
        }

        /**
         * @return a SetMultimap view of the Portfolio
         */
        public Wintellect.PowerCollections.MultiDictionaryBase<K, T> View()
        {
            return portfolio.View();
        }

        //public IEnumerator<T> GetEnumerator()
        //{
        //    return portfolio.Values.GetEnumerator(); //ImmutableSet.copyOf(portfolio.values()).iterator();
        //}

        override public string ToText()
        {
            return portfolio.ToString();
        }

        override public void Include(T item)
        {
            portfolio.Put(item.SpecificType, item);
        }

        override public void Exclude(T item)
        {
            portfolio.Remove(item.SpecificType, item);
        }
    }
}

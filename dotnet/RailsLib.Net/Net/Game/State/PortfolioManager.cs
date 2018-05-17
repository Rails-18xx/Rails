using GameLib.Net.Util;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * PortfolioManager stores links to all existing portfolios
 */

namespace GameLib.Net.Game.State
{
    public class PortfolioManager : Manager
    {
        public class PMKey
        {
            private Type type;
            private IOwner owner;

            public PMKey(IPortfolio p)
            {
                this.type = p.PortfolioType;
                this.owner = p.PortfolioOwner;
            }

            public PMKey(Type t, IOwner owner)
            {
                this.type = t;
                this.owner = owner;
            }

            override public bool Equals(object other)
            {
                if (!(other is PMKey)) return false;
                PMKey otherKey = (PMKey)other;
                if (otherKey == null) return false;

                return type.Equals(otherKey.type) && owner.Equals(otherKey.owner);
            }

            override public int GetHashCode()
            {
                return type.GetHashCode() ^ owner.GetHashCode();
            }

            override public string ToString()
            {
                //return Objects.toStringHelper(this).add("Type", type).add("Owner", owner).toString();
                return $"{this.GetType().Name}{{Type={type}}}{{Owner={owner}}}";
            }

        }

        private DictionaryState<PMKey, IPortfolio> portfolios;

        private UnknownOwner unknown;


        private PortfolioManager(IItem parent, string id) : base(parent, id)
        {
            portfolios = DictionaryState<PMKey, IPortfolio>.Create(this, "portfolios");
            unknown = UnknownOwner.Create(this, "unknown");
        }

        public static PortfolioManager Create(StateManager parent, String id)
        {
            return new PortfolioManager(parent, id);
        }

        public UnknownOwner UnknownOwner
        {
            get
            {
                return unknown;
            }
        }

        /**
         * @param portfolio to add
         * @throws IllegalArgumentException if a portfolio of that type is already added
         */
        public void AddPortfolio<T>(Portfolio<T> portfolio) where T : IOwnable
        {
            PMKey key = new PMKey(portfolio);
            Precondition.CheckArgument(!portfolios.ContainsKey(key), "A portfolio of that type is defined for that owner already");
            portfolios.Put(key, portfolio);
        }

        /**
         * @param portfolio to remove
         */

        public void RemovePortfolio<T>(Portfolio<T> p) where T : IOwnable
        {
            portfolios.Remove(new PMKey(p));
        }

        /**
         * Returns the Portfolio that stores items of specified type for the specified owner
         * @param type class of items stored in portfolio
         * @param owner owner of the portfolio requested
         * @return portfolio for type/owner combination (null if none is available)
         */
        public Portfolio<T> GetPortfolio<T>(IOwner owner) where T : IOwnable
        {
            return (Portfolio<T>)portfolios.Get(new PMKey(typeof(T), owner));
        }

        // backdoor for testing
        public PMKey CreatePMKey<T>(IOwner owner) where T : IOwnable
        {
            return new PMKey(typeof(T), owner);
        }
    }
}

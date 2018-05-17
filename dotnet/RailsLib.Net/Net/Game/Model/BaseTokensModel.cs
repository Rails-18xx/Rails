using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;

/**
 * A model presenting the number of tokens
 */

namespace GameLib.Net.Game.Model
{
    public class BaseTokensModel : RailsModel
    {
        // the free tokens belong to the company
        private PortfolioSet<BaseToken> freeBaseTokens;
        // a list of all base tokens, configured later
        private SortedSet<BaseToken> allTokens;

        private BaseTokensModel(PublicCompany parent, string id) : base(parent, id)
        {
            freeBaseTokens = PortfolioSet<BaseToken>.Create(parent, "freeBaseTokens");
            freeBaseTokens.AddModel(this);
        }

        public static BaseTokensModel Create(PublicCompany parent, string id)
        {
            return new BaseTokensModel(parent, id);
        }

        /**
         * Initialize a set of tokens
         */
        public void InitTokens(IEnumerable<BaseToken> tokens)
        {
            allTokens = new SortedSet<BaseToken>(tokens);// ImmutableSortedSet.copyOf(tokens);
            Portfolio.MoveAll(allTokens, Parent);
        }

        /**
         * @return parent the public company
         */
        new public PublicCompany Parent
        {
            get
            {
                return (PublicCompany)base.Parent;
            }
        }

        /**
         * @return the next (free) token to lay, null if none is available
         */
        public BaseToken GetNextToken()
        {
            if (freeBaseTokens.Count == 0) return null;
            var i = freeBaseTokens.GetEnumerator();
            i.MoveNext();
            return i.Current;
            //return Iterables.get(freeBaseTokens, 0);
        }

        public IReadOnlyCollection<BaseToken> GetAllTokens()
        {
            return allTokens;
        }

        public IReadOnlyCollection<BaseToken> GetFreeTokens()
        {
            return freeBaseTokens.Items;
        }

        public IReadOnlyCollection<BaseToken> GetLaidTokens()
        {
            var diff = new SortedSet<BaseToken>(allTokens);
            diff.ExceptWith(freeBaseTokens.Items);
            return diff;
            //return Sets.difference(allTokens, freeBaseTokens.items()).immutableCopy();
        }

        public int NbAllTokens
        {
            get
            {
                return allTokens.Count;
            }
        }

        public int NbFreeTokens
        {
            get
            {
                return freeBaseTokens.Count;
            }
        }

        public int NbLaidTokens
        {
            get
            {
                return allTokens.Count - freeBaseTokens.Count;
            }
        }

        /**
         * @return true if token is laid
         */
        public bool TokenIsLaid(BaseToken token)
        {
            return allTokens.Contains(token);
        }

        override public string ToText()
        {
            int allTokens = NbAllTokens;
            int freeTokens = NbFreeTokens;
            if (allTokens == 0)
            {
                return "";
            }
            else if (freeTokens == 0)
            {
                return "-/" + allTokens;
            }
            else
            {
                return freeTokens + "/" + allTokens;
            }
        }
    }
}

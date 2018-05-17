using GameLib.Net.Game;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Util
{
    public static class SequenceUtil
    {
        private static List<T> SelectMoneyOwners<T>(IEnumerable<IMoneyOwner> coll) where T : IMoneyOwner
        {

            // select all cashholders of that type
            List<T> list = new List<T>();

            foreach (IMoneyOwner c in coll)
            {
                if (typeof(T).IsAssignableFrom(c.GetType()))
                {
                    T cast = (T)c;
                    list.Add(cast);
                }
            }

            return list;
        }

        /**
         * Defines a sorting on cashHolders
         * @return sorted list of cashholders
         */
        public static List<IMoneyOwner> SortCashHolders(ICollection<IMoneyOwner> coll)
        {
            List<IMoneyOwner> sortedList = new List<IMoneyOwner>();

            // first add players
            List<Player> players = SelectMoneyOwners<Player>(coll);
            players.Sort();
            sortedList.AddRange(players);

            // then public companies
            List<PublicCompany> PublicCompanys = SelectMoneyOwners<PublicCompany>(coll);
            PublicCompanys.Sort(CompanyComparer.COMPANY_COMPARATOR);
            sortedList.AddRange(PublicCompanys);

            // last add the bank
            sortedList.AddRange(SelectMoneyOwners<Bank>(coll));

            return sortedList;
        }
    }
}

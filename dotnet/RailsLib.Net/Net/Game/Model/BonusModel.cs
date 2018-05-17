using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class BonusModel : RailsModel
    {
        private ListState<Bonus> bonuses;

        protected BonusModel(IRailsItem parent, string id) : base(parent, id)
        {

        }

        public static BonusModel Create(IRailsItem parent, string id)
        {
            return new BonusModel(parent, id);
        }

        //public IRailsItem getParent()
        //{
        //    return (RailsItem)super.getParent();
        //}

        public void SetBonuses(ListState<Bonus> bonuses)
        {
            this.bonuses = bonuses;
            bonuses.AddModel(this);
        }

        override public string ToText()
        {
            if (bonuses == null || bonuses.IsEmpty) return "";

            StringBuilder b = new StringBuilder("<html><center>");

            foreach (Bonus bonus in bonuses.View())
            {
                if (b.Length > 14)
                {
                    b.Append("<br>");
                }
                b.Append(bonus.GetIdForView()).Append("+").Append(Bank.Format(Parent, bonus.Value));
            }

            return b.ToString();
        }
    }
}

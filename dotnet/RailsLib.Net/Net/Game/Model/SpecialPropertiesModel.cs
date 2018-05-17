using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class SpecialPropertiesModel : RailsModel, ITriggerable
    {
        public const string ID = "SpecialPropertiesModel";

        private PortfolioSet<SpecialProperty> specialProperties;

        private SpecialPropertiesModel(IRailsOwner parent) : base(parent, ID)
        {
            // specialProperties have the Owner as parent directly
            specialProperties = PortfolioSet<SpecialProperty>.Create(parent, "specialProperties");
        // so make this model updating
        specialProperties.AddModel(this);
        // and add it as triggerable
        specialProperties.AddTrigger(this);
        }

        public static SpecialPropertiesModel Create(IRailsOwner parent)
        {
            return new SpecialPropertiesModel(parent);
        }

        new public IRailsOwner Parent
        {
            get
            {
                return (IRailsOwner)base.Parent;
            }
        }

        public PortfolioSet<SpecialProperty> Portfolio
        {
            get
            {
                return specialProperties;
            }
        }

        // triggerable interface

        public void Triggered(Observable observable, Change change)
        {

            // checks if the specialproperty moved into the portfolio carries a LocatedBonus

            if (!(change is SetChange<SpecialProperty>)) 
                {
                return;
            }

            //@SuppressWarnings("rawtypes")
            SetChange<SpecialProperty> sChange = change as SetChange<SpecialProperty>;
            if (!sChange.IsAddToSet) return;

            SpecialProperty property = sChange.Element;
            if ((Parent is PublicCompany) && (property is LocatedBonus)) 
                {
                PublicCompany company = (PublicCompany)Parent;
                LocatedBonus locBonus = (LocatedBonus)property;
                Bonus bonus = new Bonus(company, locBonus.Id, locBonus.Value, locBonus.Locations);
                company.AddBonus(bonus);
                ReportBuffer.Add(this, LocalText.GetText("AcquiresBonus",
                        Parent.Id,
                        locBonus.Name,
                        Bank.Format(company, locBonus.Value),
                        locBonus.LocationNameString));
            }
        }
    }
}

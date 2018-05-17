using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * RightsModel stores purchased SpecialRight(s)
 *
 */

namespace GameLib.Net.Game.Model
{
    public class RightsModel : RailsModel
    {
        private HashSetState<SpecialRight> rights;

        private RightsModel(PublicCompany parent, string id) : base(parent, id)
        {
            rights = HashSetState<SpecialRight>.Create(this, "rightsModel");
        }

        public static RightsModel Create(PublicCompany parent, string id)
        {
            return new RightsModel(parent, id);
        }

        new public PublicCompany Parent
        {
            get
            {
                return (PublicCompany)Parent;
            }
        }

        public void Add(SpecialRight right)
        {
            rights.Add(right);
        }

        public bool Contains(SpecialRight right)
        {
            return rights.Contains(right);
        }

        override public String ToText()
        {
            List<String> rightsText = new List<string>();
            foreach (SpecialRight right in rights)
            {
                rightsText.Add(right.Name);
            }
            return string.Join(",", rightsText); //Joiner.on(",").join(rightsText.build()).toString();
        }
    }
}

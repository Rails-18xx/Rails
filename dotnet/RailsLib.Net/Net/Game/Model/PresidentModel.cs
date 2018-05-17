using System;
using System.Collections.Generic;
using System.Text;

/**
 * model object for the current company president
 * gets registered by the ShareModels
 * 
 * FIXME: Finalize implementation, this does not work currently
 * TODO: Check if this is all done correctly, where is the observable stored?
 */

namespace GameLib.Net.Game.Model
{
    public class PresidentModel : RailsModel
    {
        public const string ID = "PresidentModel";

        private PresidentModel(PublicCompany parent, string id) : base(parent, id)
        {

        }

        public static PresidentModel Create(PublicCompany parent)
        {
            return new PresidentModel(parent, ID);
        }

        /**
         * @return restricted to PublicCompany
         */
        new public PublicCompany Parent
        {
            get
            {
                return (PublicCompany)base.Parent;
            }
        }

        override public string ToText()
        {
            Player president = Parent.GetPresident();
            if (president == null) return "";
            else return Parent.GetPresident().GetNameAndPriority();
        }
    }
}

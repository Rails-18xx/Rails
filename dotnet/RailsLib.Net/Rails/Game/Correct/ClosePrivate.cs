using GameLib.Net.Game;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * Action that allows manual closure of a private company
 *
 * Rails 2.0: Updated equals and toString methods
*/

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class ClosePrivate : PossibleAction
    {
        new private const long serialVersionUID = 2L;

        /* Preconditions */

        /** private company to close */
        /*transient*/
        [JsonIgnore]
        private PrivateCompany privateCompany;

        /** converted to name */
        private string privateCompanyName;

        /* Postconditions: None */

        public ClosePrivate(PrivateCompany priv) : base(null)
        {
            privateCompany = priv;
            privateCompanyName = priv.Id;
        }

        public PrivateCompany PrivateCompany
        {
            get
            {
                return privateCompany;
            }
        }
        public string PrivateCompanyName
        {
            get
            {
                return privateCompanyName;
            }
        }

        public string GetInfo()
        {
            return ("Close Private " + privateCompanyName);
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            ClosePrivate action = (ClosePrivate)pa;
            return privateCompany.Equals(action.privateCompany);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("privateCompany", privateCompany)
                        .ToString()
            ;
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (!string.IsNullOrEmpty(privateCompanyName))
            {
                privateCompany = CompanyManager.GetPrivateCompany(privateCompanyName);
            }
        }
    }
}

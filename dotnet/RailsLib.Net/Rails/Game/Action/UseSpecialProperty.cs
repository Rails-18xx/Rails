using GameLib.Net.Game.Special;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * This class can only be used to offer a Special Property to the UI that does
 * NOT need any return parameters. Example: the M&H/NYC swap in 1830.
 * 
 * Rails 2.0: Updated equals and toString methods
 */
namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class UseSpecialProperty : PossibleORAction
    {
        /*--- Preconditions ---*/

        /** The special property that could be used */
        /*transient*/
        [JsonIgnore]
        protected SpecialProperty specialProperty = null;
        private int specialPropertyId;

        /*--- Postconditions ---*/

        public UseSpecialProperty(SpecialProperty specialProperty) : base()
        {
            this.specialProperty = specialProperty;
            if (specialProperty != null)
                this.specialPropertyId = specialProperty.UniqueId;
        }

        new public const long serialVersionUID = 1L;

        /**
         * @return Returns the specialProperty.
         */
        public SpecialProperty SpecialProperty
        {
            get
            {
                return specialProperty;
            }
        }

        override public string ToMenu()
        {
            return specialProperty.ToMenu();
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            UseSpecialProperty action = (UseSpecialProperty)pa;
            return specialProperty.Equals(action.specialProperty);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("specialProperty", specialProperty)
                        .ToString();
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (specialPropertyId > 0)
            {
                specialProperty = SpecialProperty.GetByUniqueId(GetRoot, specialPropertyId);
            }
        }
    }
}

using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Base class for all actions that correct the state of the game. 
 * 
 * Rails 2.0: updated equals and toString methods
 */

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class CorrectionAction : PossibleAction
    {
        /*transient*/
        [JsonIgnore]
        protected CorrectionType correctionType;
        protected String correctionName;

        new public const long serialVersionUID = 3L;

        public CorrectionAction() : base(null)
        {
        }

        public CorrectionType CorrectionType
        {
            get
            {
                return correctionType;
            }
        }

        public String CorrectionName
        {
            get
            {
                return correctionName;
            }
        }

        public void SetCorrectionType(CorrectionType correctionType)
        {
            this.correctionType = correctionType;
            this.correctionName = correctionType.Name;
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            CorrectionAction action = (CorrectionAction)pa;
            return correctionType.Equals(action.correctionType);
            // no action attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("correctionType", correctionType)
                    .ToString();
        }
    }
}

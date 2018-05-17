using GameLib.Net.Common;
using GameLib.Net.Util;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Runtime.Serialization;
using System.Text;

/**
 * Action class to request specific correction actions
 *
 * Rails 2.0: updated equals and toString methods
 */

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class CorrectionModeAction : CorrectionAction
    {
        new public const long serialVersionUID = 1L;

        // pre-conditions:  state
        protected bool active;

        // post-conditions: none (except isActed!)

        /** 
         * Initializes with all possible correction types
         */
        public CorrectionModeAction(CorrectionType correction, bool active)
        {
            this.correctionType = correction;
            correctionName = correction.Name;
            this.active = active;
        }

        public bool IsActive
        {
            get
            {
                return active;
            }
        }

        public string GetInfo()
        {
            return (LocalText.GetText(correctionName));
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // FIXME: Always allow the actions of the according type as Option
            if (asOption && (pa is CorrectionAction) && ((CorrectionAction)pa).CorrectionType == correctionType)
            {
                return true;
            }

            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            CorrectionModeAction action = (CorrectionModeAction)pa;

            // #FIXME does this work?
            return active.Equals(action.active);
            // no action attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("active", active)
                    .ToString()
            ;
        }

        /** Deserialize */
        [OnDeserialized]
        new internal void OnDeserialized(StreamingContext context)
        {
            if (!string.IsNullOrEmpty(correctionName))
            {
                correctionType = CorrectionType.ValueOf(correctionName);
            }
        }
    }
}

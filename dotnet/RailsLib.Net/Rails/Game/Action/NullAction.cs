using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Runtime.Serialization;
using System.Text;
using System.Threading.Tasks;
/**
 * 
 * Rails 2.0: Updated equals and toString methods
 */
namespace GameLib.Rails.Game.Action
{
    public class NullAction : PossibleAction
    {
#pragma warning disable 414
        new private static long serialVersionUID = 2L;
#pragma warning restore 414

        public enum Modes { DONE, PASS, SKIP, AUTOPASS, START_GAME }

        // optional label that is returned on toString instead of the standard labels defined below
        private string optionalLabel = null;
        /*transient*/ // purposefully not transient in .net
        protected Modes mode_enum;
        // Remark: it would have been better to store the enum name, however due to backward compatibility not an option
        // .net: we serialize the enum
        //protected int mode;

        public NullAction(Modes mode) : base(null)
        {
            //super(null); // not defined by an activity yet
            this.mode_enum = mode;
            //this.mode = mode.ordinal();
        }

        [JsonIgnore]
        public Modes Mode
        {
            get
            {
                return mode_enum;
            }
        }

        /** returns the NullAction itself */
        public NullAction SetLabel(string label)
        {
            this.optionalLabel = label;
            return this;
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            NullAction action = (NullAction)pa;
            return this.mode_enum.Equals(action.mode_enum)
                    && this.optionalLabel.Equals(action.optionalLabel);
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("mode", mode_enum)
                        .AddToString("optionalLabel", optionalLabel)
                        .ToString();
        }

        // serialized directly
        //[OnDeserialized]
        //internal void OnDeserialized(StreamingContext context)
        //{
        //    // required since Rails 2.0
        //    mode_enum = Mode.Values[mode];
        //}

    }
}

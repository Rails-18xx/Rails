using System;
using System.Collections.Generic;
using System.Text;

/**
 * A stateful version of a string variable

 */
namespace GameLib.Net.Game.State
{
    public class StringState : GameState
    {
        private string value;

        private StringState(IItem parent, string id, string value) : base(parent, id)
        {
            this.value = value;
        }

        /** 
         * Creates a StringState with default value of null
         */
        public static StringState Create(IItem parent, string id)
        {
            return new StringState(parent, id, null);
        }

        /**
         * @param text initial string
         */
        public static StringState Create(IItem parent, string id, string text)
        {
            return new StringState(parent, id, text);
        }

        public void Set(string value)
        {
            if (value == null)
            {
                if (this.value != null)
                {
                    new StringChange(this, value);
                } // otherwise both are null
            }
            else
            {
                if (this.value == null || !value.Equals(this.value))
                {
                    new StringChange(this, value);
                } // otherwise the non-null current value is unequal to the new value  
            }
        }

        /**
         * Append string to string state
         * No change is created if value to append is null or empty ("")
         * 
         * @param value string to append
         * @param delimiter to use before appending (only for non-empty value)
         */
        public void Append(string value, string delimiter)
        {
            if (string.IsNullOrEmpty(value)) return;

            string newValue;
            if (string.IsNullOrEmpty(this.value))
            {
                newValue = value;
            }
            else
            {
                if (delimiter == null)
                {
                    newValue = this.value + value;
                }
                else
                {
                    newValue = this.value + delimiter + value;
                }
            }
            Set(newValue);
        }

        /**
         * @return current value of string state
         */
        public string Value
        {
            get
            {
                return value;
            }
        }

        override public string ToText()
        {
            return value;
        }

        public void Change(string value)
        {
            this.value = value;
        }
    }
}

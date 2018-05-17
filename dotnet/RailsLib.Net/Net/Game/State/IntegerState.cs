using System;

/**
 * A stateful version of an integer variable
 */

namespace GameLib.Net.Game.State
{
    public class IntegerState : GameState
    {
        private int value;

        private IntegerState(IItem parent, string id, int value) : base(parent, id)
        {
            this.value = value;
        }

        /** 
         * Creates an IntegerState with default value of Zero
         */
        public static IntegerState Create(IItem parent, string id)
        {
            return new IntegerState(parent, id, 0);
        }

        /**
         * @param value initial value
         */
        public static IntegerState Create(IItem parent, string id, int value)
        {
            return new IntegerState(parent, id, value);
        }

        public void Set(int value)
        {
            if (value != this.value) new IntegerChange(this, value);
        }

        public int Add(int value)
        {
            int newValue = this.value + value;
            Set(this.value + value);
            return newValue;
        }

        public int Value
        {
            get
            {
                return value;
            }
        }

        override public string ToText()
        {
            return value.ToString();
        }

        public void Change(int value)
        {
            this.value = value;
        }
    }
}

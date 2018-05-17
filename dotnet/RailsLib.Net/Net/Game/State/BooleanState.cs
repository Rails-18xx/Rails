using System;

/**
 * A stateful version of a bool variable
 */
namespace GameLib.Net.Game.State
{
    public class BooleanState : GameState
    {
        private bool value;

        private BooleanState(IItem parent, string id, bool value) : base(parent, id)
        {
            this.value = value;
        }

        /** 
         * Creates a BooleanState with default value false
         */
        public static BooleanState Create(IItem parent, string id)
        {
            return new BooleanState(parent, id, false);
        }

        /**
         * Creates a BooleanState with defined initial value
         * @param value initial value
         */
        public static BooleanState Create(IItem parent, string id, Boolean value)
        {
            return new BooleanState(parent, id, value);
        }

        /**
         * @param value set state to this value
         */
        public void Set(bool value)
        {
            if (value != this.value) new BooleanChange(this, value);
        }

        /**
         * @return current value of state variable
         */
        public bool Value
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

        public void Change(bool value)
        {
            this.value = value;
        }
    }
}

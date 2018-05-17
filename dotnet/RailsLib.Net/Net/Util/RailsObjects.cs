using GameLib.Net.Game;
using GameLib.Rails.Game.Action;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Util
{
    public static class RailsObjects
    {
        public class StringHelper
        {
            private StringBuilder text = new StringBuilder();

            private StringHelper()
            {
                // do nothing => empty
            }

            private bool TestIfNull(object test, string replace)
            {
                if (test == null)
                {
                    text.Append(replace);
                    return true;
                }
                return false;
            }

            public StringHelper(IRailsItem item)
            {
                text.Append(item.Id + "(" + item.GetType().Name + ")");
            }

            public StringHelper AddToString(string name, object value)
            {
                if (TestIfNull(value, "NULL")) return this;

                text.Append(name + " = " + value.ToString());
                return this;
            }

            public StringHelper AddToText(string name, IRailsItem value)
            {
                if (TestIfNull(value, "NULL")) return this;

                text.Append(name + " = " + value.ToText());
                return this;
            }

            public StringHelper AddId(string name, IRailsItem value)
            {
                if (TestIfNull(value, "NULL")) return this;

                text.Append(name + " = " + value.Id);
                return this;
            }

            public StringHelper AddURI(string name, IRailsItem value)
            {
                if (TestIfNull(value, "NULL")) return this;

                text.Append(name + " = " + value.URI);
                return this;
            }

            public StringHelper AddFullURI(string name, IRailsItem value)
            {
                if (TestIfNull(value, "NULL")) return this;

                text.Append(name + " = " + value.FullURI);
                return this;
            }

            public StringHelper Append(string text)
            {
                if (TestIfNull(text, "")) return this;

                this.text.Append(text);
                return this;
            }

            override public string ToString()
            {
                return text.ToString();
            }
        }

        public class StringHelperForActions
        {

            private StringBuilder text = new StringBuilder();
            private PossibleAction action;
        
        public StringHelperForActions(PossibleAction action)
            {
                this.action = action;
            }

            public StringHelperForActions AddBaseText()
            {
                text.Append(action.Player.Id);
                if (action.HasActed)
                {
                    text.Append(" executed ");
                }
                else
                {
                    text.Append(" may ");
                }
                text.Append(action.GetType().Name);
                return this;
            }

            private object TestIfNull(object value, string replace)
            {
                if (value == null)
                {
                    return replace;
                }
                else
                {
                    return value;
                }
            }

            public StringHelperForActions AddToString(string name, object value)
            {
                value = TestIfNull(value, "NULL");

                text.Append(", " + name + " = " + value.ToString());
                return this;
            }

            public StringHelperForActions AddToStringOnlyActed(string name, object value)
            {
                if (action.HasActed)
                {
                    this.AddToString(name, value);
                }
                return this;
            }

            override public string ToString()
            {
                return text.ToString();
            }
        }


        public static StringHelper GetStringHelper(IRailsItem item)
        {
            return new StringHelper(item);
        }

        public static StringHelperForActions GetStringHelper(PossibleAction action)
        {
            return new StringHelperForActions(action);
        }

        public static bool ElementEquals<T, U>(IEnumerable<T> a, IEnumerable<U> b)
        {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;

            var itA = a.GetEnumerator();
            var itB = b.GetEnumerator();
            while (itA.MoveNext())
            {
                itB.MoveNext();
                if (itB == null)
                {
                    return false;
                }

                if (!((object)itA.Current).Equals((object)itB.Current))
                {
                    return false;
                }
            }

            // if we got here, they have all the same elements
            return true;
        }
    }
}

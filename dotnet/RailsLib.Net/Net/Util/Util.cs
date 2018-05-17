using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Drawing;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Util
{
    public class Util
    {
        static Logger<Util> logger = new Logger<Util>();

        public static string ValueWithDefault(string s, string defaultValue)
        {
            if (string.IsNullOrEmpty(s))
            {
                return defaultValue;
            }
            else
            {
                return s;
            }
        }


        public static int SetBit(int value, int bitmask, bool set)
        {

            if (set)
            {
                return bitmask | value;
            }
            else
            {
                //System.out.println("Reset bit " + value + ": from " + bitmask
                //        + " to " + (bitmask & ~value));
                return bitmask & ~value;
            }
        }

        public static bool BitSet(int value, int bitmask)
        {

            return (value & bitmask) > 0;
        }

        /**
        * Convert java string to html string
        * Transformations:
        * - Converts \n to <br>
        */
        public static string ConvertToHtml(string s)
        {
            return s.Replace("\n", "<br>");
        }

        public static string JoinWithDelimiter(int[] sa, string delimiter)
        {
            StringBuilder b = new StringBuilder();
            foreach (int s in sa)
            {
                if (b.Length > 0) b.Append(delimiter);
                b.Append(s);
            }
            return b.ToString();
        }

        public static string AppendWithDelimiter(string s1, string s2, string delimiter)
        {
            StringBuilder b = new StringBuilder(s1 != null ? s1 : "");
            if (b.Length > 0) b.Append(delimiter);
            b.Append(s2);
            return b.ToString();
        }

        /** Safely add an object to a List at a given position
        * @param objects The List to add the object to.
        * @param object The object to be added.
        * @param position The position at which the object must be added.
        * <br>If between 0 and the current list size (inclusive), the object is inserted at
        * the given position.<br>If -1, the object is inserted at the end.
        * <br>If any other value, nothing is done.
        * @return True if the insertion was successful.
        * */
        //public static bool AddToList<T, U>(List<T> objects, U o, int position)
        //    where T : IMoveable
        //    where U : T
        //{
        //    if (objects == null || o == null)
        //    {
        //        return false;
        //    }
        //    if (position == -1)
        //    {
        //        objects.Add(o);
        //        return true;
        //    }
        //    else if (position >= 0 && position <= objects.Count)
        //    {
        //        objects.Insert(position, o);
        //        return true;
        //    }
        //    return false;
        //}

        /**
        * Safely move a list of objects from one holder to another, avoiding
        * ConcurrentModificationExceptions.
        *
        * @param from
        * @param to
        * @param objects
        */
        //public static void MoveObjects<T>(List<T> objects, IMoveableHolder to) where T : IMoveable
        //{
        //    if (objects == null || objects.Count == 0) return;

        //    List<T> list = new List<T>();
        //    foreach (T item in objects)
        //    {
        //        list.Add(item);
        //    }
        //    foreach (T item in list)
        //    {
        //        item.MoveTo(to);
        //    }
        //}

        public static string LowerCaseFirst(string text)
        {
            return text.Substring(0, 1).ToLower() + text.Substring(1);
        }

        /**
    * Parse a color definition string.
    * Currently supported formats:
    *   "RRGGBB" - each character being a hexadecimal digit
    *   "r,g,b"  - each letter representing an integer 0..255
    * @param s
    * @return
    */
        public static Color ParseColor(string s)
        {
            Color c;// = null;
            if (string.IsNullOrEmpty(s))
            {
            }
            else if (s.IndexOf(',') == -1)
            {
                // Assume hexadecimal RRGGBB
                try
                {
                    c = Color.FromArgb(int.Parse(s, System.Globalization.NumberStyles.HexNumber));
                }
                catch (FormatException e)
                {
                    logger.Error("Invalid hex RGB color: " + s);
                    throw new ConfigurationException(e);
                }
                catch (ArgumentException e)
                {
                    logger.Error("Invalid hex RGB color: " + s);
                    throw new ConfigurationException(e);
                }
            }
            else
            {
                // Assume decimal r,g,b
                try
                {
                    string[] parts = s.Split(',');
                    c = Color.FromArgb(int.Parse(parts[0]),
                            int.Parse(parts[1]),
                            int.Parse(parts[2]));
                }
                catch (FormatException e)
                {
                    logger.Error("Invalid numeric RGB color: " + s);
                    throw new ConfigurationException(e);
                }
                catch (ArgumentException e)
                {
                    logger.Error("Invalid numeric RGB color: " + s);
                    throw new ConfigurationException(e);
                }
            }
            return c;
        }

        // Find item without creating new list
        public static bool ListContains<T>(IReadOnlyCollection<T> lst, T item)
        {
            IEnumerator<T> it = lst.GetEnumerator();
            while (it.MoveNext())
            {
                if (it.Current.Equals(item)) return true;
            }
            return false;
        }
    }
}

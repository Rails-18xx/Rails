using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;

namespace GameLib.Net.Util
{
    static public class Precondition
    {
        public static void CheckNotNull(object o)
        {
            if (o == null)
                throw new NullReferenceException();
        }

        public static void CheckNotNull(object o, string msg)
        {
            if (o == null)
                throw new NullReferenceException(msg);
        }

        public static void CheckArgument(bool b)
        {
            if (!b)
            {
                throw new ArgumentException();
            }
        }

        public static void CheckArgument(bool b, string msg)
        {
            if (!b)
            {
                throw new ArgumentException(msg);
            }
        }

        public static void CheckState(bool b)
        {
            if (!b)
            {
                throw new InvalidOperationException();
            }
        }

        public static void CheckState(bool b, string msg)
        {
            if (!b)
            {
                throw new InvalidOperationException(msg);
            }
        }
    }
}

using System;
using System.Collections.Generic;
using System.Drawing;
using System.Text;

/**
 * Objects of this class represent a type of square on the StockMarket with
 * special properties,usually represented by a non-white square color. The
 * default type is "white", which has no special properties.
 */

namespace GameLib.Net.Game.Financial
{
    public class StockSpaceType
    {
        /*--- Class attributes ---*/
        /*--- Constants ---*/
        /** The name of the XML tag used to configure a stock space. */
        public static readonly string ELEMENT_ID = "StockSpaceType";

        /**
         * The name of the XML attribute for the stock price type's name (any
         * string, usually the space color).
         */
        public static readonly string NAME_TAG = "name";

        /**
         * The name of the XML attribute for the stock price's color. (optional;
         * only provided as a possible help to the UI, which is free to redefine the
         * color as it seems fit).
         */
        public static readonly string COLOR_TAG = "color";

        /**
         * The name of the XML tag for the "NoCertLimit" property. (1830: yellow
         * stock market area)
         */
        public static readonly string NO_CERT_LIMIT_TAG = "NoCertLimit";

        /**
         * The name of the XML tag for the "NoHoldLimit" property. (1830: orange
         * area)
         */
        public static readonly string NO_HOLD_LIMIT_TAG = "NoHoldLimit";

        /**
         * The name of the XML tag for the "NoBuyLimit" property. (1830: brown area)
         */
        public static readonly string NO_BUY_LIMIT_TAG = "NoBuyLimit";


        /*--- Instance attributes ---*/
        private readonly string name;
        private readonly string colorString;
        private readonly Color color;
        protected bool noCertLimit = false; // In yellow zone
        protected bool noHoldLimit = false; // In orange zone (1830)
        protected bool noBuyLimit = false; // In brown zone (1830)
        protected int addRevenue = 0; // additional Revenue for company listed at that space (1880)

        public static readonly string WHITE = "FFFFFF";

        public StockSpaceType(string name, string color)
        {
            this.name = name;
            this.colorString = color;
            this.color = Util.Util.ParseColor(colorString);
        }

        /*--- Getters ---*/
        /**
         * @return The square type's name.
         */
        public string Name
        {
            get
            {
                return name;
            }
        }

        /**
         * @return The square type's color.
         */
        public Color Color
        {
            get
            {
                return color;
            }
        }

        /**
         * @return TRUE if the square type has no buy limit ("brown area")
         */
        public bool IsNoBuyLimit
        {
            get
            {
                return noBuyLimit;
            }
            set
            {
                noBuyLimit = value;
            }
        }

        /**
         * @return TRUE if the square type has no certificate limit ("yellow area")
         */
        public bool IsNoCertLimit
        {
            get
            {
                return noCertLimit;
            }
            set
            {
                noCertLimit = value;
            }
        }

        /**
         * @return TRUE if the square type has no hold limit ("orange area")
         */
        public bool IsNoHoldLimit
        {
            get
            {
                return noHoldLimit;
            }
            set
            {
                noHoldLimit = value;
            }
        }

        public int AddRevenue
        {
            get
            {
                return addRevenue;
            }
            set
            {
                addRevenue = value;
            }
        }

    }
}

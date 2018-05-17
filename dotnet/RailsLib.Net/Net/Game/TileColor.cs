using System;
using System.Collections.Generic;
using System.Text;

/**
 * TileColour represents the different colors of Tiles
 */

namespace GameLib.Net.Game
{
    public sealed class TileColor : IComparable<TileColor>
    {
        public static TileColor RED = new TileColor("RED", -2, false);
        public static TileColor FIXED = new TileColor("FIXED", -1, false);
        public static TileColor WHITE = new TileColor("WHITE", 0, true);
        public static TileColor YELLOW = new TileColor("YELLOW", 1, true);
        public static TileColor GREEN = new TileColor("GREEN", 2, true);
        public static TileColor BROWN = new TileColor("BROWN", 3, true);
        public static TileColor GRAY = new TileColor("GRAY", 4, true);

        /**
         * The offset to convert tile numbers to tilename index. Color number 0 and
         * higher are upgradeable.
         */

        private int number;
        private bool upgradeable;
        private string name;
    
        private TileColor(string name, int number, bool upgradeable)
        {
            this.name = name;
            this.number = number;
            this.upgradeable = upgradeable;
        }

        public int Number
        {
            get
            {
                return number;
            }
        }

        public bool IsUpgradeable
        {
            get
            {
                return upgradeable;
            }
        }

        public String ToText()
        {
            return this.name.ToLower();
        }

        public static TileColor ValueOf(string colorName)
        {
            switch (colorName.ToUpper())
            {
                case "RED":
                    return RED;
                case "FIXED":
                    return FIXED;
                case "WHITE":
                    return WHITE;
                case "YELLOW":
                    return YELLOW;
                case "GREEN":
                    return GREEN;
                case "BROWN":
                    return BROWN;
                case "GRAY":
                    return GRAY;
                default:
                    throw new ArgumentException("TileColor: {colorName} doesn't match defined value");
            }
        }

        public int CompareTo(TileColor other)
        {
            if (other == null) return 1;
            return number.CompareTo(other.Number);
        }
    }
}

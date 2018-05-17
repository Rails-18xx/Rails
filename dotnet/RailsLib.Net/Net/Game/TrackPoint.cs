using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

/**
 * Abstract class to be extended by classes that are Points on a hex tile
 */

namespace GameLib.Net.Game
{
    abstract public class TrackPoint
    {
        public enum TrackPointTypeEnum { SIDE, STATION };

        public abstract int TrackPointNumber { get; }

        public abstract TrackPointTypeEnum TrackPointType { get; }

        public abstract TrackPoint Rotate(HexSide rotation);

        // Object methods
        override public bool Equals(object other)
        {
            if (!(other is TrackPoint)) return false;
            return this.TrackPointNumber == ((TrackPoint)other).TrackPointNumber;
        }

        override public int GetHashCode()
        {
            return TrackPointNumber;
        }

        // Patterns for Track tags
        private static Regex sidePattern = new Regex(@"side(\d+)");
        private static Regex cityPattern = new Regex(@"city(\d+)");

        public static TrackPoint Create(Tile tile, string trackString)
        {
            int trackPointNr = ParseTrackPointNumber(trackString);
            if (trackPointNr >= 0)
            {
                return HexSide.Get(trackPointNr);
            }
            else
            { // trackPointNr is negative
                return tile.GetStation(-trackPointNr);
            }
        }

        protected static int ParseTrackPointNumber(string trackString)
        {
            //Matcher m;
            Match match = sidePattern.Match(trackString);
            //if ((m = sidePattern.matcher(trackString)).matches())
            if (match.Success)
            {
                int sideNr = (int.Parse(match.Groups[1].Value) + 3) % 6;
                return sideNr;
            }
            //else if ((m = cityPattern.matcher(trackString)).matches())
            match = cityPattern.Match(trackString);
            if (match.Success)
            {
                int stationNr = int.Parse(match.Groups[1].Value);
                return -stationNr;
            }
            // Should add some validation!
            throw new ConfigurationException(LocalText.GetText("InvalidTrackEnd")
                    + ": " + trackString);
        }
    }
}

using System;
using System.Collections.Generic;
using System.Text;

/**
 * Represents a piece of track on one tile. Start and end of a track are TrackPoints:
 * Either TileSide or Station objects. 
 */

namespace GameLib.Net.Game
{
    public sealed class Track : IEquatable<Track>
    {
        private TrackPoint start;
        private TrackPoint end;
        private int hashCode;

        public Track(TrackPoint start, TrackPoint end)
        {
            if (start.TrackPointNumber < end.TrackPointNumber)
            {
                this.start = start;
                this.end = end;
            }
            else
            {
                this.start = end;
                this.end = start;
            }
            hashCode = 12 * this.start.TrackPointNumber + this.end.TrackPointNumber;
        }

        public Track CreateByRotation(HexSide rotation)
        {
            return new Track(start.Rotate(rotation), end.Rotate(rotation));
        }

        public Track CreateByStationMapping(Dictionary<Station, Station> mapping)
        {
            TrackPoint s = start;
            if (start is Station && mapping.ContainsKey((Station)start))
            {
                s = mapping[(Station)start];
            }
            TrackPoint e = end;
            if (end is Station && mapping.ContainsKey((Station)end))
            {
                e = mapping[(Station)end];
            }
            return new Track(s, e);
        }

        public TrackPoint Start
        {
            get
            {
                return start;
            }
        }

        public TrackPoint End
        {
            get
            {
                return end;
            }
        }

        public TrackPoint GetOpposite(TrackPoint other)
        {

            if (other == this.start)
            {
                return this.end;
            }
            else if (other == this.end)
            {
                return this.start;
            }
            else
            {
                throw new ArgumentException();
            }
        }

        // Object methods
        override public string ToString()
        {
            return ("Track " + start + "->" + end + " (hc = " + hashCode + ")");
        }

        // Two tracks are equal if they share the same hashcode
        override public bool Equals(object other)
        {
            if (!(other is Track)) return false;
            return this.GetHashCode() == other.GetHashCode();
        }

        override public int GetHashCode()
        {
            return hashCode;
        }

        public bool Equals(Track other)
        {
            if (other == null) return false;
            return this.GetHashCode() == other.GetHashCode();
        }
    }
}

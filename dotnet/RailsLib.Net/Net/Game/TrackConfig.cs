using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

/** Track configuration defines a set of tracks (e.g. on a Tile) */


namespace GameLib.Net.Game
{
    public class TrackConfig
    {
        private List<Track> tracks;
        private MultiDictionary<Station, TrackPoint> stationTracks = new MultiDictionary<Station, TrackPoint>(true);
        private MultiDictionary<HexSide, TrackPoint> sideTracks = new MultiDictionary<HexSide, TrackPoint>(true);

        private Tile tile;

        private TrackConfig(Tile tile, IEnumerable<Track> tracks, int rotation)
        {
            this.tile = tile;
            this.tracks = new List<Track>(tracks);
            foreach (Track t in tracks)
            {
                ClassifyTrack(t);
            }
        }

        public TrackConfig(Tile tile, IEnumerable<Track> tracks) : this(tile, tracks, 0)
        {

        }

        [Obsolete]
        public static TrackConfig CreateByRotation(TrackConfig baseConfig, int rotation)
        {
            return CreateByRotation(baseConfig, HexSide.Get(rotation));
        }

        public static TrackConfig CreateByRotation(TrackConfig baseConfig, HexSide rotation)
        {
            List<Track> tracks = new List<Track>();
            foreach (Track t in baseConfig.Tracks)
            {
                tracks.Add(t.CreateByRotation(rotation));
            }
            return new TrackConfig(baseConfig.Tile, tracks);
        }


        public static TrackConfig CreateByStationMapping(TrackConfig baseConfig, Dictionary<Station, Station> mapping)
        {
            List<Track> tracks = new List<Track>();
            foreach (Track t in baseConfig.Tracks)
            {
                Track n = t.CreateByStationMapping(mapping);
                if (n.Start == n.End) continue; // due to merged stations
                tracks.Add(n);
            }
            return new TrackConfig(baseConfig.Tile, tracks);
        }

        // special case in 1856: downgrade of village tiles possible
        public static TrackConfig CreateByDowngrade(TrackConfig baseConfig, Station station)
        {
            List<Track> newTracks = new List<Track>();
            /*List<TrackPoint>*/var stationConnects = baseConfig.GetStationTracks(station);
            foreach (TrackPoint start in stationConnects)
            {
                foreach (TrackPoint end in stationConnects)
                {
                    if (start == end) continue;
                    // connect all sides that are reachable
                    newTracks.Add(new Track(start, end));
                }
            }
            return new TrackConfig(baseConfig.Tile, newTracks);
        }

        private void ClassifyTrack(Track track)
        {
            TrackPoint start = track.Start;
            TrackPoint end = track.End;
            if (start.TrackPointType == TrackPoint.TrackPointTypeEnum.STATION)
            {
                stationTracks.Add((Station)start, end);
            }
            else
            {
                sideTracks.Add((HexSide)start, end);
            }
            if (end.TrackPointType == TrackPoint.TrackPointTypeEnum.STATION)
            {
                stationTracks.Add((Station)end, start);
            }
            else
            {
                sideTracks.Add((HexSide)end, start);
            }
        }

        override public int GetHashCode()
        {
            return tracks.GetHashCode();
        }

        override public bool Equals(object other)
        {
            if (!(other is TrackConfig)) return false;
            return (this.tracks.Equals(((TrackConfig)other).tracks));
        }

        public Tile Tile
        {
            get
            {
                return tile;
            }
        }

        public List<Track> Tracks
        {
            get
            {
                return tracks;
            }
        }

        public int Count
        {
            get
            {
                return tracks.Count;
            }
        }

        public bool HasNoStationTracks
        {
            get
            {
                return stationTracks.Count == 0;
            }
        }

        // #FIXME_GetHashCode_is_likely_wrong may need to convert to List<> instead of List
        public ICollection<TrackPoint> GetStationTracks(Station station)
        {
            return stationTracks[station];
        }

        public List<TrackPoint> GetSideTracks(HexSide side)
        {
            return (List<TrackPoint>)sideTracks[side];
        }

        public bool HasSideTracks(HexSide side)
        {
            return sideTracks.ContainsKey(side);
        }

        override public string ToString()
        {
            return "Track on tile " + tile.ToString() + ": " + tracks.ToString();
        }

        /**
         * For a combination of MapHex, Tile, rotation and station provide
         * a description of the connections
         * @deprecated Use {@link #getConnectionString(MapHex,Tile,HexSide,Station)} instead
         */
        public static String GetConnectionString(MapHex hex, Tile tile, int rotation,
                Station station)
        {
            return GetConnectionString(hex, tile, HexSide.Get(rotation), station);
        }

        /**
         * For a combination of MapHex, Tile, rotation and station provide
         * a description of the connections
         */
        public static string GetConnectionString(MapHex hex, Tile tile, HexSide rotation,
                Station station)
        {
            StringBuilder b = new StringBuilder();
            foreach (TrackPoint endPoint in tile.GetTracksPerStation(station))
            {
                if (endPoint.TrackPointType == TrackPoint.TrackPointTypeEnum.STATION) continue;
                HexSide direction = (HexSide)endPoint.Rotate(rotation);
                if (b.Length > 0) b.Append(",");
                b.Append(hex.GetOrientationName(direction));
            }
            return b.ToString();
        }
    }
}

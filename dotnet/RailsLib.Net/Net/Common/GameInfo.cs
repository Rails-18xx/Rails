using Newtonsoft.Json;
using System;
using System.Collections.Generic;

namespace GameLib.Net.Common
{
    [JsonObject(MemberSerialization.Fields)]
    public class GameInfo : IComparable<GameInfo>
    {
        private readonly int minPlayers;
        private readonly int maxPlayers;

        private readonly string name;
        private readonly string note;
        private readonly string description;

        private readonly int ordering;

        public GameInfo() { }

        private GameInfo(int minPlayers, int maxPlayers, String name, String note,
                String description, int ordering)
        {
            this.minPlayers = minPlayers;
            this.maxPlayers = maxPlayers;
            this.name = name;
            this.note = note;
            this.description = description;
            this.ordering = ordering;
        }

        [Obsolete("CreateLegacy is obsolete")]
        public static GameInfo CreateLegacy(string name)
        {
            return new GameInfo(0, 0, name, null, null, 0);
        }

        public int MinPlayers
        {
            get
            {
                return minPlayers;
            }
        }

        public int MaxPlayers
        {
            get
            {
                return maxPlayers;
            }
        }

        public string Name
        {
            get
            {
                return name;
            }
        }

        public string Note
        {
            get
            {
                return note;
            }
        }

        public string Description
        {
            get
            {
                return description;
            }
        }

        public int Ordering
        {
            get
            {
                return ordering;
            }
        }

        override public int GetHashCode()
        {
            return name.GetHashCode(); //Objects.hashCode(name);
        }

        override public bool Equals(object obj)
        {
            if (obj is GameInfo other)
            {
                return name == other.name;
            }

            return false;

            //if (obj == null) return false;

            //if (!(obj is GameInfo)) return false;
            //GameInfo other = (GameInfo)obj;
            //return name == other.name; // Objects.equal(this.name, other.name);
        }

        public bool Equals(GameInfo other)
        {
            if (other == null) return false;

            return name == other.name;
        }

        override public string ToString()
        {
            return name;
        }

        public int CompareTo(GameInfo other)
        {
            return ordering.CompareTo(other.ordering);// Ints.compare(this.ordering, other.ordering);
        }

        public static GameInfo FindGame(IEnumerable<GameInfo> gameList, string gameName)
        {
            foreach (GameInfo game in gameList)
            {
                if (game.name == gameName) // Objects.equal(game.name, gameName))
                {
                    return game;
                }
            }
            return null;
        }

        public static GameInfo.Builder GetBuilder()
        {
            return new GameInfo.Builder();
        }

        public class Builder
        {
            private int minPlayers;
            private int maxPlayers;
            private string name;
            private string note;
            private string description;

            // #builder_constructor
            internal Builder() { }

            public GameInfo Build(int ordering)
            {
                return new GameInfo(minPlayers, maxPlayers, name, note, description, ordering);
            }

            public int MinPlayers
            {
                set
                {
                    this.minPlayers = value;
                }
            }
            public int MaxPlayers
            {
                set
                {
                    this.maxPlayers = value;
                }
            }
            public string Name
            {
                set
                {
                    this.name = value;
                }
            }
            public string Note
            {
                set
                {
                    this.note = value;
                }
            }
            public string Description
            {
                set
                {
                    this.description = value;
                }
            }
        }
    }
}

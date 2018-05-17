using Newtonsoft.Json;
using System.Collections.Generic;


namespace GameLib.Net.Common
{
    [JsonObject(MemberSerialization.Fields)]
    public class GameData
    {
        private GameInfo game;
    private GameOptionsSet gameOptions;
    private List<string> players;

        public GameData() { }

        private GameData(GameInfo game, GameOptionsSet gameOptions, List<string> players)
        {
            this.game = game;
            this.gameOptions = gameOptions;
            this.players = players;
        }

        public static GameData Create(GameInfo game, GameOptionsSet.Builder gameOptions, List<string> players)
        {
            return new GameData(game, gameOptions.Build(players.Count), players);
        }

        public string GameName
        {
            get
            {
                return game.Name;
            }
        }

        public GameOptionsSet GameOptions
        {
            get
            {
                return gameOptions;
            }
        }

        public List<string> Players
        {
            get
            {
                return players;
            }
        }
    }
}

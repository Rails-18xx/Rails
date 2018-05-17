using GameLib.Net.Common;
using GameLib.Rails.Game.Action;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;

/**
 * Combines all elements for gameIO
 */
namespace GameLib.Net.Util
{
    [JsonObject(MemberSerialization.Fields)]
    public class GameIOData
    {
        private GameData gameData;
        private string version;
        private string date;
        private long fileVersionID;
        [JsonIgnore]
        private List<PossibleAction> actions;

        public GameIOData(GameData gameData, string version, string date, int fileVersionID, List<PossibleAction> actions)
        {
            this.gameData = gameData;
            this.version = version;
            this.date = date;
            this.fileVersionID = fileVersionID;
            this.actions = actions;
        }

        public GameIOData() { }


        public GameData GameData
        {
            get
            {
            return this.gameData;
            }

            set
            {
            this.gameData = value;
            }
        }

        //GameData getGameData()
        //{
        //    return gameData;
        //}

        //public void setVersion(string version)
        //{
        //    this.version = version;
        //}

        public string Version
        {
            get
            {
                return version;
            }
            set
            {
                version = value;
            }
        }

        //void setDate(string date)
        //{
        //    this.date = date;
        //}

        public string Date
        {
            get
            {
                return date;
            }
            set
            {
                date = value;
            }
        }

        //void setFileVersionID(long fileVersionID)
        //{
        //    this.fileVersionID = fileVersionID;
        //}

        public long FileVersionID
        {
            get
            {
                return fileVersionID;
            }
            set
            {
                fileVersionID = value;
            }
        }

        void SetActions(List<PossibleAction> actions)
        {
            this.actions = actions;
        }

        public List<PossibleAction> Actions
        {
            get
            {
                return actions;
            }
            set
            {
                actions = value;
            }
        }

        public string MetaDataAsText()
        {
            StringBuilder s = new StringBuilder();
            s.Append("Rails saveVersion = " + version + "\n");
            s.Append("File was saved at " + date + "\n");
            s.Append("Saved versionID=" + fileVersionID + "\n");
            s.Append("Save game=" + gameData.GameName + "\n");
            return s.ToString();
        }

        public string GameOptionsAsText()
        {
            StringBuilder s = new StringBuilder();
            foreach (string key in gameData.GameOptions.GetOptions().Keys)
            {
                s.Append("Option " + key + "=" + gameData.GameOptions.Get(key) + "\n");
            }
            return s.ToString();
        }

        public string PlayerNamesAsText()
        {
            StringBuilder s = new StringBuilder();
            int i = 1;
            foreach (string player in gameData.Players)
            {
                s.Append("Player " + (i++) + ": " + player + "\n");
            }
            return s.ToString();
        }
    }
}

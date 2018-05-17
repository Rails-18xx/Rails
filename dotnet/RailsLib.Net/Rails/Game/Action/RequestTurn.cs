using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Util;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Action
{
    [JsonObject(MemberSerialization.Fields)]
    public class RequestTurn : PossibleAction
    {
        new public const long serialVersionUID = 1L;

        private string requestingPlayerName;

        public RequestTurn(Player player) : base(null)
        {
            //super(null); // not defined by an activity yet
            // Override player set by superclass
            if (player != null)
            {
                requestingPlayerName = player.Id;
            }
        }

        public string RequestingPlayerName
        {
            get
            {
                return requestingPlayerName;
            }
        }


        override public string ToMenu()
        {
            return LocalText.GetText("RequestTurn", requestingPlayerName);
        }

        override protected bool EqualsAs(PossibleAction pa, bool asOption)
        {
            // identity always true
            if (pa == this) return true;
            //  super checks both class identity and super class attributes
            if (!base.EqualsAs(pa, asOption)) return false;

            // check asOption attributes
            RequestTurn action = (RequestTurn)pa;
            return requestingPlayerName == action.requestingPlayerName;
            // no asAction attributes to be checked
        }

        override public string ToString()
        {
            return base.ToString() +
                    RailsObjects.GetStringHelper(this)
                        .AddToString("requestingPlayerName", requestingPlayerName)
                        .ToString();
        }
    }
}

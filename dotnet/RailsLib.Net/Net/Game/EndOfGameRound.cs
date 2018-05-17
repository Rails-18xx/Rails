using GameLib.Net.Common;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * EndOfGameRound is a dummy implementation of the Round class
 * It generates no additional actions.
 * It also sets guiHints (default: shows map, stock market and activates status) 
 *
 *  */

namespace GameLib.Net.Game
{
    public class EndOfGameRound : Round, ICreatable
    {
        /**
         * Constructed via Configure
         */
        public EndOfGameRound(GameManager parent, string id) : base(parent, id)
        {
            guiHints.SetVisibilityHint(GuiDef.Panel.MAP, true);
            guiHints.ActivePanel = GuiDef.Panel.STATUS;
        }

    override public bool SetPossibleActions()
        {
            possibleActions.Clear();
            return true;
        }

        public GuiHints GetGuiHints()
        {
            return guiHints;
        }

    override public string ToString()
        {
            return "EndOfGameRound ";
        }

    override public string RoundName
        {
            get
            {
                return ToString();
            }
        }
    }
}

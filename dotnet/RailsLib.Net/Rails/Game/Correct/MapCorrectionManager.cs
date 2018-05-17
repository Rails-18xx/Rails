using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Correct
{
    public class MapCorrectionManager // : CorrectionManager
    {
        public enum ActionStep
        {
            SELECT_HEX, SELECT_TILE, SELECT_ORIENTATION, CONFIRM, RELAY_BASETOKENS, FINISHED, CANCELLED
        }

        public MapCorrectionManager()
        {
            throw new NotImplementedException();
        }
    }
}

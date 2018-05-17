using System;
using System.Collections.Generic;
using System.Text;

/**
 * Correction action for tile and token lays
 * 
 * Rails 2.0: updated equals and toString methods
 *
 * Deprecated since version 2.0
 * It is replaced by LayTile and LayToken actions in the UI
 */

namespace GameLib.Rails.Game.Correct
{
    [Obsolete]
    public class MapCorrectionAction : CorrectionAction
    {
        public MapCorrectionAction()
        {
            throw new NotImplementedException();
        }
    }
}

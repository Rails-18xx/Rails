package net.sf.rails.game.correct;

import net.sf.rails.game.GameManager;

/**
 * Enumerates the possible Corrections
*/
public enum CorrectionType {
    CORRECT_CASH {
        public CorrectionManager newCorrectionManager(GameManager gm)
        {return CashCorrectionManager.create(gm);}
    },
    CORRECT_MAP {
        public CorrectionManager newCorrectionManager(GameManager gm)
        {return MapCorrectionManager.create(gm);}
    }
    ;
    public abstract CorrectionManager newCorrectionManager(GameManager gm);

}

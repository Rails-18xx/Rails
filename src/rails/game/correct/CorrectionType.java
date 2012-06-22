package rails.game.correct;

import rails.game.GameManager;

/**
 * Enumerates the possible Corrections
*/
public enum CorrectionType {
    CORRECT_CASH {
        public CorrectionManagerI newCorrectionManager(GameManager gm)
        {return CashCorrectionManager.create(gm);}
    },
    CORRECT_MAP {
        public CorrectionManagerI newCorrectionManager(GameManager gm)
        {return MapCorrectionManager.create(gm);}
    }
    ;
    public abstract CorrectionManagerI newCorrectionManager(GameManager gm);

}

package rails.game.correct;

import rails.game.GameManager;

/**
 * Enumerates the possible Corrections
*/
public enum CorrectionType {
    CORRECT_CASH {
        public CorrectionManagerI newCorrectionManager(GameManager gm)
        {return new CashCorrectionManager(gm);}
    },
    CORRECT_MAP {
        public CorrectionManagerI newCorrectionManager(GameManager gm)
        {return new MapCorrectionManager(gm);}
    }
    ;
    public abstract CorrectionManagerI newCorrectionManager(GameManager gm);

}

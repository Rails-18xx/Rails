package rails.game.correct;

import rails.game.GameManager;

/**
 * Enumerates the possible Corrections
*/
public enum CorrectionType {
    CORRECT_CASH {
        public CorrectionManager getManager(GameManager gm)
            {return CashCorrectionManager.getInstance(gm);}
        };
//     CORRECT_MAP ("CorrectName") {
//            CorrectionManager getManager(GameManager gm)
//                {return MapCorrectionManager.getInstance(gm);}
//            };

    public abstract CorrectionManager getManager(GameManager gm);

}

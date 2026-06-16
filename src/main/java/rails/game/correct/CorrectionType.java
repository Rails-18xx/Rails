package rails.game.correct;

import net.sf.rails.game.GameManager;

/**
 * Enumerates the possible Corrections
*/
public enum CorrectionType {
    CORRECT_CASH {
        public CorrectionManager newCorrectionManager(GameManager gm)
        {return CashCorrectionManager.create(gm);}
    },
    // Map correction removed as requested
    
    CORRECT_MAP {
        public CorrectionManager newCorrectionManager(GameManager gm)
        {return MapCorrectionManager.create(gm);}
    },

    CORRECT_LOANS {
        public CorrectionManager newCorrectionManager(GameManager gm) 
        {return LoanCorrectionManager.create(gm);}
    },

    MOVE_PRIVATE {
public CorrectionManager newCorrectionManager(GameManager gm)
{return PrivateMoveCorrectionManager.create(gm);}
},
    
    CORRECT_TRAINS {
        public CorrectionManager newCorrectionManager(GameManager gm)
        {return TrainCorrectionManager.create(gm);}
    },
    CORRECT_SHARES {
        public CorrectionManager newCorrectionManager(GameManager gm) 
        {return ShareCorrectionManager.create(gm);}
    },
    CORRECT_STOCK {
        public CorrectionManager newCorrectionManager(GameManager gm) 
        {return StockCorrectionManager.create(gm);}
    },
    CORRECT_TIME {
        public CorrectionManager newCorrectionManager(GameManager gm) 
        {return TimeCorrectionManager.create(gm);}
    },
    CLOSE_PRIVATE {
        public CorrectionManager newCorrectionManager(GameManager gm) 
        {return PrivateCorrectionManager.create(gm);}
    }
    ;
    public abstract CorrectionManager newCorrectionManager(GameManager gm);

}
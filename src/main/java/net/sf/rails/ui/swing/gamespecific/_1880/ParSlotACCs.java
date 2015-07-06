package net.sf.rails.ui.swing.gamespecific._1880;

import net.sf.rails.game.specific._1880.ParSlot;
import net.sf.rails.game.state.ColorModel;
import net.sf.rails.game.state.Observable;
import net.sf.rails.ui.swing.core.Accessor1D;

/**
 * Accessors for the ParSlots 
 */
public class ParSlotACCs {
    
    public static final Accessor1D.AText<ParSlot> PRICE = 
            new Accessor1D.AText<ParSlot>(ParSlot.class) {
                @Override
                protected String access(ParSlot slot) {
                    return String.valueOf(slot.getPrice());
                }
    };

    public static final Accessor1D.AObservable<ParSlot> COMPANY = 
            new Accessor1D.AObservable<ParSlot>(ParSlot.class) {
                @Override
                protected Observable access(ParSlot slot) {
                    return slot.getCompany();
                }
    };
    
    public static final Accessor1D.AColorModel<ParSlot> COMPANY_COLORS =
            new Accessor1D.AColorModel<ParSlot>(ParSlot.class) {
                @Override
                protected ColorModel access(ParSlot slot) {
                    return slot.getCompanyColors();
                }
    };
            

    public static final Accessor1D.AObservable<ParSlot> LAST_TRAIN = 
            new Accessor1D.AObservable<ParSlot>(ParSlot.class) {
                @Override
                protected Observable access(ParSlot slot) {
                    return slot.getLastTrain();
                }
    };

}

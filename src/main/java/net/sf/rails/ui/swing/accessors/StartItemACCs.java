package net.sf.rails.ui.swing.accessors;

import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.state.Observable;
import net.sf.rails.ui.swing.core.Accessor1D;
import net.sf.rails.ui.swing.core.Accessor2D;

public class StartItemACCs {
    
    public static final Accessor1D.AText<StartItem> NAME =
            new Accessor1D.AText<StartItem>(StartItem.class) {
        @Override
        protected String access(StartItem item) {
            return item.getId();
        }
    };

    public static final Accessor1D.AObservable<StartItem> BASE_PRICE =
            new Accessor1D.AObservable<StartItem>(StartItem.class) {
        @Override
        protected Observable access(StartItem item) {
            return item.getBasePriceModel();
        }
    };
    
    public static final Accessor1D.AObservable<StartItem> MIN_BID =
            new Accessor1D.AObservable<StartItem>(StartItem.class) {
        @Override
        protected Observable access(StartItem item) {
            return item.getMinimumBidModel();
        }
    };
    
    public static final Accessor2D.AObservable<StartItem,Player> CUR_BID =
            new Accessor2D.AObservable<StartItem,Player>(StartItem.class, Player.class) {
        @Override
        protected Observable access(StartItem item, Player player) {
            return item.getBidForPlayerModel(player);
        }
    };
    
}

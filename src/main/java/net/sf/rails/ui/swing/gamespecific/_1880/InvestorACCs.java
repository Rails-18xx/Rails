package net.sf.rails.ui.swing.gamespecific._1880;

import net.sf.rails.game.specific._1880.Investor_1880;
import net.sf.rails.game.state.Observable;
import net.sf.rails.ui.swing.core.Accessor1D;

public class InvestorACCs {

    public static final Accessor1D.AText<Investor_1880> COMPANY =
            new Accessor1D.AText<>(Investor_1880.class) {
                @Override
                protected String access(Investor_1880 investor) {
                    return String.valueOf(investor.getLinkedCompany().getId());
                }
            };

    public static final Accessor1D.AObservable<Investor_1880> PLAYER =
            new Accessor1D.AObservable<>(Investor_1880.class) {
                @Override
                protected Observable access(Investor_1880 investor) {
                    return investor.getLinkedCompany().getPresident().getPlayerNameModel();
                }
            };

    public static final Accessor1D.AText<Investor_1880> INVESTOR =
            new Accessor1D.AText<>(Investor_1880.class) {
                @Override
                protected String access(Investor_1880 investor) {
                    return String.valueOf(investor.getId());
                }
            };
}

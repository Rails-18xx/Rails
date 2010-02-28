/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialPropertyI.java,v 1.10 2010/02/28 21:38:05 evos Exp $ */
package rails.game.special;

import rails.game.CompanyI;
import rails.game.ConfigurableComponentI;
import rails.game.PrivateCompanyI;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolder;

public interface SpecialPropertyI extends ConfigurableComponentI, Moveable {

    public void setCompany(CompanyI company);

    public CompanyI getOriginalCompany();

    public boolean isExecutionable();

    public boolean isUsableIfOwnedByCompany();

    public void setUsableIfOwnedByCompany(boolean usableIfOwnedByCompany);

    public boolean isUsableIfOwnedByPlayer();

    public void setUsableIfOwnedByPlayer(boolean usableIfOwnedByPlayer);

    public boolean isUsableDuringOR();

    public void setUsableDuringOR(boolean usableDuringOR);

    public boolean isUsableDuringSR();

    public void setUsableDuringSR(boolean usableDuringSR);

    public boolean isUsableDuringTileLayingStep();

    public void setUsableDuringTileLayingStep(boolean usableDuringTileLayingStep);

    public boolean isUsableDuringTokenLayingStep();

    public void setUsableDuringTokenLayingStep(boolean usableDuringTokenLayingStep);

    public void setExercised();

    public boolean isExercised();

    public boolean isSRProperty();

    public boolean isORProperty();

    public String getTransferText();

    public int getUniqueId();

    public String toMenu();

    public String getName();

    public String getInfo();

    public void moveTo(MoveableHolder newHolder);

}

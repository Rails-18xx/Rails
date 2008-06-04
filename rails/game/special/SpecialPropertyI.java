/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialPropertyI.java,v 1.6 2008/06/04 19:00:38 evos Exp $ */
package rails.game.special;

import rails.game.ConfigurableComponentI;
import rails.game.PrivateCompanyI;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolderI;

public interface SpecialPropertyI extends ConfigurableComponentI, Moveable {

    public void setCompany(PrivateCompanyI company);

    public PrivateCompanyI getCompany();

    public boolean isExecutionable();

    public boolean isUsableIfOwnedByCompany();

    public void setUsableIfOwnedByCompany(boolean usableIfOwnedByCompany);

    public boolean isUsableIfOwnedByPlayer();

    public void setUsableIfOwnedByPlayer(boolean usableIfOwnedByPlayer);

    public void setExercised();

    public boolean isExercised();

    public boolean isSRProperty();

    public boolean isORProperty();

    public String getTransferText();

    public int getUniqueId();

    public String toMenu();

    public String getName();

    public void moveTo(MoveableHolderI newHolder);

}

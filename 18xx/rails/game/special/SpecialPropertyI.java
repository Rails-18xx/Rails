/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialPropertyI.java,v 1.7 2010/01/08 21:30:54 evos Exp $ */
package rails.game.special;

import rails.game.ConfigurableComponentI;
import rails.game.PrivateCompanyI;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolder;

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

    public void moveTo(MoveableHolder newHolder);

}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialPropertyI.java,v 1.12 2010/03/16 21:21:59 evos Exp $ */
package rails.game.special;

import rails.game.CompanyI;
import rails.game.ConfigurableComponentI;
import rails.game.move.Moveable;
import rails.game.move.MoveableHolder;

public interface SpecialPropertyI extends ConfigurableComponentI, Moveable {

    public void setCompany(CompanyI company);

    public CompanyI getOriginalCompany();

    public void setHolder(MoveableHolder holder);
    public MoveableHolder getHolder();
    
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

    public enum Priority {
        LAST,
        ASKUSER,
        FIRST;
    };
    
    public static final Priority DEFAULT_PRIORITY = Priority.FIRST;

    public Priority getPriority();

    public void setPriority(Priority priority);

    public int getUniqueId();

    public String toMenu();

    public String getName();

    public String getInfo();

    public void moveTo(MoveableHolder newHolder);

}

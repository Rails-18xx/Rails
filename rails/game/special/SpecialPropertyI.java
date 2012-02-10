package rails.game.special;

import rails.common.parser.ConfigurableComponentI;
import rails.game.*;
import rails.game.model.Ownable;
import rails.game.state.OwnableItem;

public interface SpecialPropertyI extends ConfigurableComponentI, OwnableItem<SpecialPropertyI> {
    
    public void setCompany(Company company);

    public Company getOriginalCompany();
    
    public boolean isExecutionable();

    public boolean isUsableIfOwnedByCompany();

    public void setUsableIfOwnedByCompany(boolean usableIfOwnedByCompany);

    public boolean isUsableIfOwnedByPlayer();

    public void setUsableIfOwnedByPlayer(boolean usableIfOwnedByPlayer);

    public boolean isUsableDuringOR(GameDef.OrStep step);

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

    public String getId();

    public String getInfo();

}

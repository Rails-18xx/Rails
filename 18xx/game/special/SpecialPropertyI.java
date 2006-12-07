package game.special;

import game.PrivateCompanyI;
import game.ConfigurableComponentI;

public interface SpecialPropertyI extends ConfigurableComponentI
{

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

}

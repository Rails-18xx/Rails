/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/special/SpecialPropertyI.java,v 1.4 2007/10/05 22:02:25 evos Exp $ */
package rails.game.special;

import rails.game.ConfigurableComponentI;
import rails.game.PrivateCompanyI;

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
	
	public int getUniqueId();
    
    public String toMenu();

}

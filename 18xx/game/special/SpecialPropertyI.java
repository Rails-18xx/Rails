package game.special;

import game.PrivateCompanyI;
import game.ConfigurableComponentI;

public interface SpecialPropertyI extends ConfigurableComponentI
{

	public void setCondition(String condition);

	public String getCondition();

	public void setCompany(PrivateCompanyI company);

	public PrivateCompanyI getCompany();

	public boolean isExecutionable();

	public void setExercised();

	public boolean isExercised();

	public boolean isSRProperty();

	public boolean isORProperty();

}

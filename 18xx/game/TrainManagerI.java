package game;

import java.util.List;

/**
 * Interface for CompanyManager objects. A company manager is a factory which
 * vends Company objects.
 */
public interface TrainManagerI
{

	/**
	 * This is the name by which the TrainManager should be registered with the
	 * ComponentManager.
	 */
	static final String COMPONENT_NAME = "TrainManager";

	// public static String makeAbbreviatedList (Portfolio holder);

	// public String makeFullList (TrainI[] trains);

	// public String makeFullList (Portfolio holder);

	public List getAvailableNewTrains();

	public TrainTypeI getTypeByName(String name);

	public List getTrainTypes();

	public void checkTrainAvailability(TrainI train, Portfolio from);

	public boolean hasAvailabilityChanged();

	public void resetAvailabilityChanged();

	public boolean hasPhaseChanged();

}

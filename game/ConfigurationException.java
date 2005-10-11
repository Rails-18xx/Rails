/*
 * Created on 05-Mar-2005
 *
 * IG Adams
 */
package game;

/**
 * @author iadams
 * 
 * Class for reporting problems with reading configuration files.
 */
public class ConfigurationException extends Exception
{

	public ConfigurationException()
	{
		super();
	}

	/**
	 * @param reason
	 *            a message detailing why this Exception was thrown
	 */
	public ConfigurationException(String reason)
	{
		super(reason);
	}

	/**
	 * @param reason
	 *            a message detailing why this Exception was thrown
	 * @param cause
	 *            the underlying Throwable which caused this exception.
	 */
	public ConfigurationException(String reason, Throwable cause)
	{
		super(reason, cause);
	}

	/**
	 * @param cause
	 *            the underlying Throwable which caused this exception.
	 */
	public ConfigurationException(Throwable cause)
	{
		super(cause);
	}

}

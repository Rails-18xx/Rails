/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/ConfigurationException.java,v 1.4 2008/06/04 19:00:30 evos Exp $ */
package rails.game;

/**
 * Class for reporting problems with reading configuration files.
 */
public class ConfigurationException extends Exception {

    private static final long serialVersionUID = 1L;

    public ConfigurationException() {
        super();
    }

    /**
     * @param reason a message detailing why this Exception was thrown
     */
    public ConfigurationException(String reason) {
        super(reason);
    }

    /**
     * @param reason a message detailing why this Exception was thrown
     * @param cause the underlying Throwable which caused this exception.
     */
    public ConfigurationException(String reason, Throwable cause) {
        super(reason, cause);
    }

    /**
     * @param cause the underlying Throwable which caused this exception.
     */
    public ConfigurationException(Throwable cause) {
        super(cause);
    }

}

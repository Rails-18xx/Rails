package rails.game;

import rails.game.model.Ownable;
import rails.game.model.Portfolio;

/**
 * The superinterface of PrivateCompany and PublicCertificate, which allows
 * objects implementating these interfaces to be combined in start packets and
 * other contexts where their "certificateship" is of interest.
 */
public interface Certificate extends Ownable {

    /**
     * @return Portfolio
     */
    public Portfolio getPortfolio();

    public String getId();

}

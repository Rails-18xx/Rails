package rails.game;

import rails.game.move.Moveable;

/**
 * The superinterface of PrivateCompanyI and PublicCertificateI, which allows
 * objects implementating these interfaces to be combined in start packets and
 * other contexts where their "certificateship" is of interest.
 */
public interface Certificate extends Moveable {

    /**
     * @return Portfolio
     */
    public Portfolio getPortfolio();

    public String getName();

}

package rails.game;

/**
 * A BaseToken object represents a token that a operating public company can
 * place on the map to act as a rail building and train running starting point.
 * <p> The "Base" qualifier is used (more or less) consistently in this
 * rails.game program as it most closely the function of such a token: to act as
 * a base from which a company can operate. Other names used in various games
 * and discussions are "railhead", "station", "garrison", or just "token".
 * 
 * @author Erik Vos
 */
public class BaseToken extends Token {

    PublicCompany company;

    /**
     * Create a BaseToken.
     */
    
    // TODO: Seems that company is a duplicated reference to owner
    public BaseToken(PublicCompany company) {
        super();
        this.company = company;

        /* Initially. a BaseToken is always owned by a company. */
        company.getBaseTokensModel().
        this.moveTo(company);
    }

    public boolean isPlaced() {
        return (getOwner() instanceof Stop);
    }

    public String getId() {
        return company.getId();
    }

    public PublicCompany getCompany() {
        return company;
    }

    @Override
    public String toString() {
        return getId();
    }

}

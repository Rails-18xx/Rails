/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/BaseToken.java,v 1.1 2007/01/23 21:50:42 evos Exp $
 * 
 * Created on Jan 1, 2007
 * Change Log:
 */
package rails.game;

/**
 * A BaseToken object represents a token that a operating public company can
 * place on the map to act as a rail building and train running starting point.
 * <p>
 * The "Base" qualifier is used (more or less) consistently in this rails.game program
 * as it most closely the function of such a token: to act as a base from which a
 * company can operate.
 * Other names used in various games and discussions are "railhead", "station",
 * "garrison", or just "token". 
 * 
 * @author Erik Vos
 */
public class BaseToken extends Token {
    
    PublicCompanyI company;

    /**
     * Create a BaseToken.
     */
    public BaseToken(PublicCompanyI company) {
        super();
        this.company = company;
        
        /* Initially. a BaseToken is always owned by a company. */
        setHolder (company);
    }
    
    public boolean isPlaced () {
        return (holder instanceof Station);
    }
    
    public String getName() {
        return company.getName() + " base token"; 
    }
    
    public PublicCompanyI getCompany () {
        return company;
    }
    
}

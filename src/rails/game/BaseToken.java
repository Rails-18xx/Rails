package rails.game;

import rails.game.state.Item;

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
public final class BaseToken extends Token {

    private BaseToken() {};
    
    public static BaseToken create(PublicCompany company) {
        BaseToken token = new BaseToken();
        token.init(company);
        return token;
    }
    
    @Override
    public void init(Item parent) {
        super.checkedInit(parent, null, PublicCompany.class);

    }
    
    @Override
    public PublicCompany getParent() {
        return (PublicCompany)super.getParent();
    }
    
    public boolean isPlaced() {
        return getParent().getBaseTokensModel().tokenIsLaid(this);
    }

    // TODO: Check if this is correct? Should this really return the company id?
    public String getId() {
        return getParent().getId();
    }

    
    @Override
    public String toString() {
        return getId();
    }

}

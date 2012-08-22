package rails.game.model;

import rails.game.Company;
import rails.game.PrivateCompany;
import rails.game.state.Model;
import rails.game.state.Owner;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioSet;

public final class PrivatesModel extends Model {

    public static final String ID = "PrivatesModel";
    
    private final Portfolio<PrivateCompany> privates;
    
    private boolean addLineBreak = false;

    private PrivatesModel(Owner parent, String id) {
        super(parent, id);
        privates = PortfolioSet.create(parent, "privates", PrivateCompany.class);
    }
    
    /**
     * Creates an initialized PrivatesModel
     */
    public static PrivatesModel create(Owner parent) {
        return new PrivatesModel(parent, ID);
    }

    @Override
    public Owner getParent() {
        return (Owner)super.getParent();
    }
    
    public Portfolio<PrivateCompany> getPortfolio() {
        return privates;
    }
    
    public void moveInto(PrivateCompany p){
        privates.moveInto(p);
    }
    
    
    public void setLineBreak(boolean lineBreak) {
        this.addLineBreak = lineBreak;
    }

    @Override
    public String toText() {

        StringBuffer buf = new StringBuffer("<html>");
        for (Company priv : privates) {
            if (buf.length() > 6)
                buf.append(addLineBreak ? "<br>" : "&nbsp;");
            buf.append(priv.getId());
        }
        if (buf.length() > 6) {
            buf.append("</html>");
        }
        return buf.toString();

    }

}

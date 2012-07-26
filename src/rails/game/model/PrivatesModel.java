package rails.game.model;

import rails.game.Company;
import rails.game.PrivateCompany;
import rails.game.state.Model;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioHolder;
import rails.game.state.PortfolioSet;

public final class PrivatesModel extends Model {

    public static final String ID = "PrivatesModel";
    
    private final Portfolio<PrivateCompany> privates;
    
    private boolean addLineBreak = false;

    private PrivatesModel(PortfolioHolder parent, String id) {
        super(parent, id);
        privates = PortfolioSet.create(parent, "privates", PrivateCompany.class);
    }
    
    /**
     * Creates an initialized PrivatesModel
     * id is identical to class name "PrivatesModel"
     */
    public static PrivatesModel create(PortfolioHolder parent) {
        return new PrivatesModel(parent, ID);
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

    public String getData() {

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

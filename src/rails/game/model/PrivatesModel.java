package rails.game.model;

import rails.game.Company;
import rails.game.PrivateCompany;
import rails.game.state.Item;
import rails.game.state.Model;
import rails.game.state.Portfolio;
import rails.game.state.PortfolioList;


public final class PrivatesModel extends Model {

    public static final String id = "PrivatesModel";
    
    private final PortfolioList<PrivateCompany> privates = PortfolioList.create();
    
    private boolean addLineBreak = false;

    private PrivatesModel() {}
    
    /**
     * Creates an initialized PrivatesModel
     * id is identical to class name "PrivatesModel"
     */
    public static PrivatesModel create(Item parent) {
        PrivatesModel model = new PrivatesModel();
        model.init(parent, id);
        return model;
    }
    
    @Override
    public void init(Item parent, String id){
        super.init(parent, id);
        privates.init(this, "Privates");
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

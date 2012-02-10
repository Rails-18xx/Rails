package rails.game.model;

import rails.game.Company;
import rails.game.PrivateCompany;
import rails.game.state.Item;
import rails.game.state.Portfolio;


public final class PrivatesModel extends Model {

    private final Portfolio<PrivateCompany> privates;
    
    private boolean addLineBreak = false;

    private PrivatesModel() {
        super(PrivatesModel.class.getSimpleName());
        privates = Portfolio.createList("Privates");
    }
    
    /**
     * Creates an initialized PrivatesModel
     * id is identical to class name "PrivatesModel"
     */
    public static PrivatesModel create(Item parent) {
        return new PrivatesModel().init(parent);
    }
    
    @Override
    public PrivatesModel init(Item parent){
        super.init(parent);
        privates.init(this);
        return this;
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

package rails.game.model;

import rails.game.Portfolio;
import rails.game.PrivateCompanyI;

public final class PrivatesModel extends HolderModel<PrivateCompanyI> {

    private boolean addLineBreak = false;

    public PrivatesModel(Portfolio owner) {
        super(owner, "PrivatesModel");
    }
    
    public void setLineBreak(boolean lineBreak) {
        this.addLineBreak = lineBreak;
    }

    public String getData() {

        StringBuffer buf = new StringBuffer("<html>");
        for (PrivateCompanyI priv : this.viewList()) {
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

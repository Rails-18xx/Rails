package rails.game.model;

import rails.game.PrivateCompany;

public final class PrivatesModel extends StorageModel<PrivateCompany> {

    private boolean addLineBreak = false;

    public PrivatesModel(Portfolio owner) {
        super(owner, PrivateCompany.class);
    }
    
    public void setLineBreak(boolean lineBreak) {
        this.addLineBreak = lineBreak;
    }

    public String getData() {

        StringBuffer buf = new StringBuffer("<html>");
        for (PrivateCompany priv : this) {
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

package rails.game.specific._1880;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.PublicCompanyI;
import rails.game.StartItem;
import rails.game.action.PossibleAction;
import rails.game.action.PossibleORAction;
import rails.game.action.StartItemAction;
import rails.util.Util;

public class SetupNewPublicDetails_1880 extends StartItemAction {

    private static final long serialVersionUID = 1L;

    transient protected PublicCompanyI company;
    protected String companyName;
    private int price = 0;
    private int shares = 0;
    private int parSlotIndex = 0;
    private String buildRightsString = "";

    private SetupNewPublicDetails_1880(StartItem startItem,
            PublicCompanyI company) {
        super(startItem);
        this.company = company;
        this.companyName = company.getName();
    }

    public SetupNewPublicDetails_1880(StartItem startItem,
            PublicCompanyI company, int price, int shares) {
        this(startItem, company);
        this.price = price;
        this.shares = shares;
    }

    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        if (Util.hasValue(companyName))
            company = getCompanyManager().getPublicCompany(companyName);
    }

    public PublicCompanyI getCompany() {
        return company;
    }

    public String toString() {
        StringBuffer text = new StringBuffer();
        text.append("SetupNewPublicDetails_1880:");
        text.append("  Company " + companyName);
        text.append("  Price " + price);
        text.append("  Shares " + shares);
        text.append("  ParSlotIndex " + parSlotIndex);
        text.append("  BuildRights " + buildRightsString);
        return text.toString();
    }

    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        if (pa instanceof SetupNewPublicDetails_1880) {
            SetupNewPublicDetails_1880 cpa = (SetupNewPublicDetails_1880) pa;
            if ((cpa.getCompanyName().equals(companyName) == true)
                && (cpa.getPlayerName().equals(playerName) == true)
                && (cpa.getPrice() == price)
                && (cpa.getShares() == shares)
                && (cpa.getParSlotIndex() == parSlotIndex)
                && (cpa.getBuildRightsString().equals(buildRightsString) == true)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean equalsAsAction(PossibleAction pa) { // TODO
        if (pa instanceof SetupNewPublicDetails_1880) {
            SetupNewPublicDetails_1880 cpa = (SetupNewPublicDetails_1880) pa;
            if ((cpa.getCompanyName().equals(companyName) == true)
                && (cpa.getPlayerName().equals(playerName) == true)
                && (cpa.getPrice() == price)
                && (cpa.getShares() == shares)
                && (cpa.getParSlotIndex() == parSlotIndex)
                && (cpa.getBuildRightsString().equals(buildRightsString) == true)) {
                return true;
            }
        }
        return false;
    }

    public int getPrice() {
        return price;
    }

    public int getShares() {
        return shares;
    }

    public int getParSlotIndex() {
        return parSlotIndex;
    }

    public void setParSlotIndex(int parSlotIndex) {
        this.parSlotIndex = parSlotIndex;
    }

    public String getBuildRightsString() {
        return buildRightsString;
    }

    public void setBuildRightsString(String buildRightsString) {
        this.buildRightsString = buildRightsString;
    }

    public Object getCompanyName() {
        return companyName;
    }
}

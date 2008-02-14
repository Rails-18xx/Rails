package rails.game.specific._18EU;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.CompanyManagerI;
import rails.game.Game;
import rails.game.PublicCertificateI;
import rails.game.PublicCompanyI;
import rails.game.action.StartCompany;

public class StartCompany_18EU extends StartCompany {

    private transient List<PublicCompanyI> minorsToMerge = null;
    private String minorsToMergeNames = null;

    private transient PublicCompanyI chosenMinor = null;
    private String chosenMinorName = null;

    public static final long serialVersionUID = 1L;

    public StartCompany_18EU(PublicCertificateI certificate,
			int[] prices, List<PublicCompanyI> minorsToMerge) {
		super (certificate, prices, 1);

		this.minorsToMerge = minorsToMerge;

		if (minorsToMerge != null) {
    		StringBuffer b = new StringBuffer();
    		for (PublicCompanyI minor : minorsToMerge) {
    		    if (b.length() > 0) b.append(",");
    		    b.append (minor.getName());
    		}
    		minorsToMergeNames = b.toString();
		}
	}

	public List<PublicCompanyI> getMinorsToMerge() {
        return minorsToMerge;
    }

    public PublicCompanyI getChosenMinor() {
        return chosenMinor;
    }

    public void setChosenMinor(PublicCompanyI chosenMinor) {
        this.chosenMinor = chosenMinor;
        this.chosenMinorName = chosenMinor.getName();
    }

    @Override
    public String toString() {
		StringBuffer text = new StringBuffer(super.toString());
		if (chosenMinor != null) {
		    text.append(" merged minor="+chosenMinorName);
		} else {
            text.append(" minors=").append(minorsToMergeNames);
		}
        return text.toString();
    }

	/** Deserialize */
    private void readObject (ObjectInputStream in)
    throws IOException, ClassNotFoundException {

        in.defaultReadObject();

        CompanyManagerI cmgr = Game.getCompanyManager();
        if (minorsToMergeNames != null) {
            minorsToMerge = new ArrayList<PublicCompanyI>();
            for (String name : minorsToMergeNames.split(",")) {
                minorsToMerge.add(cmgr.getPublicCompany(name));
            }
        }
        if (chosenMinorName != null) {
            chosenMinor = cmgr.getPublicCompany(chosenMinorName);
        }
    }


}

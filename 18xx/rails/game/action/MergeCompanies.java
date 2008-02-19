/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/action/MergeCompanies.java,v 1.1 2008/02/19 20:14:15 evos Exp $
 *
 * Created on 17-Sep-2006
 * Change Log:
 */
package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;

import rails.game.CompanyManagerI;
import rails.game.Game;
import rails.game.PublicCompanyI;
import rails.game.Train;
import rails.game.TrainI;

/**
 * @author Erik Vos
 */
public class MergeCompanies extends PossibleAction {

    // Server-side settings
    transient protected PublicCompanyI mergingCompany;
    protected String mergingCompanyName;
    transient protected List<PublicCompanyI> targetCompanies;
    protected String targetCompanyNames;
    transient protected List<TrainI> minorTrains;
    protected String minorTrainNames;
    transient protected List<List<TrainI>> majorTrains;
    protected String majorTrainNames;
    protected int majorTrainLimit; // Just one, don't know any exceptions

    // Client-side settings
    transient protected PublicCompanyI selectedTargetCompany = null;
    protected String selectedTargetCompanyName = null;
    protected boolean replaceToken = false;
    transient protected List<TrainI> discardedTrains = null;
    protected String discardedTrainNames = null;

    public static final long serialVersionUID = 1L;

    /**
     * Common constructor.
     */
    public MergeCompanies(PublicCompanyI mergingCompany,
            List<PublicCompanyI>targetCompanies) {
        this.mergingCompany = mergingCompany;
        this.mergingCompanyName = mergingCompany.getName();
        this.targetCompanies = targetCompanies;
        StringBuffer b = new StringBuffer();
        for (PublicCompanyI target : targetCompanies) {
            if (b.length() > 0) b.append(",");
            b.append(target.getName());
        }
        targetCompanyNames = b.toString();

        minorTrains = mergingCompany.getPortfolio().getTrainList();
        b = new StringBuffer();
        for (TrainI train : minorTrains) {
            if (b.length() > 0) b.append(",");
            b.append(train.getUniqueId());
        }
        minorTrainNames = b.toString();

        majorTrains = new ArrayList<List<TrainI>>();
        b = new StringBuffer();
        List<TrainI> trains;
        for (PublicCompanyI major : targetCompanies) {
            trains = major.getPortfolio().getTrainList();
            majorTrains.add(trains);
            if (b.length() > 0) b.append(";");
            for (TrainI train : trains) {
                if (b.length() > 0) b.append(",");
                b.append(train.getUniqueId());
            }
        }
        majorTrainNames = b.toString();

        // Assume all targets have the same train limit - should be so
        majorTrainLimit = targetCompanies.get(0).getCurrentTrainLimit();

    }

    /** Required for deserialization */
    public MergeCompanies() {}

    public PublicCompanyI getMergingCompany () {
        return mergingCompany;
    }

    public List<PublicCompanyI> getTargetCompanies() {
        return targetCompanies;
    }


    public PublicCompanyI getSelectedTargetCompany() {
        return selectedTargetCompany;
    }

    public void setSelectedTargetCompany(PublicCompanyI selectedTargetCompany) {
        this.selectedTargetCompany = selectedTargetCompany;
        selectedTargetCompanyName = selectedTargetCompany.getName();
    }

    public void setReplaceToken (boolean value) {
        replaceToken = value;
    }

    public boolean getReplaceToken () {
        return replaceToken;
    }

    public List<TrainI> getDiscardedTrains() {
        return discardedTrains;
    }

    public void setDiscardedTrains(List<TrainI> discardedTrains) {
        this.discardedTrains = discardedTrains;
        StringBuffer b = new StringBuffer();
        for (TrainI train : discardedTrains) {
            if (b.length() > 0) b.append(",");
            b.append(train.getUniqueId());
        }
        discardedTrainNames = b.toString();
    }

    public List<TrainI> getMinorTrains() {
        return minorTrains;
    }

    public List<List<TrainI>> getMajorTrains() {
        return majorTrains;
    }

    public int getMajorTrainLimit() {
        return majorTrainLimit;
    }

    @Override
    public boolean equals (PossibleAction action) {
        if (!(action instanceof MergeCompanies)) return false;
        MergeCompanies a = (MergeCompanies) action;
        return a.mergingCompanyName.equals(mergingCompanyName)
            && a.targetCompanyNames.equals(targetCompanyNames);
        }

	@Override
    public String toString() {
		StringBuffer text = new StringBuffer();
        text.append("MergeCompanies: ")
            .append(mergingCompanyName)
            .append("[").append(minorTrainNames).append("]")
            .append(" targets=")
            .append(targetCompanyNames)
            .append("[").append(majorTrainNames).append("]");
        if (selectedTargetCompanyName != null) {
            text.append(" selectedTarget=")
        	    .append (selectedTargetCompanyName)
        	    .append (" replaceToken=")
        	    .append (replaceToken);
            if (discardedTrainNames != null) {
                text.append (" discardedTrains=")
                    .append(discardedTrainNames);
            }
        }
        return text.toString();
    }

	private void readObject (ObjectInputStream in)
			throws IOException, ClassNotFoundException {

		in.defaultReadObject();

		CompanyManagerI cmgr = Game.getCompanyManager();

		mergingCompany = cmgr.getCompanyByName (mergingCompanyName);

		targetCompanies = new ArrayList<PublicCompanyI>();
		for (String name : targetCompanyNames.split(",")) {
		    targetCompanies.add (cmgr.getCompanyByName(name));
		}

		if (selectedTargetCompanyName != null) {
		    selectedTargetCompany = cmgr.getCompanyByName(selectedTargetCompanyName);
		}

		minorTrains = new ArrayList<TrainI>();
		for (String trainId : minorTrainNames.split(",")) {
		    minorTrains.add(Train.getByUniqueId(trainId));
		}

		majorTrains = new ArrayList<List<TrainI>>();
		List<TrainI> trainsPerMajor;
		for (String trainIds : majorTrainNames.split(";")) {
		    trainsPerMajor = new ArrayList<TrainI>();
		    for (String trainId : trainIds.split(",")) {
		        if (trainId.length() == 0) continue;
		        trainsPerMajor.add(Train.getByUniqueId(trainId));
		    }
		    majorTrains.add(trainsPerMajor);
		}

		if (discardedTrainNames != null) {
	        discardedTrains = new ArrayList<TrainI>();
	        for (String trainId : discardedTrainNames.split(",")) {
	            discardedTrains.add(Train.getByUniqueId(trainId));
	        }
		}

	}
}

package rails.game.specific._18AL;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.*;

import rails.game.*;
import rails.game.action.*;

public class AssignNamedTrains extends UseSpecialProperty {

    transient private List<NameableTrain> nameableTrains;
    private String[] trainIds;
    int numberOfTrains;
    int numberOfTokens;

    transient private List<NameableTrain> preTrainPerToken;
    private String[] preTrainIds;

    transient private List<NameableTrain> postTrainPerToken;
    private String[] postTrainIds;

    public static final long serialVersionUID = 1L;

    public AssignNamedTrains(NameTrains namedTrainsSpecialProperty,
            List<TrainI> trains) {
        super(namedTrainsSpecialProperty);

        numberOfTrains = trains.size();
        List<NamedTrainToken> tokens = namedTrainsSpecialProperty.getTokens();
        numberOfTokens = tokens.size();

        nameableTrains = new ArrayList<NameableTrain>(numberOfTrains);
        for (TrainI train : trains) {
            nameableTrains.add((NameableTrain) train);
        }
        preTrainPerToken = new ArrayList<NameableTrain>(numberOfTokens);
        postTrainPerToken = new ArrayList<NameableTrain>(numberOfTokens);

        trainIds = new String[numberOfTrains];
        preTrainIds = new String[numberOfTokens];
        postTrainIds = new String[numberOfTokens];

        for (int i = 0; i < numberOfTokens; i++) {
            preTrainPerToken.add(null);
        }

        if (trains != null) {
            int trainIndex = 0;
            int tokenIndex;
            for (NameableTrain train : nameableTrains) {
                trainIds[trainIndex] = train.getUniqueId();
                NamedTrainToken token = train.getNameToken();
                if (token != null) {
                    preTrainPerToken.set(tokens.indexOf(token), train);
                    tokenIndex = tokens.indexOf(token);
                    preTrainIds[tokenIndex] = train.getUniqueId();
                }
                trainIndex++;
            }
        }
    }
    
    public boolean equalsAsAction (PossibleAction action) {
        if (!(action instanceof AssignNamedTrains)) return false;
        AssignNamedTrains a = (AssignNamedTrains) action;
        return Arrays.equals(a.postTrainIds, postTrainIds);
    }

    @Override
    public String toMenu() {
        return ((NameTrains) specialProperty).toMenu();
    }

    @Override
    public String toString() {
        StringBuffer b = new StringBuffer("AssignNamedTrains ");
        for (NamedTrainToken token : ((NameTrains) getSpecialProperty()).getTokens()) {
            b.append(token.toString()).append(",");
        }
        b.deleteCharAt(b.length() - 1);
        return b.toString();
    }

    public List<NamedTrainToken> getTokens() {
        return ((NameTrains) specialProperty).getTokens();
    }

    public List<NameableTrain> getNameableTrains() {
        return nameableTrains;
    }

    public List<NameableTrain> getPreTrainPerToken() {
        return preTrainPerToken;
    }

    public List<NameableTrain> getPostTrainPerToken() {
        return postTrainPerToken;
    }

    public void setPostTrainPerToken(List<NameableTrain> postTokensPerTrain) {
        this.postTrainPerToken = postTokensPerTrain;
        // convert to postTrainIds
        if (postTokensPerTrain != null) {
            for (NameableTrain train : postTokensPerTrain) {
                if (train == null) {
                    postTrainIds[postTokensPerTrain.indexOf(train)] = null;
                } else {
                    postTrainIds[postTokensPerTrain.indexOf(train)] = train.getUniqueId();
                }
            }
        }
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException,
            ClassNotFoundException {

        in.defaultReadObject();

        TrainManager trainManager = GameManager.getInstance().getTrainManager();

        nameableTrains = new ArrayList<NameableTrain>();
        if (trainIds != null) {
            for (String trainId : trainIds) {
                nameableTrains.add((NameableTrain) trainManager.getTrainByUniqueId(trainId));
            }
        }

        preTrainPerToken = new ArrayList<NameableTrain>(numberOfTrains);
        if (preTrainIds != null) {
            for (String trainId : preTrainIds) {
                if (trainId != null && trainId.length() > 0) {
//                    preTrainPerToken.add((NameableTrain) Token.getByUniqueId(trainId));
                  preTrainPerToken.add((NameableTrain) trainManager.getTrainByUniqueId(trainId));
                } else {
                    preTrainPerToken.add(null);
                }
            }
        }

        postTrainPerToken = new ArrayList<NameableTrain>(numberOfTrains);
        if (postTrainIds != null) {
            for (String trainId : postTrainIds) {
                if (trainId != null && trainId.length() > 0) {
//                    postTrainPerToken.add((NameableTrain) Token.getByUniqueId(trainId));
                    postTrainPerToken.add((NameableTrain) trainManager.getTrainByUniqueId(trainId));
                } else {
                    postTrainPerToken.add(null);
                }
            }
        }

    }

}

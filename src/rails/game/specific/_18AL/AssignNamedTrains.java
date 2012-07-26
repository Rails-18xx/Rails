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
    private String[] preTrainds;

    transient private List<NameableTrain> postTrainPerToken;
    private String[] postTrainds;

    public static final long serialVersionUID = 1L;

    public AssignNamedTrains(NameTrains namedTrainsSpecialProperty,
            Set<Train> trains) {
        super(namedTrainsSpecialProperty);

        numberOfTrains = trains.size();
        List<NamedTrainToken> tokens = namedTrainsSpecialProperty.getTokens();
        numberOfTokens = tokens.size();

        nameableTrains = new ArrayList<NameableTrain>(numberOfTrains);
        for (Train train : trains) {
            nameableTrains.add((NameableTrain) train);
        }
        preTrainPerToken = new ArrayList<NameableTrain>(numberOfTokens);
        postTrainPerToken = new ArrayList<NameableTrain>(numberOfTokens);

        trainIds = new String[numberOfTrains];
        preTrainds = new String[numberOfTokens];
        postTrainds = new String[numberOfTokens];

        for (int i = 0; i < numberOfTokens; i++) {
            preTrainPerToken.add(null);
        }

        if (trains != null) {
            int trainIndex = 0;
            int tokenIndex;
            for (NameableTrain train : nameableTrains) {
                trainIds[trainIndex] = train.getId();
                NamedTrainToken token = train.getNameToken();
                if (token != null) {
                    preTrainPerToken.set(tokens.indexOf(token), train);
                    tokenIndex = tokens.indexOf(token);
                    preTrainds[tokenIndex] = train.getId();
                }
                trainIndex++;
            }
        }
    }
    
    public boolean equalsAsAction (PossibleAction action) {
        if (!(action instanceof AssignNamedTrains)) return false;
        AssignNamedTrains a = (AssignNamedTrains) action;
        return Arrays.equals(a.postTrainds, postTrainds);
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
        // convert to postTrainds
        if (postTokensPerTrain != null) {
            for (NameableTrain train : postTokensPerTrain) {
                if (train == null) {
                    postTrainds[postTokensPerTrain.indexOf(train)] = null;
                } else {
                    postTrainds[postTokensPerTrain.indexOf(train)] = train.getId();
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
        if (preTrainds != null) {
            for (String trainId : preTrainds) {
                if (trainId != null && trainId.length() > 0) {
//                    preTrainPerToken.add((NameableTrain) Token.getByUniqueId(trainId));
                  preTrainPerToken.add((NameableTrain) trainManager.getTrainByUniqueId(trainId));
                } else {
                    preTrainPerToken.add(null);
                }
            }
        }

        postTrainPerToken = new ArrayList<NameableTrain>(numberOfTrains);
        if (postTrainds != null) {
            for (String trainId : postTrainds) {
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

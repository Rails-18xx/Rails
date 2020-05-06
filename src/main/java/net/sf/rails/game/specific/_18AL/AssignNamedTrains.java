package net.sf.rails.game.specific._18AL;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.google.common.base.Objects;

import net.sf.rails.game.RailsRoot;
import net.sf.rails.game.Train;
import net.sf.rails.game.TrainManager;
import net.sf.rails.util.GameLoader;
import net.sf.rails.util.RailsObjects;
import rails.game.action.PossibleAction;
import rails.game.action.UseSpecialProperty;


/**
 * Rails 2.0: Updated equals and toString methods
 */
// TODO: Rails 2.0 This seems a pretty complicated action. Is this really required for the task?
public class AssignNamedTrains extends UseSpecialProperty {

    private final transient List<NameableTrain> nameableTrains = new ArrayList<>();
    private final String[] trainIds;

    private final transient List<NameableTrain> preTrainPerToken = new ArrayList<>();
    private final String[] preTrainds;

    private transient List<NameableTrain> postTrainPerToken = new ArrayList<>();
    private final String[] postTrainds;

    public static final long serialVersionUID = 1L;

    public AssignNamedTrains(NameTrains namedTrainsSpecialProperty, @NotNull Set<Train> trains) {
        super(namedTrainsSpecialProperty);

        int numberOfTrains = trains.size();
        List<NamedTrainToken> tokens = namedTrainsSpecialProperty.getTokens();
        int numberOfTokens = tokens.size();

        for (Train train : trains) {
            nameableTrains.add((NameableTrain) train);
        }
        trainIds = new String[numberOfTrains];
        preTrainds = new String[numberOfTokens];
        postTrainds = new String[numberOfTokens];

        for ( int i = 0; i < numberOfTokens; i++) {
            preTrainPerToken.add(null);
        }

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

    @Override
    public String toMenu() {
        return specialProperty.toMenu();
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

    @Override
    protected boolean equalsAs(PossibleAction pa, boolean asOption) {
        // identity always true
        if (pa == this) return true;
        //  super checks both class identity and super class attributes
        if (!super.equalsAs(pa, asOption)) return false;

        // check asOption attributes
        AssignNamedTrains action = (AssignNamedTrains)pa;
        boolean options =
                Objects.equal(this.nameableTrains, action.nameableTrains)
                && Objects.equal(this.preTrainPerToken, action.preTrainPerToken)
        ;

        // finish if asOptions check
        if (asOption) return options;

        // check asAction attributes
        return options
                && Objects.equal(this.postTrainPerToken, action.postTrainPerToken)
        ;
    }
    @Override
    public String toString() {
        return super.toString() +
                RailsObjects.stringHelper(this)
                    .addToString("nameableTrains", nameableTrains)
                    .addToString("preTrainPerToken", preTrainPerToken)
                    .addToStringOnlyActed("postTrainPerToken", postTrainPerToken)
                    .toString()
        ;
    }

    /** Deserialize */
    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();

        TrainManager trainManager = root.getTrainManager();

        if (trainIds != null) {
            for (String trainId : trainIds) {
                nameableTrains.add((NameableTrain) trainManager.getTrainByUniqueId(trainId));
            }
        }

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

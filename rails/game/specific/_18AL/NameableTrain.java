package rails.game.specific._18AL;

import rails.game.Train;
import rails.game.TrainTypeI;
import rails.game.move.MoveableHolder;
import rails.game.move.ObjectMove;
import rails.game.move.StateChange;
import rails.game.state.State;

public class NameableTrain extends Train {

    private State nameToken;

    @Override
    public void init(TrainTypeI type, int index) {

        super.init(type, index);
        nameToken = new State(uniqueId + "_nameToken", NamedTrainToken.class);
    }

    public void setNameToken(NamedTrainToken nameToken) {
        // this.nameToken = nameToken;
        new StateChange(this.nameToken, nameToken, holder.getTrainsModel());
    }

    public NamedTrainToken getNameToken() {
        return (NamedTrainToken) nameToken.get();
    }

    @Override
    public void moveTo(MoveableHolder to) {
        if (holder != to) {
            if (getNameToken() != null) {
                setNameToken(null);
            }
            // new TrainMove (this, holder, to);
            new ObjectMove(this, holder, to);
        }
    }

    @Override
    public String toDisplay() {
        NamedTrainToken token = getNameToken();
        if (token == null) {
            return getName();
        } else {
            return getName() + "\'" + token.getName() + "\'";
        }
    }

}

package net.sf.rails.game.specific._18AL;

import net.sf.rails.game.RailsItem;
import net.sf.rails.game.Train;
import net.sf.rails.game.state.GenericState;

// FIXME: Check train creation methods

public final class NameableTrain extends Train {

    private final GenericState<NamedTrainToken> nameToken = GenericState.create(this, "nameToken");
    
    /**
     * Created via Configure
     */
    public NameableTrain(RailsItem parent, String id) {
        super(parent, id);
    }

    public void setNameToken(NamedTrainToken nameToken) {
        // TODO: Add trainsmodel as dependency
        // new StateChange(this.nameToken, nameToken, holder.getTrainsModel());
        this.nameToken.set(nameToken);
    }

    public NamedTrainToken getNameToken() {
        return (NamedTrainToken) nameToken.value();
    }

    @Override
    public String toText() {
        NamedTrainToken token = getNameToken();
        if (token == null) {
            return super.toText();
        } else {
            return super.toText() + "\'" + token.getId() + "\'";
        }
    }

}

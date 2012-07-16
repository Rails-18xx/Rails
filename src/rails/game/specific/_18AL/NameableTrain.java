package rails.game.specific._18AL;

import rails.game.Train;
import rails.game.state.GenericState;
import rails.game.state.Item;

// FIXME: Check train creation methods

public final class NameableTrain extends Train {

    private final GenericState<NamedTrainToken> nameToken = GenericState.create(this, "nameToken");
    
    private NameableTrain(Item parent, String id) {
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
    public String toDisplay() {
        NamedTrainToken token = getNameToken();
        if (token == null) {
            return getId();
        } else {
            return getId() + "\'" + token.getId() + "\'";
        }
    }

}

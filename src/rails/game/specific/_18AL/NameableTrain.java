package rails.game.specific._18AL;

import rails.game.Train;
import rails.game.state.GenericState;
import rails.game.state.Item;

public class NameableTrain extends Train {

    private final GenericState<NamedTrainToken> nameToken = GenericState.create();

    @Override
    public void init(Item parent, String id) {
        super.init(parent, id);
        nameToken.init(this, id + "_nameToken");
    }

    public void setNameToken(NamedTrainToken nameToken) {
        // TODO: Add trainsmodel as dependency
        // new StateChange(this.nameToken, nameToken, holder.getTrainsModel());
        this.nameToken.set(nameToken);
        
    }

    public NamedTrainToken getNameToken() {
        return (NamedTrainToken) nameToken.get();
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

package rails.game.specific._18AL;

import rails.game.*;
import rails.game.state.GenericState;

public class NameableTrain extends Train {

    private GenericState<NamedTrainToken> nameToken;

    @Override
    public void init(TrainCertificateType certType, TrainType type, String uniqueId) {

        super.init(certType, type, uniqueId);
        nameToken = new GenericState<NamedTrainToken>(this, uniqueId + "_nameToken");
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

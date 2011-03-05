package rails.game.action;

import java.io.IOException;
import java.io.ObjectInputStream;

import rails.game.Player;

public class RequestTurn extends PossibleAction {

    public static final long serialVersionUID = 1L;

    private String requestingPlayerName;

    public RequestTurn (Player player) {
        super();
        // Override player set by superclass
        if (player != null) {
            requestingPlayerName = player.getName();
        }
    }

       public String getRequestingPlayerName() {
        return requestingPlayerName;
    }

    @Override
    public boolean equalsAsOption(PossibleAction pa) {
        return pa != null
                && pa instanceof RequestTurn
                && requestingPlayerName.equals(((RequestTurn)pa).requestingPlayerName);
    }

    public boolean equalsAsAction(PossibleAction pa) {
        return equalsAsOption (pa);
    }

    private void readObject(ObjectInputStream in) throws IOException,
        ClassNotFoundException {

            in.defaultReadObject();
        }


    @Override
    public String toString() {
        return requestingPlayerName+" requests turn";
    }

}

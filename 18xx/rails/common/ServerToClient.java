package rails.common;

import java.io.Serializable;

/**
 * Instances of this class are intended to carry all data that
 * (after the foreseen client/server split) would be sent from
 * the server (game enigine) to the client (GUI) after completion
 * of the processing of each player action.
 * All contents of this class must be Serializable.
 * <p>This class is still in its infancy. Over time it will probably
 * absorb the current PossibleActions and DisplayBuffer classes,
 * and also include many details that the GUI now obtains
 * via direct calls to server methods.
 * @author VosE
 *
 */
public class ServerToClient implements Serializable {

    public static final long serialVersionUID = 1L;

    private GuiHints guiHints = null;

    public GuiHints getUiHints() {
        return guiHints;
    }

    public void setUiHints(GuiHints guiHints) {
        this.guiHints = guiHints;
    }


}

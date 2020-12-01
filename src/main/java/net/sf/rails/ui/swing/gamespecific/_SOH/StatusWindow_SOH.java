package net.sf.rails.ui.swing.gamespecific._SOH;

import net.sf.rails.common.LocalText;
import net.sf.rails.ui.swing.StatusWindow;

public class StatusWindow_SOH extends StatusWindow {

    /**
     * The only difference with the superclass version is,
     * that gameStatus.recreate() is not called.
     * Not only calling that method isn't necessary in SOH, but also
     * it leaves a blank StatusWindow each time treasury share
     * trading (the first OR step) is 'done'.
     */
    @Override
    public void finishRound() {
        setTitle(LocalText.getText("GAME_STATUS_TITLE"));
        gameStatus.initTurn(-1, true);
        passButton.setEnabled(false);
    }

}

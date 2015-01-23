package net.sf.rails.ui.swing;

import java.util.List;

import net.sf.rails.game.Player;
import net.sf.rails.game.StartItem;
import net.sf.rails.game.StartRound;

public class StartRoundWindowNG extends StartRoundWindow {
    
    private static final long serialVersionUID = 1L;

    @Override
    public void init(StartRound round, GameUIManager parent) {
        
        List<StartItem> startItems = round.getStartPacket().getItems();
        List<Player> players = parent.getPlayers();
        
        StartRoundStatus status = new StartRoundStatus(startItems, players);
    }

    
    
}

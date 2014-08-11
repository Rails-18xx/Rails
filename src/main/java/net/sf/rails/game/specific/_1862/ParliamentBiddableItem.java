package net.sf.rails.game.specific._1862;

import java.util.ArrayList;

import com.google.common.collect.ImmutableList;

import net.sf.rails.game.Player;
import net.sf.rails.game.RailsAbstractItem;
import net.sf.rails.game.RailsItem;
import net.sf.rails.game.model.CountingMoneyModel;

public class ParliamentBiddableItem extends RailsAbstractItem {
    private PublicCompany_1862 company;
    private ImmutableList<Player> players;
    private ArrayList<CountingMoneyModel> bids;
    private int currentBid;
    private boolean hasBeenBidOn;
    private boolean hasBeenSold;
    static private ArrayList<Integer> possibleParValues;
    static private int minParValue;
    static private int minimumBid;
    static private int bidIncrement;
    
    static {
        possibleParValues = new ArrayList<Integer>();
        possibleParValues.add(54);
        possibleParValues.add(58);
        possibleParValues.add(62);
        possibleParValues.add(68);
        possibleParValues.add(74);
        possibleParValues.add(82);
        possibleParValues.add(90);
        possibleParValues.add(100);        

        minParValue = 54;
        minimumBid = 0;
        bidIncrement = 5;   
    }

    protected ParliamentBiddableItem(RailsItem parent, String id) {
        super(parent, id);        
    }
    
    public ParliamentBiddableItem(RailsItem parent, String id, PublicCompany_1862 company) {
        this(parent, id);
        this.company = company;
        
        bids = new ArrayList<CountingMoneyModel>();
        players = getRoot().getPlayerManager().getPlayers();
        for (int i = 0; i < players.size(); i++) {
            CountingMoneyModel bid = CountingMoneyModel.create(this, "bidBy_" + players.get(i).getId(), false);
            bid.setSuppressZero(true);
            bids.add(bid);
        }
        currentBid = 0;
        hasBeenBidOn = false;
        hasBeenSold = false;
    }
    
    public PublicCompany_1862 getCompany() {
        return company;
    }

    public ArrayList<CountingMoneyModel> getBidModels() {
        return bids;
    }

    public void performBid(Player player, int actualBid) {
        for (int i = 0; i < players.size(); i++) {
            if (players.get(i) == player) {
                bids.get(i).set(actualBid);
                bids.get(i).setSuppressZero(false);
            }
        }
    }
    
    public ArrayList<Integer> getPossibleParValues() {
        return possibleParValues;
    }
    
    public int getMinParValue() {
        return minParValue;
    }
    
    public int getMinimumBid() {
        return minimumBid;
    }
    
    public int getBidIncrement() {
        return bidIncrement;
    }

    public int getCurrentBid() {
        return currentBid;
    }
    
    public void setCurrentBid(int bid) {
        currentBid = bid;
        hasBeenBidOn = true;
    }

    public boolean hasBeenBidOn() {
        return hasBeenBidOn;
    }
    
    public boolean hasBeenSold() {
        return hasBeenSold;
    }
    
    public void sold() {
        hasBeenSold = true;
    }
}



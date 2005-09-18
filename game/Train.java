/*
 * Rails: an 18xx game system. Copyright (C) 2005 Brett Lentz
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc., 59 Temple
 * Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package game;

public class Train implements TrainI
{
    protected TrainTypeI type;
    
    protected int majorStops;
    protected int minorStops;
    protected int cost;
    protected int cityScoreFactor;
    protected int townScoreFactor;
    protected int townCountIndicator;
    
    protected Portfolio holder;
    protected boolean rusted = false;
    
    protected static final Portfolio unavailable = Bank.getUnavailable();
    protected static final Portfolio ipo = Bank.getIpo();
    
    public Train (TrainTypeI type) {
        
        this.type = type;
        this.majorStops = type.getMajorStops();
        this.minorStops = type.getMinorStops();
        this.cost = type.getCost();
        this.cityScoreFactor = type.getCityScoreFactor();
        this.townScoreFactor = type.getTownScoreFactor();
        this.townCountIndicator = type.getTownCountIndicator();
        
       unavailable.addTrain(this);
        
    }
    
    
   	

    /**
     * @return Returns the cityScoreFactor.
     */
    public int getCityScoreFactor() {
        return cityScoreFactor;
    }
    /**
     * @return Returns the cost.
     */
    public int getCost() {
        return cost;
    }
    /**
     * @return Returns the majorStops.
     */
    public int getMajorStops() {
        return majorStops;
    }
    /**
     * @return Returns the minorStops.
     */
    public int getMinorStops() {
        return minorStops;
    }
    /**
     * @return Returns the townCountIndicator.
     */
    public int getTownCountIndicator() {
        return townCountIndicator;
    }
    /**
     * @return Returns the townScoreFactor.
     */
    public int getTownScoreFactor() {
        return townScoreFactor;
    }
    /**
     * @return Returns the type.
     */
    public TrainTypeI getType() {
        return type;
    }
    
    public String getName() {
        return type.getName();
    }
    
    public Portfolio getHolder () {
        return holder;
    }
    
    public CashHolder getOwner() {
        return holder.getOwner();
    }
    
    /**
     * Move the train to another Portfolio.
     */
    public void setHolder (Portfolio newHolder) {
        holder = newHolder;
    }
    
    public void setRusted () {
        rusted = true;
        Portfolio.transferTrain(this, holder, Bank.getScrapHeap());
    }
}
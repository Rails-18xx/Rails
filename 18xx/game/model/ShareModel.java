/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/model/Attic/ShareModel.java,v 1.1 2006/01/22 21:09:58 evos Exp $
 * 
 * Created on 10-Dec-2005
 * Change Log:
 */
package game.model;

import game.Portfolio;
import game.PublicCompanyI;

/**
 * @author Erik Vos
 */
public class ShareModel extends ModelObject {
    
    private int share;
    private Portfolio portfolio;
    private PublicCompanyI company; 
    
    public ShareModel (Portfolio portfolio, PublicCompanyI company) {
        this.portfolio = portfolio;
        this.company = company;
        this.share = 0;
        //System.out.println("SHAREMODEL.ShareModel share="+share);
    }
    
    public void setShare (int share) {
        this.share = share;
        //System.out.println("SHAREMODEL.setShare share="+share);
        notifyViewObjects();
    }
    
    public void setShare () {
        this.share = portfolio.ownsShare(company);
        //System.out.println("SHAREMODEL.setShare2 share="+share);
        notifyViewObjects();
    }
    
    public void addShare (int addedShare) {
        share += addedShare;
        //System.out.println("SHAREMODEL.addShare share="+share);
        notifyViewObjects();
    }
    
    public int getShare () {
        return share;
    }

    public String toString() {
        if (share == 0) return  "";
        return share + "%";
    }

}

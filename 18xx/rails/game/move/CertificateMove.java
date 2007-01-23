/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Attic/CertificateMove.java,v 1.1 2007/01/23 21:50:50 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package rails.game.move;

import rails.game.*;

/**
 * @author Erik Vos
 */
public class CertificateMove extends Move {
    
    PublicCertificateI certificate;
    Portfolio from;
    Portfolio to;
    
    public CertificateMove (Portfolio from, Portfolio to, PublicCertificateI certificate) {
        
        this.certificate = certificate;
        this.from = from;
        this.to = to;
    }


    public boolean execute() {

        Portfolio.transferCertificate(certificate, from, to);
        return true;
    }

    public boolean undo() {
        
        Portfolio.transferCertificate(certificate, to, from);
        return true;
    }
    
    public String toString() {
        return "CertMove: "+certificate.getShare()+"%"
        	+ (certificate.isPresidentShare() ? "(Pres) " : " ")
        	+ certificate.getCompany().getName()
        	+ " from "+from.getName()+" to "+to.getName();
   }

}

/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/move/Attic/CertificateMove.java,v 1.1 2006/09/14 19:33:31 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.move;

import game.*;

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

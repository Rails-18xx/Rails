/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/CertificateMove.java,v 1.1 2006/07/17 22:00:23 evos Exp $
 * 
 * Created on 17-Jul-2006
 * Change Log:
 */
package game.action;

import game.*;

/**
 * @author Erik Vos
 */
public class CertificateMove extends Move {
    
    PublicCertificateI certificate;
    Portfolio from;
    Portfolio to;
    
    public CertificateMove (PublicCertificateI certificate, Portfolio from, Portfolio to) {
        
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

}

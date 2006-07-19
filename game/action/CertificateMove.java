/* $Header: /Users/blentz/rails_rcs/cvs/18xx/game/action/Attic/CertificateMove.java,v 1.2 2006/07/19 22:08:50 evos Exp $
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

}

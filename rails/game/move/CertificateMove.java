/* $Header: /Users/blentz/rails_rcs/cvs/18xx/rails/game/move/Attic/CertificateMove.java,v 1.3 2007/05/20 20:10:19 evos Exp $
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
    
    Certificate certificate;
    Portfolio from;
    Portfolio to;
    
    public CertificateMove (Portfolio from, Portfolio to, Certificate certificate) {
        
        this.certificate = certificate;
        this.from = from;
        this.to = to;
        
        MoveSet.add (this);
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
    	
    	String certType, certDesc;
    	if (certificate instanceof PublicCertificateI) {
    		PublicCertificateI pc = (PublicCertificateI) certificate;
    		certType = "public";
	        certDesc = pc.getShare()+"%"
	        	+ (pc.isPresidentShare() ? "(Pres) " : " ")
	        	+ pc.getCompany().getName();
    	} else if (certificate instanceof PrivateCompanyI) {
    		certType = "private";
    		certDesc = ((PrivateCompanyI) certificate).getName();
    	} else {
    		certType = "unknown: " + certificate.getClass().getName();
    		certDesc = certificate.getName();
    	}
		return "CertMove (" + certType + "): " + certDesc 
			+ " from " + from.getName() + " to " + to.getName();
    	
   }

}

package net.sf.rails.game;

import java.util.HashMap;
import java.util.Map;

import net.sf.rails.game.financial.PublicCertificate;

public class CertificateManager  extends RailsManager {

    protected static Map<String, PublicCertificate> certMap = new HashMap<>();

    protected CertificateManager(RailsItem parent, String id) {
        super(parent, id);
    }

    public void addCertificate(String certId, PublicCertificate certificate) {
        certMap.put(certId, certificate);
    }

    public PublicCertificate getCertificate(String certId) {
        return certMap.get(certId);
    }

}

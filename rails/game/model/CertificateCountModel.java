package rails.game.model;


// TODO: Check what is required to get the update correctly
public final class CertificateCountModel extends AbstractModel<String> {

    private final Portfolio owner;
    
    public CertificateCountModel(Portfolio owner) {
        super(owner, "CertificateCountModel");
        this.owner = owner;
    }

    public String getData() {
        return ("" + owner.getCertificateCount()).replaceFirst("\\.0", "").replaceFirst("\\.5", "\u00bd");
    }

}

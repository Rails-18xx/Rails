package rails.game;

/**
 * The superinterface of PrivateCompany and PublicCertificate, which allows
 * objects implementating these interfaces to be combined in start packets and
 * other contexts where their "certificateship" is of interest.
 * 
 * TODO: Check if this is still needed (or replaced by Ownable) or could be extended by 
 * combining methods from both public and private certificates
 */
public interface Certificate extends RailsItem {


}

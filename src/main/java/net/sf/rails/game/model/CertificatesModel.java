package net.sf.rails.game.model;

import com.google.common.collect.*;
import net.sf.rails.game.Player;
import net.sf.rails.game.PublicCompany;
import net.sf.rails.game.RailsOwner;
import net.sf.rails.game.financial.Bank;
import net.sf.rails.game.financial.BankPortfolio;
import net.sf.rails.game.financial.PublicCertificate;
import net.sf.rails.game.state.PortfolioMap;
import org.paukov.combinatorics.CombinatoricsFactory;
import org.paukov.combinatorics.Generator;
import org.paukov.combinatorics.ICombinatoricsVector;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.SortedSet;


/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies,
 * but I doubt it
 * // TODO: find out where the president model has to be linked
 * // this.addModel(company.getPresidentModel());
 */
public class CertificatesModel extends RailsModel implements Iterable<PublicCertificate> {

    public final static String ID = "CertificatesModel";

    private final PortfolioMap<PublicCompany, PublicCertificate> certificates;

    private final HashMap<PublicCompany, ShareModel> shareModels = Maps.newHashMap();

    private final HashMap<PublicCompany, ShareDetailsModel> shareDetailsModels = Maps.newHashMap();

    private CertificatesModel(RailsOwner parent) {
        super(parent, ID);
        // certificates have the Owner as parent directly
        certificates = PortfolioMap.create(parent, "certificates", PublicCertificate.class);
        // so make this model updating
        certificates.addModel(this);
    }

    public static CertificatesModel create(RailsOwner parent) {
        return new CertificatesModel(parent);
    }

    @Override
    public RailsOwner getParent() {
        return (RailsOwner) super.getParent();
    }

    void initShareModels(Iterable<PublicCompany> companies) {
        // create shareModels
        for (PublicCompany company : companies) {
            ShareModel model = ShareModel.create(this, company);
            shareModels.put(company, model);
            ShareDetailsModel modelDetails = ShareDetailsModel.create(this, company);
            shareDetailsModels.put(company, modelDetails);
        }
    }

    ShareModel getShareModel(PublicCompany company) {
        return shareModels.get(company);
    }

    ShareDetailsModel getShareDetailsModel(PublicCompany company) {
        return shareDetailsModels.get(company);
    }

    float getCertificateCount() {
        float number = 0;
        for (PublicCertificate cert : certificates) {
            PublicCompany company = cert.getCompany();
            if (!company.hasFloated() || !company.hasStockPrice()
                    || !cert.getCompany().getCurrentSpace().isNoCertLimit()) {
                number += cert.getCertificateCount();
            }
        }
        return number;
    }

    boolean contains(PublicCompany company) {
        return certificates.containsKey(company);
    }

    ImmutableSortedSet<PublicCertificate> getCertificates(
            PublicCompany company) {
        return certificates.items(company);
    }

    ImmutableMultimap<String, PublicCertificate> getCertificatesByType(PublicCompany company) {
        ImmutableMultimap.Builder<String, PublicCertificate> certs = ImmutableMultimap.builder();
        for (PublicCertificate c : certificates.items(company)) {
            certs.put(c.getTypeId(), c);
        }
        return certs.build();
    }

    SortedMultiset<Integer> getCertificateTypeCounts(PublicCompany company) {
        ImmutableSortedMultiset.Builder<Integer> certCount = ImmutableSortedMultiset.naturalOrder();
        for (PublicCertificate cert : getCertificates(company)) {
            if (!cert.isPresidentShare()) {
                certCount.add(cert.getShares());
            }
        }
        return certCount.build();
    }

    PortfolioMap<PublicCompany, PublicCertificate> getPortfolio() {
        return certificates;
    }

    @Override
    public Iterator<PublicCertificate> iterator() {
        return certificates.iterator();
    }

    int getShare(PublicCompany company) {
        int share = 0;
        for (PublicCertificate cert : certificates.items(company)) {
            share += cert.getShare();
        }
        return share;
    }

    int getShareNumber(PublicCompany company) {
        int shareNumber = 0;
        for (PublicCertificate cert : certificates.items(company)) {
            shareNumber += cert.getShares();
        }
        return shareNumber;
    }

    SortedSet<Integer> getshareNumberCombinations(PublicCompany company, int maxShareNumber) {
        return shareNumberCombinations(certificates.items(company), maxShareNumber, true);
    }

    boolean containsMultipleCert(PublicCompany company) {
        for (PublicCertificate cert : certificates.items(company)) {
            if (cert.getShares() != 1 && !cert.isPresidentShare()) {
                return true;
            }
        }
        return false;
    }

    String toText(PublicCompany company) {
        int share = this.getShare(company);

        //if (share == 0) return "";
        StringBuilder b = new StringBuilder();
        if (share > 0) b.append(share).append("%");

        RailsOwner parent = getParent();
        BankPortfolio pool = Bank.getPool(parent);
        if (parent instanceof Player && company.getPresident() == parent
                || parent == pool && pool.getPortfolioModel().findCertificate(company, true) != null) {
            b.append("P");
            if (!company.hasFloated()) b.append("U");
            b.append(company.getExtraShareMarks());
        }
        return b.toString();
    }

    @Override
    public String toText() {
        return certificates.toString();
    }


    /**
     * @param certificates   list of certificates
     * @param maxShareNumber maximum share number that is to achieved
     * @return sorted list of share numbers that are possible from the list of certificates
     */
    public static SortedSet<Integer> shareNumberCombinations(Collection<PublicCertificate> certificates,
                                                             int maxShareNumber,
                                                             boolean includePresident) {

        // create vector for combinatorics
        ICombinatoricsVector<PublicCertificate> certVector = CombinatoricsFactory.createVector(certificates);

        // create generator for subsets
        Generator<PublicCertificate> certGenerator = CombinatoricsFactory.createSubSetGenerator(certVector);

        ImmutableSortedSet.Builder<Integer> numbers = ImmutableSortedSet.naturalOrder();
        for (ICombinatoricsVector<PublicCertificate> certSubSet : certGenerator) {
            int sum = 0;
            for (PublicCertificate cert : certSubSet) {
                if (cert.isPresidentShare() && !includePresident) continue;
                sum += cert.getShares();
                if (sum > maxShareNumber) {
                    break;
                }
            }
            if (sum <= maxShareNumber) {
                numbers.add(sum);
            }
        }

        return numbers.build();
    }

    public static SortedSet<PublicCertificate.Combination> certificateCombinations(Collection<PublicCertificate> certificates, int shareNumber) {

        // create vector for combinatorics
        ICombinatoricsVector<PublicCertificate> certVector = CombinatoricsFactory.createVector(certificates);

        // create generator for subsets
        Generator<PublicCertificate> certGenerator = CombinatoricsFactory.createSubSetGenerator(certVector);

        // add all subset that equal the share number to the set of combinations
        ImmutableSortedSet.Builder<PublicCertificate.Combination> combinations = ImmutableSortedSet.naturalOrder();

        for (ICombinatoricsVector<PublicCertificate> certSubSet : certGenerator) {
            int sum = 0;
            for (PublicCertificate cert : certSubSet) {
                sum += cert.getShares();
                if (sum > shareNumber) {
                    break;
                }
            }
            if (sum == shareNumber) {
                combinations.add(PublicCertificate.Combination.create(certSubSet));
            }
        }
        return combinations.build();
    }
}

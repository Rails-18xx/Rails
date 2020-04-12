package net.sf.rails.game;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import net.sf.rails.common.LocalText;
import net.sf.rails.common.parser.Configurable;
import net.sf.rails.common.parser.ConfigurationException;
import net.sf.rails.common.parser.Tag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static java.util.function.Predicate.not;

public class CompanyManager extends RailsManager implements Configurable {

    private static final Logger log = LoggerFactory.getLogger(CompanyManager.class);

    /**
     * This is the name by which the CompanyManager should be registered with
     * the ComponentManager.
     */
    public static final String COMPONENT_NAME = "CompanyManager";

    /**
     * A List with all private companies
     */
    private final List<PrivateCompany> lPrivateCompanies = new ArrayList<>();

    /**
     * A List with all public companies
     */
    private final List<PublicCompany> lPublicCompanies = new ArrayList<>();

    /**
     * A map with all private companies by name
     */
    private final Map<String, PrivateCompany> mPrivateCompanies = new HashMap<>();

    /**
     * A map with all public (i.e. non-private) companies by name
     */
    private final Map<String, PublicCompany> mPublicCompanies = new HashMap<>();

    /**
     * A map of all type names to maps of companies of that type by name
     */
    // TODO Redundant, current usage can be replaced.
    private final Map<String, Map<String, Company>> mCompaniesByTypeAndName = new HashMap<>();

    /**
     * A list of all company types
     */
    private final List<CompanyType> lCompanyTypes = new ArrayList<>();

    /**
     * A list of all start packets (usually one)
     */
    protected final List<StartPacket> startPackets = new ArrayList<>();
    /**
     * A map of all start packets, keyed by name. Default name is "Initial"
     */
    private final Map<String, StartPacket> startPacketMap = new HashMap<>();

    /**
     * A map to enable translating aliases to names
     */
    protected Map<String, String> aliases;

    protected GameManager gameManager;

    private int numberOfPublicCompanies = 0;

    /**
     * Used by Configure (via reflection) only
     */
    public CompanyManager(RailsRoot parent, String id) {
        super(parent, id);
    }

    /*
     * NOTES: 1. we don't have a map over all companies, because some games have
     * duplicate names, e.g. B&O in 1830. 2. we have both a map and a list of
     * private/public companies to preserve configuration sequence while
     * allowing direct access.
     */

    /**
     *
     */
    public void configureFromXML(Tag tag) throws ConfigurationException {
        this.gameManager = getRoot().getGameManager();

        /** A map with all company types, by type name */
        // Localised here as it has no permanent use
        Map<String, CompanyType> mCompanyTypes = new HashMap<>();

        //NEW//
        Map<String, Tag> typeTags = new HashMap<>();

        for (Tag compTypeTag : tag.getChildren(CompanyType.ELEMENT_ID)) {
            // Extract the attributes of the Component
            String name = compTypeTag.getAttributeAsString(CompanyType.NAME_TAG);
            if (name == null) {
                throw new ConfigurationException(LocalText.getText("UnnamedCompanyType"));
            }

            String className = compTypeTag.getAttributeAsString(CompanyType.CLASS_TAG);
            if (className == null) {
                throw new ConfigurationException(LocalText.getText("CompanyTypeHasNoClass", name));
            }

            if (mCompanyTypes.get(name) != null) {
                throw new ConfigurationException(LocalText.getText("CompanyTypeConfiguredTwice", name));
            }

            CompanyType companyType = CompanyType.create(this, name, className);
            mCompanyTypes.put(name, companyType);
            lCompanyTypes.add(companyType);

            // Further parsing is done within CompanyType
            companyType.configureFromXML(compTypeTag);

            //NEW//
            typeTags.put(name, compTypeTag);
        }

        /* Read and configure the companies */
        for (Tag companyTag : tag.getChildren(Company.COMPANY_ELEMENT_ID)) {
            // Extract the attributes of the Component
            String name = companyTag.getAttributeAsString(Company.COMPANY_NAME_TAG);
            if (name == null) {
                throw new ConfigurationException(LocalText.getText("UnnamedCompany"));
            }

            String type = companyTag.getAttributeAsString(Company.COMPANY_TYPE_TAG);
            if (type == null) {
                throw new ConfigurationException(LocalText.getText("CompanyHasNoType", name));
            }

            CompanyType cType = mCompanyTypes.get(type);
            if (cType == null) {
                throw new ConfigurationException(LocalText.getText("CompanyHasUnknownType", name, type));
            }

            try {
                //NEW//Company company = cType.createCompany(name, companyTag);
                Tag typeTag = typeTags.get(type);
                Company company = cType.createCompany(name, typeTag, companyTag);

                /* Private or public */
                if (company instanceof PrivateCompany) {
                    final PrivateCompany privateCompany = (PrivateCompany) company;

                    mPrivateCompanies.put(name, privateCompany);
                    lPrivateCompanies.add(privateCompany);
                } else if (company instanceof PublicCompany) {
                    final PublicCompany publicCompany = (PublicCompany) company;

                    publicCompany.setIndex(numberOfPublicCompanies++);
                    mPublicCompanies.put(name, publicCompany);
                    lPublicCompanies.add(publicCompany);
                }

                /* By type and name */
                if (!mCompaniesByTypeAndName.containsKey(type)) {
                    mCompaniesByTypeAndName.put(type, new HashMap<>());
                }
                mCompaniesByTypeAndName.get(type).put(name, company);

                String alias = company.getAlias();
                if (alias != null) {
                    createAlias(alias, name);
                }
            } catch (Exception e) {
                throw new ConfigurationException(LocalText.getText("ClassCannotBeInstantiated", cType.getClassName()), e);
            }
        }

        /* Read and configure the start packets */
        List<Tag> packetTags = tag.getChildren("StartPacket");

        if (packetTags != null) {
            for (Tag packetTag : tag.getChildren("StartPacket")) {
                // Extract the attributes of the Component
                String name = packetTag.getAttributeAsString("name", StartPacket.DEFAULT_ID);

                String roundClass = packetTag.getAttributeAsString("roundClass");
                if (roundClass == null) {
                    throw new ConfigurationException(LocalText.getText("StartPacketHasNoClass", name));
                }

                StartPacket sp = StartPacket.create(this, name, roundClass);
                startPackets.add(sp);
                startPacketMap.put(name, sp);

                sp.configureFromXML(packetTag);
            }
        }

    }

    // Post XML parsing initialisations
    public void finishConfiguration(RailsRoot root) throws ConfigurationException {
        for (PublicCompany comp : lPublicCompanies) {
            comp.finishConfiguration(root);
        }

        for (PrivateCompany comp : lPrivateCompanies) {
            comp.finishConfiguration(root);
        }
    }

    public void initStartPackets(GameManager gameManager) {
        // initialize startPackets
        for (StartPacket packet : startPackets) {
            packet.init(gameManager);
        }
    }

    private void createAlias(String alias, String name) {
        if (aliases == null) {
            aliases = new HashMap<String, String>();
        }
        aliases.put(alias, name);
    }

    public String checkAlias(String alias) {
        if (aliases != null && aliases.containsKey(alias)) {
            return aliases.get(alias);
        } else {
            return alias;
        }
    }

    public String checkAliasInCertId(String certId) {
        final String[] parts = certId.split("-");
        final String realName = checkAlias(parts[0]);

        if (!parts[0].equals(realName)) {
            return realName + "-" + parts[1];
        } else {
            return certId;
        }
    }

    public PrivateCompany getPrivateCompany(String name) {
        return mPrivateCompanies.get(name);
    }

    public PublicCompany getPublicCompany(String name) {
        return mPublicCompanies.get(checkAlias(name));
    }

    public List<PrivateCompany> getAllPrivateCompanies() {
        return lPrivateCompanies;
    }

    public List<PublicCompany> getAllPublicCompanies() {
        return lPublicCompanies;
    }

    public List<CompanyType> getCompanyTypes() {
        return lCompanyTypes;
    }

    public Company getCompany(String type, String name) {
        if (mCompaniesByTypeAndName.containsKey(type)) {
            return mCompaniesByTypeAndName.get(type).get(checkAlias(name));
        } else {
            return null;
        }
    }

    public void closeAllPrivates() {
        if (lPrivateCompanies != null) {
            lPrivateCompanies.stream()
                    .filter(PrivateCompany::isCloseable)
                    .forEach(PrivateCompany::setClosed);
        }
    }

    public List<PrivateCompany> getPrivatesOwnedByPlayers() {
        return getAllPrivateCompanies().stream()
                .filter(priv -> priv.getOwner() instanceof Player)
                .collect(Collectors.toList());
    }

    public StartPacket getStartPacket(int index) {
        return startPackets.get(index);
    }

    public StartPacket getStartPacket(String name) {
        return startPacketMap.get(name);
    }

    /**
     * Pass number of turns for which a certain company type can lay extra tiles of a certain colour.
     */
    // NOTE: Called by phase.finishConfiguration().
    // This implies, that the CompanyManager configuration must finished be BEFORE PhaseManager.
    // (We shouldn't have such dependencies...)
    // TODO: Resolve the issues mentioned above
    public void addExtraTileLayTurnsInfo(Map<String, Integer> extraTileTurns) {
        for (String typeAndColour : extraTileTurns.keySet()) {
            String[] keys = typeAndColour.split("~");
            Map<String, Company> companies = mCompaniesByTypeAndName.get(keys[0]);
            if (companies != null) {
                for (Company company : companies.values()) {
                    ((PublicCompany) company).addExtraTileLayTurnsInfo(keys[1], extraTileTurns.get(typeAndColour));
                }
            }
        }
    }

    public StartPacket getNextUnfinishedStartPacket() {
        return startPackets.stream()
                .filter(not(StartPacket::areAllSold))
                .findFirst()
                .orElse(null);
    }

    /**
     * @param id of the startItem
     * @return the startItem with that id
     */
    public StartItem getStartItemById(String id) {
        return startPackets.stream()
                .flatMap(packet -> packet.getItems().stream())
                .filter(item -> item.getId().equals(id))
                .findFirst()
                .orElse(null);
    }

}

using GameLib.Net.Common;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Special;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;
using Wintellect.PowerCollections;

// #FIXME: Solve id, name and uniquename clashes

/**
 * A Portfolio(Model) stores several portfolios
 */
namespace GameLib.Net.Game.Model
{
    public class PortfolioModel : RailsModel
    {
        public const string ID = "PortfolioModel";

        protected static Logger<PortfolioModel> log = new Logger<PortfolioModel>();

        /** Owned certificates */
        private CertificatesModel certificates;

        /** Owned private companies */
        private PrivatesModel privates;

        /** Owned trains */
        private TrainsModel trains;

        /** Owned tokens */
        // TODO Currently only used to discard expired Bonus tokens.
        private Portfolio<BonusToken> bonusTokens;

        /**
         * Private-independent special properties. When moved here, a special
         * property no longer depends on the private company being alive. Example:
         * 18AL named train tokens.
         */
        private SpecialPropertiesModel specialProperties;

        private PortfolioModel(IRailsOwner parent, string id) : base(parent, id)
        {
            // create internal models and portfolios
            certificates = CertificatesModel.Create(parent);
            privates = PrivatesModel.Create(parent);
            trains = TrainsModel.Create(parent);
            bonusTokens = PortfolioSet<BonusToken>.Create(parent, "BonusTokens");
            specialProperties = SpecialPropertiesModel.Create(parent);

            // change display style dependent on owner
            if (parent is PublicCompany)
            {
                trains.SetAbbrList(false);
                privates.SetLineBreak(false);
            }
            else if (parent is BankPortfolio)
            {
                trains.SetAbbrList(true);
            }
            else if (parent is Player)
            {
                privates.SetLineBreak(true);
            }
        }

        public static PortfolioModel Create(IPortfolioOwner parent)
        {
            return new PortfolioModel(parent, ID);
        }

        public void FinishConfiguration()
        {
            certificates.InitShareModels(GetRoot.CompanyManager.GetAllPublicCompanies());
            // TODO (Rails2.0): the linkage of all portfolios to the GameManager should be removed
            GetRoot.GameManager.AddPortfolio(this);
        }

        new public IPortfolioOwner Parent
        {
            get
            {
                return (IPortfolioOwner)base.Parent;
            }
        }

        // returns the associated MoneyOwner
        public IMoneyOwner MoneyOwner
        {
            get
            {
                if (Parent is BankPortfolio)
                {
                    return ((BankPortfolio)Parent).Parent;
                }
                return (IMoneyOwner)Parent;
            }
        }

        public void TransferAssetsFrom(PortfolioModel otherPortfolio)
        {

            // Move trains
            otherPortfolio.TrainsModel.Portfolio.MoveAll(Parent);

            // Move treasury certificates
            otherPortfolio.MoveAllCertificates(Parent);
        }

        /**
         * Low-level method, only to be called by the local addObject() method and
         * by initialization code.
         */
        // TODO: Ignores position now, is this necessary?
        public void AddPrivateCompany(PrivateCompany company)
        {

            // add to private Model
            privates.MoveInto(company);

            if (company.HasSpecialProperties)
            {
                log.Debug(company.Id + " has special properties!");
            }
            else
            {
                log.Debug(company.Id + " has no special properties");
            }

            // TODO: This should not be necessary as soon as a PlayerModel works
            // correctly
            UpdatePlayerWorth();
        }

        // FIXME: Solve the presidentShare problem, should not be identified at
        // position zero

        protected void UpdatePlayerWorth()
        {
            if (Parent is Player)
            {
                ((Player)Parent).UpdateWorth();
            }
        }

        public CertificatesModel CertificatesModel
        {
            get
            {
                return certificates;
            }
        }

        public ShareModel GetShareModel(PublicCompany company)
        {
            return certificates.GetShareModel(company);
        }

        public IReadOnlyCollection<PrivateCompany> PrivateCompanies
        {
            get
            {
                return privates.Portfolio.Items;
            }
        }

        public IReadOnlyCollection<PublicCertificate> Certificates
        {
            get
            {
                return certificates.Portfolio.Items;
            }
        }

        public ShareDetailsModel GetShareDetailsModel(PublicCompany company)
        {
            return certificates.GetShareDetailsModel(company);
        }

        /** Get the number of certificates that count against the certificate limit */
        public float GetCertificateCount()
        {
            return privates.CertificateCount + certificates.GetCertificateCount();
        }

        public Wintellect.PowerCollections.MultiDictionaryBase<PublicCompany, PublicCertificate> GetCertsPerCompanyMap()
        {
            return certificates.Portfolio.View();
        }

        public IReadOnlyCollection<PublicCertificate> GetCertificates(PublicCompany company)
        {
            return certificates.GetCertificates(company);
        }

        /**
         * Find a certificate for a given company.
         * 
         * @param company The public company for which a certificate is found.
         * @param president Whether we look for a president or non-president
         * certificate. If there is only one certificate, this parameter has no
         * meaning.
         * @return The certificate, or null if not found./
         */
        public PublicCertificate FindCertificate(PublicCompany company, bool president)
        {
            return FindCertificate(company, 1, president);
        }

        /**
         * Find a specified certificate
         * 
         * @return (first) certificate found, null if not found
         */
        public PublicCertificate FindCertificate(PublicCompany company, int shares, bool president)
        {
            foreach (PublicCertificate cert in certificates.Portfolio.GetItems(company))
            {
                if (company.GetShareUnit() == 100 || president
                    && cert.IsPresidentShare || !president
                    && !cert.IsPresidentShare && cert.GetShares() == shares)
                {
                    return cert;
                }
            }
            return null;
        }

        public List<PublicCertificate> GetCertsOfType(string certTypeId)
        {
            List<PublicCertificate> list = new List<PublicCertificate>();
            foreach (PublicCertificate cert in certificates)
            {
                if (cert.TypeId.Equals(certTypeId))
                {
                    list.Add(cert);
                }
            }
            return list;
        }

        /** 
         * @return a sorted Multiset<Integer> of shareNumbers of the certificates
         * Remark: excludes the presdident share
         */
        // FIXME: Integers could be replaced later by CerficateTypes
        public OrderedBag<int> GetCertificateTypeCounts(PublicCompany company)
        {
            return certificates.GetCertificateTypeCounts(company);
        }

        public PublicCertificate GetAnyCertOfType(string certTypeId)
        {
            foreach (PublicCertificate cert in certificates)
            {
                if (cert.TypeId.Equals(certTypeId))
                {
                    return cert;
                }
            }
            return null;
        }

        /**
         * Returns percentage that a portfolio contains of one company.
         */
        public int GetShare(PublicCompany company)
        {
            return certificates.GetShare(company);
        }


        /**
         * @return the number of shares owned by the PorfolioModel for this company
         */
        public int GetShareNumber(PublicCompany company)
        {
            return certificates.GetShareNumber(company);
        }

        /**
         * @param maxShareNumber maximum share number that is to achieved
         * @return sorted list of share numbers that are possible for that company
         */
        public OrderedBag<int> GetShareNumberCombinations(PublicCompany company, int maxShareNumber)
        {
            return certificates.GetshareNumberCombinations(company, maxShareNumber);
        }

        /** 
         * @return true if portfolio contains a multiple (non-president) certificate
         */
        public bool ContainsMultipleCert(PublicCompany company)
        {
            return certificates.ContainsMultipleCert(company);
        }


        public int OwnsCertificates(PublicCompany company, int unit, bool president)
        {
            int certs = 0;
            if (certificates.Contains(company))
            {
                foreach (PublicCertificate cert in certificates.Portfolio.GetItems(company))
                {
                    if (president)
                    {
                        if (cert.IsPresidentShare) return 1;
                    }
                    else if (cert.GetShares() == unit)
                    {
                        certs++;
                    }
                }
            }
            return certs;
        }

        public void MoveAllCertificates(IOwner owner)
        {
            certificates.Portfolio.MoveAll(owner);
        }

        /**
         * Swap this Portfolio's President certificate for common shares in another
         * Portfolio.
         *
         * @param company The company whose Presidency is handed over.
         * @param other The new President's portfolio.
         * @return The common certificates returned.
         */
        public List<PublicCertificate> SwapPresidentCertificate(PublicCompany company, PortfolioModel other)
        {
            return SwapPresidentCertificate(company, other, 0);
        }

        public List<PublicCertificate> SwapPresidentCertificate(PublicCompany company, PortfolioModel other, int swapShareSize)
        {
            List<PublicCertificate> swapped = new List<PublicCertificate>();
            PublicCertificate swapCert;

            // Find the President's certificate
            PublicCertificate presCert = FindCertificate(company, true);
            if (presCert == null) return null;
            int shares = presCert.GetShares();

            // If a double cert is requested, try that first
            if (swapShareSize > 1 && other.OwnsCertificates(company, swapShareSize, false) * swapShareSize >= shares)
            {
                swapCert = other.FindCertificate(company, swapShareSize, false);
                swapCert.MoveTo(Parent);
                swapped.Add(swapCert);
            }
            else if (other.OwnsCertificates(company, 1, false) >= shares)
            {
                // Check if counterparty has enough single certificates
                for (int i = 0; i < shares; i++)
                {
                    swapCert = other.FindCertificate(company, 1, false);
                    swapCert.MoveTo(Parent);
                    swapped.Add(swapCert);
                }
            }
            else if (other.OwnsCertificates(company, shares, false) >= 1)
            {
                swapCert = other.FindCertificate(company, 2, false);
                swapCert.MoveTo(Parent);
                swapped.Add(swapCert);
            }
            else
            {
                return null;
            }
            presCert.MoveTo(other.Parent);

            return swapped;
        }

        public int NumberOfTrains
        {
            get
            {
                return trains.Portfolio.Count;
            }
        }

        public IReadOnlyCollection<Train> GetTrainList()
        {
            return trains.Portfolio.Items;
        }

        public Train[] GetTrainsPerType(TrainType type)
        {

            List<Train> trainsFound = new List<Train>();
            foreach (Train train in trains.Portfolio)
            {
                if (train.GetTrainType() == type) trainsFound.Add(train);
            }

            return trainsFound.ToArray();
        }

        public TrainsModel TrainsModel
        {
            get
            {
                return trains;
            }
        }

        /** Returns one train of any type held */
        public List<Train> GetUniqueTrains()
        {
            List<Train> trainsFound = new List<Train>();
            HashSet<TrainType> trainTypesFound = new HashSet<TrainType>();
            foreach (Train train in trains.Portfolio)
            {
                if (!trainTypesFound.Contains(train.GetTrainType()))
                {
                    trainsFound.Add(train);
                    trainTypesFound.Add(train.GetTrainType());
                }
            }
            return trainsFound;
        }

        public Train GetTrainOfType(TrainCertificateType type)
        {
            return trains.GetTrainOfType(type);
        }

        /**
         * Add a train to the train portfolio
         */
        public bool AddTrain(Train train)
        {
            return trains.Portfolio.Add(train);
        }

        /**
         * Add an object. Low-level method, only to be called by Move objects.
         * 
         * @param object The object to add.
         * @return True if successful.
         */
        // TODO: Is this still required?

        /*
         * public bool addObject(Holdable object, int position) { if (object
         * instanceof PublicCertificate) { if (position == null) position = new
         * int[] {-1, -1, -1}; addCertificate((PublicCertificate) object, position);
         * return true; } else if (object instanceof PrivateCompany) {
         * addPrivate((PrivateCompany) object, position == null ? -1 : position[0]);
         * return true; } else if (object instanceof Train) { if (position == null)
         * position = new int[] {-1, -1, -1}; addTrain((Train) object, position);
         * return true; } else if (object instanceof SpecialProperty) { return
         * addSpecialProperty((SpecialProperty) object, position == null ? -1 :
         * position[0]); } else if (object instanceof Token) { return
         * addToken((Token) object, position == null ? -1 : position[0]); } else {
         * return false; } }
         */

        /**
         * Remove an object. Low-level method, only to be called by Move objects.
         * 
         * @param object The object to remove.
         * @return True if successful.
         */
        // TODO: Is this still required?
        /*
         * public bool removeObject(Holdable object) { if (object instanceof
         * PublicCertificate) { removeCertificate((PublicCertificate) object);
         * return true; } else if (object instanceof PrivateCompany) {
         * removePrivate((PrivateCompany) object); return true; } else if (object
         * instanceof Train) { removeTrain((Train) object); return true; } else if
         * (object instanceof SpecialProperty) { return
         * removeSpecialProperty((SpecialProperty) object); } else if (object
         * instanceof Token) { return removeToken((Token) object); } else { return
         * false; } }
         */

        // TODO: Check if this is still required
        /*
         * public int[] getListIndex (Holdable object) { if (object instanceof
         * PublicCertificate) { PublicCertificate cert = (PublicCertificate) object;
         * return new int[] { certificates.indexOf(object),
         * certPerCompany.get(cert.getCompany().getId()).indexOf(cert),
         * certsPerType.get(cert.getTypeId()).indexOf(cert) }; } else if (object
         * instanceof PrivateCompany) { return new int[]
         * {privateCompanies.indexOf(object)}; } else if (object instanceof Train) {
         * Train train = (Train) object; return new int[] { trains.indexOf(train),
         * train.getPreviousType() != null ?
         * trainsPerType.get(train.getPreviousType()).indexOf(train) : -1,
         * trainsPerCertType.get(train.getCertType()).indexOf(train) }; } else if
         * (object instanceof SpecialProperty) { return new int[]
         * {specialProperties.indexOf(object)}; } else if (object instanceof Token)
         * { return new int[] {tokens.indexOf(object)}; } else { return
         * Holdable.AT_END; } }
         */

        /**
         * @return Set of all special properties we have.
         */
        public IReadOnlyCollection<SpecialProperty> GetPersistentSpecialProperties()
        {
            return specialProperties.Portfolio.Items;
        }

        public List<SpecialProperty> GetAllSpecialProperties()
        {
            List<SpecialProperty> sps = new List<SpecialProperty>();
            sps.AddRange(specialProperties.Portfolio.Items);
            foreach (PrivateCompany priv in privates.Portfolio)
            {
                if (priv.GetSpecialProperties() != null)
                {
                    sps.AddRange(priv.GetSpecialProperties());
                }
            }
            return sps;
        }

        /**
         * Do we have any special properties?
         * 
         * @return Boolean
         */
        public bool HasSpecialProperties
        {
            get
            {
                return !specialProperties.Portfolio.IsEmpty;
            }
        }

        // TODO: Check if this code can be simplified
        public List<T> GetSpecialProperties<T>(bool includeExercised) where T : SpecialProperty
        {
            List<T> result = new List<T>();
            IReadOnlyCollection<SpecialProperty> sps;

            if (Parent is Player
                    || Parent is PublicCompany)
            {
                foreach (PrivateCompany priv in privates.Portfolio)
                {
                    sps = priv.GetSpecialProperties();
                    if (sps == null) continue;

                    foreach (SpecialProperty sp in sps)
                    {
                        if (typeof(T).IsAssignableFrom(sp.GetType())
                            && sp.IsExecutionable
                            && (!sp.IsExercised() || includeExercised)
                            && (Parent is ICompany
                                    && sp.IsUsableIfOwnedByCompany || Parent is Player
                                                                        && sp.IsUsableIfOwnedByPlayer))
                        {
                            log.Debug("Portfolio " + Parent.Id
                                      + " has SP " + sp);
                            result.Add((T)sp);
                        }
                    }
                }

                // Private-independent special properties
                foreach (SpecialProperty sp in specialProperties.Portfolio)
                {
                    if (typeof(T).IsAssignableFrom(sp.GetType())
                        && sp.IsExecutionable
                        && (!sp.IsExercised() || includeExercised)
                        && (Parent is ICompany
                            && sp.IsUsableIfOwnedByCompany || Parent is Player
                                                                && sp.IsUsableIfOwnedByPlayer))
                    {
                        log.Debug("Portfolio " + Parent.Id + " has persistent SP " + sp);
                        result.Add((T)sp);
                    }
                }

            }

            return result;
        }

        public PrivatesModel PrivatesOwnedModel
        {
            get
            {
                return privates;
            }
        }

        public bool AddBonusToken(BonusToken token)
        {
            return bonusTokens.Add(token);
        }

        public Portfolio<BonusToken> TokenHolder
        {
            get
            {
                return bonusTokens;
            }
        }

        public void RustObsoleteTrains()
        {
            List<Train> trainsToRust = new List<Train>();
            foreach (Train train in trains.Portfolio)
            {
                if (train.IsObsolete())
                {
                    trainsToRust.Add(train);
                }
            }
            // Need to separate selection and execution,
            // otherwise we get a ConcurrentModificationException on trains.
            foreach (Train train in trainsToRust)
            {
                ReportBuffer.Add(this, LocalText.GetText("TrainsObsoleteRusted",
                        train.ToText(), Parent.Id));
                log.Debug("Obsolete train " + train.Id + " (owned by "
                          + Parent.Id + ") rusted");
                train.SetRusted();
            }
            // FIXME:: Still required?
            // trains.update();
        }

        /**
         * Used to identify portfolios on reload
         */
        [Obsolete]
        public string Name
        {
            get
            {
                return Parent.Id;
            }
        }

        /**
         * Used to identify portfolios on reload TODO: Remove that in the future
         */
        [Obsolete]
        public string UniqueName
        {
            get
            {
                // For BankPortfolios use Bank
                if (Parent is BankPortfolio)
                {
                    return typeof(Bank).GetType().Name + "_" + Parent.Id;
                }
                return Parent.GetType().Name + "_" + Parent.Id;
            }
        }
    }
}

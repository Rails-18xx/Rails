using Combinatorics.Collections;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Collections.ObjectModel;
using System.Text;
using Wintellect.PowerCollections;

/**
 * Model that contains and manages the certificates
 * TODO: It might improve performance to separate the large multimap into smaller ones per individual companies, but I doubt it
        // TODO: find out where the president model has to be linked
        // this.addModel(company.getPresidentModel());
 */
namespace GameLib.Net.Game.Model
{
    public class CertificatesModel : RailsModel, IEnumerable<PublicCertificate>
    {
        public const string ID = "CertificatesModel";

        private PortfolioMap<PublicCompany, PublicCertificate> certificates;

        private Dictionary<PublicCompany, ShareModel> shareModels = new Dictionary<PublicCompany, ShareModel>();

        private Dictionary<PublicCompany, ShareDetailsModel> shareDetailsModels = new Dictionary<PublicCompany, ShareDetailsModel>();

        private CertificatesModel(IRailsOwner parent) : base(parent, ID)
        {
            //super(parent, ID);
            // certificates have the Owner as parent directly
            certificates = PortfolioMap<PublicCompany, PublicCertificate>.Create(parent, "certificates");
            // so make this model updating
            certificates.AddModel(this);
        }

        public static CertificatesModel Create(IRailsOwner parent)
        {
            return new CertificatesModel(parent);
        }

        new public IRailsOwner Parent
        {
            get
            {
                return (IRailsOwner)base.Parent;
            }
        }

        public void InitShareModels(IEnumerable<PublicCompany> companies)
        {
            // create shareModels
            foreach (PublicCompany company in companies)
            {
                ShareModel model = ShareModel.Create(this, company);
                shareModels[company] = model;
                ShareDetailsModel modelDetails = ShareDetailsModel.Create(this, company);
                shareDetailsModels[company] = modelDetails;
            }
        }

        public ShareModel GetShareModel(PublicCompany company)
        {
            return shareModels[company];
        }

        public ShareDetailsModel GetShareDetailsModel(PublicCompany company)
        {
            return shareDetailsModels[company];
        }

        public float GetCertificateCount()
        {
            float number = 0;
            foreach (PublicCertificate cert in certificates)
            {
                PublicCompany company = cert.Company;
                if (!company.HasFloated() || !company.HasStockPrice
                        || !cert.Company.GetCurrentSpace().IsNoCertLimit)
                {
                    number += cert.CertificateCount;
                }
            }
            return number;
        }

        public bool Contains(PublicCompany company)
        {
            return certificates.ContainsKey(company);
        }

        public IReadOnlyCollection<PublicCertificate> GetCertificates(PublicCompany company)
        {
            return certificates.GetItems(company);
        }

        public MultiDictionary<string, PublicCertificate> GetCertificatesByType(PublicCompany company)
        {
            MultiDictionary<string, PublicCertificate> certs = new MultiDictionary<string, PublicCertificate>(true);
            foreach (PublicCertificate c in certificates.GetItems(company))
            {
                certs.Add(c.TypeId, c);
            }
            return certs;
        }

        public OrderedBag<int> GetCertificateTypeCounts(PublicCompany company)
        {
            OrderedBag<int> certCount = new OrderedBag<int>();
            foreach (PublicCertificate cert in GetCertificates(company))
            {
                if (!cert.IsPresidentShare)
                {
                    certCount.Add(cert.GetShares());
                }
            }
            return certCount;
        }

        public PortfolioMap<PublicCompany, PublicCertificate> Portfolio
        {
            get
            {
                return certificates;
            }
        }

        public IEnumerator<PublicCertificate> GetEnumerator()
        {
            return certificates.GetEnumerator();
        }

        IEnumerator IEnumerable.GetEnumerator()
        {
            return certificates.GetEnumerator();
        }

        public int GetShare(PublicCompany company)
        {
            int share = 0;
            foreach (PublicCertificate cert in certificates.GetItems(company))
            {
                share += cert.Share;
            }
            return share;
        }

        public int GetShareNumber(PublicCompany company)
        {
            int shareNumber = 0;
            foreach (PublicCertificate cert in certificates.GetItems(company))
            {
                shareNumber += cert.GetShares();
            }
            return shareNumber;
        }

        public OrderedBag<int> GetshareNumberCombinations(PublicCompany company, int maxShareNumber)
        {
            return ShareNumberCombinations(certificates.GetItems(company), maxShareNumber);
        }

        public bool ContainsMultipleCert(PublicCompany company)
        {
            foreach (PublicCertificate cert in certificates.GetItems(company))
            {
                if (cert.GetShares() != 1 && !cert.IsPresidentShare)
                {
                    return true;
                }
            }
            return false;
        }

        virtual public string ToText(PublicCompany company)
        {
            int share = this.GetShare(company);

            if (share == 0) return "";
            StringBuilder b = new StringBuilder();
            b.Append(share).Append("%");

            if (Parent is Player
            && company.GetPresident() == Parent)
            {
                b.Append("P");
                if (!company.HasFloated()) b.Append("U");
                b.Append(company.GetExtraShareMarks());
            }
            return b.ToString();
        }

        override public string ToText()
        {
            return certificates.ToString();
        }


        /**
         * @param certificates list of certificates 
         * @param maxShareNumber maximum share number that is to achieved
         * @return sorted list of share numbers that are possible from the list of certificates
         */
        public static OrderedBag<int> ShareNumberCombinations(IEnumerable<PublicCertificate> certificates, int maxShareNumber)
        {
            OrderedBag<int> numbers = new OrderedBag<int>();
            List<PublicCertificate> certList = new List<PublicCertificate>(certificates);

            for (int index=1; index <= certList.Count; ++index)
            {
                // create a combination of index number of elements from the list.
                // This is "choose".  For the list { 1, 2, 3} :
                // index 1:  {1}, {2}, {3}
                // index 2:  {1,2}, {1,3}, {2,3}
                // etc...
                Combinations<PublicCertificate> combinations = new Combinations<PublicCertificate>(certList, index);

                foreach (var certSubSet in combinations)
                {
                    int sum = 0;
                    foreach (PublicCertificate cert in certSubSet)
                    {
                        sum += cert.GetShares();
                        if (sum > maxShareNumber)
                        {
                            break;
                        }
                    }
                    if (sum <= maxShareNumber)
                    {
                        numbers.Add(sum);
                    }
                }
            }

            return numbers;

            //// create vector for combinatorics
            //ICombinatoricsVector<PublicCertificate> certVector = Factory.createVector(certificates);

            //// create generator for subsets
            //Generator<PublicCertificate> certGenerator = Factory.createSubSetGenerator(certVector);

            //ImmutableSortedSet.Builder<Integer> numbers = ImmutableSortedSet.naturalOrder();
            //for (ICombinatoricsVector<PublicCertificate> certSubSet:certGenerator)
            //{
            //    int sum = 0;
            //    for (PublicCertificate cert:certSubSet)
            //    {
            //        sum += cert.getShares();
            //        if (sum > maxShareNumber)
            //        {
            //            break;
            //        }
            //    }
            //    if (sum <= maxShareNumber)
            //    {
            //        numbers.add(sum);
            //    }
            //}

            //return numbers.build();
        }

        public static OrderedBag<PublicCertificate.Combination> CertificateCombinations(IEnumerable<PublicCertificate> certificates, int shareNumber)
        {
            OrderedBag<PublicCertificate.Combination> certCombos = new OrderedBag<PublicCertificate.Combination>();
            List<PublicCertificate> certList = new List<PublicCertificate>(certificates);

            for (int index = 1; index <= certList.Count; ++index)
            {
                // create a combination of index number of elements from the list.
                // This is "choose".  For the list { 1, 2, 3} :
                // index 1:  {1}, {2}, {3}
                // index 2:  {1,2}, {1,3}, {2,3}
                // etc...
                Combinations<PublicCertificate> combinations = new Combinations<PublicCertificate>(certList, index);

                foreach (var certSubSet in combinations)
                {
                    int sum = 0;
                    foreach (PublicCertificate cert in certSubSet)
                    {
                        sum += cert.GetShares();
                        if (sum > shareNumber)
                        {
                            break;
                        }
                    }
                    if (sum == shareNumber)
                    {
                        certCombos.Add(PublicCertificate.Combination.Create(certSubSet));
                    }
                }
            }

            return certCombos;
            //// create vector for combinatorics
            //ICombinatoricsVector<PublicCertificate> certVector = Factory.createVector(certificates);

            //// create generator for subsets
            //Generator<PublicCertificate> certGenerator = Factory.createSubSetGenerator(certVector);

            //// add all subset that equal the share number to the set of combinations
            //ImmutableSortedSet.Builder<PublicCertificate.Combination> combinations = ImmutableSortedSet.naturalOrder();

            //for (ICombinatoricsVector<PublicCertificate> certSubSet:certGenerator)
            //{
            //    int sum = 0;
            //    for (PublicCertificate cert:certSubSet)
            //    {
            //        sum += cert.getShares();
            //        if (sum > shareNumber)
            //        {
            //            break;
            //        }
            //    }
            //    if (sum == shareNumber)
            //    {
            //        combinations.add(PublicCertificate.Combination.create(certSubSet));
            //    }
            //}
            //return combinations.build();
        }
    }
}

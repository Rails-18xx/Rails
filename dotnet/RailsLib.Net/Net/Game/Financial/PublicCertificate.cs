using GameLib.Net.Common;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections;
using System.Collections.Generic;
using System.Text;

/**
 * Rails 2.x TODO:
 * Create an abstraction that defines a CertificateType (currently attributes president, shares, certificateCount)
 * Each certificate belongs to a Certificate Type and a Company
 * Then clean up the current mess, including the static map, which is only for backward compatibility for early 1.x versions
 */

namespace GameLib.Net.Game.Financial
{
    public class PublicCertificate : RailsOwnableItem<PublicCertificate>, ICertificate, ICloneable, ITypable<PublicCompany>, IComparable<IOwnable>, IComparable, IComparable<PublicCertificate>
    {
        /**
          * Combination defines a set of certificates
          */
        public class Combination : IComparable<Combination>, IEnumerable<PublicCertificate>
        {

            private SortedSet<PublicCertificate> certs;

            private Combination(SortedSet<PublicCertificate> certs)
            {
                this.certs = certs;
            }

            public static Combination Create(IEnumerable<PublicCertificate> certs)
            {
                return new Combination(new SortedSet<PublicCertificate>(certs)); // ImmutableSortedSet.copyOf(certs));
            }

            public SortedSet<PublicCertificate> Certificates
            {
                get
                {
                    return certs;
                }
            }

            public int Count
            {
                get
                {
                    return certs.Count;
                }
            }

            public int CompareTo(Combination other)
            {
                return certs.Count.CompareTo(other.Count);
            }

            public IEnumerator<PublicCertificate> GetEnumerator()
            {
                return ((IEnumerable<PublicCertificate>)certs).GetEnumerator();
            }

            IEnumerator IEnumerable.GetEnumerator()
            {
                return ((IEnumerable<PublicCertificate>)certs).GetEnumerator();
            }

            //public Iterator<PublicCertificate> iterator()
            //{
            //    return certs.iterator();
            //}
        }


        /** From which public company is this a certificate */
        protected PublicCompany company;
        /**
         * Share percentage represented by this certificate
         */
        protected IntegerState shares;
        /** President's certificate? */
        protected bool president;
        // FIXME: If this is changable, it should be a state variable, otherwise UNDO problems
        /** Count against certificate limits */
        protected float certificateCount = 1.0f;

        /** Availability at the start of the game */
        protected bool initiallyAvailable;

        /** A key identifying the certificate's unique ID */
        protected string certId;

        // FIMXE: 
        /** Index within company (to be maintained in the IPO) */
        protected int indexInCompany;

        /** A map allowing to find certificates by unique id */
        // FIXME: Remove static map, replace by other location mechanisms
        protected static Dictionary<string, PublicCertificate> certMap =
                new Dictionary<string, PublicCertificate>();


        protected static Logger<PublicCertificate> log = new Logger<PublicCertificate>();

        // TODO: Rewrite constructors
        // TODO: Should every certificate have its own id and be registered with the parent?
        public PublicCertificate(IRailsItem parent, string id, int shares, bool president,
                bool available, float certificateCount, int index) : base(parent, id)
        {

            this.shares = IntegerState.Create(this, "shares");
            this.shares.Set(shares);
            this.president = president;
            this.initiallyAvailable = available;
            this.certificateCount = certificateCount;
            this.indexInCompany = index;
        }

        // TODO: Can be removed, as
        //    most likely this does not work, as it duplicates ids
        //    public PublicCertificate(PublicCertificate oldCert) {
        //        super(oldCert.getParent(), oldCert.getId(), PublicCertificate.class);
        //        this.shares = oldCert.getShares();
        //        this.president = oldCert.isPresidentShare();
        //        this.initiallyAvailable = oldCert.isInitiallyAvailable();
        //        this.certificateCount = oldCert.getCertificateCount();
        //        this.indexInCompany = oldCert.getIndexInCompany();
        //    }

        new public IRailsItem Parent
        {
            get
            {
                return (IRailsItem)base.Parent;
            }
        }


        new public RailsRoot GetRoot
        {
            get
            {
                return (RailsRoot)base.GetRoot;
            }
        }

        /** Set the certificate's unique ID, for use in deserializing */
        public void SetUniqueId(string name, int index)
        {
            certId = name + "-" + index;
            certMap[certId] = this;
        }

        /** Set the certificate's unique ID */
        public string GetUniqueId()
        {
            return certId;
        }

        public int IndexInCompany
        {
            get
            {
                return indexInCompany;
            }
        }

        public static PublicCertificate GetByUniqueId(string certId)
        {
            return certMap[certId];
        }


        // FIXME: There is no guarantee that the parent of a certificate portfolio is a portfolioModel
        // Replace that by something that works
        public CertificatesModel Holder
        {
            get
            {
                //return getPortfolio().getParent().getShareModel(company);
                return null;
            }
        }

        /**
         * @return if this is a president's share
         */
        public bool IsPresidentShare
        {
            get
            {
                return president;
            }
            set
            {
                president = value;
            }
        }

        /**
         * Get the number of shares that this certificate represents.
         *
         * @return The number of shares.
         */
        public int GetShares()
        {
            return shares.Value;
        }

        /**
         * Get the percentage of ownership that this certificate represents. This is
         * equal to the number of shares * the share unit.
         *
         * @return The share percentage.
         */
        public int Share
        {
            get
            {
                return shares.Value * company.GetShareUnit();
            }
        }

        public bool IsInitiallyAvailable
        {
            get
            {
                return initiallyAvailable;
            }
            set
            {
                this.initiallyAvailable = value;
            }
        }

        public PublicCompany Company
        {
            get
            {
                return company;
            }
            set
            {
                company = value;
            }
        }


        public string TypeId
        {
            get
            {
                string certTypeId = company.Id + "_" + Share + "%";
                if (president) certTypeId += "_P";
                return certTypeId;
            }
        }

        // Typable interface
        public PublicCompany SpecificType
        {
            get
            {
                return company;
            }
        }

        public object Clone()
        {
            return MemberwiseClone();
            //try
            //{
            //    return super.clone();
            //}
            //catch (CloneNotSupportedException e)
            //{
            //    log.error("Cannot clone certificate:", e);
            //    return null;
            //}
        }

        public PublicCertificate Copy()
        {
            return (PublicCertificate)this.Clone();
        }

        /**
         * Compare is based on 
         * A) Presidency (presidency comes first in natural ordering)
         * B) Number of Shares (more shares means come first)
         * C) Id of CertificateType
         * D) Id of Certificate
         */
        // FIXME: The default comparator can only contain final attributes, otherwise 
        // otherwise Portfolios (TreeMaps) might get confused
        // Implement another comparator for display that does not define a standard sorting
        new public int CompareTo(IOwnable other)
        {
            if (other is PublicCertificate)
            {
                PublicCertificate otherCert = (PublicCertificate)other;
                int result = this.Company.CompareTo(otherCert.Company);
                if (result != 0) return result;

                return this.Id.CompareTo(otherCert.Id);

                // sort by the criteria defined above
                //return ComparisonChain.start()
                //        .compare(this.getCompany(), otherCert.getCompany())
                //        //                    .compare(otherCert.isPresidentShare(), this.isPresidentShare())
                //        //                    .compare(otherCert.getShares(), this.getShares())
                //        //                    .compare(this.getType().getId(), otherCert.getType().getId())
                //        .compare(this.getId(), otherCert.getId())
                //        .result();
            }
            else
            {
                return base.CompareTo(other);
            }
        }

        public int CompareTo(PublicCertificate other)
        {
            return this.CompareTo((IOwnable)other);
        }

        public int CompareTo(object obj)
        {
            if (obj is IOwnable)
            {
                return CompareTo((IOwnable)obj);
            }

            throw new ArgumentException();
        }


        //public int CompareTo(object other)
        //{
        //    if (other == null) return 1;

        //    if (other is PublicCertificate)
        //    {
        //        PublicCertificate otherCert = (PublicCertificate)other;
        //        int result = this.Company.CompareTo(otherCert.Company);
        //        if (result != 0) return result;

        //        return this.Id.CompareTo(otherCert.Id);
        //    }
        //    else
        //    {
        //        return base.CompareTo(other as IOwnable);
        //    }
        //}

        public void SetShares(int numShares)
        {
            this.shares.Set(numShares);
        }

        // Certificate Interface
        public float CertificateCount
        {
            get
            {
                return certificateCount;
            }
            set
            {
                // [Obsolete]
                certificateCount = value;
            }
        }

        // Item interface
        /**
         * Get the name of a certificate. The name is derived from the company name
         * and the share percentage of this certificate. If it is a 100% share (as
         * occurs with e.g. 1835 minors), only the company name is given. If it is a
         * president's share, that fact is mentioned.
         */
        override public string ToText()
        {
            int share = Share;
            if (share == 100)
            {
                /* Applies to shareless minors: just name the company */
                return company.Id;
            }
            else if (president)
            {
                return LocalText.GetText("PRES_CERT_NAME",
                        company.Id,
                        Share);
            }
            else
            {
                return LocalText.GetText("CERT_NAME",
                        company.Id,
                        Share);
            }
        }
    }
}

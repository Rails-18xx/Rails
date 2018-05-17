using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Special;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class CompanyComparer : IComparer<ICompany>
    {
        /**
             * A Comparator for Companies
             */
        public static IComparer<ICompany> COMPANY_COMPARATOR = new CompanyComparer();
        //{
        // int compare(Company c0, Company c1)
        //{
        //    return ComparisonChain.start()
        //            .compare(c0.getType().getId(), c1.getType().getId())
        //            .compare(c0.getId(), c1.getId())
        //            .result();
        //}

        public int Compare(ICompany x, ICompany y)
        {
            int result;

            result = x.CompanyType.Id.CompareTo(y.CompanyType.Id);
            if (result != 0) return result;

            return x.Id.CompareTo(y.Id);
        }
    }

    public static class ICompanyConsts
    {
        /** The name of the XML tag used to configure a company. */
        public const string COMPANY_ELEMENT_ID = "Company";

        /** The name of the XML attribute for the company's name. */
        public const string COMPANY_NAME_TAG = "name";

        /** The name of the XML attribute for the company's type. */
        public const string COMPANY_TYPE_TAG = "type";
    }

    public interface ICompany : IRailsOwner, IConfigurable, ICloneable
    {
        void InitType(CompanyType type);

        /**
         * @return Type of company (Public/Private)
         */
        CompanyType CompanyType { get; }

        /**
         * @return whether this company is closed
         */
        bool IsClosed();

        /**
         * Close this company.
         */
        void SetClosed();

        string LongName { get; }

        string Alias { get; }

        string InfoText { get; }

        // Since 1835 required for both private and  companies
        /**
         * @return Set of all special properties we have.
         */
        IReadOnlyCollection<SpecialProperty> GetSpecialProperties();
    }
}

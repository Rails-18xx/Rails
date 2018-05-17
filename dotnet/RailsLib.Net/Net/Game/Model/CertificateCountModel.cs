using System;
using System.Collections.Generic;
using System.Text;
using System.Text.RegularExpressions;

namespace GameLib.Net.Game.Model
{
    public class CertificateCountModel : RailsModel
    {
        public const string ID = "CertificateCountModel";

        private CertificateCountModel(PortfolioModel parent) : base(parent, ID)
        {
        }

        public static CertificateCountModel Create(PortfolioModel parent)
        {
            CertificateCountModel model = new CertificateCountModel(parent);
            // lets certificate count model update on portfolio changes
            parent.AddModel(model);
            return model;
        }

        /**
         * @return restricted to PortfolioModel
         */
        new public PortfolioModel Parent
        {
            get
            {
                return (PortfolioModel)base.Parent;
            }
        }

        override public string ToText()
        {
            Regex r1 = new Regex("\\.0");
            Regex r2 = new Regex("\\.5");
            return r1.Replace(r2.Replace(Parent.GetCertificateCount().ToString(), "", 1), "\u00bd", 1);
        }
    }
}

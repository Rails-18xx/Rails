using System;
using System.Collections.Generic;
using System.Text;

/**
 * ShareModel for displaying the share percentages
 */

namespace GameLib.Net.Game.Model
{
    public class ShareModel : RailsModel
    {
        private PublicCompany company;

        private ShareModel(CertificatesModel parent, PublicCompany company) : base(parent, "shareModel_" + company.Id)
        {
            this.company = company;
            // have share model observe floatation status of company
            company.GetFloatedModel().AddModel(this);
        }

        public static ShareModel Create(CertificatesModel certModel, PublicCompany company)
        {
            ShareModel model = new ShareModel(certModel, company);
            certModel.AddModel(model);
            return model;
        }

        new public CertificatesModel Parent
        {
            get
            {
                return (CertificatesModel)base.Parent;
            }
        }

        override public string ToText()
        {
            return Parent.ToText(company);
        }
    }
}

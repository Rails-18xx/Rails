using GameLib.Net.Game.Financial;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * Model for displaying the share details (used for ToolTips)
*/

namespace GameLib.Net.Game.Model
{
    public class ShareDetailsModel : RailsModel
    {
        private PublicCompany company;

    private ShareDetailsModel(CertificatesModel parent, PublicCompany company) : base(parent, "shareDetailsModel_" + company.Id)
        {
            
            this.company = company;
        }

        public static ShareDetailsModel Create(CertificatesModel certModel, PublicCompany company)
        {
            ShareDetailsModel model = new ShareDetailsModel(certModel, company);
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
            Wintellect.PowerCollections.MultiDictionary<string, PublicCertificate> certs = Parent.GetCertificatesByType(company);
            if (certs.Count == 0) return null;

            StringBuilder text = new StringBuilder();
            foreach (string certType in certs.Keys)
            {
                if (text.Length > 0) text.Append("<br>");
                // parse certType
                // TODO: Create true CertificateTypes
                string[] items = certType.Split('_');
                string type = items[1] + (items.Length > 2 && items[2].Contains("P") ? "P" : "");
                text.Append(type).Append(" x ").Append(certs[certType].Count);
            }
            return "<html>" + text.ToString() + "</html>";
        }
    }
}

using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Model
{
    public class PrivatesModel : RailsModel
    {
        public const string ID = "PrivatesModel";

        private Portfolio<PrivateCompany> privates;

        private bool addLineBreak = false;

        private PrivatesModel(IRailsOwner parent, string id) : base(parent, id)
        {
            privates = PortfolioSet<PrivateCompany>.Create(parent, "privates");
            // PrivatesModel is an indirect owner of privates, so add it to the state
            privates.AddModel(this);
        }

        /**
         * Creates an initialized PrivatesModel
         */
        public static PrivatesModel Create(IRailsOwner parent)
        {
            return new PrivatesModel(parent, ID);
        }


        new public IRailsOwner Parent
        {
            get
            {
                return (IRailsOwner)base.Parent;
            }
        }

        public Portfolio<PrivateCompany> Portfolio
        {
            get
            {
                return privates;
            }
        }

        public float CertificateCount
        {
            get
            {
                float count = 0;
                foreach (PrivateCompany p in privates)
                {
                    count += p.CertificateCount;
                }
                return count;
            }
        }

        public void MoveInto(PrivateCompany p)
        {
            privates.Add(p);
        }


        public void SetLineBreak(bool lineBreak)
        {
            this.addLineBreak = lineBreak;
        }

        override public string ToText()
        {

            StringBuilder buf = new StringBuilder("<html>");
            foreach (ICompany priv in privates)
            {
                if (buf.Length > 6)
                    buf.Append(addLineBreak ? "<br>" : "&nbsp;");
                buf.Append(priv.Id);
            }
            if (buf.Length > 6)
            {
                buf.Append("</html>");
            }
            return buf.ToString();
        }

    }
}

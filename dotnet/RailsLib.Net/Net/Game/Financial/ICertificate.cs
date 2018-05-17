using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

/**
 * The superinterface of PrivateCompany and PublicCertificate, which allows
 * objects implementating these interfaces to be combined in start packets and
 * other contexts where their "certificateship" is of interest.
 * 
 * TODO: Check if this is still needed (or replaced by Ownable) or could be extended by 
 * combining methods from both public and private certificates

 */
namespace GameLib.Net.Game.Financial
{
    public interface ICertificate : IOwnable
    {
        float CertificateCount
        {
            get;
            // FIXME: Should this really be changeable?
            //[Obsolete]
            set;
        }
    }
}

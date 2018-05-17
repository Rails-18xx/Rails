using System;
using System.Collections.Generic;
using System.Text;

/**
 * A BaseToken object represents a token that a operating public company can
 * place on the map to act as a rail building and train running starting point.
 * <p> The "Base" qualifier is used (more or less) consistently in this
 * rails.game program as it most closely the function of such a token: to act as
 * a base from which a company can operate. Other names used in various games
 * and discussions are "railhead", "station", "garrison", or just "token".
 */

namespace GameLib.Net.Game
{
    public class BaseToken : Token<BaseToken>
    {
        private BaseToken(PublicCompany parent, string id) : base(parent, id)
        {
            
    }

    public static BaseToken Create(PublicCompany company)
    {
        string uniqueId = CreateUniqueId(company);
        return new BaseToken(company, uniqueId);
    }

    new public PublicCompany Parent
    {
            get
            {
                return (PublicCompany)base.Parent;
            }
    }

    public bool IsPlaced
    {
            get
            {
                return !(Owner == Parent);
            }
    }


    // FIXME: Check if this works, previously it returned the parent id
    // However this was invalid for the portfolio storage
    //    public string getId() {
    //        return getParent().getId();
    //    }

}
}

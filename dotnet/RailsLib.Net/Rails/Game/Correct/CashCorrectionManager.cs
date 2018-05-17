using GameLib.Net.Common;
using GameLib.Net.Game;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.State;
using Newtonsoft.Json;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Rails.Game.Correct
{
    [JsonObject(MemberSerialization.Fields)]
    public class CashCorrectionManager : CorrectionManager
    {
        private CashCorrectionManager(GameManager parent) : base(parent, CorrectionType.CORRECT_CASH)
        {
        }

        public static CashCorrectionManager Create(GameManager parent)
        {
            return new CashCorrectionManager(parent);
        }

        override public List<CorrectionAction> CreateCorrections()
        {
            List<CorrectionAction> actions = base.CreateCorrections();

            if (IsActive())
            {
                IReadOnlyCollection<Player> players = GetRoot.PlayerManager.Players;
                foreach (Player pl in players)
                {
                    actions.Add(new CashCorrectionAction(pl));
                }

                IReadOnlyCollection<PublicCompany> publicCompanies = Parent.GetAllPublicCompanies();
                foreach (PublicCompany pc in publicCompanies)
                {
                    if (pc.HasFloated() && !pc.IsClosed())
                        actions.Add(new CashCorrectionAction(pc));
                }
            }

            return actions;
        }

        override public bool ExecuteCorrection(CorrectionAction action)
        {
            if (action is CashCorrectionAction)
                return Execute((CashCorrectionAction)action);
            else
                return base.ExecuteCorrection(action);
        }

        private bool Execute(CashCorrectionAction cashAction)
        {

            bool result = false;

            IMoneyOwner ch = cashAction.CashHolder;
            int amount = cashAction.Amount;

            string errMsg = null;

            while (true)
            {
                if (amount == 0)
                {
                    errMsg =
                        LocalText.GetText("CorrectCashZero");
                    break;
                }
                if ((amount + ch.Cash) < 0)
                {
                    errMsg =
                        LocalText.GetText("NotEnoughMoney",
                                ch.Id,
                                Bank.Format(this, ch.Cash),
                                Bank.Format(this, -amount));
                    break;
                }
                break;
            }

            if (errMsg != null)
            {
                DisplayBuffer.Add(this, LocalText.GetText("CorrectCashError",
                        ch.Id,
                        errMsg));
                result = true;
            }
            else
            {
                // no error occurred 
                string msg;
                if (amount < 0)
                {
                    // negative amounts: remove cash from cashholder
                    string text = Currency.ToBank(ch, -amount);

                    msg = LocalText.GetText("CorrectCashSubstractMoney",
                            ch.Id,
                            text);
                }
                else
                {
                    // positive amounts: add cash to cashholder
                    string text = Currency.FromBank(amount, ch);
                    msg = LocalText.GetText("CorrectCashAddMoney",
                            ch.Id,
                            text);
                }
                ReportBuffer.Add(this, msg);
                Parent.AddToNextPlayerMessages(msg, true);
                result = true;
            }

            return result;
        }
    }
}

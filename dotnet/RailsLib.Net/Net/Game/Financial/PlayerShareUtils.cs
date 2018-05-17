using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game.Financial
{
    public static class PlayerShareUtils
    {
        public static SortedSet<int> SharesToSell(PublicCompany company, Player player)
        {
            if (company.HasMultipleCertificates)
            {
                if (player == company.GetPresident())
                {
                    return PresidentSellMultiple(company, player);
                }
                else
                {
                    return OtherSellMultiple(company, player);
                }
            }
            else
            {
                if (player == company.GetPresident())
                {
                    return PresidentSellStandard(company, player);
                }
                else
                {
                    return OtherSellStandard(company, player);
                }
            }

        }

        public static int PoolAllowsShareNumbers(PublicCompany company)
        {
            int poolShares = Bank.GetPool(company).PortfolioModel.GetShareNumber(company);
            int poolMax = (GameDef.GetGameParameterAsInt(company, GameDef.Parm.POOL_SHARE_LIMIT) / company.GetShareUnit()
                    - poolShares);
            return poolMax;
        }

        // President selling for companies WITHOUT multiple certificates
        private static SortedSet<int> PresidentSellStandard(PublicCompany company, Player president)
        {
            int presidentShares = president.PortfolioModel.GetShareNumber(company);
            int poolShares = PoolAllowsShareNumbers(company);

            // check if there is a potential new president ...
            int presidentCertificateShares = company.GetPresidentsShare().GetShares();
            Player potential = company.FindPlayerToDump();

            int maxShares;
            if (potential == null)
            {
                // ... if there is none, selling is only possible until the presidentCerificate or pool maximum
                maxShares = Math.Min(presidentShares - presidentCertificateShares, poolShares);
            }
            else
            {
                // otherwise until pool maximum only
                maxShares = Math.Min(presidentShares, poolShares);
            }

            SortedSet<int> sharesToSell = new SortedSet<int>();
            for (int s = 1; s <= maxShares; s++)
            {
                sharesToSell.Add(s);
            }
            return sharesToSell;
        }

        // Non-president selling for companies WITHOUT multiple certificates
        private static SortedSet<int> OtherSellStandard(PublicCompany company, Player player)
        {
            int playerShares = player.PortfolioModel.GetShareNumber(company);
            int poolShares = PoolAllowsShareNumbers(company);

            SortedSet<int> sharesToSell = new SortedSet<int>();
            for (int s = 1; s <= Math.Min(playerShares, poolShares); s++)
            {
                sharesToSell.Add(s);
            }
            return sharesToSell;
        }


        // President selling for companies WITH multiple certificates
        private static SortedSet<int> PresidentSellMultiple(PublicCompany company, Player president)
        {

            // first: check what number of shares have to be dumped
            int presidentShareNumber = president.PortfolioModel.GetShare(company);
            PublicCertificate presidentCert = company.GetPresidentsShare();
            Player potential = company.FindPlayerToDump();
            int potentialShareNumber = potential.PortfolioModel.GetShare(company);
            int shareNumberDumpDifference = presidentShareNumber - potentialShareNumber;

            // ... if this is less than what the pool allows => goes back to non-president selling
            int poolAllows = PoolAllowsShareNumbers(company);
            if (shareNumberDumpDifference <= poolAllows)
            {
                return OtherSellMultiple(company, president);
            }

            // second: separate the portfolio into other shares and president certificate
            List<PublicCertificate> otherCerts = new List<PublicCertificate>();
            foreach (PublicCertificate c in president.PortfolioModel.GetCertificates(company))
            {
                if (!c.IsPresidentShare)
                {
                    otherCerts.Add(c);
                }
            }

            // third: retrieve the share number combinations of the non-president certificates
            SortedSet<int> otherShareNumbers = new SortedSet<int>(CertificatesModel.ShareNumberCombinations(otherCerts, poolAllows));

            // fourth: combine pool and potential certificates, those are possible returns
            List<PublicCertificate> returnCerts = new List<PublicCertificate>();
            returnCerts.AddRange(Bank.GetPool(company).PortfolioModel.GetCertificates(company));
            returnCerts.AddRange(potential.PortfolioModel.GetCertificates(company));
            SortedSet<int> returnShareNumbers = new SortedSet<int>(CertificatesModel.ShareNumberCombinations(returnCerts, presidentCert.GetShares()));

            SortedSet<int> sharesToSell = new SortedSet<int>();
            foreach (int s in otherShareNumbers)
            {
                if (s <= shareNumberDumpDifference)
                {
                    // shareNumber is below or equal the dump difference => add as possibility to sell without dump
                    sharesToSell.Add(s);
                }
                // now check if there are dumping possibilities
                for (int d = 1; d <= presidentCert.GetShares(); d++)
                {
                    if (s + d <= poolAllows)
                    {
                        // d is the amount sold in addition to standard shares, returned has the remaining part of the president share
                        int remaining = presidentCert.GetShares() - d;
                        if (returnShareNumbers.Contains(remaining))
                        {
                            sharesToSell.Add(s);
                        }
                    }
                    else
                    {
                        break; // pool is full
                    }
                }
            }
            return sharesToSell;
        }

        // Non-president selling for companies WITH multiple certificates
        private static SortedSet<int> OtherSellMultiple(PublicCompany company, Player player)
        {

            // check if there is a multiple certificate inside the portfolio
            if (player.PortfolioModel.ContainsMultipleCert(company))
            {
                int poolAllows = PoolAllowsShareNumbers(company);
                SortedSet<PublicCertificate> certificates = new SortedSet<PublicCertificate>(player.PortfolioModel.GetCertificates(company));
                return new SortedSet<int>(CertificatesModel.ShareNumberCombinations(certificates, poolAllows));
            }
            else
            { // otherwise standard case
                return OtherSellStandard(company, player);
            }
        }

        // FIXME: Rails 2.x This is a helper function as long as the sold certificates are not stored
        public static int PresidentShareNumberToSell(PublicCompany company, Player president, Player dumpedPlayer, int nbCertsToSell)
        {
            int dumpThreshold = president.PortfolioModel.GetShareNumber(company) - dumpedPlayer.PortfolioModel.GetShareNumber(company);
            if (nbCertsToSell > dumpThreshold)
            {
                // reduce the nbCertsToSell by the presidentShare (but it can be sold partially...)
                return Math.Min(company.GetPresidentsShare().GetShares(), nbCertsToSell);
            }
            else
            {
                return 0;
            }
        }

        // FIXME: Rails 2.x This is a helper function as long as the sold certificates are not stored
        public static List<PublicCertificate> FindCertificatesToSell(PublicCompany company, Player player, int nbCertsToSell, int shareUnits)
        {

            // check for <= 0 => empty list
            if (nbCertsToSell <= 0)
            {
                return new List<PublicCertificate>();
            }

            List<PublicCertificate> certsToSell = new List<PublicCertificate>();
            foreach (PublicCertificate cert in player.PortfolioModel.GetCertificates(company))
            {
                if (!cert.IsPresidentShare && cert.GetShares() == shareUnits)
                {
                    certsToSell.Add(cert);
                    nbCertsToSell--;
                    if (nbCertsToSell == 0)
                    {
                        break;
                    }
                }
            }

            return certsToSell;
        }

        public static void ExecutePresidentTransferAfterDump(PublicCompany company, Player newPresident, BankPortfolio bankTo, int presSharesToSell)
        {

            // 1. Move the swap certificates from new president to the pool
            PublicCertificate presidentCert = company.GetPresidentsShare();

            // ... get all combinations for the presidentCert share numbers
            /*SortedSet<PublicCertificate.Combination>*/
            var combinations = CertificatesModel.CertificateCombinations(
                newPresident.PortfolioModel.GetCertificates(company), presidentCert.GetShares());

            // ... move them to the Bank
            // FIXME: this should be based on a selection of the new president, however it chooses the combination with most certificates
            PublicCertificate.Combination swapToBank = combinations.GetLast();
            Portfolio.MoveAll(swapToBank, bankTo);

            // 2. Move the replace certificates from the bank to the old president

            // What is the difference between the shares to sell and the president share number
            int replaceShares = presidentCert.GetShares() - presSharesToSell;
            if (replaceShares > 0)
            {
                combinations = CertificatesModel.CertificateCombinations(
                        bankTo.PortfolioModel.GetCertificates(company), replaceShares);
                // FIXME: this should be based on a selection of the previous president, however it chooses the combination with least certificates
                PublicCertificate.Combination swapFromBank = combinations.GetFirst();
                // ... move to (old) president
                Portfolio.MoveAll(swapFromBank, company.GetPresident());
            }

            // 3. Transfer the president certificate
            presidentCert.MoveTo(newPresident);
        }
    }
}

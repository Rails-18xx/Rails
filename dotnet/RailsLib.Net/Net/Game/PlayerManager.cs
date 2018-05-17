using GameLib.Net.Common;
using GameLib.Net.Common.Parser;
using GameLib.Net.Game.Financial;
using GameLib.Net.Game.Model;
using GameLib.Net.Game.State;
using System;
using System.Collections.Generic;
using System.Text;

namespace GameLib.Net.Game
{
    public class PlayerManager : RailsManager, IConfigurable
    {
        private static Logger<PlayerManager> log = new Logger<PlayerManager>();

        // static data
        private Dictionary<string, Player> playerNames; // static, but set later in setPlayers()

        // configure data
        private int maxPlayers;
        private int minPlayers;
        private Dictionary<int, int> playerStartCash = new Dictionary<int, int>();
        private Dictionary<int, int> playerCertificateLimits = new Dictionary<int, int>();

        // dynamic data
        private PlayerOrderModel playerModel;
        private GenericState<Player> currentPlayer;
        private GenericState<Player> priorityPlayer;
        private IntegerState playerCertificateLimit;

        /**
         * nextPlayerMessages collects all messages to be displayed to the next player
         */
        private ListState<string> nextPlayerMessages;


        /**
         * Used by Configure (via reflection) only
         */
        public PlayerManager(RailsRoot parent, string id) : base(parent, id)
        {
            playerModel = new PlayerOrderModel(this, "playerModel");
            currentPlayer = GenericState<Player>.Create(this, "currentPlayer");
            priorityPlayer = GenericState<Player>.Create(this, "priorityPlayer");
            playerCertificateLimit = IntegerState.Create(this, "playerCertificateLimit");
            nextPlayerMessages = ListState<string>.Create(this, "nextPlayerMessages");
        }

        public void ConfigureFromXML(Tag tag)
        {
            int number, startCash, certLimit;

            List<Tag> playerTags = tag.GetChildren("Players");
            minPlayers = 99;
            maxPlayers = 0;
            foreach (Tag playerTag in playerTags)
            {
                number = playerTag.GetAttributeAsInteger("number");
                startCash = playerTag.GetAttributeAsInteger("cash");
                playerStartCash[number] = startCash;
                certLimit = playerTag.GetAttributeAsInteger("certLimit");
                playerCertificateLimits[number] = certLimit;

                minPlayers = Math.Min(minPlayers, number);
                maxPlayers = Math.Max(maxPlayers, number);
            }
        }

        // TODO: rename to initPlayers
        public void SetPlayers(List<string> playerNames, Bank bank)
        {

            int startCash = playerStartCash[playerNames.Count];

            int playerIndex = 0;
            string cashText = null;
            Dictionary<string, Player> playerNamesBuilder = new Dictionary<string, Player>();
            foreach (string playerName in playerNames)
            {
                Player player = Player.Create(this, playerName, playerIndex++);
                playerModel.PlayerOrder.Add(player);
                playerNamesBuilder[player.Id] = player;
                cashText = Currency.FromBank(startCash, player);
                ReportBuffer.Add(this, LocalText.GetText("PlayerIs",
                        playerIndex,
                        player.Id));
            }
            this.playerNames = playerNamesBuilder;

            ReportBuffer.Add(this, LocalText.GetText("PlayerCash", cashText));
            ReportBuffer.Add(this, LocalText.GetText("BankHas", Bank.Format(this, bank.Cash)));
        }

        public void FinishConfiguration(RailsRoot root)
        {
            foreach (Player player in playerModel.PlayerOrder)
            {
                player.FinishConfiguration(root);
            }
        }

        // sets initial priority player and certificate limits
        // TODO: rename method
        public void Init()
        {
            priorityPlayer.Set(playerModel.PlayerOrder.Get(0));
            int startCertificates = playerCertificateLimits[playerModel.PlayerOrder.Count];
            playerCertificateLimit.Set(startCertificates);
        }

        public int MinPlayers
        {
            get
            {
                return minPlayers;
            }
        }

        public int MaxPlayers
        {
            get
            {
                return maxPlayers;
            }
        }

        public IReadOnlyCollection<Player> Players
        {
            get
            {
                return playerModel.PlayerOrder.View();
            }
        }

        public Player GetPlayerByName(string name)
        {
            return playerNames[name];
        }

        /**
         * @return number of players including those which are bankrupt
         */
        public int NumberOfPlayers
        {
            get
            {
                return playerModel.PlayerOrder.Count;
            }
        }

        int NumberOfActivePlayers
        {
            get
            {
                int number = 0;
                foreach (Player player in Players)
                {
                    if (!player.IsBankrupt) number++;
                }
                return number;
            }
        }

        // dynamic getter/setters
        public GenericState<Player> CurrentPlayerModel
        {
            get
            {
                return currentPlayer;
            }
        }

        public Player CurrentPlayer
        {
            get
            {
                return currentPlayer.Value;
            }
        }

        public void SetCurrentPlayer(Player player)
        {
            // transfer messages for the next player to the display buffer
            // TODO: refactor nextPlayerMessages inside DisplayBuffer
            if (CurrentPlayer != player && !nextPlayerMessages.IsEmpty)
            {
                DisplayBuffer.Add(this,
                        LocalText.GetText("NextPlayerMessage", CurrentPlayer.Id));
                foreach (string s in nextPlayerMessages.View())
                    DisplayBuffer.Add(this, s);
                nextPlayerMessages.Clear();
            }
            currentPlayer.Set(player);
        }

        public Player PriorityPlayer
        {
            get
            {
                return priorityPlayer.Value;
            }
        }

        public GenericState<Player> PriorityPlayerState
        {
            get
            {
                return priorityPlayer;
            }
        }

        public void SetPriorityPlayer(Player player)
        {
            priorityPlayer.Set(player);
        }

        public int GetPlayerCertificateLimit(Player player)
        {
            return playerCertificateLimit.Value;
        }

        public void SetPlayerCertificateLimit(int newLimit)
        {
            playerCertificateLimit.Set(newLimit);
        }

        public IntegerState PlayerCertificateLimitModel
        {
            get
            {
                return playerCertificateLimit;
            }
        }

        /**
         * Use setCurrentPlayer instead
         */
        [Obsolete]
        public void SetCurrentPlayerIndex(int currentPlayerIndex)
        {
            // DO NOTHING
        }

        public Player SetCurrentToNextPlayer()
        {
            Player nextPlayer = GetNextPlayer();
            SetCurrentPlayer(nextPlayer);
            return nextPlayer;
        }

        public Player SetCurrentToPriorityPlayer()
        {
            SetCurrentPlayer(priorityPlayer.Value);
            return priorityPlayer.Value;
        }

        public Player SetCurrentToNextPlayerAfter(Player player)
        {
            Player nextPlayer = GetNextPlayerAfter(player);
            SetCurrentPlayer(nextPlayer);
            return nextPlayer;
        }

        public Player GetNextPlayer()
        {
            return playerModel.GetPlayerAfter(currentPlayer.Value);
        }

        public Player GetNextPlayerAfter(Player player)
        {
            return playerModel.GetPlayerAfter(player);
        }

        /**
         * @bool include the current player at the start
         * @return a list of the next (active) playerOrder after the current player
         * (including/excluding the current player at the start)
         */
        public IReadOnlyCollection<Player> GetNextPlayers(bool include)
        {
            return GetNextPlayersAfter(currentPlayer.Value, include, false);
        }

        /**
         * @param bool include the argument player at the start
         * @param bool include the argument player at the end
         * @return a list of the next (active) playerOrder after the argument player
         * (including / excluding the argument player)
         */
        public IReadOnlyCollection<Player> GetNextPlayersAfter(Player player, bool includeAtStart, bool includeAtEnd)
        {
            List<Player> playersAfter = new List<Player>();
            if (includeAtStart)
            {
                playersAfter.Add(player);
            }
            Player nextPlayer = playerModel.GetPlayerAfter(player);
            while (nextPlayer != player)
            {
                playersAfter.Add(nextPlayer);
                nextPlayer = playerModel.GetPlayerAfter(nextPlayer);
            }
            if (includeAtEnd)
            {
                playersAfter.Add(player);
            }
            return playersAfter;
        }

        // TODO: Check if this change is valid to set only non-bankrupt playerOrder
        // to be priority playerOrder
        public Player SetPriorityPlayerToNext()
        {
            Player priorityPlayer = GetNextPlayer();
            SetPriorityPlayer(priorityPlayer);
            return priorityPlayer;
        }

        [Obsolete]
        public Player GetPlayerByIndex(int index)
        {
            return playerModel.PlayerOrder.Get(index % NumberOfPlayers);
        }

        /**
        *
        *@param ascending Boolean to determine if the playerlist will be sorted in ascending or descending order based on their cash
        *@return Returns the player at index position 0 that is either the player with the most or least cash depending on sort order.
*/
        public class CashComparator : IComparer<Player>
        {
            bool ascending;
            public CashComparator(bool asc)
            {
                ascending = asc;

            }
            public int Compare(Player p1, Player p2)
            {
                return ascending ? p1.Cash - p2.Cash : p2.Cash - p1.Cash;
            }
        }

        public Player ReorderPlayersByCash(bool ascending)
        {

            bool ascending_f = ascending;

            CashComparator cashComparator = new CashComparator(ascending);

            playerModel.Reorder(cashComparator);

            // only provide some logging
            int p = 0;
            foreach (Player player in playerModel.PlayerOrder)
            {
                log.Debug($"New player {++p} is {player.Id}" +
                        " (cash=" + Bank.Format(this, player.Cash) + ")");
            }

            return playerModel.PlayerOrder.Get(0);
        }

        public void ReversePlayerOrder(bool reverse)
        {
            playerModel.Reverse.Set(reverse);
        }

        public PlayerOrderModel GetPlayerOrderModel()
        {
            return playerModel;
        }

        public class PlayerOrderModel : RailsModel
        {
            private ListState<Player> playerOrder;
            private BooleanState reverse;

            public PlayerOrderModel(PlayerManager parent, string id) : base(parent, id)
            {
                playerOrder = ListState<Player>.Create(this, "playerOrder");
                reverse = BooleanState.Create(this, "reverse");
                reverse.Set(false);
            }

            public Player GetPlayerAfter(Player player)
            {
                int nextIndex = playerOrder.IndexOf(player);
                do
                {
                    if (reverse.Value)
                    {
                        nextIndex = (nextIndex - 1 + playerOrder.Count) % playerOrder.Count;
                    }
                    else
                    {
                        nextIndex = (nextIndex + 1) % playerOrder.Count;
                    }
                } while (playerOrder.Get(nextIndex).IsBankrupt);
                return playerOrder.Get(nextIndex);
            }

            // this creates a new order based on the comparator provided
            // last tie-breaker is the old order of playerOrder
            public void Reorder(IComparer<Player> comparator)
            {
                //Ordering<Player> ordering = Ordering.from(comparator);
                //List<Player> newOrder = ordering.sortedCopy(playerOrder.view());
                List<Player> newOrder = new List<Player>(playerOrder.View());
                newOrder.Sort(comparator);
                playerOrder.SetTo(newOrder);
                for (int i = 0; i < newOrder.Count; i++)
                {
                    Player player = newOrder[i];
                    player.SetIndex(i);
                }
            }

            public BooleanState Reverse
            {
                get
                {
                    return reverse;
                }
            }

            public bool IsReverse
            {
                get
                {
                    return reverse.Value;
                }
            }

            public ListState<Player> PlayerOrder
            {
                get
                {
                    return playerOrder;
                }
            }

            override public string ToText()
            {
                // #FIXME
                // FIXME: This has to be checked if this returns the correct structure
                // and may be it is better to use another method instead of toText?
                List<string> li = new List<string>(playerOrder.View().Count);
                foreach (Player p in playerOrder.View())
                {
                    li.Add(p.ToString());
                }
                return string.Join(";", li.ToArray()); //.Util.JoinWithDelimiter(new List<Player>(playerOrder.View()).ToArray(), ";");
            }
        }

        /**
         * @return number of players (non-bankrupt)
         */
        public static int GetNumberOfActivePlayers(IRailsItem item)
        {
            return item.GetRoot.PlayerManager.NumberOfActivePlayers;
        }
    }
}

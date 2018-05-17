using GameLib.Net.Util;
using System;
using System.Collections.Generic;


/**
 * WalletManager stores links to all existing wallets
 */

namespace GameLib.Net.Game.State
{
    public class WalletManager : Manager
    {
        public class WMKey<T> : IEquatable<WMKey<T>> where T : ICountable
        {
            private readonly Type type;
            private readonly IOwner owner;

            public WMKey(Wallet<T> p)
            {
                this.type = p.GetWalletType();
                this.owner = p.Parent;
            }

            public WMKey(Type type, IOwner owner)
            {
                this.type = type;
                this.owner = owner;
            }

            override public bool Equals(object other)
            {
                var otherKey = other as WMKey<T>;
                if (!(other is WMKey<T>)) return false;
                //WMKey <?> otherKey = (WMKey <?>)other;
                //return Objects.equal(type, otherKey.type) && Objects.equal(owner, otherKey.owner);
                return type.Equals(otherKey.type) && owner.Equals(otherKey.owner);
            }

            public bool Equals(WMKey<T> other)
            {
                if (other == null) return false;
                return type.Equals(other.type) && owner.Equals(other.owner);
            }


            override public int GetHashCode()
            {
                //return Objects.hashCode(type, owner);
                return type.GetHashCode() ^ owner.GetHashCode();
            }

            override public string ToString()
            {
                //return Objects.toStringHelper(this).add("Type", type).add("Owner", owner).toString();
                return $"{this.GetType().Name}{{Type={type.Name}}}{{Owner}}={{{owner.ToString()}}}";
            }

        }

        private readonly DictionaryState</*WMKey<T>*/object, GameState/*Wallet<U>*/> wallets;

        private readonly UnknownOwner unknown;


        private WalletManager(IItem parent, string id) : base(parent, id)
        {
            wallets = DictionaryState<object, GameState>.Create(this, "wallets");
            unknown = UnknownOwner.Create(this, "unknown");
        }

        public static WalletManager Create(StateManager parent, string id)
        {
            return new WalletManager(parent, id);
        }

        public UnknownOwner UnknownOwner
        {
            get
            {
                return unknown;
            }
        }

        /**
         * @param Wallet to add
         * @throws IllegalArgumentException if a Wallet of that type is already added
         */
        public void AddWallet<T>(Wallet<T> Wallet) where T : ICountable
        {
            WMKey<T> key = new WMKey<T>(Wallet);
            Precondition.CheckArgument(!wallets.ContainsKey(key),
                    "A Wallet of that type is defined for that owner already");
            wallets.Put(key, Wallet);
        }

        /**
         * @param Wallet to remove
         */

        public void RemoveWallet<T>(Wallet<T> p) where T : ICountable
        {
            wallets.Remove(new WMKey<T>(p));
        }

        /**
         * Returns the Wallet that stores items of specified type for the specified owner
         * @param type class of items stored in Wallet
         * @param owner owner of the Wallet requested
         * @return Wallet for type/owner combination (null if none is available)
         */
        // This suppress unchecked warnings is required as far I understand the literature on generics
        // however it should not be a problem as we store only type-safe Wallets
        public Wallet<T> GetWallet<T>(Type type, IOwner owner) where T : ICountable
        {
            //#CHECKME does this actually look up the right thing?
            return (Wallet<T>)wallets.Get(new WMKey<T>(type, owner));
        }

        // backdoor for testing
        public WMKey<T> CreateWMKey<T>(Type type, IOwner owner) where T : ICountable
        {
            return new WMKey<T>(type, owner);
        }


    }
}

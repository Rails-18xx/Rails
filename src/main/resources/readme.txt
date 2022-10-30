# Rails 2.5 New Release Version

Major Contributor: Erik Vos

1837: Actually, the main achievement of this release is the completion of 1837 (still marked beta though).
      1826 is being worked on (I'm currently wrestling with the hex distance to determine tokening cost to be counted via laid track. A few weeks ago it seemed to work, but later appeared to break with closed loops.)


    The new <Shares> tag replaces <ShareUnit> and now allows specification of different share sizes, like ... unit="20,10". This will be used in 1826, where companies start with 5 shares, and convert later to 10-share companies. The <Certificate> tags are now children of the <Shares> tag.

    The <Payout> tag now also includes the details of the former <AdjustPriceOnPayout> tag.

The above changes have been applied to all Rails games.
Also included:

    In 1851 and 18EU: implemented the (previously missed) rule that a company in its first OR may buy (but still not sell) Treasury shares. These two allowances can now be specified separately in the <TradeShares> tag. The default is that both are allowed (as in SOH).

    Some initial work to expand the 1826 prototype.

1826: added implementation details (1), and several fixes.
 18EU, 1851: In its first OR, a company may redeem (buy) but not issue…

… (sell) treasury shares.
 Fix GREEDY edge status prohibiting valid tile rotation

The check in NetworkGraph.getReachableSides() on an edge having GREEDY status has been disabled. This resolves issue #478.

Also a bug in ListAndFixSavedFiles that mixed up Stop and Station numbers has been fixed.

    master (#471)
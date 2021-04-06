# Rails 2.4 Maintenance Version 6

Major Contributor: Erik Vos
Minor Contributors : Martin Brumm, Marc Arndt


Fix for 18EU& others: Single Trains could cause a Java Exception on Mandatory Discard if not obsolete.
Fixes for 1837: Mine revenues of EPP and SPB are incorrect #376
          1837: Sold-out company where president has <= 40% shares does not rise at end of SR #375
          1837: More than 2 companies can have same par price #374
          1837: Price drops one row for each sold share. #373
          1837: Hexes K21 (U2 home) and L22 (town SE of U2) behave as unconnected
          1837: Bosnia and Italy map hex colours #361

          [1] In version 1, the S5 home hex location is still done in the initial round. It works, but is not according to the rules.
          Status: Fixed, not yet released. During testing I discovered, that this action was assigned to the previous player (S4 owner) rather than the S5 owner. Apparently, the new player's turn was not yet started. This has been fixed (perhaps the code needs to be revisited).

          [2] During testing several problems with coal company exchanges have been encountered. The code seems to offer this option only directly after a major company has floated. In practice, offering an exchange at the start of any round sometimes occurs and sometimes not, whether mandatory or optional. It has also happened automatically. Further testing is needed to sort this all out.
          Status: fixed, should be in release 2.4.5 (this also applies to cases [3], [12] and [13]).
          P.S.: I have no info yet on what is included in either release.

          [3] If, after a coal company exchange, the related major company has too many trains, a "voluntary" train discard is offered, including a "None" option, even though it is really mandatory.
          Status: see [2].

          [4]-[8]: See next comment.

          [9abc] No connection between preprinted tiles on hexes K21 (U2 home) and L22.
          Status: Confirmed, cause still unknown. Discussed further in issue #360.

          [10] Revenue rounding.
          Status: Fixed, see issue #372 for details.

          [11] Major capitalisation wrong (always 100%).
          Status: Fixed, should be in release 2.4.5.

          [12] Missing coal exchange rounds
          Status: See [2].

          [13] Forced coal merge does not give the owner a 10% share.
          Status: Whatever was the situation in your run, it currently works fine. Should be in release 2.4.5.

          [14] More than two companies with same par price.
          Status: Fixed, see #374.

          [15] Share price dropping should be as in 1835, not 1830.
          Status: Fixed, see issue #373.

          Edit: Added some remaining issues

          [16ab] Crashes and other problems after buying the first 4-train.
          Status: Testing past that point has not yet been started, as too many problems were found in earlier stages. Once these pre-4T problems are fixed to the extent that at least the finances are stable (so I don't have to recreate the SR4 test files again and again), further testing will start.

          [17ab] Wrong share price movements for sold-out majors.
          Status: Fixed, see #375.

          [18] Different mine income values in v1 vs. v2?
          Status: Fixed, see issue #376.

          [19] Coal companies moving to different player?
          Status: Never seen this. May have been fixed by the overhaul of the Coal Exchange Round some time ago. Consider it fixed until any contrary report is posted.



# Rails 2.0 Beta 5 release:

## Beta 5 Improvements

### 18NL supported

For details of the game see <a href="http://www.ohley.de/18nl/start.htm">18NL documentation</a>.

### Tile and Token lays

Implementation of edge cases / rules ambiguities, see [document on special tiles and token lays.](PrivatesTileToken)

### 1835
* New implementation to exchange the president certificate, for details see [document on selling the president certificate.](SellingPresident)

* A new game option allows to select the Westermann ruling that NF/PfB token powers are restricted to the token lay step

## Bugs fixed in Beta 5 Release
 
### Fixed UI Bugs
* fixed display of tokens in hex tooltip
* fixed display of tokens of map for 4-slots cities (e.g. NYC in 1830 Coalfields)
* in StartRoundWindow the priority player indication is updated
 
### Fixed 1830 (and derivatives) Bugs
* fixed bug during auction that zero price private was bought by wrong player
 
### Fixed 1835 bugs:
* (partial) fix for 1835: swap of presidency after prussian merger round: exchange 10% before 5% shares

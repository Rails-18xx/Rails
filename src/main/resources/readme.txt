# Rails 2.0 Beta 3 release:

## New features

- First round sell restriction
Added implementation that allows correct handling of the sell restriction during the first round of an 18xx games. 
There is a new GameOption that decides when the sell restriction ends.

<GameOption name="FirstRoundSellRestriction" values="First Round,First Stock Round" default="First Round" /> 

More information are found [here] (FirstRoundSellRestriction).

- New map correction mode
During the correction mode tile lays are possible anywhere on the map. Only upgrades are possible, no downgrades. 
Terrain costs are ignored and (if required) have to be assigned to a company with a cash correction. 

## Bug Fixes

### Fixed 1830 bugs:
- Add correct handling of Coalfield rights (both tile laying and revenue calculation)

### Fixed 1880 bugs:
- Definition of tiles 8858, 8854, 8885 were wrong (sides connected to sides, instead sides to the towns), Effect was wrong revenue calculations
- Added Beijing 8887, 8888 to handemade tiles, as they cannot be setup correctly with TileDesigner
- Fixed definition of tile 8879 (Shanghai): Loop track from city to city
- Fixed definition of tile 8880 (Shanghai): Duplicate track definition, wrong side to side connection instead of side to city
- Fixed definition of tile 455: wrong name (495)
- Fixed definition of Bejing green 8886: Loop track from city to city, duplicate track definition
- Added Beijing -80004 to handemade tiles, cannot be setup correctly with TileDesigner
- Reverted to the working version of Ferry modifier (based on RevenueBonuses defined on map.xml)
- Fixed omission in Express Modifier: Protect ferry modifier vertices to incorporate ferry maluses for express trains
- Fixed bug on Mouse-Overs for player certificates details: president was always shown as 20% certificate 
- Only offer tile lays for which the company has the permission to lay that tile colour
- Improved undo/redo behavior during operating rounds by adding state variables
- Fixed wrong 1880 Stockmarket attribute
- Fixed double reporting of 'company operates' for investors in 1880
- Fixed display bug in Investor exchange message
- Reordered player order shown in report window
- Selection of Building Rights: Show only selections with the correct number of building rights
- Added Par-Slot and Rights chosen after purchase of director to the report window 
- Fixed bug that have the game terminate before the last company operates
- Fixed bug that 6E, 8, 8E trains ignored towns
- Added brokerage fee to report window
- Do not show zero payoffs of Privates in later phases
- Fixed wrong output to report window: no discard of trains to pool anymore, added option to TrainManager where to discard trains

### Fixed 18EU bugs:
- Rails stopped during StartRound


## Remarks:
- A few reported bugs from previous 2.0 Alpha/Beta releases are still open.

- All automated test games run.
Rails release 1.7.0:

A new release for Rails 1.x series

This release celebrates many improvements to the UI due to the 
work of Frederick Weld.

Contributors: Erik Vos, Frederick Weld, Martin Brumm, Stefan Frey

Includes all previously reported bug fixes of the 1.6.x branch.

Please be aware that this release contains many changes, so bugs are more likely.
Any feedback is welcome.

Many of the included changes are NOT activated by default: Either switch them off
using the configuration dialog or use one of the new predefined profiles:

prettyUI --> all UI changes except docking activated
ORdocking --> only activates the docking framework
(currently only available for the OR window) 

Note: To show a background map, the option has to be switched on in Configuration => Map/Report => Display background map.
Background maps are only available for 1856, 1889, 18EU, 18GA (incl. Cotton Port) and 18AL so far.

The following are the major changes by topic:

General UI improvements:
* Interactive highlighting of hexes
* Enhanced highlighting of active table cells during Operating Round
* Adaptive rendering of token labels
* Lay Tile: Invalid tile lays are displayed (incl. the reason for
not being valid)
* Support for icons
* Splash screen with improvement startup behavior

Added UI options:
* Map / Zoom Fit Option: Fit-to-window and more
* Map / Display Routes of Active Company
* Map / Highlight company locations
* Appearance / Display borders in grid layout
* Windows / Flexible panels for operating round: 
Applies a docking framework to Operating Round Window

Added Music/Sound Effects options (mp3 only):
* Music: Background music can be specified per round-type and phase
* Sound Effects: Sound effects can be associated to more than 20 game events
* Includes support of "intelligent" effects (eg., revenue
sound as in RailroadTycoon1)
* No music/sound files are provided by Rails

Rails configuration:
* Added support for OS dependent centralized rails file storage 
(UNIX: ~/rails, Windows: %APPDIR, MacOS: ~/Library/Preferences/net.sourceforge.rails)
* New configuration profile system based on profile hierachy
* Several predefined profiles can be distributed

Further changes:
* Added StatusWindow File menu action to dump the (transposed) contents into a csv file
* Several changes to use non-modal dialogs
* Several updates to 1880 development
* Added user-friendly network info including keyboard shortcut
* Added highlighting of private companies' hexes triggered by mouse-over
* Added invalid tile lays to upgrade panel (grayed out & reason)
* Added option to play 1856 as a 2-player game.

 
Further bug fixes:
* Fix for 1835 bug: BY presidency not transferred during start round.
* Always address the company president when a home token must be relaid.
* Fixed 'Load Recent' by running it in a separate thread.
* Fixed the glitch of initially displaying map images in the wrong scale
* Fixed 1856 background map's mini-map (Windsor area)
* Added precise sizing and positioning of token labels


# Rails-18xx 2.5 Maintenance Release
This release is meant to show the current state of the Rails-18xx implementation.

Due to different reasons Rails-18xx 2.0 and 2.1 have not been release as production releases. We hope that the audience
finds the release usefull. As usual we welcome bug reports and save games that show the affected states of the game.

We would like to **thank** the various bug reporters and the developers who have contributed over the years for one
feature or ground breaking work.
Please take note that to run Rails-18xx as a JAR directly you need to install a JDK with Java V11 as minimum. The game comes with its own JRE compiled in.

Please dont use the .bat from earlier versions.

**Using the installer on Windows and the same directory as previous installations, will overwrite everything in that directory.**

We would like to **thank** the varios bug reporters and the developers who have contributed over the years for one feature or ground breaking work.

## New games

## Status of Games

* Implementation of 18Chesapeake finished, needs playtesting, bug reports welcome
* Implementation of 1837
  * Coal Minors merging round implementation needs to be tested.

## Bug Fixes
# Rails 2.5 Release Version

Major Contributor: Erik Vos
Minor Contributor: Martin Brumm

Added 1826 (alpha)
Added a new configuration item to display a short extra bit of text on a hex.
Used to display mine names in 1837.

Also added stuff to the initial configuration of 18VA.

1826: tokening cost 40K step was missing, national start price calculation ignored duplicate prices

1837: sold-out price did not increase when president has a 40% share

Updated 1826 and 1837 game descriptions
1835: Fixing share selling

The 1835 rule of selling certificates rather than shares (in other words: certificates being unsplittable) was not implemented correctly. It is now no longer possible to sell different share sizes in one action (but still it can be done in two separate actions).

This has also much simplified the selling code, which had got almost impossible to understand. The action preparation code in ShareSellingRound now could be and has been merged into StockRound.

A consequence is, that multiple sell actions of the same company in the same turn are now done at the same price. At this violates English rule XV.8, German rule 3.3.7, a new GameOption has been added by which this special behaviour can be undone at game start.

Also a new action named AdjustSharePrice has been added, which is only available through the Special Menu, immediately after the second (or third, etc.) sale of shares of the same company has occurred, to lower its price anyway. This action has been prepared for later reuse as a generic Correction activity.



# Rails-18xx 2.2 Release
This release is meant to show the current state of the Rails-18xx implementation.

Due to different reasons Rails-18xx 2.0 and 2.1 have not been release as production releases. We hope that the audience finds the release usefull.
As usual we welcome bug reports and  save games that show the affected states of the game.

We would like to **thank** the varios bug reporters and the developers who have contributed over the years for one feature or ground breaking work.


## New Features

* Notification of Moves via Slack
* Notification of Moves via Discord
* Autoloading and Autopolling for near time gaming allowing you to reload the save game that has changed during turns
* Direct Revenue Allocation for various Contracts possible
   * State of Direct Revenue Allocation is static towards company operations budget (no Half pay) - Workaround you half the allocation and add the rest to normal revenue distribution
 * Installers for Windows, Mac OS, Linux (Redhat)

## New games
We continue on expanding the game base, the first new game on this for this release is 18Chesapeake.

## Status of Games
* Implementation of 18Chesapeake started, needs playtesting, bug reports welcome
* Implementation of 1837 - Have a look but be prepared that it might break... 
   * Open Topics in 1837 Implementation:
     * Revenue Calculation of Coal Trains
     * Coal Minors merging round implementation not always working as intended..

## Bug Fixes
* 1835 : 
  * Numerous Fixes in regard to the director shares (you cant split sell them in 1835)
  * Fixes for the Forced Sale of Shares 
* 1856:
  * Fix multiple Rights issue upon foundation of the CGR

## Issues fixed:
 * #55
 * #205 
 * #180
 * #179
 * #129 
 * #130 
 * #75 
 * #71 
 * #72 
 * #73
 * #76 
 * #77 
 * #78 
 * #90 
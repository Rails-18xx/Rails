Rails release 1.8.3

This is a minor release fixing a number of minor glitches:

Martin Brumm (22):
      Fixing a map bug that lead to problems in upgrades and subsequently randomly placement of upgrade tokens for the home base of the SCR.
      Reverting Companymanager Starting hex change for SCR fixing tiles for 1880.
      Fixed 8863 city position
      The Standard of setNextPlayer from StartRound.java should suffice.
      Prepared for Release 1.8.2
      Moving ParSlotModel to rails.game.model needs to be able to be reused for other variants.
      Fixing the bug reported by Pieter Lust for 1835.
      Fixing incorrect display of Tile 235
      Fixing reported bug regarding 2R being counted as emergency train.
      Need to introduce a new Class to fix the 2R getting counted as blocker against the next TrainType becoming available.
      Fixing Train Definition for 6e and 8e.
      The Train 2R got discarded wrongly if no train was bought.  
      Fix for 1880 Medium Type cities that might be reduced to towns on initial yellow tile lay.
      Removing unnecessary stub check in finishTurn().
      Fixing a small glitch in the OR Numbering code. Introducing a mechanismn to handle OR limits .
      Fixed bug in process that prevented the game end to happen, causing an infinite loop.
      Cleaning up optics of 1880 specific tiles.
      Allow AddBuildingPermit to be used at any Turn Step inside an OR.

Michael Alexander (4):
      Fixed the following problems: 
                - Companies opened when 60% is required got 50% too much money 
                - Sold out companies on the top row should not move down and left     
                - Tile #8855 had the routes defined incorrectly.
      Trains cannot be purchased between companies until a 3-train is purchased.
      Fixed problem with a tile definition, and problem where when all trains of a given type were scapped, a phase change did not occur.
      "Fixed" problem where tokens were not being displayed consistantly after Beijing was upgraded to brown while one or more companies with their home in Beijing were not yet open. The problem is that the "home" defined in the .xml file for those companies is no longer valid, but how should this be fixed better?

 
Please report any bugs you find on the mailinglist as usual.

Thank you for the patience and feedback.

Martin & Michael (together with the rest of the rails crew :))


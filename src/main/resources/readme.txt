# Rails 2.0 Release Candidate 3

Major Contributor: Stefan Frey
Minor Contributors : Erik Vos, Chris Noon, Marek Jeszka, Martin Brumm

Be carefull while using this Release on games in progress on 1880 and 1835. 
Due to the bugfixes here it is recommended that you replay the game using the game log from the old version. 
It was not possible to make the savegames compatible if you ht the bug in your ongoing game (1835).

Fixes:
Fixed the Reload Command, missing was the RailsRoot Instance, Move some
Code around to resemble the initial load sequence.
Condition check for zero vertexes added (Marek Jeszka)

18AL : BonusToken Situation in 18AL
		Quick Fix to make MapHex.upgrade working for hexes that have no station
		and get upgraded to a station by a specialtilelay.
		18AL has exactly that scenario and a number of other games too.

1880 : 	Fix Tile 8877 Shanghai Yellow value was wrong now corrected to 30
		Fixing a bug in 1880s handling of players debt during emergency raise of trains.
		Fixing the bug that an investor was still able to operate in Phase 4.
			This apparently didnt get transfered to 2.0 RC1 while being fixed in 1.9.
		Fixed the problem that a Special Train Buy using the RC could trigger a
		phase change that wouldnt be recognised for Train Discards.
		Fixed missing Stockmarket field F8.

1835 :	Corrected game behaviour for 1835. According to Rules a sell share action only does NOT prolong the Stockround but is treated as a pass.
		Only share buying actions are valid to prolong a round.	


1856: 	Fixes by Erik Vos
		fixed reload and press done button bug in 1856 (SF)
		quick fix to remove token of ship company in 1856 in phase 5 
		(first 6 train), this is related to name and realName attributes of Phase (SF)

18EU: 	added 18EU test games from Volker Schnell, fixed two reporting issues (SF)
		preliminary bugfix for 18EU offboard revenue modifier (SF)
		fix for StartCompany_18EU equality problems (SF)
		added third station on B/V -3005 tile to prevent error on upgrade to 581 (SF)
		
Internal reworking of code and structure too numerous to mention; all being done by Stefan Frey
			
UI Fixes: (Chris Noon)
allow use of the edit box on spin controls, i.e.  font size.
Sizeable Fonts in Stockspace enabled

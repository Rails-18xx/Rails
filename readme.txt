Rails release 1.8.6

This is a minor release fixing the following bugs:

    - 1856:   Fixing the bug reported on the CGR Formation if previous to the
			  formationround companies have already been closed.
			  Fixing a bug with a player being forced to buy a 10 percent share
			  but couldnt to become president of the CGR after formationround.
			  
	- 1880:	  Fixed problem when an investor was the last company to run in an
			  operating round.  nextStep(FINAL) (which calls done()) should not
			  be called inside getPossibleActions() - instead, done() should be
			  called inside process()
    	    

Thank you to all who reported the bugs.

Please report any bugs you find on the mailinglist as usual.

Thank you for the patience and feedback.

The Rails Crew wish you a Happy New Year 2014 !

Martin & Michael (together with the rest of the rails crew :))


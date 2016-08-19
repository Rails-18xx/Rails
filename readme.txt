Rails release 1.9.2

This is a minor release fixing the following bugs:
 
	-1880: 	Created a silent Discard Train Routine to handle the case that a special
			train buy will trigger a Phase Change. 
			Fixes a Bug reported by Rick Westermann.	 
	-1835: 	Corrected behaviour in 1835 Stockround that treated a share selling
			action so far as prolonging the stockround. According to the german
			rules that was not correct. A Share selling action only does not prolong
			the round, a share must be bought.
	-1856: 	Corrected Bug in Phase handling of ports. The port marker got removed
			one phase earlier than intended by the rules.
			Fixes a Bug reported by Erik Vos
			
	1856: 	fixed bugs in certificate limit recalculation

			Recalculation was:
			- not done if CGR did not form
			- not done when companies closed after CGR formation 
			- incorrect even if done if CGR did not form and companies closed
			- missing some checks  

Thank you to all our testers. 

Please report any bugs you find on the mailinglist as usual.  
 


Thank you for the patience and feedback.

Martin & Erik


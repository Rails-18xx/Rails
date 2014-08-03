Rails release 1.8.9

This is a minor release fixing the following bugs:
 
	- 1880:	  Fixed a bug reported by Volker Schnell: Multiple sales of Share in the same action
	          didnt result in the stock marker to be adjusted correctly. This would lead to higher
	          stock values.
	          
          ATTENTION: This fix will break/ affect all games in which a player has sold more than one
          share in a single action. 
			  

Thank you to all our testers.

Please report any bugs you find on the mailinglist as usual.

Thank you for the patience and feedback.

Martin 


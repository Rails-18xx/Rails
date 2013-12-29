Rails release 1.8.5

This is a minor release fixing the following bugs:

    - 1880: Operation rounds in 1880 would end after 3 occurrences with a 
            hard reset towards the first company in the current round.
            Stockrounds would be ignored. The hard coded end after the 
            third OR is now enforced at game end if specific conditions are met.
            Fixed definition error on Map - Water tile J16 didn't have its negative value set. 
            Please check if the YC-Specialproperty is working as intended.

    - 1856: The position of the CGR-share on the stockmarket was hardcoded 
            to the second row on formation. This has now been moved to row 0. 
            Please check. This will break all 1856 games in progress so caution is advised. 

    - 1835: Introducing a switch name PrussianReservedIgnored. This switch if
            toggled to yes on game start will tell rails to ignore unavailable
            prussian shares (shares that have not yet been converted in the
            Prussianformation process) in determining if the prussian shares are
            sold out or not. 


Thank you to all who reported the bugs.

Please report any bugs you find on the mailinglist as usual.

Thank you for the patience and feedback.

The Rails Crew wish you a Happy New Year 2014 !

Martin (together with the rest of the rails crew :))


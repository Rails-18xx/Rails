Rails release 1.7.13:

A new maintenance release for Rails 1.x series

   - 1856: The position of the CGR-share on the stockmarket was hardcoded 
            to the second row on formation. This has now been moved to row 0. 
            Please check. This will break all 1856 games in progress so caution is advised. 

    - 1835: Introducing a switch name PrussianReservedIgnored. This switch if
            toggled to yes on game start will tell rails to ignore unavailable
            prussian shares (shares that have not yet been converted in the
            Prussianformation process) in determining if the prussian shares are
            sold out or not. 
      		Fixing a bug reported by Pieter Lust regarding the income of prussion minors during formation
      		Fixing a Bug reported by Mikaela Kumlander regarding the Presidency Changes during Prussion formation.

Contributors: Martin Brumm

Bug reported by Volker Schnell, Mikaela Kumlander, Pieter Lust

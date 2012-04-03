Rails release 1.7.1:

A new maintenance release for Rails 1.x series

This release fixes several recent bugs.

Contributors: Erik Vos, Martin Brumm, Stefan Frey

Bugs reported by John David Galt, Volker Schnell, Mike Bourke, Arne Östlund, Jerry Anderson
(I hope I forgot to list no one).

A few minor improvements to the rails setup:
- An (initial) configuration profile for face-to-face/hotseat play is added ("hotseat"): Activates autosave and changes filename pattern
suggested for save. 
- Windows settings (including docking settings if activated) and autosave backup are stored in the centralized rails configuraton folder.
Thus from this release on those settings should survive upgrades to new releases.

List of bugs fixed:

- Typo in manifest that caused image errors in some background maps
- 18EU: Not possible to lay home token for companies started outside of current network.
- 1856, 18EU, 1830 Coalfield and others: Fixed wrong calculation for routes for Goderich tile, red off-board run-through (-939)
- 18Kaas: Enables unlimited D-trains option
- 1835: Fixed wrong total player worth calculation
- 1835: Fixed wrong revenue calculation with respect to Elsas/Alsace outside of green phase

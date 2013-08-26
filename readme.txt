Rails release 1.7.12:

A new maintenance release for Rails 1.x series

This release fixes a single bug.

Contributors: Erik Vos

Bug reported by Volker Schnell

1835: after resuming an OR after a PR formation round, a check was missing if the (minor) operating company still exists.
Fix: finish the turn if the operating company is closed at that point.

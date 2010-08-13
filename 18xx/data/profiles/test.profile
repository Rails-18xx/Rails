####################### Test preferences ################################
#
# Those are the settings used for automated testing
#
########################################################################
# 
# Preferred tile format. 
# The only currently supported format is svg. Anything else is ignored.
#tile.format_preference=svg
# Root directory for the tile images (just above directory 'tiles').
# Not required if tile images are provided included in the Rails jar file.
#tile.root_directory=

### Locale ####
# Language: two-letter ISO code (lower case; default is en=English).
# Country: two-letter ISO code (upper case; specifies country 
# (implying a language variant of that country; no default).
# Locale: concatenation of the above. If present, overrides any of the above.
# Examples: en, en_US, en_UK, fr_FR, fr_CA.
locale=te_ST
#language=
#country=

### Money display ###
# Each game has a specific format for monetary amounts (e.g. $100, 100M).
# An overriding format can be specified here, but then applies to all games.
# The @ character must be present and is replaced by the amount.
# Example: �@ to specify a pound sign prefix: �100.
#money_format=$@

### Save file directory
# If the below entry exists, is not empty, and specifies an existing
# directory, that directory is used as a starting point for any
# file choosing action for the Save and Load commands.
# The path may be relative or absolute.
save.directory=test/data
# The default Save filename is <gamename>_<datetimepattern>.<extension>
# This name will be initially proposed.
# As soon as that proposal has been changed once in a Save action,
# the last used name is always proposed in stead.
# The default date/time pattern is yyyyMMdd_HHmm
# The pattern codes are as defined in the Java class 
# SimpleDateFormat (just Google that name to find the codes).  
#save.filename.date_time_pattern=yyyyMMdd_HHmm
# The default timezone is local time.
# A specific timezone (such as UTC) can be set; the value must be a Java timezone ID
#save.filename.date_time_zone=UTC
# Optionally, a suffix (e.g. player name) can be added after the time stamp
# with a preceding underscore (which is automatically added)
# The special value NEXT_PLAYER puts the next moving player name into this spot. 
#save.filename.suffix=NEXT_PLAYER
# The default extension is .rails
save.filename.extension=rails

### Game report directory
# If the below entry exists, is not empty, and specifies an existing
# directory, a copy of the Game Report (as displayed in the Report Window)
# will be saved there. The path may be relative or absolute.
#report.directory=log
# The default file name includes the game name and the game start time: 
# 18XX_yyyymmdd_hhmm.txt where 18XX is the game name.
# You can specify different values for the date/time part and teh extension here.
# The date/time pattern must be as defined in the Java SimpleDateFormat class.
#report.filename.date_time_pattern=yyyyMMdd
report.filename.extension=report

### Windows
##  Report window visibility
#   By default the report window is hidden when starting or loading a game.
#   This property allows to open it automatically.
#   Valid values are yes and no (default).
#report.window.open=yes
##  Report window editability
#   Specify if the report window is editable, so you can add your own comments.
#   Valid values are yes and no (default).
#report.window.editable=yes
##  Stock Chart window visibility
#   By default the stock chart hides at the end of an SR.
#   By specifying "yes" here, the window will not be automatically hidden any more
#stockchart.window.open=yes

### Player info
##  Default players
#   Comma-separated list of player names. 
#   Useful for game testing purposes.
#default_players=Alice,Bob,Charlie 
#
##  Local player name
#   Useful for distributed usage (Internet, PBEM, cloud storage/dropbox)
#   Required for "request turn" facility with cloud storage (dropbox)
#local.player.name=Alice

### Default game
# Name of game selected in the game selection window.
# Useful for game testing purposes.
#default_game=1830

### Various options
# Show simple (ORx) or composite (ORx.x) OR number.
# Valid values: "simple" and "composite" (default)
#or.number_format=simple

####################### Log4J properties ##############################
# For information on how to customise log4j logging, see for instance
# http://www.vipan.com/htdocs/log4jhelp.html
# It's a bit outdated: Category is now named Logger,
# and Priority is now named Level. 
# But it's the best intro I know on how to configure Appenders. (EV)
#######################################################################
# Set root logger level to DEBUG and use appender F(file)
#log4j.debug=true
log4j.rootLogger=DEBUG, F

# Define the Log file appender
log4j.appender.F=org.apache.log4j.FileAppender

# Log file properties
log4j.appender.F.File=test/test.log
log4j.appender.F.append=false

# Log file layout
log4j.appender.F.layout=org.apache.log4j.PatternLayout
log4j.appender.F.layout.ConversionPattern=%-5p  %m%n
################## End of Log4J properties #############################
use File::Copy;
$dirWID = "TDwithID";
$dirWOID = "TDwoID";
$dirSVG = "svg";
$dirHandMade = "handmade";

# Positive tile numbers: include ID
opendir (DIR, $dirWID) or die "Cannot open dir $dir: $!\n";
while (($file = readdir DIR)) {
	next unless $file =~/\.svg$/;
	copy "$dirWID/$file", "$dirSVG/$file" if $file =~ /^tile\d+\.svg$/;
}
closedir DIR;

# Nonpositive tile numbers: omit ID
opendir (DIR, $dirWOID) or die "Cannot open dir $dir: $!\n";
while (($file = readdir DIR)) {
	next unless $file =~/\.svg$/;
	copy "$dirWOID/$file", "$dirSVG/$file" if $file =~ /^tile-\d+\.svg$/;
}
closedir DIR;

# Finally overwrite with the handmade tiles
opendir (DIR, $dirHandMade) or die "Cannot open dir $dir: $!\n";
while (($file = readdir DIR)) {
	copy "$dirHandMade/$file", "$dirSVG/$file" if $file =~ /^tile-?\d+\.svg$/;
}
closedir DIR;

# Notes on creating new tiles:
# 1. In TileDesigner, export SVG tiles with size=170 and filename template=tile<c0>.
#    Do this with ID checked into directory tiles/TDwithID, and again
#    with ID unchecked into directory tiles/TDwoID.
# 2. If the saved tiles turn out to invisible, use program FixInvisibility.pl
#    to remove superfluous strings ' xmlns=""'. It is unknown why TileDesigner
#    sometimes includes this string in the path tags.
# 3. If tiles are modified with Inkscape, before saving, set the following properties
#    via File|DocumentProperties:
#    - First press 'Fit page to selection'. This should change the size to
#      Width=393.00 and Height=341.00.
#    - To add the extra whitespace below the tile image that TileDesigner also adds
#      (for unknown reasons), change the Height to 357.50.
#    - Then save the tile.
# 4. Use this program to combine tiles from the following directories into tiles/svg:
#    - From tiles/TDwithID: all tiles with an ID > 0 (not preprinted tiles).
#      These images have the id on the tile.
#    - From tiles/TDwoID: all tiles with an ID <= 0 (preprinted tiles).
#      These images do not have the ID on the tile (create these separately from TD).
#    - From tiles/handmade: all tiles in that dir will overwrite any of the above.
#      These are the tiles modified by hand or with Inkscape.

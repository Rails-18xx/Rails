# !/bin/bash
# author Stefan Frey
# requires installed tools: java, batik library, mmv and imagemagick (montage command)
# use whereis to retrieve path information

# creates temporary directory
mkdir svg_catalog_tmp

# convert svg to png
java -jar /usr/share/batik/batik-rasterizer.jar -d svg_catalog_tmp/ -w 400 -h 400 -dpi 400 -m image/png svg/ 

# rename to have all filenames in -xxxxx.png notation
/usr/bin/mmv 'svg_catalog_tmp/tile0.png' 'svg_catalog_tmp/m00000.png'
/usr/bin/mmv 'svg_catalog_tmp/tile-[0-9].png' 'svg_catalog_tmp/m0000#1.png'
/usr/bin/mmv 'svg_catalog_tmp/tile-[0-9][0-9].png' 'svg_catalog_tmp/m000#1#2.png'
/usr/bin/mmv 'svg_catalog_tmp/tile-[0-9][0-9][0-9].png' 'svg_catalog_tmp/m00#1#2#3.png'
/usr/bin/mmv 'svg_catalog_tmp/tile-[0-9][0-9][0-9][0-9].png' 'svg_catalog_tmp/m0#1#2#3#4.png'
/usr/bin/mmv 'svg_catalog_tmp/tile-[0-9][0-9][0-9][0-9][0-9].png' 'svg_catalog_tmp/m#1#2#3#4#5.png'

# preprinted catalog
/usr/bin/montage -tile 9x6 -label '%t' svg_catalog_tmp/m*.png svg_catalog_preprinted.pdf

# rename to have all filenames in xxxxx.png notation
/usr/bin/mmv 'svg_catalog_tmp/tile[0-9].png' 'svg_catalog_tmp/t000#1.png'
/usr/bin/mmv 'svg_catalog_tmp/tile[0-9][0-9].png' 'svg_catalog_tmp/t00#1#2.png'
/usr/bin/mmv 'svg_catalog_tmp/tile[0-9][0-9][0-9].png' 'svg_catalog_tmp/t0#1#2#3.png'
/usr/bin/mmv 'svg_catalog_tmp/tile[0-9][0-9][0-9][0-9].png' 'svg_catalog_tmp/t#1#2#3#4.png'

# tiles catalog
/usr/bin/montage -tile 9x6 -label '%t' svg_catalog_tmp/t*.png svg_catalog_tiles.pdf

# remove temporary directory
rm svg_catalog_tmp/*.png
rmdir svg_catalog_tmp
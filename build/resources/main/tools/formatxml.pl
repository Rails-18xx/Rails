# Formats XML files using tabs and newlines only.
#
# In its current form, this script is (or should be) platform- and machine-independent.
# Please leave it so!
#
$ARGV[0] or die "Usage: perl formatxml.pl <filepath>...\n";
for $filename (@ARGV) {
    open (IN,$filename) or die "Cannot open file '$filename' for reading: $!\n";
    $xml = join ('', <IN>);
    close IN;

    @elements = split /(?<=>)\s*(?=<)/, $xml;
    
    open (OUT, ">$filename") or die "Cannot open file '$filename' for writing: $!\n";
    binmode OUT;
    $indent = 0;
    for (@elements) {
        $indent-- if /^<\//;
        print OUT "\t" x $indent, $_, "\n";
        $indent++ unless /^<\?/ || /^<\// || /\/\s*>$/;
    }
    close OUT;
}

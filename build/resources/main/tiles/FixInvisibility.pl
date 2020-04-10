$dir = $ARGV[0] || "TileDictionary";
opendir (DIR, $dir) and print "Opened $dir\n" or die "Cannot open dir $dir: $!\n";
while (($file = readdir DIR)) {
	next unless $file =~/\.svg$/;
	open (IN, "$dir/$file") or die "Cannot open $file for reading: $!\n";
	open (OUT, ">$dir/$file.new") or die "Cannot open $file.new for writing: $!\n";
	while (<IN>) {
		s/ xmlns=""//g;
		print OUT $_;
	}
	close IN;
	close OUT;
	rename "$dir/$file.new", "$dir/$file"
		and print "$file done\n" or die "Cannot rename $file.new to $file: $!\n";
}
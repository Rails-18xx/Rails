#!/bin/bash

set -e

if [ -z $1 ]; then
	echo "Usage: $0 [version]"
	exit
fi

home_dir="/home/freystef/rails/release"
pushd $home_dir/git_repo/rails/

tar cjpf $home_dir/rails-$1.tar.bz2 rails-$1
zip -9 -r $home_dir/rails-$1.zip rails-$1

popd

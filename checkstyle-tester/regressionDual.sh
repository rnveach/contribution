#!/bin/bash

cd "${0%/*}"

source launch_diff_variables.sh

# parameters
# $1 = instance number
# $2 = base config file path
# $3 = patch config file path
# $4 = PR branch name
# $5 = master branch name (optional)

echo "Running instance '$1'"

echo "Cleaning..."
echo "./launch_diff.sh -clean"
./launch_diff.sh -clean

if [ $? -ne 0 ]; then
	echo "Clean Failed!"
	exit 1
fi

OUTPUT="/var/www/html/reports/$1"

if [ "$5" != "" ]; then
	EXTRA="-master $5"
	EXTRA_TEXT="and master branch '$5'"
fi

echo "Running full regression with PR branch '$4'$EXTRA_TEXT and base config $2 and patch config $3"
echo "./launch_diff.sh $4 -baseConfig $2 -patchConfig $3 $EXTRA"
./launch_diff.sh $4 -baseConfig $2 -patchConfig $3 $EXTRA

if [ $? -ne 0 ]; then
	echo "Regression Failed!"
	exit 1
fi

if [ ! -d "$OUTPUT" ]; then
	mkdir $OUTPUT
else
	rm -rf $OUTPUT/*
fi

mv $FINAL_RESULTS_DIR/* $OUTPUT

echo "Instance '$1' Complete for PR branch '$3'$EXTRA_TEXT and base config $2 and patch config $3"
exit 0

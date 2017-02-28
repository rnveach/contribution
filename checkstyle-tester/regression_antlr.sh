#!/bin/bash

cd "${0%/*}"

source launch_diff_variables.sh

# parameters
# $1 = instance number
# $2 = PR branch name
# $3 = master branch name (optional)

echo "Running instance '$1'"

echo "Cleaning..."
echo "./launch_diff_antlr.sh -clean"
./launch_diff_antlr.sh -clean

if [ $? -ne 0 ]; then
	echo "Clean Failed!"
	exit 1
fi

OUTPUT="/var/www/html/reports/$1"

if [ "$3" != "" ]; then
	EXTRA="-master $3"
	EXTRA_TEXT="and master branch '$3'"
fi

echo "Running full regression with PR branch '$2' and $EXTRA_TEXT"
echo "./launch_diff_antlr.sh $2 $EXTRA"
./launch_diff_antlr.sh $2 $EXTRA

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

echo "Instance '$1' Complete for PR branch '$2' and $CONFIG_TEXT"
exit 0

#!/bin/bash

cd "${0%/*}"

source launch_diff_variables.sh

# parameters
# $1 = instance number
# $2 = PR branch name
# $3 = profile name

echo "Running instance '$1'"

echo "Cleaning..."
echo "./launch_pitest.sh -clean"
./launch_pitest.sh -clean

if [ $? -ne 0 ]; then
	echo "Clean Failed!"
	exit 1
fi

OUTPUT="/var/www/html/pitest-reports/$1"

echo "Running pitest with PR branch '$2' and profile '$3'"
echo "./launch_pitest.sh $2 $3"
./launch_pitest.sh $2 $3

if [ $? -ne 0 ]; then
	echo "Pitest Failed!"
	exit 1
fi

if [ ! -d "$OUTPUT" ]; then
	mkdir $OUTPUT
else
	rm -rf $OUTPUT/*
fi

mv $FINAL_RESULTS_DIR/* $OUTPUT

echo "Instance '$1' Complete for PR branch '$2' and profile '$3'"
exit 0

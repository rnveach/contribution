#!/bin/bash

cd "${0%/*}"

source launch_diff_variables.sh

# parameters
# $1 = instance number
# $2 = config file path
# $3 = PR branch name
# $4 = PR only?
# $5 = indentation?
# $6 = master branch name (optional)

echo "Running instance '$1'"

echo "Cleaning..."
echo "./launch_diff.sh -clean"
./launch_diff.sh -clean

if [ $? -ne 0 ]; then
	echo "Clean Failed!"
	exit 1
fi

OUTPUT="/var/www/html/reports/$1"

if [ "$5" == "true" ]; then
	CONFIG=""
	CONFIG_TEXT="project specific config"
else
	CONFIG="-config $2"
	CONFIG_TEXT="config '$2'"
fi

if [ "$4" == "true" ]; then
	echo "Running PR only with PR branch '$3' and $CONFIG_TEXT"
	echo "./launch_diff.sh $3 $CONFIG -patchOnly"
	./launch_diff.sh $3 $CONFIG -patchOnly

	if [ $? -ne 0 ]; then
		echo "Regression Failed!"
		exit 1
	fi
else
	if [ "$6" != "" ]; then
		CONFIG="$CONFIG -master $6"
		CONFIG_TEXT="$CONFIG_TEXT and master branch '$6'"
	fi

	echo "Running full regression with PR branch '$3' and $CONFIG_TEXT"
	echo "./launch_diff.sh $3 $CONFIG"
	./launch_diff.sh $3 $CONFIG

	if [ $? -ne 0 ]; then
		echo "Regression Failed!"
		exit 1
	fi
fi

if [ ! -d "$OUTPUT" ]; then
	mkdir $OUTPUT
else
	rm -rf $OUTPUT/*
fi

mv $FINAL_RESULTS_DIR/* $OUTPUT

echo "Instance '$1' Complete for PR branch '$3' and $CONFIG_TEXT"
exit 0

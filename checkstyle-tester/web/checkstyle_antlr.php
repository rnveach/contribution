<?php
// Configuration

$basedir = "/var/www/html";
$saveDir = $basedir . "/reports/";
$instanceFile = $basedir . "/checkstyleInstance.txt";
$runScript = "/home/ricky/opensource/contribution/checkstyle-tester/regression_antlr.sh";

?>

<html>
<head>
	<title>CheckStyle Web Regression - ANTLR</title>
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
	<script src="jquery-linedtextarea.js"></script>
	<link href="jquery-linedtextarea.css" type="text/css" rel="stylesheet" />
</head>
<center><h1>CheckStyle Web Regression - ANTLR</h1></center>

<?php

ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', dirname(__FILE__) . '/error_log.txt');
error_reporting(E_ALL);

date_default_timezone_set('US/Eastern');

$action = getParameter("action");

if (!isset($action)) {
	echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "?action=past'>View Past Reports</a><br /><br />";

	showForm();
} else if ($action == "run") {
	$branchName = getParameter("branchName");
	$masterBranchName = getParameter("masterBranchName");

	if (!isset($branchName)) {
		echo "Some fields are missing for a save.";
	} else if (preg_match("/[^a-zA-Z0-9#_\-]/i", $branchName)) {
		die("Improper branch '" . $branchName . "' was supplied.");
	} else if (preg_match("/[^a-zA-Z0-9#_\-]/i", $masterBranchName)) {
		die("Improper branch '" . $masterBranchName . "' was supplied.");
	// Size Limitations
	} else if (strlen($branchName) > 40) {
		echo "Size Security: PR Branch name is larger than 40 bytes";
	} else if (strlen($masterBranchName) > 40) {
		echo "Size Security: Mater Branch name is larger than 40 bytes";
	//
	} else {
		$instanceNumber = intval(@file_get_contents($instanceFile));
		$reportSave = $saveDir . "report" . $instanceNumber . ".txt";

		shell_exec("nohup " . $runScript . " " . $instanceNumber . " " . $branchName . " " . $masterBranchName . " > " . $reportSave . " 2>&1&");

		@file_put_contents($instanceFile, $instanceNumber + 1);

		echo "You are <a href='" . $_SERVER['SCRIPT_NAME'] . "?action=view&instance=" . $instanceNumber . "'>instance " . $instanceNumber . "</a>.<br />";
		echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "'>Main</a><br />";
	}
} else if ($action == "view") {
	$instanceNumber = getParameter("instance");
	$tail = getParameter("tail");

	if (!isset($tail)) {
		$tail = 10;
	}

	if (preg_match("/[^0-9]/i", $instanceNumber)) {
		die("Improper instance '" . $instanceNumber . "' was supplied.");
	} else if (preg_match("/[^0-9]/i", $tail)) {
		die("Improper tail '" . $tail . "' was supplied.");
	}

	$reportSave = $saveDir . "report" . $instanceNumber . ".txt";

	if (!file_exists($reportSave)) {
		die("Can't find report file '" . $reportSave . "'");
	}

	if ($tail == 0) {
		$reportContents = file_get_contents($reportSave);
	} else {
		$reportContents = shell_exec("tail " . $reportSave . " -n " . $tail);
	}

	echo "Log (" . human_filesize(filesize($reportSave)) . "):<br />";
	echo "<div style='border: 1px solid black; overflow-wrap: break-word;'>";

	// pretty display
	echo str_replace("\n", "<br />", str_replace("  ", "&nbsp; ", str_replace("\t", "&nbsp;&nbsp;&nbsp;&nbsp;", _sanitizeText($reportContents))));

	echo "</div><br /><br />";

	echo "Your report's final results will be <a href='reports/" . $instanceNumber . "'>here</a> when it completes successfully.<br />";
	echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "'>Main</a><br />";
} else if ($action == "past") {
	echo "<h3>Past Reports:</h3>";

	$dhandle = opendir($saveDir);

	if ($dhandle != FALSE) {
		$list = array();

		while (($name = readdir($dhandle)) !== false) {
			if (($name == ".") || ($name == "..")) continue;
			if (@filetype($saveDir. $name) == "dir") {
				$list[] = $name;
			} else if ((substr($name, 0, 6) == "report") && (substr($name, -4) == ".txt")) {
				$list[] = substr($name, 6, strlen($name) - 10);
			}
		}

		natcasesort($list);
		$list = array_unique($list, SORT_REGULAR);

		foreach ($list as $name) {
			echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "?action=view&instance=" . $name . "'>Log " . $name . "</a>, ";
			echo "<a href='reports/" . $name . "'>Report " . $name . "</a><br />";
		}

		closedir($dhandle);
	}

	echo "<br /><br />";
	echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "'>Main</a><br />";
}

function showForm() {
	echo "<form action=\"" . $_SERVER['SCRIPT_NAME'] . "\" method=\"POST\">";
	echo "<input type='hidden' name='action' value='run'>";

	echo "<input type=\"submit\" value=\"Submit\"><br /><br />";
	echo "Override Master Branch Name: <input type=\"text\" name=\"masterBranchName\"><br /><br />";
	echo "PR Branch Name: <input type=\"text\" name=\"branchName\"><br /><br />";
	echo "<input type=\"submit\" value=\"Submit\">";
	echo "</form>";
	echo "<script>$(function() { $(\"#cs_config\").linedtextarea(); });</script>";
}

function getParameter($param) {
	return (isset($_POST[$param]) ? $_POST[$param] : (isset($_GET[$param]) ? $_GET[$param] : null));
}

function fix_post_text($in, $r_sq = "'", $r_dq = "\\\"", $r_ds = "\\\\") {
	$in = str_replace(
		array("\r",	$r_sq),
		array("",	"'"),
		$in);

	if (!ini_get('safe_mode')) {
		$in = str_replace(
			array($r_dq,	$r_ds),
			array("\"",	"\\"),
			$in);
	}

	return $in;
}

function _sanitizeText($in) {
	$in = str_replace(
		array(chr(38) . "",	"<",		">",		"'"),
		array(chr(38) . "amp;",	chr(38) . "lt;",chr(38) . "gt;","'"),
		$in);

	if (ini_get('safe_mode')) {
		$in = str_replace(
			array("\\\"",	"\\\\"),
			array("\"",	"\\"),
			$in);
	}

	return $in;
}

function human_filesize($bytes, $decimals = 1) {
	$sz = 'BKMGTP';
	$factor = floor((strlen($bytes) - 1) / 3);
	return sprintf("%.{$decimals}f", $bytes / pow(1024, $factor)) . @$sz[$factor];
}

?>
</body></html>


<?php
// Configuration

$basedir = "/var/www/html";
$saveDir = $basedir . "/reports/";
$instanceFile = $basedir . "/checkstyleInstance.txt";
$runScript = "/home/ricky/opensource/contribution/checkstyle-tester/regression.sh";

?>

<html>
<head>
	<title>CheckStyle Web Regression</title>
	<script src="http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js"></script>
	<script src="jquery-linedtextarea.js"></script>
	<link href="jquery-linedtextarea.css" type="text/css" rel="stylesheet" />
</head>
<center><h1>CheckStyle Web Regression</h1></center>

<?php

ini_set('display_errors', 1);
ini_set('log_errors', 1);
ini_set('error_log', dirname(__FILE__) . '/error_log.txt');
error_reporting(E_ALL);

date_default_timezone_set('US/Eastern');

$action = getParameter("action");
$config = getParameter("config");

if (!isset($action)) {
	echo "<a href='" . $_SERVER['SCRIPT_NAME'] . "?action=past'>View Past Reports</a><br /><br />";

	if (!isset($config)) {
		$config = "<?xml version=\"1.0\"?>\n<!DOCTYPE module PUBLIC\n          \"-//Puppy Crawl//DTD Check Configuration 1.3//EN\"\n          \"http://www.puppycrawl.com/dtds/configuration_1_3.dtd\">\n\n<module name=\"Checker\">\n    <property name=\"charset\" value=\"UTF-8\"/>\n    <property name=\"severity\" value=\"warning\"/>\n    <property name=\"haltOnException\" value=\"false\"/>\n\n    <module name=\"TreeWalker\">\n    </module>\n</module>";
	}

	showForm($config);
} else if ($action == "run") {
	$branchName = getParameter("branchName");
	$masterBranchName = getParameter("masterBranchName");
	$prOnly = getParameter("prOnly");
	$indentation = getParameter("indentation");

	if (!isset($prOnly)) {
		$prOnly = "false";
	}
	if (!isset($indentation)) {
		$indentation = "false";
	}

	if (!isset($config) || !isset($branchName)) {
		echo "Some fields are missing for a save.";
	} else if (preg_match("/[^a-zA-Z0-9#_\-]/i", $branchName)) {
		die("Improper branch '" . $branchName . "' was supplied.");
	} else if (preg_match("/[^a-zA-Z0-9#_\-]/i", $masterBranchName)) {
		die("Improper branch '" . $masterBranchName . "' was supplied.");
	} else if (preg_match("/[^a-zA-Z0-9#_\-]/i", $prOnly)) {
		die("Improper pr attribute '" . $prOnly . "' was supplied.");
	// Size Limitations
	} else if (strlen($config) > 20480) { // 20kb
		echo "Size Security: Configuration is larger than 20kb";
	} else if (strlen($branchName) > 40) {
		echo "Size Security: PR Branch name is larger than 40 bytes";
	} else if (strlen($masterBranchName) > 40) {
		echo "Size Security: Mater Branch name is larger than 40 bytes";
	// Vulnerabilities
	// http://stackoverflow.com/questions/1906927/xml-vulnerabilities/1907500#1907500
	} else if (stripos($config, "<!ENTITY") !== false) {
		echo "XML Security: '<!ENTITY' is not allowed";
	} else if (stripos($config, "<!ELEMENT") !== false) {
		echo "XML Security: '<!ELEMENT' is not allowed";
	//
	} else {
		$config = fix_post_text($config);
		$configMD = hash("md5", $config);
		$configFile = $saveDir . $configMD;

		if (!file_exists($configFile)) {
			$fhandle = @fopen($configFile, "w");
			if ($fhandle != FALSE) {
				if (fwrite($fhandle, $config) == FALSE) {
					die("Failed to save configuration to " . $configMD);
				} else {
					echo "Configuration file saved.<br />";
				}
			} else {
				die("Failed to save configuration to " . $configMD);
			}
		}

		$instanceNumber = intval(@file_get_contents($instanceFile));
		$reportSave = $saveDir . "report" . $instanceNumber . ".txt";

		shell_exec("nohup " . $runScript . " " . $instanceNumber . " " . $configFile . " " . $branchName . " " . $prOnly . " " . $indentation . " " . $masterBranchName . " > " . $reportSave . " 2>&1&");

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

function showForm($config) {
	echo "<form action=\"" . $_SERVER['SCRIPT_NAME'] . "\" method=\"POST\">";
	echo "<input type='hidden' name='action' value='run'>";

	echo "<input type=\"submit\" value=\"Submit\"><br /><br />";
	echo "Override Master Branch Name: <input type=\"text\" name=\"masterBranchName\"><br /><br />";
	echo "PR Branch Name: <input type=\"text\" name=\"branchName\"><br /><br />";
	echo "Configuration:<br />";
	echo "<textarea name='config' rows='12' wrap='off' style='width: 100%' id='cs_config' onchange='report_on_change();' onkeyup='report_on_change();'>";
	echo _sanitizeText($config);
	echo "</textarea><br /><br />";
	echo "<input type=\"checkbox\" name=\"prOnly\" value=\"true\"> Run PR Only<br />";
	echo "<input type=\"checkbox\" name=\"indentation\" value=\"true\"> Indentation Run<br /><br /><br />";
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


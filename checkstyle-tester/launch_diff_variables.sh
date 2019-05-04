#!/bin/bash

# ============================================================
# Custom Options
# Note: Use full paths
# ============================================================

CONTACTSERVER=true

PULL_REMOTE=pull

CHECKSTYLE_DIR=/home/ricky/opensource/checkstyle
SEVNTU_DIR=/home/ricky/opensource/sevntu.checkstyle
CONTRIBUTION_DIR=/home/ricky/opensource/contribution
TEMP_DIR=/home/ricky/regression_temp_files

TESTER_DIR=$CONTRIBUTION_DIR/checkstyle-tester
DIFF_JAR=$CONTRIBUTION_DIR/patch-diff-report-tool/target/patch-diff-report-tool-0.1-SNAPSHOT-jar-with-dependencies.jar

REPOSITORIES_DIR=/home/ricky/regression_repositories
FINAL_RESULTS_DIR=/home/ricky/regression_reports/diff

SITE_SAVE_MASTER_DIR=/home/ricky/regression_reports/savemaster
SITE_SAVE_PULL_DIR=/home/ricky/regression_reports/savepull

# to be removed after confirmation
MINIMIZE=true
# to be removed after antlr update
SITE_SOURCES_DIR=$TESTER_DIR/src/main/java
SITE_SAVE_REF_DIR=/home/ricky/regression_reports/saverefs

# ============================================================
# ============================================================
# ============================================================

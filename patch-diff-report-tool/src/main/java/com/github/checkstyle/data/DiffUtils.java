////////////////////////////////////////////////////////////////////////////////
// checkstyle: Checks Java source code for adherence to a set of rules.
// Copyright (C) 2001-2021 the original author or authors.
//
// This library is free software; you can redistribute it and/or
// modify it under the terms of the GNU Lesser General Public
// License as published by the Free Software Foundation; either
// version 2.1 of the License, or (at your option) any later version.
//
// This library is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
// Lesser General Public License for more details.
//
// You should have received a copy of the GNU Lesser General Public
// License along with this library; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
////////////////////////////////////////////////////////////////////////////////

package com.github.checkstyle.data;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * Utility class to calculate difference between 2 sorted lists.
 */
public final class DiffUtils {

    /** Number of records to process at a time when looking for differences. */
    private static final int SPLIT_SIZE = 100;

    /** Private ctor. */
    private DiffUtils() {
    }

    /**
     * Creates difference between 2 lists of records.
     *
     * @param list1
     *        the first list.
     * @param list2
     *        the second list.
     * @return the difference list.
     */
    public static List<CheckstyleRecord> produceDiff(
            List<CheckstyleRecord> list1, List<CheckstyleRecord> list2) {
        final List<CheckstyleRecord> diff;
        try {
            diff = produceDiffEx(list1, list2);
            diff.addAll(produceDiffEx(list2, list1));
        }
        catch (InterruptedException | ExecutionException ex) {
            throw new IllegalStateException("Multi-threading failure reported", ex);
        }

        return diff;
    }

    private static List<CheckstyleRecord> produceDiffEx(
            List<CheckstyleRecord> list1, List<CheckstyleRecord> list2)
            throws InterruptedException, ExecutionException {
        final List<CheckstyleRecord> diff = new ArrayList<>();
        if (list1.size() < SPLIT_SIZE) {
            for (CheckstyleRecord rec1 : list1) {
                if (!isInList(list2, rec1)) {
                    diff.add(rec1);
                }
            }
        }
        else {
            final ExecutorService executor =
                Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
            final List<Future<List<CheckstyleRecord>>> futures = new ArrayList<>();
            final int size = list1.size();
            for (int i = 0; i < size; i += SPLIT_SIZE) {
                futures.add(executor.submit(new MultiThreadedDiff(list1, list2, i, Math.min(size, i
                        + SPLIT_SIZE))));
            }

            for (Future<List<CheckstyleRecord>> future : futures) {
                diff.addAll(future.get());
            }

            executor.shutdown();
        }
        return diff;
    }

    /**
     * Compares the record against list of records.
     *
     * @param list
     *        of records.
     * @param testedRecord
     *        the record.
     * @return true, if has its copy in a list.
     */
    private static boolean isInList(List<CheckstyleRecord> list,
            CheckstyleRecord testedRecord) {
        boolean belongsToList = false;
        for (CheckstyleRecord checkstyleRecord : list) {
            if (testedRecord.compareTo(checkstyleRecord) == 0) {
                belongsToList = true;
                break;
            }
        }
        return belongsToList;
    }

    /** Separate class to multi-thread 2 lists checking if items from 1 is in the other. */
    private static final class MultiThreadedDiff implements Callable<List<CheckstyleRecord>> {
        /** First list to examine. */
        private List<CheckstyleRecord> list1;
        /** Second list to examine. */
        private List<CheckstyleRecord> list2;
        /** Inclusive start position of the first list. */
        private int list1Start;
        /** Non-inclusive End position of the first list. */
        private int list1End;

        /**
         * Default constructor.
         *
         * @param list1 First list to examine.
         * @param list2 Second list to examine.
         * @param list1Start Inclusive start position of the first list.
         * @param list1End Non-inclusive End position of the first list.
         */
        private MultiThreadedDiff(List<CheckstyleRecord> list1, List<CheckstyleRecord> list2,
                int list1Start, int list1End) {
            this.list1 = list1;
            this.list2 = list2;
            this.list1Start = list1Start;
            this.list1End = list1End;
        }

        @Override
        public List<CheckstyleRecord> call() throws Exception {
            final List<CheckstyleRecord> diff = new ArrayList<>();

            for (int i = list1Start; i < list1End; i++) {
                final CheckstyleRecord rec1 = list1.get(i);

                if (!isInList(list2, rec1)) {
                    diff.add(rec1);
                }
            }
            return diff;
        }
    }

}

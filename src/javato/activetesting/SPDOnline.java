package javato.activetesting;

import javato.activetesting.lockset.LockSetTracker;
import javato.activetesting.reentrant.IgnoreRentrantLock;
import javato.activetesting.analysis.AnalysisImpl;
import javato.activetesting.analysis.ObserverForActiveTesting;
import javato.activetesting.common.Parameters;
import javato.activetesting.common.MutableLong;
import javato.activetesting.activechecker.ActiveChecker;
import javato.activetesting.threadrepro.EqualObjectBreakpoint;
import javato.activetesting.deterministicscheduler.StallBreaker;
import javato.activetesting.syncpd.util.VectorClock;

import java.util.Set;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.io.PrintWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.FileNotFoundException;
import java.util.Scanner;
import java.util.Random;

import javato.activetesting.syncpd.SyncPDState;;

/**
 * Copyright (c) 2006-2009,
 * Koushik Sen <ksen@cs.berkeley.edu>
 * All rights reserved.
 * <p/>
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 * <p/>
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * <p/>
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * <p/>
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 * <p/>
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS
 * IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL,
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
public class SPDOnline extends AnalysisImpl {

    private SyncPDState state;
    private LockSetTracker lsTracker;
    private IgnoreRentrantLock ignoreRentrantLock;
    private int numAcqEvents;
    private Set<Integer> pauseThreads;
    private HashMap<Long, Integer> pauseCountPerLoc;
    private HashMap<Integer, Integer> pauseCountPerThread;
    private int execNumber;
    private int sleepDuration;
    private Object writeLock;
    private final int MAX_PAUSE = 5;

    public void initialize() {
        synchronized (ActiveChecker.lock) {
            state = new SyncPDState();
            lsTracker = new LockSetTracker();
            ignoreRentrantLock = new IgnoreRentrantLock();
            pauseThreads = new HashSet<Integer>();
            pauseCountPerLoc = new HashMap<Long, Integer>();
            pauseCountPerThread = new HashMap<Integer, Integer>();
            writeLock = new Object();
            numAcqEvents = 0;
            try {
                File f = new File(Parameters.execNumberFile);
                Scanner myReader = new Scanner(f);
                execNumber = myReader.nextInt();
                System.out.println("execNumberFile: " + Parameters.execNumberFile);
            } catch (FileNotFoundException e) {
                System.out.println("exec number file not found!");
                execNumber = 1;
            }
            sleepDuration = (int) Math.floor(execNumber * 0.2);
            System.out.println("execNumber: " + execNumber + ", sleepDuration: " + sleepDuration);
        }
    }

    public void lockBefore(Integer iid, Integer thread, Integer lock, Object actualLock) {
        synchronized (ActiveChecker.lock) {
            state.addThread(thread);

            int vertexIndex = state.keepCycleBooks(thread, lock, iid);
            if (vertexIndex >= 0) {
                boolean foundDeadlock = state.findDeadlocks(vertexIndex, thread, lock, iid);
            }

            state.incClockThread(thread);
            state.addToLocksHeld(thread, lock);
            state.addAcquireToHist(thread, lock, numAcqEvents);

            pauseThreads.add(thread);
            this.numAcqEvents++;

            if (state.getLockHeldCount(thread, lock) == 1) {
                boolean isDeadlock = lsTracker.lockBefore(iid, thread, lock);
                if (isDeadlock) {
                    System.out.println("deadlock found in the current execution!");
                    System.exit(1);
                }
            }
        }
    }

    public void unlockAfter(Integer iid, Integer thread, Integer lock) {
        synchronized (ActiveChecker.lock) {
            if (state.getLockHeldCount(thread, lock) == 1)
                lsTracker.unlockAfter(thread);

            state.incClockThread(thread);
            state.updateRelease(thread, lock);
            state.removeLockFromLocksHeld(thread, lock);
        }
    }

    public void startBefore(Integer iid, Integer parent, Integer child) {
        synchronized (ActiveChecker.lock) {
            state.addThread(parent);
            state.incClockThread(parent);
            state.addThread(parent, child);
        }
    }

    public void joinAfter(Integer iid, Integer parent, Integer child) {
        synchronized (ActiveChecker.lock) {
            state.incClockThread(parent);

            VectorClock C_t = state.getThreadVC(parent);
            VectorClock C_tc = state.getThreadVC(child);
            C_t.updateMax(C_tc);
        }
    }

    public void readBefore(Integer iid, Integer thread, Long memory, boolean isVolatile) {
        synchronized (ActiveChecker.lock) {
            state.addThread(thread);
            state.incClockThread(thread);
            VectorClock C_t = state.getThreadVC(thread);

            if (state.variableToLastWriteThread.containsKey(memory)) {
                int lastWriteThread = state.variableToLastWriteThread.get(memory);
                VectorClock LW_v = state.lastWriteVariable.get(memory);

                int lastWriteThreadId = state.getThreadId(lastWriteThread);
                if (C_t.getClockIndex(lastWriteThreadId) < LW_v.getClockIndex(lastWriteThreadId)) 
                    C_t.updateMax(LW_v);
            }
        }
    }

    public void writeBefore(Integer iid, Integer thread, Long memory, boolean isVolatile) {
        boolean pause = false;
        Random rd = new Random();

        if (rd.nextBoolean()) {
            synchronized (writeLock) {
                if (pauseThreads.contains(thread)) {
                    pauseThreads.remove(thread);
                    if (pauseCountPerLoc.containsKey(memory)) {
                        pauseCountPerLoc.put(memory, pauseCountPerLoc.get(memory) + 1);
                    } else {
                        pauseCountPerLoc.put(memory, 1);
                    }

                    if (pauseCountPerThread.containsKey(thread)) {
                        pauseCountPerThread.put(thread, pauseCountPerThread.get(thread) + 1);
                    } else {
                        pauseCountPerThread.put(thread, 1);
                    }

                    if (pauseCountPerLoc.get(memory) < MAX_PAUSE &&
                            pauseCountPerThread.get(thread) < MAX_PAUSE &&
                            sleepDuration > 0) {
                        pause = true;
                    }
                }
            }
        }

        if (pause) {
            try {
                Thread.sleep(sleepDuration);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        synchronized (ActiveChecker.lock) {
            state.addThread(thread);

            state.incClockThread(thread);
            VectorClock C_t = state.getThreadVC(thread);

            state.lastWriteVariable.put(memory, new VectorClock(C_t));
            state.variableToLastWriteThread.put(memory, thread);
        }
    }

    public void finish() {
        System.out.println("\nTotal number of unique deadlocks: " + state.uniqueDeadlockCount);
        writeStat(Parameters.execNumberFile);
    }

    public void writeStat(String file) {
        try {
            PrintWriter pw = new PrintWriter(new FileWriter(file, false));
            pw.println(execNumber + 1);
            pw.close();
            System.out.println("wrote execNumber: " + (execNumber + 1));
        } catch (IOException e) {
            System.err.println("Error while writing to " + file);
            System.exit(1);
        }
    }
}

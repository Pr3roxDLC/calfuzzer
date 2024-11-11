package javato.activetesting.syncpd;

import java.util.Map;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import com.google.common.collect.HashBasedTable;
import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Table;

import javato.activetesting.syncpd.util.Pair;
import javato.activetesting.syncpd.util.Quaternary;
import javato.activetesting.syncpd.util.Triplet;

import javato.activetesting.syncpd.util.VectorClock;
import javato.activetesting.analysis.ObserverForActiveTesting;


public class SyncPDState {

	public int uniqueDeadlockCount, numThreads, numLocks, numVariables;

	public Set<Integer> threadSet, lockSet;
	private HashMap<Integer, Integer> threadMap;
	
	private Set<Set<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>> declaredDeadlocks;
	private Map<Integer, VectorClock> clockThread; // threadIndex -> VC
	public Map<Long, VectorClock> lastWriteVariable; // variableIndex -> VC
	public Map<Long, Integer> variableToLastWriteThread; // variableIndex -> threadIndex

	private Table<Integer, Integer, LinkedList<Triplet<Integer, VectorClock, VectorClock>>> csHist;
	private Table<Integer, Integer, Integer> reentrantLockCounter;
	private Map<Integer, Multiset<Integer>> locksHeld;
	
	private Map<Set<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>, VectorClock> vertexSetToSPIdeal; 
	private Map<Quaternary<Integer, Integer, HashSet<Integer>, Integer>, Integer> vertexToIndex;
	private Map<Integer, List<Pair<VectorClock, VectorClock>>> vertexToVCs;
	private Map<Integer, Set<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>> verticesHoldingLock; 
	private Table<Integer, Integer, Integer> vertexToEventQueueIndex; 

	public SyncPDState() {
		uniqueDeadlockCount = numThreads = numLocks = numVariables = 0;
		initDS();
	}

	public void initDS() {
		this.clockThread = new HashMap<Integer, VectorClock>();

		this.threadSet = new HashSet<Integer>();
		this.threadMap = new HashMap<Integer, Integer>();
		this.lockSet = new HashSet<Integer>();

		// initialize lastWriteVariable
		this.lastWriteVariable = new HashMap<Long, VectorClock>();

		// initialize csHist
		this.csHist = HashBasedTable.create();
		
		// initialize locksHeld and numAcquriesPerThread
		this.locksHeld = new HashMap<Integer, Multiset<Integer>>();
		
		// initialize lockHistory
		this.reentrantLockCounter = HashBasedTable.create();
		this.declaredDeadlocks = new HashSet<Set<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>>();
		this.variableToLastWriteThread = new HashMap<Long, Integer>();
		
		this.vertexToIndex = new HashMap();
		this.vertexToVCs = new HashMap();
		this.vertexToEventQueueIndex = HashBasedTable.create();
		this.verticesHoldingLock = new HashMap();
		this.vertexSetToSPIdeal = new HashMap();
	}
	
	public void addAcquireToHist(Integer t, Integer l, int acquireId){
		if (!lockSet.contains(l))
			lockSet.add(l);

		if (!csHist.contains(t, l)) {
			this.csHist.put(t, l, new LinkedList<Triplet<Integer, VectorClock, VectorClock>>());
		}

		if (this.csHist.get(t, l).size() > 0 && this.csHist.get(t, l).getLast().third == null) {
			if (this.reentrantLockCounter.contains(t, l)) {
				this.reentrantLockCounter.put(t, l, this.reentrantLockCounter.get(t, l) + 1);
			} else {
				this.reentrantLockCounter.put(t, l, 1);
			}
		} else {
			VectorClock copyClock = new VectorClock(this.clockThread.get(t));
			this.csHist.get(t, l).add(new Triplet<Integer, VectorClock, VectorClock>(acquireId, copyClock, null));
		}		
	}

	public void handleDeadlock(boolean deadlock, Quaternary<Integer, Integer, HashSet<Integer>, Integer> vertex1, Quaternary<Integer, Integer, HashSet<Integer>, Integer> vertex2) {
		if (deadlock) {
			HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>> vertexSet = new HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>();
			vertexSet.add(vertex1);
			vertexSet.add(vertex2);
			if (!declaredDeadlocks.contains(vertexSet)) {
				System.out.println("Deadlock found on cycle: " + vertexSet);
				System.out.println("locations: " + ObserverForActiveTesting.getIidToLine(vertex1.fourth) + ", " + ObserverForActiveTesting.getIidToLine(vertex2.fourth));
				
				this.declaredDeadlocks.add(vertexSet);
				this.uniqueDeadlockCount++;
			}
		}
	}

	public int keepCycleBooks(Integer t, Integer l, Integer locationId) {
		if (this.locksHeld.containsKey(t) && this.locksHeld.get(t).size() > 0) {
			HashSet<Integer> lockSet = new HashSet<Integer>(this.locksHeld.get(t));
			Quaternary<Integer, Integer, HashSet<Integer>, Integer> vertex = new Quaternary(t, l, lockSet, locationId);
			
			int vertexIndex;
			if (!this.vertexToIndex.containsKey(vertex)) {
				vertexIndex = this.vertexToIndex.size();
				this.vertexToIndex.put(vertex, vertexIndex);
				this.vertexToVCs.put(vertexIndex, new LinkedList<Pair<VectorClock, VectorClock>>());
			} else {
				vertexIndex = this.vertexToIndex.get(vertex);
			}
			
			VectorClock C_prev = new VectorClock(this.clockThread.get(t));
			VectorClock C = new VectorClock(this.clockThread.get(t));
			int threadId = getThreadId(t);
			C.inc(threadId);
			
			this.vertexToVCs.get(vertexIndex).add(new Pair<VectorClock, VectorClock>(C_prev, C));
			
			return vertexIndex;
		} else {
			return -1;
		}
	}

	public boolean findDeadlocks(int vertexIndex, int t, int l, int locationId) {
		HashSet<Integer> lockSet = new HashSet<Integer>(this.locksHeld.get(t));
		Quaternary<Integer, Integer, HashSet<Integer>, Integer> currentVertex = new Quaternary(t, l, lockSet, locationId);
		
		List<Quaternary> deadlockPatterns = getDeadlockPatterns(currentVertex, t, l, locationId);
		boolean foundDeadlock = false;
		for (Quaternary<Integer, Integer, HashSet<Integer>, Integer> vertexPrime : deadlockPatterns) {
			int vertexPrimeIndex = this.vertexToIndex.get(vertexPrime);
			int threadQueueIndex = 0;
			if (this.vertexToEventQueueIndex.contains(vertexPrimeIndex, vertexIndex)) 
				threadQueueIndex = this.vertexToEventQueueIndex.get(vertexPrimeIndex, vertexIndex);


			Pair<VectorClock, VectorClock> vcPair = this.vertexToVCs.get(vertexIndex).get(this.vertexToVCs.get(vertexIndex).size()-1);
			for (Pair<VectorClock, VectorClock> vcPrimePair : this.vertexToVCs.get(vertexPrimeIndex).subList(threadQueueIndex, this.vertexToVCs.get(vertexPrimeIndex).size())) {
				HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>> vertexSet = new HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>();
				vertexSet.add(currentVertex);
				vertexSet.add(vertexPrime);
				if (!declaredDeadlocks.contains(vertexSet) && (!vertexSetToSPIdeal.containsKey(vertexSet) || vertexSetToSPIdeal.get(vertexSet).isLessThanOrEqual(vcPrimePair.second))) {
					Pair<Boolean, VectorClock> deadlock = checkForDeadlocks(vcPrimePair, vcPair);
					this.handleDeadlock(deadlock.first, currentVertex, vertexPrime);
					if (deadlock.first) {
						foundDeadlock = true;
						break;
					} else 
						vertexSetToSPIdeal.put(vertexSet, deadlock.second);
				}
			}
		}

		return foundDeadlock;
	}


	private List<Quaternary> getDeadlockPatterns(Quaternary<Integer, Integer, HashSet<Integer>, Integer> currentVertex, int t, int l, int locationId) {
		for (Integer heldLock : this.locksHeld.get(t) ) {
			if (verticesHoldingLock.containsKey(heldLock)) {
				verticesHoldingLock.get(heldLock).add(currentVertex);
			} else {
				Set<Quaternary<Integer, Integer, HashSet<Integer>, Integer>> vertexSet = new HashSet();
				vertexSet.add(currentVertex);
				verticesHoldingLock.put(heldLock, vertexSet);
			}
		}
		
		List<Quaternary> verticesInDeadlockPattern = new LinkedList();
		if (verticesHoldingLock.containsKey(l)) {
			for (Quaternary<Integer, Integer, HashSet<Integer>, Integer> vertexPrime : this.verticesHoldingLock.get(l)) {
				if (!vertexPrime.first.equals(t)) {
					if (!vertexPrime.second.equals(l)) {
						HashSet<Integer> intersection = new HashSet<Integer>(vertexPrime.third);
						intersection.retainAll(currentVertex.third);
						
						if (intersection.size() == 0) {							
							if (vertexPrime.third.contains(l) && currentVertex.third.contains(vertexPrime.second)) { 
								HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>> vertexSet = new HashSet<Quaternary<Integer, Integer, HashSet<Integer>, Integer>>();
								vertexSet.add(currentVertex);
								vertexSet.add(vertexPrime);
								if (!declaredDeadlocks.contains(vertexSet))
									verticesInDeadlockPattern.add(vertexPrime);
							}
						}
					}
				}
			}
		}
		
		return verticesInDeadlockPattern;
	}

	long countCheckForDeadlocks = 0L;
	private Pair<Boolean, VectorClock> checkForDeadlocks(Pair<VectorClock, VectorClock> beforeEvent, Pair<VectorClock, VectorClock> currentEvent) {
		VectorClock ideal = new VectorClock(this.threadSet.size());
		
		ideal.updateMax(beforeEvent.first);
		ideal.updateMax(currentEvent.first);
		VectorClock SPIdeal = this.computeSPIdeal(ideal);

		countCheckForDeadlocks++;
		if (beforeEvent.second.isLessThanOrEqual(SPIdeal)) 
			return new Pair(false, SPIdeal);
		else 
			return new Pair(true, SPIdeal);
	}

	private Triplet<Integer, VectorClock, VectorClock> maxLowerBound(VectorClock U, LinkedList<Triplet<Integer, VectorClock, VectorClock>> Lst) {
		Triplet<Integer, VectorClock, VectorClock> maxTriplet = new Triplet<Integer, VectorClock, VectorClock>(0, null, null); 
		
		int frontComputation = Lst.size() < 10 ? Lst.size() : 10;
		for (int i = 0; i < frontComputation; i++) {
			Triplet<Integer, VectorClock, VectorClock> triplet = Lst.get(i);
			if (triplet.second.isLessThanOrEqual(U)) {
				maxTriplet = new Triplet<Integer, VectorClock, VectorClock>(triplet.first, triplet.second, triplet.third);
			} else {
				return maxTriplet;
			}
		}

		for (int i = Lst.size()-1; i >= frontComputation; i--) {
			Triplet<Integer, VectorClock, VectorClock> triplet = Lst.get(i);
			if (triplet.second.isLessThanOrEqual(U)) {
				maxTriplet = new Triplet<Integer, VectorClock, VectorClock>(triplet.first, triplet.second, triplet.third);
				break;
			}
		}
		return maxTriplet;
	}

	public VectorClock computeSPIdeal(VectorClock I) {		
		VectorClock IOrig = new VectorClock(this.threadSet.size());
		do {
			for (Integer l : this.lockSet) {
				Integer maxIndex = -1;
				Integer maxThread = null;
				
				Table<Integer, Integer, VectorClock> releaseClock = HashBasedTable.create(); 
	
				for (Integer t : this.threadSet) {
					if (csHist.get(t, l) != null) {
						Triplet<Integer, VectorClock, VectorClock> triplet = maxLowerBound(I, csHist.get(t, l));
						if (triplet.first > maxIndex) {
							maxIndex = triplet.first;
							maxThread = t;
						}						
						if (triplet.third != null) {
							releaseClock.put(t, l, triplet.third);
						}						
					}
				}
				IOrig = new VectorClock(I);
				for (Integer t : this.threadSet) {
					if (!t.equals(maxThread)) {
						if (releaseClock.get(t, l) != null) {
							I.updateMax(releaseClock.get(t, l));
						}
					}
				}
			}
		} while(IOrig.equals(I));
		return I;
	}

	public void updateRelease(Integer t, Integer l) {
		if (this.reentrantLockCounter.contains(t, l) && this.reentrantLockCounter.get(t, l) > 0) {
			this.reentrantLockCounter.put(t, l, this.reentrantLockCounter.get(t, l) - 1);
		} else {
			VectorClock copyClock = new VectorClock(this.clockThread.get(t));
			this.csHist.get(t, l).getLast().third = copyClock;
		}
	}

	public void addToLocksHeld(Integer t, Integer l) {
		if (!this.locksHeld.containsKey(t)) {
			HashMultiset<Integer> hms = new HashMultiset<>();
			this.locksHeld.put(t, hms);
		} 
		this.locksHeld.get(t).add(l);
	}
	
	public void removeLockFromLocksHeld(Integer t, Integer l) {
		this.locksHeld.get(t).remove(l);
	}

	public int getLockHeldCount(Integer t, Integer l) {
		return this.locksHeld.get(t).count(l);
	}
	
	public int uniqueLocksHeld(Integer t) {
		return this.locksHeld.get(t).elementSet().size();
	}

	public void incClockThread(int index) {
		VectorClock C_t = this.clockThread.get(index);
		int threadId = getThreadId(index);
		C_t.inc(threadId);
	}

	public VectorClock getThreadVC(int thread) {
		if (this.clockThread.containsKey(thread)) {
			return this.clockThread.get(thread);
		} else {
			return null;
		}
	}

	public void addThread(int parent, int child) {
		int parentThreadId = getThreadId(parent);
		int childThreadId = this.threadSet.size();
		threadMap.put(child, childThreadId);

		VectorClock newVC = new VectorClock(this.getThreadVC(parent));
		newVC.inc(childThreadId);
		this.clockThread.put(child, newVC);
		this.threadSet.add(child);
		this.getThreadVC(parent).adjustSize(this.threadSet.size());
	}

	public void addThread(int newThread) {
		if (!this.threadSet.contains(newThread)) {
			int threadId = this.threadSet.size();
			threadMap.put(newThread, threadId);
			this.threadSet.add(newThread);
			VectorClock newVC = new VectorClock(this.threadSet.size());
			newVC.inc(threadId);
			this.clockThread.put(newThread, newVC);
		}
	}

	public Integer getThreadId(int thread) {
		return this.threadMap.get(thread);
	}
}
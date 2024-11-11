package javato.activetesting.syncpd.util;

import java.util.ArrayList;

public class VectorClock implements Comparable<VectorClock> {

	private int dim;
	private ArrayList<Integer> clock;

	public VectorClock(int d) {
		this.dim = d;
		this.clock = new ArrayList<Integer>();
		for (int ind = 0; ind < this.dim; ind++) 
			this.clock.add(0);
	}

	public VectorClock(VectorClock fromVectorClock) {
		this.dim = fromVectorClock.getDim();
		this.clock = (ArrayList<Integer>) fromVectorClock.getClock().clone();
	}

	public int getDim() {
		if (!(this.dim == this.clock.size())) {
			throw new IllegalArgumentException("Mismatch in dim and clock size");
		}
		return this.dim;
	}

	public ArrayList<Integer> getClock() {
		return this.clock;
	}

	public String toString() {
		return this.clock.toString();
	}

	public boolean isZero() {
		boolean itIsZero = true;
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind);
			if (thisVal != 0) {
				itIsZero = false;
				break;
			}
		}
		return itIsZero;
	}

	public boolean isEqual(VectorClock vc) {
		if (!(this.dim == vc.getDim())) 
			return false;
		boolean itIsEqual = true;
		ArrayList<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < this.dim; ind++) {
			int thisVal = this.clock.get(ind);
			int vcVal = vcClock.get(ind);
			if (thisVal != vcVal) {
				itIsEqual = false;
				break;
			}
		}
		return itIsEqual;
	}

	public boolean isLessThanOrEqual(VectorClock vc) {
		if (this.clock.size() <= vc.getDim()) 
			adjustSize(vc.getDim()-1);
		boolean itIsLessThanOrEqual = true;
		ArrayList<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < vc.getDim(); ind++) {
			int thisVal = this.clock.get(ind);
			int vcVal = vcClock.get(ind);
			if (!(thisVal <= vcVal)) {
				itIsLessThanOrEqual = false;
				break;
			}
		}
		return itIsLessThanOrEqual;
	}
	
	public boolean isGreaterThanOrEqual(VectorClock vc) {
		if (this.clock.size() <= vc.getDim())
			adjustSize(vc.getDim()-1);
		boolean itIsGreaterThanOrEqual = true;
		ArrayList<Integer> vcClock = vc.getClock();
		for (int ind = 0; ind < vc.getDim(); ind++) {
			int thisVal = this.clock.get(ind);
			int vcVal = vcClock.get(ind);
			if (!(thisVal >= vcVal)) {
				itIsGreaterThanOrEqual = false;
				break;
			}
		}
		return itIsGreaterThanOrEqual;
	}

	public void setToZero() {
		for (int ind = 0; ind < this.dim; ind++)
			this.clock.set(ind, 0);
	}

	public void copyFrom(VectorClock vc) {
		if (this.clock.size() <= vc.getDim()) 
			adjustSize(vc.getDim()-1);
		for (int ind = 0; ind < vc.getDim(); ind++) 
			this.clock.set(ind, vc.clock.get(ind)); 
	}

	public void updateMax(VectorClock vc) {
		if (this.clock.size() <= vc.getDim()) 
			this.adjustSize(vc.getDim()-1);
		for (int ind = 0; ind < vc.getDim(); ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind); 
			int max_c = this_c > vc_c ? this_c : vc_c;
			this.clock.set(ind, max_c);
		}
	}

	private void updateMax2(VectorClock vc) {
		if (this.clock.size() <= vc.getDim())
			adjustSize(vc.getDim()-1);
		for (int ind = 0; ind < vc.getDim(); ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind);
			int max_c = this_c > vc_c ? this_c : vc_c;
			this.clock.set(ind, max_c);
		}
	}

	// The following function update this as : this := \lambda t . if t == tIndex
	// then this[tIndex] else max(this[t], vc[t])
	public void updateMax2WithoutLocal(VectorClock vc, int tIndex) {
		if (this.clock.size() <= vc.getDim())
			adjustSize(vc.getDim()-1);
		if ((this.clock.size() < vc.getDim()))
			throw new IllegalArgumentException("Mismatch in this.dim and argument.dim");

		for (int ind = 0; ind < vc.getDim(); ind++) {
			if (ind != tIndex) {
				int this_c = this.clock.get(ind);
				int vc_c = vc.clock.get(ind);
				int max_c = this_c > vc_c ? this_c : vc_c;
				this.clock.set(ind, max_c);
			}
		}
	}

	public void updateWithMax(VectorClock... vcList) {
		if (!(vcList.length >= 1)) {
			throw new IllegalArgumentException(
					"Insuffiecient number of arguments provided");
		}
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			VectorClock vc = vcList[i];
			this.updateMax2(vc);
		}
	}

	private void updateMin2(VectorClock vc) {
		if (this.clock.size() <= vc.getDim()) 
			adjustSize(vc.getDim()-1);
		for (int ind = 0; ind < vc.getDim(); ind++) {
			int this_c = this.clock.get(ind);
			int vc_c = vc.clock.get(ind);
			int min_c = this_c < vc_c ? this_c : vc_c;
			this.clock.set(ind, min_c); 		
		}
	}

	public void updateWithMin(VectorClock... vcList) {
		if (!(vcList.length >= 1)) {
			throw new IllegalArgumentException(
					"Insuffiecient number of arguments provided");
		}
		this.copyFrom(vcList[0]);
		for (int i = 1; i < vcList.length; i++) {
			VectorClock vc = vcList[i];
			this.updateMin2(vc);
		}
	}

	public int getClockIndex(int tIndex) {
		if (tIndex >= this.clock.size())
			return 0;
		else
			return this.clock.get(tIndex);
	}

	public void setClockIndex(int tIndex, int tValue) {
		adjustSize(tIndex);
		this.clock.set(tIndex, tValue);
	}

	public void inc(int tIndex) {
		adjustSize(tIndex);
		this.clock.set(tIndex, this.clock.get(tIndex)+1);
	}

	public void adjustSize(int tIndex) {
		while (tIndex >= this.clock.size())
			this.clock.add(0);
		this.dim = this.clock.size();
	}

	@Override
	public int compareTo(VectorClock vc) {
		if (this.isEqual(vc)) {
			return 0;
		} else if (this.isLessThanOrEqual(vc)) {
			return -1;
		} else
			return 1;
	}

}
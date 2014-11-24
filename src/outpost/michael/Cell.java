package outpost.michael;

import java.util.*;
import outpost.sim.Pair;

public class Cell implements Comparable<Cell> {

	public Pair location;
	public int w_value;
	public int l_value;

	public Cell(Pair location) {
		this.location = location;
	}

	public int compareTo(Cell o) {
		
		if (this.w_value < o.w_value) {
			return 1;
		}

		if (this.w_value > o.w_value) {
			return -1;
		}

		return 0;
	}
}
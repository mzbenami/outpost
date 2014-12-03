package outpost.michael;

import java.util.*;
import outpost.sim.Pair;

public class Cell implements Comparable<Cell> {

	public Pair location;
	public double w_value;
	public double l_value;
	public double r_value;

	public Cell(Pair location) {
		this.location = location;
	}

	public int compareTo(Cell o) {
		
		if (this.r_value < o.r_value) {
			return 1;
		}

		if (this.r_value > o.r_value) {
			return -1;
		}

		return 0;
	}
}
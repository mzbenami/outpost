package outpost.michael;

import java.util.*;
import outpost.sim.Pair;

public class Post {

	public int id;
	public Pair current;
	public Pair target;

	public Post(int id) {
		this.id = id;
	}

	public String toString() {
		return "Id: " + id + " current: " + current.x + "," + current.y + " target: " + target.x + "," + target.y;
	}
}
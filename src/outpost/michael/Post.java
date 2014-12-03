package outpost.michael;

import java.util.*;
import outpost.sim.Pair;

public class Post {

	public int id;
	public Pair current;
	public Pair target;
	public int w_value;
	public int l_value;
	public int r_value;
	public String role;

	public boolean targetSet;

	public Post(int id) {
		this.id = id;
		targetSet = false;
	}

	public Post() {
		targetSet = false;
	}

	public Post(Post o)
	{
		id = o.id;
		current = o.current;
		target = o.target;
		w_value = o.w_value;
		l_value = o.l_value;
		o.r_value = o.r_value;
		targetSet = o.targetSet;
	}

	public String toString() {
		return "Id: " + id + " current: " + current.x + "," + current.y + " target: " + target.x + "," + target.y + " w: " + w_value + " l: " + l_value + " r: " + r_value;
	}
}
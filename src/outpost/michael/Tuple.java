package outpost.michael;

import java.util.*;
import outpost.sim.Pair;

public class Tuple
{
    public int x;
    public int y;
    public Tuple(Pair p)
    {
        x = p.x;
        y = p.y;
    }

    @Override
    public boolean equals(Object o)
    {
        if(o == this)
            return true;
        if (!(o instanceof Tuple)) 
        {
            return false;
        }
        Tuple t = (Tuple)o;
        if(x == t.x && y == t.y)
            return true;
        else
            return false;
    }

    @Override
    public int hashCode()
    {
        return (x*17+y*11);
    }
}
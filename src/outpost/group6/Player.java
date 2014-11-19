package outpost.group6;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

import java.lang.management.*;
 
public class Player extends outpost.sim.Player {
    static int size =100;
	static Point[] grid = new Point[size*size];
	Pair[] home = new Pair[4];
    boolean [][] opponentOutpost = new boolean[size][size];
    Random random;

    int outpostCounter = 0;
    int my_id;
    //For use in floodfill
    int [][] tempGrid = new int[size][size];
    boolean[][] vst = new boolean[size][size];
    int[] cx = {0, 0, 1, -1};
    int[] cy = {1, -1, 0, 0};

    int sx, sy;
    int[] mx = {1, 0, 1};
    int[] my = {0, 1, 1};
    int mSize = 3;

    ArrayList<Pair> target = new ArrayList<Pair>();

    boolean initDone = false;

    long totalCpuTime = 0;


    public Player(int id_in) {
        super(id_in);

        my_id = id_in;
	}

    /** Get CPU time in nanoseconds. */
    public long getCpuTime( ) {
        ThreadMXBean bean = ManagementFactory.getThreadMXBean( );
        return bean.isCurrentThreadCpuTimeSupported( ) ?
            bean.getCurrentThreadCpuTime( ) : 0L;
    }


	public void init() {
        home[0] = new Pair(0,0);
        home[1] = new Pair(size-1, 0);
        home[2] = new Pair(size-1, size-1);
        home[3] = new Pair(0,size-1);

        random = new Random();
        for(int id = 0; id < 4; id++)
        {
            if (id == my_id)
                continue;
            int dx = home[id].x - home[my_id].x;
            int dy = home[id].y - home[my_id].y;

            if(dx != 0)
            {
                sx = dx/Math.abs(dx);
            }
            if(dy != 0)
            {
                sy = dy/Math.abs(dy);
            }
        }

        for(int i = 0; i < mSize; i++)
        {
            mx[i] *= sx;
            my[i] *= sy;
        }
        System.out.printf("[Group6][INIT] sx=%d, sy=%d\n", sx, sy);
        System.out.printf("[Group6][INIT] mx=[%d, %d, %d]\n", mx[0], mx[1], mx[2]);
        System.out.printf("[Group6][INIT] my=[%d, %d, %d]\n", my[0], my[1], my[2]);



    }
    
    static double distance(Point a, Point b) {
        return Math.sqrt((a.x-b.x) * (a.x-b.x) +
                         (a.y-b.y) * (a.y-b.y));
    }
    
    public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
    	System.out.printf("haha, we are trying to delete a outpost for player %d\n", this.id);
    	int del = king_outpostlist.get(my_id).size() - 1;
    	return del;
    }

    public Pair findNextMovePos(Pair current, Pair target)
    {
        System.out.printf("[Group6][LOG] Finding path (%d, %d) -> (%d, %d)\n", current.x, current.y, target.x, target.y);
        for (int i = 0; i < size; ++i)
        {
            for (int j = 0; j < size; ++j)
            {
                vst[i][j] = false;
                tempGrid[i][j] = 4*size;
            }
        }

        vst[target.x][target.y] = true;
        tempGrid[target.x][target.y] = 0;
       
        Queue<Pair> q = new LinkedList<Pair>();
        q.add(target);
       
        while (!q.isEmpty()) {
            Pair p = q.poll();
            // if (p.equals(current))
            //     break;
            int d = tempGrid[p.x][p.y];
            for (int i = 0; i < 4; ++i) {
                int x = p.x + cx[i], y = p.y + cy[i];
                if (x < 0 || x >= size || y < 0 || y >= size || vst[x][y]) continue;
                if (x == current.x && y == current.y)
                {
                    System.out.printf("[Group6][LOG] Path Found\n");
                    return new Pair(p);
                }
                Point pt = PairtoPoint(p);
                if (!pt.water && (pt.ownerlist.size() == 0 || (pt.ownerlist.size() == 1 && pt.ownerlist.get(0).x == my_id))) {
                    vst[x][y] = true;
                    tempGrid[x][y] = Math.min(tempGrid[x][y], d+1);
                    q.add(new Pair(x, y));
                }
            }
        }

        // int min = tempGrid[current.x][current.y];
        // Pair move = new Pair(current);
        // for (int i = 0; i < 4; ++i) 
        // {
        //     int x = current.x + cx[i], y = current.y + cy[i];
        //     if (x < 0 || x >= size || y < 0 || y >= size) continue;
        //     if(min > tempGrid[x][y])
        //     {
        //         min = tempGrid[x][y];
        //         move.x = x;
        //         move.y = y;
        //     }
        // }

        System.out.printf("[Group6][LOG] Path Found\n");
        return new Pair(current);

    }
    
	public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int t){

        System.out.printf("[Group6][START]\n");
        long startCpuTime = getCpuTime();

    	for (int i=0; i<gridin.length; i++) {
    		grid[i]=new Point(gridin[i]);
    	}

        for(int x = 0; x < size; x++)
        {
            for(int y = 0; y < size; y++)
            {
                opponentOutpost[x][y] = false;
            }
        }


        for(int playerId = 0; playerId < king_outpostlist.size(); playerId++)
        {
            if(playerId == my_id)
            {
                continue;
            }
            
            opponentOutpost[home[playerId].x][home[playerId].y] = true;
            
            for(int outpostId = 0; outpostId < king_outpostlist.get(playerId).size(); outpostId++)
            {
                Pair opponentOutpostLocation = king_outpostlist.get(playerId).get(outpostId);
                opponentOutpost[opponentOutpostLocation.x][opponentOutpostLocation.y] = true;

                for(int i = 0; i < 4; i++)
                {
                    int x = opponentOutpostLocation.x + cx[i], y = opponentOutpostLocation.y + cy[i];//Forbidden region around outpost
                    
                    if (x < 0 || x >= size || y < 0 || y >= size) 
                        continue;
                    
                    opponentOutpost[x][y] = true;
                }
            }
        }

        ArrayList<movePair> nextlist = new ArrayList<movePair>();
        
        Pair target = new Pair();
        Pair outpostLocation;

        for(int outpostId = 0; outpostId < king_outpostlist.get(my_id).size(); outpostId++)
        {
            
            outpostLocation = king_outpostlist.get(my_id).get(outpostId);
            
            if(outpostId%mSize == 0)
            {
                System.out.printf("[Group6][LOG] Diagonal Mover Id = %d\n", outpostId);
                do
                {
                    int moveType = random.nextInt(mSize);
                    target.x = outpostLocation.x + mx[moveType];
                    target.y = outpostLocation.y + my[moveType];
                    if(target.x < 0 || target.x >= size || target.y < 0 || target.y >= size)
                    {
                        target.x = outpostLocation.x;
                        target.y = outpostLocation.y;
                        break;
                    }

                }while(PairtoPoint(target).water);
            }
            else
            {
                target.x = (size-1)*mx[outpostId%mSize] + home[my_id].x;
                target.y = (size-1)*my[outpostId%mSize] + home[my_id].y;

            }
        
            Pair nextPos = findNextMovePos(outpostLocation, target);
            if(opponentOutpost[nextPos.x][nextPos.y] == true)
            {
                nextPos.x  = outpostLocation.x;
                nextPos.y = outpostLocation.y;
            }
            nextlist.add(new movePair(outpostId, nextPos));


            System.out.printf("[Group6][LOG] Moving Outpost[%d] (%d, %d) -> (%d, %d)\n", outpostId, outpostLocation.x, outpostLocation.y, nextPos.x, nextPos.y);
        }

        long endCpuTime = getCpuTime();

        long timeTaken = (endCpuTime - startCpuTime); //ns
        totalCpuTime += timeTaken/(1000000); 

    	System.out.printf("[Group6][END] Cpu Time, This iteration: %d ns, Total: %d ms\n", timeTaken, totalCpuTime);
    	return nextlist;
    
    }
    
    static Point PairtoPoint(Pair pr) {
    	return grid[pr.x*size+pr.y];
    }
    static Pair PointtoPair(Point pt) {
    	return new Pair(pt.x, pt.y);
    }
}

package outpost.michael;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

import java.lang.management.*;

 
public class Player extends outpost.sim.Player {
    

    Integer outpost_id = 0;
    static int size =100;
    static Point[] grid = new Point[size*size];
    int r;
    int L;
    int W;
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

    ArrayList<Cell> allCells;
    ArrayList<Cell> allCloseCells;
    ArrayList<Cell> allFarCells;
    ArrayList<Pair> homeCells;
    ArrayList<Post> ourPosts;
    Pair[] region = new Pair[2];
    int[] rx = {10, -10, -10, 10};
    int[] ry = {10, 10, -10, -10};
    int[] startx = {0, size - 1, size -1, 0};
    int[] starty = {0, 0, size -1, size -1};
    int moveCount = 0;
    int resizeCount = 0;
    
    int RG_THRESH = 2;
    int PR_THRESH = 6;//Maximum Protector OutPosts

    //TODO-Done: Create ourPostsHash, resourceGettersList, explorersList, protectorsList
    HashMap<Integer, Post> ourPostsHash = new HashMap<Integer, Post>();
    ArrayList<Integer> resourceGettersList = new ArrayList<Integer>();
    ArrayList<Integer> explorersList = new ArrayList<Integer>();
    ArrayList<Integer> protectorsList = new ArrayList<Integer>();
    
    double[] water = new double[4];
    double[] soil = new double[4];
    int[] noutpost = new int[4];

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

    static int manDistance(Pair a, Pair b) {
        return Math.abs(a.x-b.x) + Math.abs(a.y-b.y);
    }

    static boolean isInBounds(Pair a) {
        return !(a.x < 0 || a.x >= size || a.y < 0 || a.y >= size);
    }

    static boolean isInRegion(Pair p, Pair a, Pair b) {
        int left, right, top, bottom;

        if (a.x < b.x) {
            left = a.x;
            right = b.x;
        } else {
            left = b.x;
            right = a.x;
        }

        if (a.y < b.y) {
            bottom = a.y;
            top = b.y;
        } else {
            bottom = b.y;
            top = a.y;
        }

        return !(p.x < left || p.x > right || p.y < bottom || p.y > top);
    }
    
    public int delete(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin) {
        System.out.printf("[Group6] Deleting post due to shortage of resources\n");
        
        ArrayList<Pair> ourKingList = king_outpostlist.get(my_id);
        Pair farthestPair = null;
        int farthestDistance = 0;
        int del = 0;

        for (int i = 0; i < ourKingList.size(); i++) {
            Pair pair = ourKingList.get(i);
            int d;
            if ((d = manDistance(pair, home[my_id])) > farthestDistance) {
                farthestDistance = d;
                farthestPair = pair;
                del = i;
            }
        }

        Post p = ourPosts.get(del);
        ourPosts.remove(ourPosts.size() - 1);
        ourPostsHash.remove(p.id);
        return del;
    }

    public Pair findNextMovePos(Pair current, Pair target)
    {

        Point ptt = PairtoPoint(target);
        if(ptt.water == true)
        {
            return new Pair(current);
        }
     //   System.out.printf("[Group6][LOG] Finding path (%d, %d) -> (%d, %d)\n", current.x, current.y, target.x, target.y);
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
                  //  System.out.printf("[Group6][LOG] Path Found\n");
                    return new Pair(p);
                }
                Pair pr = new Pair(x, y);
                Point pt = PairtoPoint(pr);
                if (!pt.water /*&& (pt.ownerlist.size() == 0 || (pt.ownerlist.size() == 1 && pt.ownerlist.get(0).x == my_id)) /*&& (!opponentPresence(p))*/) {
                    vst[x][y] = true;
                    tempGrid[x][y] = Math.min(tempGrid[x][y], d+1);
                    q.add(pr);
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

//System.out.printf("[Group6][LOG] Path Found\n");
        return new Pair(current);

    }

    //Check for opponents presence in nearby area (8 surrounding cells)
    public boolean opponentPresence(Pair p)
    {
        int[] cx = {0,0,0,0,1,-1,2,-2};
        int[] cy = {1,-1,2,-2,0,0,0,0};
        
        for (int i = 0; i < 8; ++i) {
            int x = p.x + cx[i], y = p.y + cy[i];
            if (x < 0 || x >= size || y < 0 || y >= size) continue;
            Pair p_new = new Pair(x,y);
            Point pt = PairtoPoint(p_new);
            if (pt.ownerlist.size() == 0 || (pt.ownerlist.size() == 1 && pt.ownerlist.get(0).x == my_id))
                continue;
            else
                return true;             
        }
        return false;
    }

    public void printOupost()
    {
        System.out.printf("[Group6][Outpost] Total Outposts Alive: %d, Total Spawned: %d\n", ourPosts.size(), outpost_id);

        for(Post p: ourPosts)
        {
            if(p.targetSet == true)
            {
                System.out.printf("[Group6][Outpost] id:%d, role = %s, current:(%d, %d), target:(%d, %d) \n", p.id, p.role, p.current.x, p.current.y, p.target.x, p.target.y);
            }
            else
            {
                System.out.printf("[Group6][Outpost] id:%d, role = %s, current:(%d, %d), \n", p.id, p.role, p.current.x, p.current.y);
            }
        }
    }
  
    public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> king_outpostlist, Point[] gridin, int r, int L, int W, int t){
     //   System.out.printf("[Group6][START]\n");
        long startCpuTime = getCpuTime();

        moveCount++;
        this.grid = gridin;

        if (!initDone) {
            this.r = r;
            this.L = L;
            this.W = W;
            initCells();
            calcCellValues(); //sort every cell on the board by their "water value", descending
            ourPosts = new ArrayList<Post>(); // a list of our Outposts that persists through "move" calls
            region[0] = new Pair(startx[my_id], starty[my_id]);
            region[1] = new Pair (50 , 50);


            initDone = true;
        }

        refreshPosts(king_outpostlist); //allign our outpost list with the one passed in from the simulator
                                        //also sets targets for any newly created outposts, stored in Post.target
        //refreshTargets(region[0], region[1]); //TODO-Done: Not Needed now

        printOupost();
        
        // if (moveCount % 300 == 0) {
        //     region[1].x += rx[my_id];
        //     region[1].y += ry[my_id];
        
        // }

        //TODO-Done: Call resourceGetterTask
        resourceGetterTask();

        //TODO-Done: Call protectorTask
        protectorTask();

        //TODO-Done: Call explorerTask
        explorerTask();

        ArrayList<movePair> nextlist = new ArrayList<movePair>();

        for (int i =0; i < ourPosts.size(); i++) 
        {
            Post post = ourPosts.get(i);
            Pair next = findNextMovePos(post.current, post.target);

   //         System.out.println("[Group6][LOG] " + post + " Next: " + next.x + "," + next.y);
            
            nextlist.add(new movePair(i, next));
            post.current = next;
        }
        long endCpuTime = getCpuTime();

        long timeTaken = (endCpuTime - startCpuTime)/(1000000); //ms
        totalCpuTime += timeTaken; 

      //  System.out.printf("[Group6][END] Cpu Time, This iteration: %d ms, Total: %d ms\n", timeTaken, totalCpuTime);

        System.out.printf("[Group6] Blank\n");
        printOupost();

        System.out.printf("[Group6] Blank\n");
        System.out.printf("[Group6] Blank\n");

        return nextlist;
    
    }


    /*allign our outpost list with the one passed in from the simulator
    also sets targets for any newly created outposts, stored in Post.target */

    void printHashMap(HashMap<Tuple, ArrayList<Integer>> map)
    {
        System.out.printf("[refreshPosts][print] Size %d\n", map.size());
        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext())
        {
            Tuple t = (Tuple)iterator.next();
            System.out.printf("[refreshPosts][print] Key %d, %d, %d\n", t.x, t.y, t.hashCode());
            System.out.printf("[refreshPosts][print] Value size %d\n", map.get(t).size());

        }
    }

    void refreshPosts(ArrayList<ArrayList<Pair>> king_outpostlist) {

        //TODO-Done: Review, add newly spawed outposts to explorer list and create ourPostsHash
        ArrayList<Pair> ourKingList = king_outpostlist.get(my_id);

        HashMap<Tuple, ArrayList<Integer>> map = new HashMap<Tuple, ArrayList<Integer>>();
        ArrayList<Integer> temp;
        Tuple key;

        for(Integer i = 0; i < ourPosts.size(); i++)
        {
            key = new Tuple(ourPosts.get(i).current);
            //System.out.printf("[refreshPosts]Adding Key  %d, %d, %d\n", key.x, key.y, key.hashCode());
            map.put(key, new ArrayList<Integer>());
            
        }

        for(Integer i = 0; i < ourPosts.size(); i++)
        {
            key = new Tuple(ourPosts.get(i).current);
            //System.out.printf("[refreshPosts]Adding value to Key  %d, %d, %d\n", key.x, key.y, key.hashCode());
            map.get(key).add(i);
            
        }
        //printHashMap(map);

        for(Integer i = 0; i < ourKingList.size(); i++)
        {
            key =  new Tuple(ourKingList.get(i));
            temp = map.get(key);

            if(temp == null || temp.size() == 0)
            {
                //System.out.printf("[refreshPosts]Key Not present %d, %d, %d\n", key.x, key.y, key.hashCode());
                Post post = new Post(outpost_id);
                outpost_id++;
                post.current = ourKingList.get(i);
                ourPosts.add(post);
                //TODO-Done: Add id to explorersList
                explorersList.add(post.id);
                System.out.printf("[Group6][RefreshPosts] Adding new outpost[%d] at %d, %d \n", post.id, post.current.x, post.current.y);
            }
            else
            {
                temp.remove(temp.size()-1);
            }
        }

        HashMap<Integer, Boolean> postToRemove = new HashMap<Integer, Boolean>();

        Iterator iterator = map.keySet().iterator();
        while(iterator.hasNext())
        {
            temp = map.get(iterator.next());
            if(temp != null)
            {
                for(Integer index:temp)
                {
                    postToRemove.put(index, true);
                }
            }
        }

        ArrayList<Post> newPosts = new ArrayList<Post>();
        for(Integer i = 0; i < ourPosts.size(); i++)
        {
            if(postToRemove.get(i) != null)
            {
                Pair toRemove = ourPosts.get(i).current;
                System.out.printf("[Group6][RefreshPosts] Removing outpost[%d] at %d, %d \n",ourPosts.get(i).id, toRemove.x, toRemove.y);
            }
            else
            {
                newPosts.add(new Post(ourPosts.get(i)));
            }
        }
    
        ourPosts.clear();
        ourPostsHash.clear();

        ourPosts = newPosts;

        for(Post p:ourPosts)
        {
            ourPostsHash.put(p.id, p);
        }

        ArrayList<Integer> newRG = new ArrayList<Integer>();
        for(Integer id: resourceGettersList)
        {
            if(ourPostsHash.containsKey(id) == true)
            {
                newRG.add(id);
            }
        }

        ArrayList<Integer> newP = new ArrayList<Integer>();
        for(Integer id: protectorsList)
        {
            if(ourPostsHash.containsKey(id) == true)
            {
                newP.add(id);
            }
        }

        ArrayList<Integer> newE = new ArrayList<Integer>();
        for(Integer id: explorersList)
        {
            if(ourPostsHash.containsKey(id) == true)
            {
                newE.add(id);
            }
        }

        resourceGettersList.clear();
        resourceGettersList = newRG;

        explorersList.clear();
        explorersList = newE;

        protectorsList.clear();
        protectorsList = newP;
        
    }

    
    /* finds a suitable target cell in the region specified by Pair a, and Pair b. We can define rectangular regions this way
    with two pairs making up two opposite corners of the rectangle */
    Pair targetInRegion(Pair a, Pair b) {
        for (Cell c : allCells) {
            if (!isInRegion(c.location, a, b))
                continue;

            int postCount = 0;
            for (Post post : ourPosts) {
                if (manDistance(post.target, c.location) > 2*r) {
                    postCount++;
                } else {
                    break;
                }
            }

            if (postCount == ourPosts.size()) {
                return c.location;
            }
        }

        int c = moveCount % 4;
        return new Pair(startx[c], starty[c]);
    }

        // void refreshTargets(Pair a, Pair b) {
            
        //     ArrayList<Post> ourPostsCopy = new ArrayList<Post>();
        //     ourPostsCopy.addAll(ourPosts);

        //     for (Post post : ourPostsCopy) {
        //         post.targetSet = false;
        //     }

        //     for (Cell c : allCells) {
        //         if (!isInRegion(c.location, a, b))
        //             continue;

        //         int postCount = 0;
        //         for (Post post : ourPosts) {
        //             if (post.targetSet == false || manDistance(post.target, c.location) > 2*r) {
        //                 postCount++;
        //             } else {
        //                 break;
        //             }
        //         }

        //         if (postCount != ourPosts.size()) {
        //             continue;
        //         }

        //         int bestDist = 1000;
        //         Post closestPost = null;
        //         for (Post post : ourPostsCopy) {
        //             int d = manDistance(post.current, c.location);
        //             if (d < bestDist) {
        //                 bestDist = d;
        //                 closestPost = post;
        //             }
        //         }
        //         closestPost.target = c.location;
        //         closestPost.l_value = c.l_value;
        //         closestPost.w_value = c.w_value;
        //         closestPost.r_value = c.r_value;
        //         closestPost.targetSet = true;
        //         ourPostsCopy.remove(closestPost);

        //         if (ourPostsCopy.size() == 0) {
        //             break;
        //         }      
        //     }

        //     if (ourPostsCopy.size() > 0) {
               
        //         if (resizeCount < 2) {
        //             region[1].x += rx[my_id];
        //             region[1].y += ry[my_id];
        //             refreshTargets(region[0], region[1]);
        //             resizeCount++;
        //         } else {
        //             int count = 0;

        //             for (Post post : ourPostsCopy) {
        //                 count = count % 4;
        //                 post.target = new Pair(startx[count], starty[count]);
        //                 post.targetSet = true;
        //                 count++;
        //             }
        //         }

        //     }
        // }

    void resourceGetterTask() {

        calculateres();

        System.out.println("posts size: " + ourPosts.size());
        System.out.println("supported outposts: " + noutpost[my_id]);

        if (!(noutpost[my_id] >= ourPosts.size() + RG_THRESH)) {
            assignResourceGetters();
        }
    }

    void assignResourceGetters() {
        int nResourceGetters = resourceGettersList.size();
        int sustainableOutposts = noutpost[my_id];
        int nExplorers = explorersList.size();
        ArrayList<Post> resourceGetters = getCurrentresourceGetters();
        ArrayList<Post> explorers = getCurrentExplorers();

        ArrayList<Pair> targets = new ArrayList<Pair>();
        int neededOutposts = ourPosts.size() + RG_THRESH + 1;

        while (targets.size() == 0) {
            neededOutposts--;

            double neededWater = W * (neededOutposts - 1);
            double neededLand = L * (neededOutposts - 1);

            double currentLand = soil[my_id];
            double currentWater = water[my_id];

            neededWater -= currentWater;
            neededLand -= currentLand;

            System.out.println("needed water" + neededWater);
            System.out.println("needed land" + neededLand);

            Pair[] searchRegion = {home[my_id], new Pair(50, 50)};

            int availablePostSize = nResourceGetters + nExplorers; //TODO: Review, This will use up all explorers

            for (int nTargets = 1; nTargets <= 1; nTargets++) {

                String neededType;

                if (neededLand > neededWater) {
                    neededType = "land";
                } else {
                    neededType = "water";                
                }
                
                for (Cell c : allCloseCells) {

                    if (c.w_value >= neededWater / nTargets && c.l_value >= neededLand / nTargets && !postWithinDistance(c.location, 2 * r)) {
                        targets.add(c.location);
                        System.out.println("adding: " + c.w_value + " " + c.l_value);
                        neededWater -= c.w_value;
                        neededLand -= c.l_value;
                        if (targets.size() >= nTargets) {
                            break;
                        }                    
                    }
                }
                
                if (targets.size() >= nTargets) {
                    break;
                }
            }

            System.out.println("Targetsize: " + targets.size());
            for (Pair p : targets) {
                System.out.println("Target: " + stringifyPair(p));
            }
        }

        // for (int i = 0; i < targets.size(); i++) {
   
        //     int bestDist = 10000;
        //     Post closestPost = null;
        //     for (Post post : resourceGetters) {
        //         int d = manDistance(post.current, targets.get(i));
        //         if (d < bestDist) {
        //             bestDist = d;
        //             closestPost = post;
        //         }
        //     }
        //     if(closestPost != null)
        //     {
        //         closestPost.target = targets.get(i);
        //         closestPost.targetSet = true;
        //         targets.remove(i);
        //         resourceGetters.remove(closestPost);
        //     }
        // }

        for (int i = 0; i < targets.size(); i++) {
            if (explorersList.size() > 0) {
                int bestDist = 10000;
                Post closestPost = null;
                for (Post post : explorers) {
                    int d = manDistance(post.current, targets.get(i));
                    if (d < bestDist) {
                        bestDist = d;
                        closestPost = post;
                    }
                }
                if(closestPost != null)
                {
                    closestPost.target = targets.get(i);
                    closestPost.targetSet = true;
                    targets.remove(i);
                    closestPost.role = "Resource Getter";
                    explorersList.remove(closestPost.id);
                    explorers.remove(closestPost);
                    //TODO-Done:Add id to resourceGettersList
                    resourceGettersList.add(closestPost.id);
                }
            }
        }     
    }

    boolean postWithinDistance(Pair cell, int distance) {
        for (Post p : ourPosts) {
            if (p.target == null) continue;
            if (manDistance(cell, p.target) < distance) return true;
        }

        return false;
    }

    ArrayList<Post> getCurrentExplorers() {
        
        ArrayList<Post> currentExplorers = new ArrayList<Post>();

        for (Integer id : explorersList) {
            Post post = ourPostsHash.get(id);
            currentExplorers.add(post);
        }

        return currentExplorers;
    }

    ArrayList<Post> getCurrentresourceGetters() {
        
        ArrayList<Post> currentResourceGetters = new ArrayList<Post>();

        for (Integer id : resourceGettersList) {
            Post post = ourPostsHash.get(id);
            currentResourceGetters.add(post);
        }

        return currentResourceGetters;
    }


    void calculateres() {
        //System.out.println("calculate resouce");
        for (int i=0; i<4; i++) {
            water[i] =0.0;
            soil[i] =0.0;
        }
        for (int i=0; i<size*size; i++) {
            if (grid[i].ownerlist.size() == 1) {
                if (grid[i].water) {
                    water[grid[i].ownerlist.get(0).x]++;
                }
                else {
                    soil[grid[i].ownerlist.get(0).x]++;
                }
            }
            else if (grid[i].ownerlist.size() > 1){
                for (int f=0; f<grid[i].ownerlist.size(); f++) {
                    if (grid[i].water) {
                        water[grid[i].ownerlist.get(f).x]=water[grid[i].ownerlist.get(f).x]+1/grid[i].ownerlist.size();
                    }
                    else {
                        soil[grid[i].ownerlist.get(f).x]=soil[grid[i].ownerlist.get(f).x]+1/grid[i].ownerlist.size();
                    }
                }

            }
        }
        for (int i=0; i<4; i++) {
            noutpost[i] = (int) Math.min(soil[i]/L, water[i]/W)+1;
        }

    }

    /* Calculate the water value and land value for every cell */
    void calcCellValues() {

        for (Cell cell : allCells) {
            Pair orig = cell.location;

            if (PairtoPoint(orig).water) {
                    cell.w_value = -1;
                    cell.l_value = -1;
                    cell.r_value = -1;
                    continue;
            }

            ArrayList<Pair> diamond = diamondFromPair(orig, r);
            for (Pair p : diamond) {

                if (PairtoPoint(p).water) {
                    cell.w_value += 1; 
                } else {
                    cell.l_value += 1;
                }      
            }
        
            cell.r_value = Math.min(cell.w_value / W, cell.l_value / L);
        }
        Collections.sort(allCells);
    }

    void initCells() {
    	//Create Home Cells
    	homeCells = new ArrayList<Pair>();
    	createHomeCellsList();
    	
        allCells = new ArrayList<Cell>();
        for (int i = 0; i < grid.length; i++) {
            allCells.add(new Cell(PointtoPair(grid[i])));
        }

        allCloseCells = new ArrayList<Cell>();
        allCloseCells.addAll(allCells);

        Collections.sort(allCloseCells, new Comparator<Cell>() {
            public int compare(Cell c1, Cell c2) {
                if (manDistance(c1.location, home[my_id]) < manDistance(c2.location, home[my_id])) {
                    return -1;
                }
                
                if (manDistance(c1.location, home[my_id]) > manDistance(c2.location, home[my_id])) {
                    return 1;
                }

                return 0;
            }
        });

        allFarCells = new ArrayList<Cell>();
        allFarCells.addAll(allCloseCells);
        Collections.reverse(allFarCells);
    }

    ArrayList<Pair> diamondFromPair(Pair a, int r) {
        ArrayList<Pair> diamond = new ArrayList<Pair>();

        for (int i = -r; i <= r; i++) {
            for (int j = -r; j <= r; j++) {
                if (Math.abs(i) + Math.abs(j) > r) continue;
                Pair tmp = new Pair(a.x + i, a.y + j);
                if (isInBounds(tmp)) diamond.add(tmp);
            }
        }

        return diamond;
    }


    //Create the list of the home cells
    void createHomeCellsList()
    {
    	int[] incX = {1,-1,-1,1};
    	int[] incY = {1,1,-1,-1};
    	
    	//First Home Cell
    	Pair p= new Pair(home[my_id].x, home[my_id].y + r*(incY[my_id]));
    	homeCells.add(p);
    	
    	//Second Home Cell
    	Pair p2= new Pair(home[my_id].x+ r*(incX[my_id]), home[my_id].y);
    	homeCells.add(p2);
    	
    	//Third Home Cell
    	Pair p3= new Pair(home[my_id].x+ r*(incX[my_id]), home[my_id].y + r*(incY[my_id]));
    	homeCells.add(p3);
    	
    	//Fourth Home Cell
    	Pair p4 = new Pair(p.x, p.y+(incY[my_id]));
    	homeCells.add(p4);
    	
    	//Fifth Home Cell
    	Pair p5 = new Pair(p2.x+(incX[my_id]), p2.y);    	
    	homeCells.add(p5);
    
    	//Sixth Home Cell    	
    	Pair p6 = new Pair(p3.x+(incX[my_id]), p3.y+(incY[my_id]));    	
    	homeCells.add(p6);
    }

    //Assign Protector outposts their targets
    void protectorTask(){ 	
    	    	
    	//First create the copy of the home cells and then get the list of unassigned home cells    	        
    	ArrayList<Pair> unassignedHome = new ArrayList<Pair>();
    	unassignedHome.addAll(homeCells);    	    	
    	for(Integer id: protectorsList)
    	{	
			Post p = ourPostsHash.get(id);
			for(Pair pr: homeCells){
				if(pr.equals(p.target))
				{
					unassignedHome.remove(pr);
					break;
				}
			}
    		
    	}
    	
    	//Assign the outposts a targetvalue basedon the unassigned homeCells
    	Integer i = 0;
    	while(PR_THRESH- protectorsList.size() >0 && explorersList.size()>0)//TODO-Done: Check should be PR_THRESH- protectorsList.size() >0
    	{ 
    		int curr_id = explorersList.get(0);
    		Post p = ourPostsHash.get(curr_id);
    		p.target = unassignedHome.get(i);  
    		p.targetSet = true;
    		p.role = "Protectors";
    		i++;
    		explorersList.remove(0);
            //TODO-Done: Add id to protectorsList
            protectorsList.add(p.id);
    	}
    }  
    
  //Assign Explorer Outposts suitable targets
    void explorerTask()
    {
    	//Create a list of unassigned explores
    	ArrayList<Integer> expList = new ArrayList<Integer>();
    	for(Integer id : explorersList)
    	{  
    		Post exp = ourPostsHash.get(id);
    		if(exp.targetSet== false)   //TODO-Done: This check sould be on targetSet 		
    			expList.add(id);    		
    	}
    	
    	//Function call to assign targets to the Explorers 
    	setExplorerTargets(region[0], region[1], (2*r), expList);
    }
    
    
    //Set targets for the Explorers
    void setExplorerTargets(Pair a, Pair b, int dist, ArrayList<Integer> expList){
        if(expList.size()==0)
            return;
    	//Check each cell starting from farthest post
    	for (Cell c : allFarCells) {
            if (!isInRegion(c.location, a, b))
                continue;
            
            //Check if there exists any existing outpost at a certain distance to this cell
            //If so the we do not assign this cell as target for the explorer
            boolean postExists = false;
            for(Post p: ourPosts)
            {
            	if(p.target != null && manDistance(p.target,c.location) <= dist)
            	{
            		postExists = true;
            		break;
            	}
            }
            
            //If no post exists near by then assign this cell as target
            if(!postExists)
            {
            	int id = expList.get(0);
            	Post exp = ourPostsHash.get(id);
            	exp.target = c.location;
    			exp.targetSet = true;
    			expList.remove(0);
            }
            
            //Checkif more explorers remain to be assigned targets
            //If not then exit the loop
            if(expList.size()==0)
            	break;
    	}
    	
    	//After the current iteration through all cells if still there are
    	//Explorers remaining to be assined targets because of "distance" condition
    	//not being satisfied then call this function recursively by halving the distance
    	if(expList.size()!=0)
    		setExplorerTargets(region[0], region[1], dist/2, expList);
    	else return;    	
    }
    
    
    static Point PairtoPoint(Pair pr) {
        return grid[pr.x*size+pr.y];
    }
    static Pair PointtoPair(Point pt) {
        return new Pair(pt.x, pt.y);
    }

    static String stringifyPair(Pair pr) {
        return String.format("%d,%d", pr.x, pr.y);
    }
}

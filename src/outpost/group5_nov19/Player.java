//
// TODO: HashMap to cache dst and path, save reduncant path finding
//
//
//
//
//
//
package outpost.group5;

import java.util.*;

import outpost.sim.Pair;
import outpost.sim.Point;
import outpost.sim.movePair;

public class Player extends outpost.sim.Player {
  static int size = 100;
	static Point[][] grid = new Point[size][size];
	static Random random = new Random();
	static int[] theta = new int[100];
	static int counter = 0;
  boolean initFlag = false;
  ArrayList<Pair> ourOutpostLst = new ArrayList<Pair>();
  Set<Pair> opponentOutpostSet = new HashSet<Pair>();
  ArrayList<Point> opponentBaseLst = new ArrayList<Point>();
  ArrayList<ArrayList<Point>> radianLsts = new ArrayList<ArrayList<Point>>();

  public Player(int id_in) {
    super(id_in);
    initFlag = true;
	}
    
  public ArrayList<movePair> move(ArrayList<ArrayList<Pair>> outpostLsts, Point[] gridin, int r, int L, int W, int t){
    // cell     : Point (int x, int y, boolean water)
    // outpost  : Pair (int x, int y)
    // move     : movePair (int id, Pair pr)

    if(initFlag){
      initFlag = false; 
      parseGrid(gridin);
      populateRadianLsts(r);
    }
     
    updateOpponentOutpost(outpostLsts);

    ArrayList<movePair> returnLst = new ArrayList<movePair>();
    ourOutpostLst = outpostLsts.get(this.id);

    returnLst = radianOutreach(ourOutpostLst);
    // failed suffocate logic
    /*
    // harverster outpost
    returnLst.add(moveTo(0, grid[50][50]));

    // killer outpost
    returnLst.addAll(suffocate(ourOutpostLst));
    */
    return returnLst;
  }
    
  //=========================================================    
  //
  // Utility logic
  //
  //========================================================= 

  public void init(){
    // update opponent base position for suffocation   muahaha
    System.out.println("size of the list: " + opponentBaseLst.size());
    opponentBaseLst.add(new Point(0, 0, false));
    opponentBaseLst.add(new Point(size - 1, 0, false));
    opponentBaseLst.add(new Point(size - 1, size - 1, false));
    opponentBaseLst.add(new Point(0, size - 1, false));
    opponentBaseLst.remove(id);
    System.out.println("-----");
    for(Point p : opponentBaseLst){
      System.out.println(p.x + ":" + p.y);
    }
    System.out.println("-----");
  }

  double distance(Point a, Point b) {
    return Math.sqrt((a.x-b.x) * (a.x-b.x) + (a.y-b.y) * (a.y-b.y));
  }

  // logic for remove over populated outpost 
  // kill the youngest one 
  public int delete(ArrayList<ArrayList<Pair>> outpostLsts, Point[] gridin) {
    return outpostLsts.get(id).size() - 1;
  }

  // convert grid into 2-D plane
  void parseGrid(Point[] gridin){
    for(int i = 0; i < size; ++i){
      for(int j = 0; j < size; ++j){
        grid[i][j] = gridin[i * size + j];
        //System.out.println("(" + i + ":" + j + ")  ;  (" + grid[i][j].x + ":" + grid[i][j].y + ")");
      }
    }
    System.out.println("Grid parsing finished");
  }

  // setup radian mesh
  void populateRadianLsts(int r){
    // increase r to diameter
    //r *= 2;

    int boundary = 6 * size / 10;
    for(int i = boundary; i >= 0.1 * boundary; i -= r){
      ArrayList<Point> newLst = new ArrayList<Point>();
      for(int j = 0; j < i; j += r){
        if(!grid[i][j].water){
          newLst.add(new Point(i, j, false));
        }
        if(!grid[j][i].water){
          newLst.add(new Point(j, i, false));
        }
      }
      radianLsts.add(newLst);
    }

    for(ArrayList<Point> arrayLst : radianLsts){
      for(Point p : arrayLst){
        System.out.println("(" + p.x + ", " + p.y + ")");
      }
    }
  }

  void updateOpponentOutpost(ArrayList<ArrayList<Pair>> outpostLsts){
    opponentOutpostSet.clear();
    for(int i = 0; i < outpostLsts.size(); ++i){
      if(i == id){
        continue;
      }
      ArrayList<Pair> tempLst = outpostLsts.get(i);
      for(Pair p : tempLst){
        opponentOutpostSet.add(p);
      }
    }
    System.out.println("OpponentSet updated, total hostile outpost : " + opponentOutpostSet.size());
  }


  // basic path finding logic
  // TODO: implement priority queue to switch from BFS to A*
  movePair moveTo(int index, Point dst){
    Pair src = ourOutpostLst.get(index);
    if(dst.water){
      System.out.printf("destination (%d, %d) is water, stay\n", dst.x, dst.y);
      return new movePair(index, src); 
    }
    if(src.x == dst.x && src.y == dst.y){
      System.out.println("dst reached");
      return new movePair(index, src);
    }
    // BFS for rechability finding 
    int [][] visitedMap = new int[size][size];
    Queue<Pair> q  = new LinkedList<Pair>();
    q.offer(src);
    visitedMap[src.x][src.y] = 1;
    boolean reachableFlag = false;
    while(q.size() > 0){
      Pair curP = q.poll();
      ArrayList<Pair> nextHop = nextHopLst(curP);
      for(Pair p : nextHop){
        if(!grid[p.x][p.y].water && visitedMap[p.x][p.y] == 0){
          q.offer(p);
          visitedMap[p.x][p.y] = visitedMap[curP.x][curP.y] + 1;
        }
        if(p.equals(new Pair(dst.x, dst.y))){
          reachableFlag = true;
          break;
        }
      }
    }
    // reverse searching for path formation
    if(reachableFlag){
      System.out.printf("a path is found for src: (%d, %d); dst: (%d, %d)\n", src.x, src.y, dst.x, dst.y);
      Pair prevP = new Pair(dst.x, dst.y);
      while(true){
        ArrayList<Pair> prevHop = nextHopLst(prevP);
        for(Pair p : prevHop){
          //System.out.print("(" + prevP.x + "," + prevP.y + ")");
          // reach src
          if(visitedMap[p.x][p.y] == 1){
            return new movePair(index, prevP);
          } 
          if(visitedMap[p.x][p.y] == visitedMap[prevP.x][prevP.y] - 1) {
            prevP = p;
            break;
          }
        }
      }
    }
    System.out.printf("no path is found for src: (%d, %d); dst: (%d, %d)\n", src.x, src.y, dst.x, dst.y);
    return new movePair(index, src);
  }
  
  // get adjacent cells 
  ArrayList<Pair> nextHopLst(Pair start) {
    ArrayList<Pair> prLst = new ArrayList<Pair>();
    for (int i = 0; i < 4; ++i) {
    	Pair tmp0 = new Pair(start);
    	Pair tmp = null;
      switch(i){
        case 0: 
          tmp = new Pair(tmp0.x - 1, tmp0.y);
          break;
        case 1:
          tmp = new Pair(tmp0.x + 1, tmp0.y);
          break;
        case 2:
          tmp = new Pair(tmp0.x, tmp0.y - 1);
          break;
        case 3:
          tmp = new Pair(tmp0.x, tmp0.y + 1);
          break;
      }
      if(tmp.x >= 0 && tmp.x < size && tmp.y >= 0 && tmp.y < size){
        prLst.add(tmp);
      }
    }
    return prLst;
  }
    

  static Pair PointtoPair(Point pt) {
    return new Pair(pt.x, pt.y);
  }

  //=========================================================    
  //
  // Game Logic
  //
  //========================================================= 
  
  ArrayList<movePair> suffocate(ArrayList<Pair> outpostLst){
    ArrayList<movePair> res = new ArrayList<movePair>();
    for(int i = 1; i < outpostLst.size(); ++i){
      switch(i % 3){
        case 0:
          res.add(moveTo(i, opponentBaseLst.get(0)));
          break;
        case 1:
          res.add(moveTo(i, opponentBaseLst.get(1)));
          break;
        case 2:
          res.add(moveTo(i, opponentBaseLst.get(2)));
          break;
       }
    }
    
    System.out.println("killer list size: " + res.size()); 
    return res;
  }

  ArrayList<movePair> radianOutreach(ArrayList<Pair> outpostLst){
    ArrayList<movePair> res = new ArrayList<movePair>();
    int counter = 0;
    for(ArrayList<Point> arrayLst : radianLsts){
      for(Point p : arrayLst){
        res.add(moveTo(counter++, p));
        if(counter >= outpostLst.size()){
          return res;
        }
      }
    }
    while(counter < outpostLst.size()){
      res.add(moveTo(counter++, opponentBaseLst.get(counter % 3)));
    }
    return res;
  }

}

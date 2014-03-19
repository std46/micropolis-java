package micropolisj.engine;

import java.math.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;

/**
 * Simluates traffic stuff
 */
public class TrafficSim {
	Micropolis engine;
	HashMap<Integer,SpecifiedTile> unready;
	HashMap<CityLocation,SpecifiedTile> ready;
	HashMap<CityLocation,Integer> mapBack;
	HashSet<CityLocation> goal;
	HashSet<CityLocation> found;
	
	
	
	
	public TrafficSim(){
		this(new Micropolis());
	}
	
	public TrafficSim(Micropolis city){
		engine = city;
		ready = new HashMap<CityLocation,SpecifiedTile>();
		unready = new HashMap<Integer,SpecifiedTile>();
		goal = new HashSet<CityLocation>();
		found = new HashSet<CityLocation>();
		mapBack = new HashMap<CityLocation,Integer>();
	}
	/**
	 * The function is called to generate traffic from the starting position
	 * uses A*-Algorithm to find ways
	 * @param startP starting position
	 * @return length of the way (-1 for no way)
	 */
	public int genTraffic(CityLocation startP) {
		CityLocation end=findEnd(startP);
		int way=findWay(startP,end);
		if (way!=-1) {
			engine.putVisits(startP);
			engine.putVisits(end);
		}
		return way;
	}
	
	/**
	 * determinates the end of a way starting at a given field
	 * @param startpos
	 * @return the endpos
	 */
	
	private CityLocation findEnd(CityLocation startpos){
		/* iterates through engine.visits and puts them (together with a specifically calculated weight)
		 * into a new HashMap. From there we will randomly create the "end" of the route.
		 */
		HashMap<CityLocation,Integer> help = new HashMap<CityLocation,Integer>();
		Iterator it = engine.visits.keySet().iterator();
		while(it.hasNext()){
			CityLocation temp = (CityLocation) it.next();		
			help.put(temp,getValue(startpos,temp));
		}
		it.remove();
		
		int sum=0;
		int t;
		Iterator it2 = help.values().iterator();
		while(it.hasNext()){
			t=(int)it2.next();
			sum+=t;
		}		
		int i = engine.PRNG.nextInt(sum)+1;
		for(CityLocation b : help.keySet()){
			i-=(int)help.get(b);
			if(i<=0){
				return b;
			}
		}
		return startpos;
	}
	
	
	/**
	 * This function returns the weight for later more or less randomly determine the end 
	 * of the route you want to travel.
	 * @param start
	 * @param end
	 * @return
	 */
	private int getValue(CityLocation start, CityLocation end){
		int factor;
		factor = getFactor(engine.getTile(start.x, start.y), engine.getTile(end.x,end.y));
		return (200000/(evalfunc(start,(HashSet<CityLocation>) findPeriphereRoad(end).keySet()))+20)*factor; 
		//factor 200 000 for making randomization easyer later on.
	}
	
	/**
	 * Just calculating the factor for later calculating the weight in "getValue"
	 * @param start
	 * @param end
	 * @return
	 */
	private static int getFactor(char start, char end){
		if(TileConstants.isResidentialZone((int)start)){
			if(TileConstants.isResidentialZone((int)end)){
				return 2;
			}
			if(TileConstants.isCommercialZone((int)end)){
				return 20;
			}
			if(TileConstants.isIndustrialZone((int)end)){
				return 12;
			}
			if((int)end==964){
				return 8;	//School
			}
			if((int)end==982 || (int)end==991){
				return 9;	//UniversityA or UniversityB
			}
			if((int)end==973){
				return 7;	//Museum
			}
			if((int)end==1012){
				return 5;	//OpenAir
			}
			if((int)end==750 || (int)end==816){
				return 5;	//PowerPlant or Nuclear	
			}
			if((int)end==716){
				return 6;	//Airport
			}
			if((int)end==784 || (int)end==800){
				return 7;	//Stadium or FullStadium
			}			
		}
		if(TileConstants.isCommercialZone((int)start)){
			if((int)end==716){
				return 2;	//Airport
			}
			if((int)end==698){
				return 1;	//Port
			}
		}
		if(TileConstants.isIndustrialZone((int)start)){
			if((int)end==698){
				return 1;
			}
		}
		if((int)start==964){	//School
			if((int)end==973){
				return 5;	//Museum
			}
			if((int)end==982 || (int)end==991){
				return 1;	//UniversityA or UniversityB
			}
		}
		if((int)start==716){	//Airport
			if((int)end==784 || (int)end==800){
				return 2;	//Stadium or FullStadium
			}
			if((int)end==1012){
				return 1;	//OpenAir
			}
		}		
		return 0;
	}
	
	
	/**
	 * finds way from A to B, if exists
	 * in general, does A*-algorithm
	 * and increase traffic along the way
	 * @param startpos
	 * @param endpos
	 * @return length of the way
	 */
	
	public int findWay(CityLocation startpos, CityLocation endpos){
		int currentCost=0;
		CityLocation currentLocation=new CityLocation(-1,-1);
		ready=findPeriphereRoad(startpos); //generate starts
		goal=(HashSet<CityLocation>) findPeriphereRoad(endpos).keySet(); //generate ends
		found=(HashSet<CityLocation>) ready.keySet();
		int best=200;
		CityLocation fastGoal=new CityLocation(-1,-1);
		if (ready.isEmpty()) {
			return -1;
		}
		for (CityLocation f : ready.keySet()) { //take roads adj to starts
			for (CityLocation g : findAdjRoads(f)) {
				int keyi=16384*evalfunc(f,goal)+g.y;
				unready.put(keyi,new SpecifiedTile(g,f,false));
				mapBack.put(g, keyi);
				found.add(g);
			}
		}
		while (!unready.isEmpty() && best>(Collections.min(unready.keySet()))) { //main algorithm A*
			int current=Collections.min(unready.keySet()); //add new field to ready
			currentLocation=unready.get(current).getLoc();
			currentCost=engine.getTrafficCost(currentLocation,0);
			ready.put(currentLocation, new SpecifiedTile(currentCost,unready.get(current).getPred(),true));
			unready.remove(current);
			for (CityLocation g : findAdjRoads(currentLocation)) { //go through adj roads
				if (!found.contains(g)) { //new road part found
					this.found.add(g);
					int keyi=16384*evalfunc(currentLocation,goal)+g.y;
					unready.put(keyi,new SpecifiedTile(g,currentLocation,false));
					mapBack.put(g, keyi);
				} else { //was already found before
					if (g!=ready.get(current).getPred()) {//if not pred
						if (ready.containsKey(g)) { //if it is alreay ready update ready
							int c=evalfunc(g, goal)+currentCost+ready.get(ready.get(g).getPred()).getCosts();
							if (ready.get(g).getCosts()<=c) {
								ready.put(g, new SpecifiedTile(c,currentLocation,true));
							}
						} else { //if not, update it unready
							int keyi=16384*evalfunc(currentLocation,goal)+g.y;
							if (keyi<= mapBack.get(g)) {
								unready.put(keyi,new SpecifiedTile(g,currentLocation,false));
								mapBack.put(g, keyi);
							}
						}
					}
				}
			}
			if (goal.contains(currentLocation)) { //if it is a goal update goal
				best=Math.min(best, ready.get(currentLocation).getCosts());
				fastGoal=new CityLocation(currentLocation.x,currentLocation.y);
			}
		}
		if (best==200) {
			return -1;
		}
		Vector<CityLocation> way=new Vector<CityLocation>();
		while (ready.get(fastGoal).getPred()!=new SpecifiedTile().getLoc()) { //add traffic to way 
			engine.addTraffic(fastGoal.x, fastGoal.y, engine.getTrafficCost(fastGoal,0));
			way.add(fastGoal);
			fastGoal=ready.get(fastGoal).getPred();
		}
		engine.paths(way);
		return best;
	}
	
	private HashSet<CityLocation> findAdjRoads(CityLocation loc) {
		HashSet<CityLocation> ret=new HashSet<CityLocation>();
		for (int dir=0;dir<4;dir++) {
			if (engine.onMap(loc,dir) && TileConstants.isRoad(engine.getTile(Micropolis.goToAdj(loc,dir).x, Micropolis.goToAdj(loc,dir).y))) {
				ret.add(Micropolis.goToAdj(loc,dir));
			}
		}
		return ret;
	}
	
	/**
	 * finds out if there are streets next to our zone and return a HashMap with default values
	 * @param pos zone center
	 * @return keys are the streets next to the zone, values are default
	 */
	
	public HashMap<CityLocation,SpecifiedTile> findPeriphereRoad(CityLocation pos){
		char tiletype;
		HashMap<CityLocation,SpecifiedTile> ret=new HashMap<CityLocation,SpecifiedTile>();
		tiletype=engine.getTile(pos.x, pos.y);
		int dimension; //height (and so width) of the tile
		if(tiletype==716){       //if tiletype==AIRPORT -> see TileConstants  needs to be changed for some new buildings potentially!!!
			dimension=4;
		}else{
			dimension=3;
		}
		for(int i=-1; i<dimension-1;i++){
			if (engine.onMap(new CityLocation(pos.x-2,pos.y+i))&&TileConstants.isRoadAny(engine.getTile(pos.x-2, pos.y+i))){  
				ret.put(new CityLocation(pos.x-2,pos.y+i),new SpecifiedTile());
			}
			if (engine.onMap(new CityLocation(pos.x+dimension-1,pos.y+i))&&TileConstants.isRoadAny(engine.getTile(pos.x+2, pos.y+i))){  
				ret.put(new CityLocation(pos.x+dimension-1,pos.y+i),new SpecifiedTile());
			}
			if (engine.onMap(new CityLocation(pos.x+i,pos.y-2))&&TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y-2))){  
				ret.put(new CityLocation(pos.x+i,pos.y-2),new SpecifiedTile());
			}
			if (engine.onMap(new CityLocation(pos.x+i,pos.y+dimension-1))&&TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y+2))){  
				ret.put(new CityLocation(pos.x+i,pos.y+dimension-1),new SpecifiedTile());
			}
		}
		
		return ret;
	}
	/**
	 *  increases the traffic on a given tile by value
	 * @param pos
	 * @param value is the zone type of the tile
	 */
	public void makeTraffic(CityLocation pos, int value){
		
	}
	/**
	 * capped at 32 767
	 * @param start
	 * @param finish
	 * @return
	 */
	
	public static int evalfunc(CityLocation start, HashSet<CityLocation> finish){
		int ret=32767;
		for (CityLocation g : finish) {
			ret=Math.min(ret,Math.abs(start.x-g.x)+Math.abs(start.y-g.y)+1);
		}
		return ret;
	}
	
	
	
	
	
}
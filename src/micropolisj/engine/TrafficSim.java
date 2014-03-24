package micropolisj.engine;

import java.math.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Vector;
import java.util.Iterator;

/**
 * Simulates traffic stuff
 */
public class TrafficSim {
	Micropolis engine;
	HashMap<Integer,SpecifiedTile> unready;
	HashMap<RoadSpecifiedTile,SpecifiedTile> ready;
	HashMap<RoadSpecifiedTile,Integer> mapBack;
	HashSet<RoadSpecifiedTile> goal;
	HashSet<RoadSpecifiedTile> found;
	int currentRoadType;
	
	
	
	
	public TrafficSim(){
		this(new Micropolis());
	}
	
	public TrafficSim(Micropolis city){
		engine = city;
		ready = new HashMap<RoadSpecifiedTile,SpecifiedTile>();
		unready = new HashMap<Integer,SpecifiedTile>();
		goal = new HashSet<RoadSpecifiedTile>();
		found = new HashSet<RoadSpecifiedTile>();
		mapBack = new HashMap<RoadSpecifiedTile,Integer>();
	}
	/**
	 * The function is called to generate traffic from the starting position
	 * uses A*-Algorithm to find ways
	 * @param startP starting position
	 * @return length of the way (-1 for no way)
	 */
	public int genTraffic(CityLocation startP) {
		CityLocation end=findEnd(startP);
		if(CityLocation.equals(end, new CityLocation(-1,-1))){
			return -1;
		}
		int way=findWay(startP,end);
		if (way!=-1) {
			engine.putVisits(startP);
			engine.putVisits(end);
		} else {
			engine.noWay();
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
		for(CityLocation cl : engine.visits.keySet()){
			CityLocation temp = cl;
			help.put(temp, getValue(startpos,temp));
		}		
		int sum=0;
		int t;
		Iterator<Integer> it2 = help.values().iterator();
		while(it2.hasNext()){
			t=(int)it2.next();
			sum+=t;
		}
		if(sum==0){
			return new CityLocation(-1,-1);
		}
		int i = engine.PRNG.nextInt(sum)+1;
		for(CityLocation b : help.keySet()){
			i-=(int)help.get(b);
			if(i<=0){
				return b;
			}
		}
		return new CityLocation(19,10);
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
		if (evalfunc(start,toHashSet(findPeriphereRoad(end)))>=150 || CityLocation.equals(start,end)) {
			return 0;
		}
		if(evalfunc(start,toHashSet(findPeriphereRoad(end)))<10){
			return (200000/(evalfunc(start,toHashSet(findPeriphereRoad(end))))+20)*factor/100;
		}
		else{
			return (200000/(evalfunc(start,toHashSet(findPeriphereRoad(end))))+20)*factor; 
			//factor 200 000 for making randomization easyer later on.
		}
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
			if((int)end==698){ //Port
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
	
	protected int findWay(CityLocation startpos, CityLocation endpos){
		int currentCost=0;
		CityLocation currentLocation=new CityLocation(-1,-1);
		ready = new HashMap<RoadSpecifiedTile,SpecifiedTile>();
		unready = new HashMap<Integer,SpecifiedTile>();
		found.clear();
		HashMap<CityLocation,SpecifiedTile> temp=findPeriphereRoad(startpos);
		for (CityLocation f : temp.keySet()) {
			for (Integer tm : calcRoadType(f,1)) {
				ready.put(new RoadSpecifiedTile(f,tm), temp.get(f)); //generate starts
				found.add(new RoadSpecifiedTile(f,tm));
			}
		}
		goal.clear();
		for (CityLocation f : toHashSet(findPeriphereRoad(endpos))) {
			for (int g : calcRoadType(f,1)) {
				goal.add(new RoadSpecifiedTile(f,g)); //generate ends
			}
		}
		int best=16384*3000*20;
		RoadSpecifiedTile fastGoal=new RoadSpecifiedTile(new CityLocation(-1,-1),1);
		if (ready.isEmpty()) {
			return -1;
			}
		for (RoadSpecifiedTile f : ready.keySet()) { //take roads adj to starts
			for (RoadSpecifiedTile g : findAdjRoads(f.getLocation(),f.getRoadType())) {
				if (!searchRoSpec(toHashSetTwo(ready),g)) {
					for (int roadType : calcRoadType(engine.getTile(g.getLocation()),ready.get(f).getRoadType())) {
						int keyi=16384*10*evalfuncHelp(f.getLocation(),goal)+g.getLocation().y*600+g.getLocation().x*5+roadType; //map size limited at 120
						unready.put(keyi,new SpecifiedTile(g.getLocation(),f,false,roadType));
						mapBack.put(g,keyi);
						found.add(g);
					}
				}
				
			}
		}
		ready.put(new RoadSpecifiedTile(new CityLocation(-1,-1),0), new SpecifiedTile(0,new RoadSpecifiedTile(new CityLocation(-1,-1),0),true,1)); //default vortex
		while (!unready.isEmpty() && best>(Collections.min(unready.keySet()))) { //main algorithm A*
			int current=Collections.min(unready.keySet()); //add new field to ready
			currentLocation=unready.get(current).getLoc();
			currentRoadType=unready.get(current).getRoadType();
			currentCost=engine.getTrafficCost(currentLocation,currentRoadType);
			
			RoadSpecifiedTile Pred=unready.get(current).getPred();
			ready.put(new RoadSpecifiedTile(currentLocation,currentRoadType), new SpecifiedTile(search(ready,Pred).getCosts()+currentCost,Pred,true,currentRoadType));
			unready.remove(current);
			for (RoadSpecifiedTile g : findAdjRoads(currentLocation, currentRoadType)) { //go through adj roads
				if (!searchRoSpec(found,g)) { //new road part found
					int keyi=16384*(10*evalfuncHelp(currentLocation,goal)+search(ready,new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts())+g.getLocation().y*600+g.getLocation().x*5+g.getRoadType();
					unready.put(keyi+g.getRoadType(),new SpecifiedTile(g.getLocation(),new RoadSpecifiedTile(currentLocation,currentRoadType),false,g.getRoadType()));
					mapBack.put(g, keyi+g.getRoadType());
					this.found.add(g);
				} else { //was already found before
					if (!RoadSpecifiedTile.equals(g,search(ready,new RoadSpecifiedTile(currentLocation,currentRoadType)).getPred())) {//if not pred
						if (searchRoSpec(toHashSetTwo(ready),g)) { //if it is already ready update ready
							int c=currentCost+search(ready,search(ready,g).getPred()).getCosts()+engine.getTrafficCost(g.getLocation(), g.getRoadType());
							if (search(ready,g).getCosts()<=c) {
								ready.put(g, new SpecifiedTile(c,new RoadSpecifiedTile(currentLocation,currentRoadType),true,search(ready,g).getRoadType()));
							}
						} else { //if not, update it unready
							int keyi=16384*(10*evalfuncHelp(currentLocation,goal)+search(ready,new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts())+g.getLocation().y*600+g.getLocation().x*5+g.getRoadType();
							if (keyi<= searchTwo(mapBack,g)) {
								for (int tm : calcRoadType(g.getLocation(),currentRoadType)) {
									unready.put(keyi+tm,new SpecifiedTile(g.getLocation(),new RoadSpecifiedTile(currentLocation,currentRoadType),false,tm));
									mapBack.put(new RoadSpecifiedTile(g.getLocation(),tm), keyi+tm);
								}
							}
						}
					}
				}
			}
			if (searchRoSpec(goal,new RoadSpecifiedTile(currentLocation,currentRoadType))) { //if it is a goal update goal
				best=Math.min(best, search(ready,new RoadSpecifiedTile(currentLocation,currentRoadType)).getCosts());
				fastGoal=new RoadSpecifiedTile(new CityLocation(currentLocation.x,currentLocation.y),currentRoadType);
			}
		}
		if (best==16384*3000*20) {
			return -1;
		}
		System.out.println(best);
        //Vector<CityLocation> way=new Vector<CityLocation>();
        engine.paths.clear();
		while (!RoadSpecifiedTile.equals(search(ready,fastGoal).getPred(),new RoadSpecifiedTile(new CityLocation(-1,-1),0))) { //add traffic to way 
			engine.addTraffic(fastGoal.getLocation().x, fastGoal.getLocation().y, engine.getTrafficCost(fastGoal.getLocation(),fastGoal.getRoadType()));
			//way.add(fastGoal.getLocation());
			fastGoal=search(ready,fastGoal).getPred();
		}
		return best;
	}
	
	/*private boolean sameRoadType(int roadType1, int roadType2) {
		return roadType1==4 || roadType2==4 || (RoadSpecifiedTile.isRoad(roadType1)==RoadSpecifiedTile.isRoad(roadType2) && RoadSpecifiedTile.isRail(roadType1)==RoadSpecifiedTile.isRail(roadType2));
	}*/
	
	/**
	 *  caluclate possible roadTypes
	 * @param me tileID
	 * @param prevType int between 1 and 4
	 * @return int between 1 and 4
	 */
	
	private static Vector<Integer> calcRoadType(int me,int prevType) {
		Vector<Integer> ret =new Vector<Integer>();
		int myType =TileConstants.roadType(me);
		if (myType==4) {
			ret.add(4);
			return ret;
		}
		if (myType!=5 && myType!=6) {
			if (myType==3 && prevType==3) {
				ret.add(myType);
				return ret;
			}
			if ((myType==1 || myType==2) && (prevType%4==1 || prevType%4==2)) {
				ret.add(myType);
				return ret;
			}
			return ret;
		}
		if (prevType==4) {
			ret.add(myType-4);
			ret.add(3);
			return ret;
		} else {
			ret.add(prevType);
			return ret;
		}
	}
	
	private Vector<Integer> calcRoadType(CityLocation me,int prevType) {
		if (engine.onMap(me)) {
		return calcRoadType(engine.getTile(me), prevType);
		}
		Vector<Integer> ret=new Vector<Integer>();
		if (CityLocation.equals(new CityLocation(-1,-1), me)) {
			ret.add(1);
		} else {
			ret.add(0);
		}
		return ret;
	}
	
	/*private HashSet<RoadSpecifiedTile> findAdjRoads(CityLocation loc) {
		HashSet<RoadSpecifiedTile> ret=new HashSet<RoadSpecifiedTile>();
		
		for (int dir=0;dir<4;dir++) {
			if (engine.onMap(loc,dir) && TileConstants.isRoadAny(engine.getTile(Micropolis.goToAdj(loc,dir).x, Micropolis.goToAdj(loc,dir).y))) {
				CityLocation destination=Micropolis.goToAdj(loc,dir);
				for (int u : calcRoadType(destination,engine.getTile(loc))) {
					ret.add(new RoadSpecifiedTile(destination,u));
				}
			}
		}
		return ret;
	}*/
	
	private HashSet<RoadSpecifiedTile> findAdjRoads(CityLocation loc, int roadTyp) {
		HashSet<RoadSpecifiedTile> ret=new HashSet<RoadSpecifiedTile>();
		
		for (int dir=0;dir<4;dir++) {
			if (engine.onMap(loc,dir) && TileConstants.isRoadAny(engine.getTile(Micropolis.goToAdj(loc,dir).x, Micropolis.goToAdj(loc,dir).y))) {
				CityLocation destination=Micropolis.goToAdj(loc,dir);
				for (int u : calcRoadType(destination,roadTyp)) {
					ret.add(new RoadSpecifiedTile(destination,u));
				}
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
			for (int roadType : calcRoadType(new CityLocation(pos.x-2,pos.y+i),1)) {
				if (engine.onMap(new CityLocation(pos.x-2,pos.y+i))){
					if(TileConstants.isRoadAny(engine.getTile(pos.x-2, pos.y+i))){
						ret.put(new CityLocation(pos.x-2,pos.y+i),new SpecifiedTile(roadType));
					}					
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+dimension-1,pos.y+i),1)) {
				if (engine.onMap(new CityLocation(pos.x+dimension-1,pos.y+i))){
					if(TileConstants.isRoadAny(engine.getTile(pos.x+dimension-1, pos.y+i))){
						ret.put(new CityLocation(pos.x+dimension-1,pos.y+i),new SpecifiedTile(roadType));
					}					
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+i,pos.y-2),1)) {
				if (engine.onMap(new CityLocation(pos.x+i,pos.y-2))){
					if(TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y-2))){
						ret.put(new CityLocation(pos.x+i,pos.y-2),new SpecifiedTile(roadType));
					}					
				}
			}
			for (int roadType : calcRoadType(new CityLocation(pos.x+i,pos.y+dimension-1),1)) {
				if (engine.onMap(new CityLocation(pos.x+i,pos.y+dimension-1))){
					if(TileConstants.isRoadAny(engine.getTile(pos.x+i, pos.y+dimension-1))){
						ret.put(new CityLocation(pos.x+i,pos.y+dimension-1),new SpecifiedTile(roadType));
					}					
				}
			}
		}
		return ret;
	}
	/**
	 * capped at 32 767
	 * @param start
	 * @param finish
	 * @return
	 */
	
	private static int evalfunc(CityLocation start, HashSet<CityLocation> finish){
		int ret=32767;
		for (CityLocation g : finish) {
			ret=Math.min(ret,Math.abs(start.x-g.x)+Math.abs(start.y-g.y)+1);
		}
		return ret;
	}
	private static int evalfuncHelp(CityLocation start, HashSet<RoadSpecifiedTile> finish){
		HashSet<CityLocation> newFinish=new HashSet<CityLocation>();
		for (RoadSpecifiedTile g : finish) {
			newFinish.add(g.getLocation());
		}
		return evalfunc(start,newFinish);
	}
	
	
	public static HashSet<CityLocation> toHashSet(HashMap<CityLocation,SpecifiedTile> input){
		HashSet<CityLocation> ret = new HashSet<CityLocation>();
		for(CityLocation c : input.keySet()){
			ret.add(c);
		}
		return ret;
	}
	public static HashSet<RoadSpecifiedTile> toHashSetTwo(HashMap<RoadSpecifiedTile,SpecifiedTile> input){
		HashSet<RoadSpecifiedTile> ret = new HashSet<RoadSpecifiedTile>();
		for(RoadSpecifiedTile c : input.keySet()){
			ret.add(c);
		}
		return ret;
	}
	
	public static SpecifiedTile search(HashMap<RoadSpecifiedTile,SpecifiedTile> hm, RoadSpecifiedTile x){
		for(RoadSpecifiedTile r : hm.keySet()){
			if(RoadSpecifiedTile.equals(r, x)){
				return hm.get(r);
			}
		}
		return null;
	}
	
	public static boolean searchRoSpec(HashSet<RoadSpecifiedTile> input, RoadSpecifiedTile x){
		for(RoadSpecifiedTile r : input){
			if(RoadSpecifiedTile.equals(r, x)){
				return true;
			}
		}
		return false;
	}
	public static int searchTwo(HashMap<RoadSpecifiedTile,Integer> input, RoadSpecifiedTile x){
		for(RoadSpecifiedTile r : input.keySet()){
			if(RoadSpecifiedTile.equals(r, x)){
				return input.get(r);
			}
		}
		return -1;
	}
	
	
	
}

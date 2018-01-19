//import the API.
//See xxx for the javadocs.
import java.util.ArrayList;

import bc.*;

public class SeedingPlayer {
	public static Direction[] directions = new Direction[8];
	public static boolean finishedFactories = false;
	public static boolean finishedRockets = false;
	public static MapLocation swarmLoc;
	public static Team myTeam;
	public static Team opponentTeam;
	public static PlanetMap planetMap;
	public static Planet thisPlanet;
	public static MapLocation landLoc = new MapLocation(Planet.Mars, 10, 10);
	
	public static void main(String[] args) {

     // One slightly weird thing: some methods are currently static methods on a static class called bc.
     // This will eventually be fixed :/
     System.out.println("Opposite of " + Direction.North + ": " + bc.bcDirectionOpposite(Direction.North));

     // Connect to the manager, starting the game
     GameController gc = new GameController();
     
     // get planet and planet map
     thisPlanet = gc.planet();
     planetMap = gc.startingMap(thisPlanet);

     // Direction is a normal java enum.
     directions[0] = Direction.North;
     directions[1] = Direction.Northeast;
     directions[2] = Direction.East;
     directions[3] = Direction.Southeast;
     directions[4] = Direction.South;
     directions[5] = Direction.Southwest;
     directions[6] = Direction.West;
     directions[7] = Direction.Northwest;
     MapLocation buildLoc = null;
     myTeam = gc.team();
     if (myTeam.equals(Team.Blue)) {
    	 	opponentTeam = Team.Red;
     } else {
    	 	opponentTeam = Team.Blue;
     }
     
     // research code
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Rocket);
     gc.queueResearch(UnitType.Knight);
     gc.queueResearch(UnitType.Knight);
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Worker);
     gc.queueResearch(UnitType.Rocket);
     gc.queueResearch(UnitType.Worker);

     // karbonite finding stuff
     
     //map dimensions
     int h = (int) planetMap.getHeight();
     int w = (int) planetMap.getWidth();
     
     
     EarthDeposit[][] karboniteAmts = new EarthDeposit[w][h];
     //loads matrix of earth deposits that have an earth deposit
     for(int i = 0; i < w; i++){
     	for(int j = 0; j < h; j++){
     		MapLocation loca = new MapLocation(thisPlanet, j, i);
     		long x = (int) planetMap.initialKarboniteAt(loca);
     		if(x != 0){
     			karboniteAmts[i][j] = new EarthDeposit(loca, x);
     		}
     		else{
     			karboniteAmts[i][j] = new EarthDeposit(loca, 0);
     		}
     	}
     }
     
     while (true) {
    	 	 int roundNum = (int) gc.round();
         System.out.println("Current round: "+roundNum);
         // VecUnit is a class that you can think of as similar to ArrayList<Unit>, but immutable.
         VecUnit units = gc.myUnits();
         // set initial base
         if (roundNum < 10 && buildLoc == null && units.size() != 0) {
        	 	MapLocation initialLoc = units.get(0).location().mapLocation();
        	 	buildLoc = findOpenAdjacentSpot(gc, initialLoc);
         }
         ArrayList<Unit> workers = new ArrayList<Unit>();
         ArrayList<Unit> factories = new ArrayList<Unit>();
         ArrayList<Unit> knights = new ArrayList<Unit>();
         ArrayList<Unit> rockets = new ArrayList<Unit>();
         for (int i = 0; i < units.size(); i++) {
             Unit unit = units.get(i);
             
     	 	
             switch (unit.unitType()) {
             case Factory:
            	 		factories.add(unit);
             		break;
             case Healer:
             		runHealer();
             		break;
             case Knight:
             		knights.add(unit);
             		break;
             case Mage:
             		runMage();
             		break;
             case Ranger:
             		runRanger();
             		break;
             case Rocket:
             		rockets.add(unit);
             		runRocket(gc, unit);
             		break;
             case Worker:
            	 		MapLocation myLoc = unit.location().mapLocation();
            	 		int visionRange = (int) unit.visionRange();
            	 		VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, visionRange, opponentTeam);
            	 		if (enemies.size() > 0) {
            	 			Unit enemy = findClosestEnemy(gc, unit, enemies);
                	 		if (swarmLoc == null) {
                	 			swarmLoc = enemy.location().mapLocation();
                	 		}
            	 		}
            	 		workers.add(unit);
             		break;
             }

             // Most methods on gc take unit IDs, instead of the unit objects themselves.
//             Direction random = directions[(int) (Math.random() * 8 + 1)];
//             if (gc.isMoveReady(unit.id()) && gc.canMove(unit.id(), random)) {
//                 gc.moveRobot(unit.id(), random);
//             }
         }
         
         // worker code
         int buildTeamSize = 4;
         // loop through workers
         for (int i = 0; i < workers.size(); i++) {
        	 	Unit worker = workers.get(i);
 	 		
 	 	
        	 	if (workers.size() >= buildTeamSize && i < buildTeamSize) {
        	 		UnitType buildType = null;
        	 		int size = 0;
        	 		boolean areBuilding = true;
        	 		if (factories.size() < 3 || !finishedFactories) {
        	 			buildType = UnitType.Factory;
        	 			size = factories.size();
        	 		} else if (roundNum > 125 && (rockets.size() == 0 || !finishedRockets)) {
        	 			buildType = UnitType.Rocket;
        	 			size = rockets.size();
        	 		} else {
        	 			areBuilding = false;
        	 			runIdle(gc, worker);
        	 		}
        	 		
        	 		// if building, run build sequences
        	 		if (areBuilding) {
        	 			switch (i) {
            	 		case 0:
            	 			runBuildSequence(gc, worker, buildLoc, buildType, size);
            	 		case 1:
            	 			runBuildSequence(gc, worker, buildLoc, buildType, size);
            	 		case 2:
            	 			runBuildSequence(gc, worker, buildLoc, buildType, size);
            	 		case 3:
            	 			runBuildSequence(gc, worker, buildLoc, buildType, size);
            	 		}
        	 		}
        	 	} else {
        	 		if (workers.size() < 4) {
        	 			produceWorkers(gc, worker);
        	 		}
        	 		if (workers.size() < 10 && factories.size() == 3) {
        	 			produceWorkers(gc, worker);
        	 		} else {
        	 		// leon code
                	 	
                	 	int m = 0;
                 	int n = 0;
                 	MapLocation playerLocation = worker.location().mapLocation();
                 	for(int a = 0; a < w; a++){
                 		for(int b = 0; b < h; b++){
                 			if(gc.round() > 100 && gc.round()%10 == 0 && gc.canSenseLocation(karboniteAmts[a][b].getLoc())){
                 				karboniteAmts[a][b].changeCount(gc.karboniteAt(karboniteAmts[a][b].getLoc()));
                 			}
                 			if(karboniteAmts[a][b].getCount() != 0){
                 				if(karboniteAmts[m][n].getValue(playerLocation) > karboniteAmts[a][b].getValue(playerLocation)){
                 					m = a;
                 					n = b;
                 				}
                 			}
                 		}
                 	}
                
                 	
                 	
                 	if(karboniteAmts[m][n].getLoc().equals(worker.location().mapLocation())){
                 		if(gc.canHarvest(worker.id(), Direction.Center)){
                 			System.out.println("Harvested!" + gc.karbonite() + " " + gc.karboniteAt(karboniteAmts[m][n].getLoc()));
                 			gc.harvest(worker.id(), Direction.Center);
                 			karboniteAmts[m][n].changeCount(gc.karboniteAt(karboniteAmts[m][n].getLoc()));
                 		}
                 	}
            
                 	else{
                 		moveToLoc(gc, worker, karboniteAmts[m][n].getLoc());
                 	}
        	 		}
        	 	}
         }
         
         // knight code
         for (int i = 0; i < knights.size(); i++) {
        	 	Unit knight = knights.get(i);
        	 	if (!knight.location().isInGarrison() && !knight.location().isInSpace()) {
        	 		MapLocation myLoc = knight.location().mapLocation();
        	 		if (i < 5 && rockets.size() > 0) {
        	 			MapLocation rocketLoc = rockets.get(0).location().mapLocation();
        	 			if (myLoc.isAdjacentTo(rocketLoc)) {
        	 				moveToLoc(gc, knight, rockets.get(0).location().mapLocation());
        	 			}
        	 		} else {
        	 			int visionRange = (int) knight.visionRange();
                	 	VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, visionRange, opponentTeam);
                	 	MapLocation attackLoc = swarmLoc;
                	 	
                	 	// if reached target, stop swarming
            	 		if (swarmLoc != null && myLoc.equals(swarmLoc)) {
            	 			swarmLoc = null;
            	 		}
            	 		
                	 	if (enemies.size() > 0) {
                	 		// find closest enemy
                	 		Unit closestEnemy = findClosestEnemy(gc, knight, enemies);
                	 		long dist = myLoc.distanceSquaredTo(closestEnemy.location().mapLocation());
                	 		
                	 		// attack closest enemy
                	 		attackLoc = closestEnemy.location().mapLocation();
                	 		if (dist == 1 && gc.isAttackReady(knight.id()) && gc.canAttack(knight.id(), closestEnemy.id())) {
                	 			gc.attack(knight.id(), closestEnemy.id());
                	 		}
                	 		
                	 		if (swarmLoc == null) {
                	 			swarmLoc = attackLoc;
                	 		}
                	 	}
                	 	if (attackLoc == null) {
                	 		runIdle(gc, knight);
                	 	} else {
                	 		moveToLoc(gc, knight, attackLoc);
                	 	}
        	 		}
        	 	}
         }
         
         // factory code
         if (roundNum > 125 && rockets.size() == 0) {
        	 	runFactories(gc, factories, 5);
         } else {
        	 	runFactories(gc, factories, 1);
         }
         
         // Submit the actions we've done, and wait for our next turn.
         gc.nextTurn();
     }
 }
	
	private static Unit findClosestEnemy(GameController gc, Unit unit, VecUnit enemies) {
		Unit closestEnemy = enemies.get(0);
		MapLocation myLoc = unit.location().mapLocation();
 		long shortestDist = myLoc.distanceSquaredTo(closestEnemy.location().mapLocation());
 		for (int j = 1; j < enemies.size(); j++) {
 			Unit enemy = enemies.get(j);
 			long dist = myLoc.distanceSquaredTo(enemy.location().mapLocation());
 			if (dist < shortestDist) {
 				shortestDist = dist;
 				closestEnemy = enemy;
 			}
 		}
 		return closestEnemy;
	}

	private static void runBuildSequence(GameController gc, Unit worker, MapLocation buildLoc, UnitType buildType, int builtNum) {
 		MapLocation myLoc = worker.location().mapLocation();
 		if (myLoc.isAdjacentTo(buildLoc)) {
 			Direction buildDir = myLoc.directionTo(buildLoc);
 			// if no blueprint, then put one there
 			// else if there is a blueprint, work on blueprint
 			if (gc.canBlueprint(worker.id(), buildType, buildDir)) {
 				gc.blueprint(worker.id(), buildType, buildDir);
 				if (buildType.equals(UnitType.Factory)) {
 					finishedFactories = false;
 				}
 				if (buildType.equals(UnitType.Rocket)) {
 					finishedRockets = false;
 				}
 			} else if (gc.hasUnitAtLocation(buildLoc)){
 				Unit blueprint = gc.senseUnitAtLocation(buildLoc);
 				// if can build blueprint, then do so
 				// if done, then move on to next factory
 				if (gc.canBuild(worker.id(), blueprint.id())) {
 					gc.build(worker.id(), blueprint.id());
 				}
 				if (blueprint.health() == blueprint.maxHealth()) {
 					System.out.println("Move to next building");
 					MapLocation possibleLoc = null;
 					if (buildType.equals(UnitType.Factory)) {
 						possibleLoc = findOpenAdjacentSpot(gc, myLoc);
 					} else {
 						possibleLoc = findFarAwaySpot(gc, myLoc);
 					}
 					if (possibleLoc != null) {
 						buildLoc.setX(possibleLoc.getX());
 						buildLoc.setY(possibleLoc.getY());
 					}
 					// stop building factories
 					if (builtNum == 3 && buildType.equals(UnitType.Factory)) {
 						finishedFactories = true;
 					}
 					// stop building rockets
 					if (builtNum == 1 && buildType.equals(UnitType.Rocket)) {
 						finishedRockets = true;
 					}
 				}
 			}
 		} else {
 			// move towards build location
 	 		moveToLoc(gc, worker, buildLoc);
 		}
	}

	private static MapLocation findFarAwaySpot(GameController gc, MapLocation myLoc) {
		MapLocation possibleLoc = null;
		boolean foundOpenSpace = false;
		int distance = 3;
		while (!foundOpenSpace && distance < 6) {
			for (int i = 0; i < directions.length; i++) {
				MapLocation newLoc = myLoc.addMultiple(directions[i], distance);
				if (planetMap.onMap(newLoc) && gc.isOccupiable(newLoc) == 1) {
					possibleLoc = newLoc;
					foundOpenSpace = true;
				}
			}
			distance++;
		}
		return possibleLoc;
	}

	private static MapLocation findOpenAdjacentSpot(GameController gc, MapLocation myLoc) {
		MapLocation returnLoc = null;
		boolean dirFound = false;
		int counter = 0;
		while (!dirFound && counter < 8) {
			MapLocation possibleLoc = myLoc.add(directions[counter]);
			if (planetMap.onMap(possibleLoc) && gc.isOccupiable(possibleLoc) == 1) {
				dirFound = true;
				returnLoc = possibleLoc;
			}
			counter++;
		}
		return returnLoc;
	}

	private static void moveToLoc(GameController gc, Unit unit, MapLocation targetLoc) {
 		// get current location and target direction
		MapLocation myLoc = unit.location().mapLocation();
 		Direction targetDir = myLoc.directionTo(targetLoc);
 		
 		// if unit can move to target location then it does
 		if (gc.isMoveReady(unit.id())) {
 			if (gc.canMove(unit.id(), targetDir)) {
 				gc.moveRobot(unit.id(), targetDir);
 			} else {
 				// finds position in directions array
 				int position = 0;
 				for (int i = 0; i < directions.length; i++) {
 					if (directions[i].equals(targetDir)) {
 						position = i;
 					}
 				}
 				
 				Direction moveDir = null;
 				// rotate left to find viable direction
 				int counter = 1;
 				while (moveDir == null && counter < 4) {
 					int dirPos = position - counter;
 					if (dirPos < 0) {
 						dirPos += 8;
 					}
 					if (gc.canMove(unit.id(), directions[dirPos])) {
 						moveDir = directions[dirPos];
 					}
 					counter++;
 				}
 				
 				// rotate right to find viable direction
 				counter = 1;
 				while (moveDir == null && counter < 4) {
 					int dirPos = position + counter;
 					if (dirPos > 7) {
 						dirPos -= 8;
 					}
 					if (gc.canMove(unit.id(), directions[dirPos])) {
 						moveDir = directions[dirPos];
 					}
 					counter++;
 				}
 				
 				// if position found, move there
 				if (moveDir != null) {
 					gc.moveRobot(unit.id(), moveDir);
 				}
 			}
 		}
	}

	private static void runIdle(GameController gc, Unit unit) {
 		MapLocation myLoc = unit.location().mapLocation();
 		Direction random = directions[(int) (Math.random() * 8)];
 		//move randomly
 		moveToLoc(gc, unit, myLoc.add(random));
	}


	private static void produceWorkers(GameController gc, Unit worker) {
 		Direction random = directions[(int) (Math.random() * 8)];
 		//replicates worker in random direction
 		if(gc.karbonite() >= 15 && gc.canReplicate(worker.id(), random)) {
 			gc.replicate(worker.id(), random);
 		}
	}

	private static void runRocket(GameController gc, Unit rocket) {
		MapLocation myLoc = rocket.location().mapLocation();
		VecUnitID garrison = rocket.structureGarrison();
		if (rocket.rocketIsUsed() == 0) {
			VecUnit adjacentUnits = gc.senseNearbyUnitsByType(myLoc, 1, UnitType.Knight);
			if (adjacentUnits.size() > 0) {
				for (int i = 0; i < adjacentUnits.size(); i++) {
					Unit knight = adjacentUnits.get(i);
					if (knight.team().equals(myTeam) && gc.canLoad(rocket.id(), knight.id()) && gc.isMoveReady(knight.id())) {
						gc.load(rocket.id(), knight.id());
						System.out.println("Successfully loaded a knight");
					}
				}
			}
			
			// decide when to launch rockets
			findLandLoc(gc);
			if (gc.canLaunchRocket(rocket.id(), landLoc) && garrison.size() == 8) {
				gc.launchRocket(rocket.id(), landLoc);
			}
		} else {
			for (int i = 0; i < garrison.size(); i++) {
				int counter = 0;
				boolean unloaded = false;
				while (!unloaded && counter < 8) {
					if (gc.canUnload(rocket.id(), directions[counter])) {
						gc.unload(rocket.id(), directions[counter]);
						unloaded = true;
					}
					counter++;
				}
			}
		}
	}

	private static void findLandLoc(GameController gc) {
		if (planetMap.onMap(landLoc) && gc.isOccupiable(landLoc) != 1) {
			landLoc = findFarAwaySpot(gc, landLoc);
		}
		if (landLoc == null) {
			landLoc = new MapLocation(Planet.Mars, (int) (Math.random() * 10), (int) (Math.random() * 10));
		}
		
	}

	private static void runRanger() {
		// TODO Auto-generated method stub
		
	}

	private static void runMage() {
		// TODO Auto-generated method stub
		
	}

	private static void runHealer() {
		// TODO Auto-generated method stub
		
	}

	private static void runFactories(GameController gc, ArrayList<Unit> factories, int slowDownRate) {
		Direction random = directions[(int) (Math.random() * 8)];
		if (factories.size() == 3) {
			for (Unit factory : factories) {
				if (gc.canProduceRobot(factory.id(), UnitType.Knight) && (int) (Math.random() * slowDownRate) == 0) {
					// if no rockets, slow production down
					gc.produceRobot(factory.id(), UnitType.Knight);
				}
				if (gc.canUnload(factory.id(), random)) {
					gc.unload(factory.id(), random);
				}
			}
		}
	}
}

//import the API.
//See xxx for the javadocs.
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;

import bc.*;

public class Player {
	public static Direction[] directions = new Direction[8];
	public static boolean finishedFactories = false;
	public static boolean finishedRockets = false;
	public static MapLocation swarmLoc;
	public static Team myTeam;
	public static Team opponentTeam;
	public static PlanetMap earthMap;
	public static Planet thisPlanet;
	public static PlanetMap marsMap;
	public static PlanetMap thisMap;
	public static MapLocation landLoc = new MapLocation(Planet.Mars, 1, 1);
	public static int buildTeamSize;
	public static long totalKarboniteAmount;
	public static MapLocation buildLoc = null;
	public static int troopSize;
	public static int workforceSize;

	// pathfinding static variables
	public static Direction[][] spreadPathfindingMapEarthSwarm;
	public static Direction[][] spreadPathfindingMapMarsSwarm;
	private static Direction[][] spreadPathfindingMapEarthBuildLoc;

	//can make robotDirections a class variable
	public static HashMap<Integer, Integer> robotDirections = new HashMap<Integer, Integer>();
	//directions already is a class variable, directions is the movement shit

	//make maplocation dootadoot for saving locations, checking if things get stuck and such
	public static HashMap<Integer, MapLocation> robotChecker = new HashMap<Integer, MapLocation>();

	// production chances
	public static int knightFactoryEarlyChance;
	public static int rangerFactoryEarlyChance;
	public static int mageFactoryEarlyChance;

	public static void main(String[] args) {

		// Connect to the manager, starting the game
		GameController gc = new GameController();

		// get planet and planet map
		thisPlanet = gc.planet();
		earthMap = gc.startingMap(Planet.Earth);
		marsMap = gc.startingMap(Planet.Mars);
		thisMap = gc.startingMap(thisPlanet);

		// don't build if on mars
		if (thisPlanet.equals(Planet.Mars)) {
			buildTeamSize = 0;
		} else {
			buildTeamSize = 4;
		}

		// Direction is a normal java enum.
		directions[0] = Direction.North;
		directions[1] = Direction.Northeast;
		directions[2] = Direction.East;
		directions[3] = Direction.Southeast;
		directions[4] = Direction.South;
		directions[5] = Direction.Southwest;
		directions[6] = Direction.West;
		directions[7] = Direction.Northwest;
		myTeam = gc.team();
		if (myTeam.equals(Team.Blue)) {
			opponentTeam = Team.Red;
		} else {
			opponentTeam = Team.Blue;
		}

		// karbonite finding stuff

		//map dimensions
		int h, w;
		if(thisPlanet.equals(Planet.Earth)){
			h = (int) earthMap.getHeight();
			w = (int) earthMap.getWidth();
		}
		else{
			h = (int) marsMap.getHeight();
			w = (int) marsMap.getWidth();
		}

		EarthDeposit[][] karboniteAmts = new EarthDeposit[w][h];
		//loads matrix of earth deposits that have an earth deposit
		for(int i = 0; i < w; i++){
			for(int j = 0; j < h; j++){
				MapLocation loca = new MapLocation(thisPlanet, i, j);
				long x;
				if(thisPlanet.equals(Planet.Earth)){
					x = earthMap.initialKarboniteAt(loca);
				}
				else{
					x = marsMap.initialKarboniteAt(loca);
				}
				if(x != 0){
					karboniteAmts[i][j] = new EarthDeposit(loca, x);
				}
				else{
					karboniteAmts[i][j] = new EarthDeposit(loca, 0);
				}
				totalKarboniteAmount += x;
			}


		}

		// research code
		gc.queueResearch(UnitType.Worker);
		// round 25
		
		if (h * w <= 1000) {
			if (h * w <= 500) {
				knightFactoryEarlyChance = 4;
				rangerFactoryEarlyChance = 10;
			} else {
				knightFactoryEarlyChance = 2;
				rangerFactoryEarlyChance = 10;
			}
			
			gc.queueResearch(UnitType.Ranger);
			// round 50
			gc.queueResearch(UnitType.Knight);
			// round 75
			gc.queueResearch(UnitType.Ranger);
			// round 175
			gc.queueResearch(UnitType.Knight);
			// round 250
			gc.queueResearch(UnitType.Rocket);
			// round 300
			gc.queueResearch(UnitType.Mage);
			// round 325
			gc.queueResearch(UnitType.Knight);
			// round 425 JAVELIN UNLOCKED
			gc.queueResearch(UnitType.Mage);
			// round 500
			gc.queueResearch(UnitType.Rocket);
			//round 600
			gc.queueResearch(UnitType.Mage);
			// round 700
		} else {
			knightFactoryEarlyChance = 1;
			rangerFactoryEarlyChance = 10;
			gc.queueResearch(UnitType.Ranger);
			// round 50
			gc.queueResearch(UnitType.Ranger);
			// round 150
			gc.queueResearch(UnitType.Knight);
			// round 175
			gc.queueResearch(UnitType.Knight);
			// round 250
			gc.queueResearch(UnitType.Rocket);
			// round 300
			gc.queueResearch(UnitType.Mage);
			// round 325
			gc.queueResearch(UnitType.Knight);
			// round 425 JAVELIN UNLOCKED
			gc.queueResearch(UnitType.Mage);
			// round 500
			gc.queueResearch(UnitType.Rocket);
			//round 600
			gc.queueResearch(UnitType.Mage);
			// round 700
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
				spreadPathfindingMapEarthBuildLoc = updatePathfindingMap(buildLoc, earthMap);
			}
			ArrayList<Unit> workers = new ArrayList<Unit>();
			ArrayList<Unit> factories = new ArrayList<Unit>();
			ArrayList<Unit> knights = new ArrayList<Unit>();
			ArrayList<Unit> mages = new ArrayList<Unit>();
			ArrayList<Unit> rangers = new ArrayList<Unit>();
			ArrayList<Unit> rockets = new ArrayList<Unit>();
			for (int i = 0; i < units.size(); i++) {
				Unit unit = units.get(i);

				if (!unit.location().isInSpace() && !unit.location().isInGarrison()) {
					switch (unit.unitType()) {
					case Factory:
						factories.add(unit);
						break;
					case Healer:
						runHealer(unit, rockets, gc, units);
						break;
					case Knight:
						knights.add(unit);
						break;
					case Mage:
						mages.add(unit);
						break;
					case Ranger:
						rangers.add(unit);
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
								if (thisPlanet.equals(Planet.Earth)) {
									spreadPathfindingMapEarthSwarm = updatePathfindingMap(swarmLoc, thisMap);
								} else {
									spreadPathfindingMapMarsSwarm = updatePathfindingMap(swarmLoc, thisMap);
								}
							}
							if (gc.isMoveReady(unit.id())) {
								runAwayWorker(gc, unit, enemies);
							}
						}
						workers.add(unit);
						break;
					}
				}
			}

			// keep rocket building going
			if (buildLoc == null && workers.size() > 0 && !thisPlanet.equals(Planet.Mars)) {
				buildLoc = findFarAwaySpot(gc, workers.get(0).location().mapLocation());
				if (buildLoc != null && factories.size() > 1) {
					spreadPathfindingMapEarthBuildLoc = updatePathfindingMap(buildLoc, earthMap);
				}
			}

			// get troop size and workforce size
			troopSize = knights.size() + rangers.size() + mages.size();
			workforceSize = workers.size();

			// worker code
			// loop through workers
			if (thisPlanet.equals(Planet.Earth)) {
				for (int i = 0; i < workers.size(); i++) {
					Unit worker = workers.get(i);
					if (roundNum < 5) {
						produceWorkers(gc, worker);
					}
					if (i % 3 > 0) {
						UnitType buildType = null;
						int size = 0;
						boolean areBuilding = true;
						if (factories.size() < 2 || !finishedFactories) {
							buildType = UnitType.Factory;
							size = factories.size();
						} else if (workers.size() < 5 && gc.isAttackReady(worker.id())
								&& gc.karbonite() >= 60) {
							areBuilding = false;
							produceWorkers(gc, worker);
						} else if (roundNum > 300 && (rockets.size() == 0 || !finishedRockets)) {
							buildType = UnitType.Rocket;
							size = rockets.size();
						} else {
							areBuilding = false;
							harvestKarbonite(gc, worker, karboniteAmts);
							bounceMove(worker, gc);
						}

						// if building, run build sequences
						if (areBuilding) {
							runBuildSequence(gc, worker, buildLoc, buildType, size, h);
						}
					} else {
						if (workers.size() < 4  && gc.isAttackReady(worker.id()) && gc.karbonite() >= 60) {
							produceWorkers(gc, worker);
						}
						else if ((workers.size() < h / 7 || workers.size() < 7) && factories.size() > 1
								&& gc.isAttackReady(worker.id())
								&& gc.karbonite() >= 60) {
							produceWorkers(gc, worker);
						}
						else if (rockets.size() > 0 && i % 4 == 2) {
							MapLocation myLoc = worker.location().mapLocation();
							MapLocation rocketLoc = rockets.get(0).location().mapLocation();
							if (!myLoc.isAdjacentTo(rocketLoc)) {
								moveToLoc(gc, worker, rocketLoc);
							}
						}
						else if (gc.isMoveReady(worker.id())) {
							harvestKarbonite(gc, worker, karboniteAmts);
						}
					}
				}
			} else { // on mars
				for(int i = 0; i < workers.size(); i++){
					Unit worker = workers.get(i);
					if(totalKarboniteAmount > 200 || workers.size() < 4 && gc.isAttackReady(worker.id())
							&& gc.karbonite() >= 60){
						produceWorkers(gc, worker);
					}
					if (gc.isMoveReady(worker.id())) {
						harvestKarbonite(gc, worker, karboniteAmts);
						bounceMove(worker, gc);
					}
				}
			}


			// knight code
			for (int i = 0; i < knights.size(); i++) {
				boolean goingToMars = false;
				Unit knight = knights.get(i);
				if (!knight.location().isInGarrison() && !knight.location().isInSpace()) {
					MapLocation myLoc = knight.location().mapLocation();
					if (!thisPlanet.equals(Planet.Mars) && rockets.size() > 0) {
						Unit rocket = rockets.get(0);
						MapLocation rocketLoc = rocket.location().mapLocation();
						if (myLoc.isAdjacentTo(rocketLoc)) {
							goingToMars = true;
						} else if ((myLoc.distanceSquaredTo(rocketLoc) < 64 || i < 2) && rocket.health() == rocket.maxHealth()) {
							moveToLoc(gc, knight, rocketLoc);
						}
					}
					if (!goingToMars) {
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
							if (gc.isAttackReady(knight.id())) {
								if (dist == 1 && gc.canAttack(knight.id(), closestEnemy.id())) {
									gc.attack(knight.id(), closestEnemy.id());
								} else if (knight.researchLevel() == 3) {
									if (gc.isJavelinReady(knight.id()) && gc.canJavelin(knight.id(), closestEnemy.id())) {
										gc.javelin(knight.id(), closestEnemy.id());
									}
								}
							}

							if (swarmLoc == null && attackLoc != null) {
								swarmLoc = attackLoc;
								if (thisPlanet.equals(Planet.Earth)) {
									spreadPathfindingMapEarthSwarm = updatePathfindingMap(swarmLoc, thisMap);
								} else {
									spreadPathfindingMapMarsSwarm = updatePathfindingMap(swarmLoc, thisMap);
								}
							}
						}
						if (attackLoc == null) {
							// bounce if no enemies
							bounceMove(knight, gc);
						} else if (enemies.size() > 0){
							// move towards enemy if nearby or go to swarmLoc if enough troops 
							moveToLoc(gc, knight, attackLoc);
						} else if (swarmLoc != null && (troopSize > 15 || thisPlanet.equals(Planet.Mars))) {
							moveAlongBFSPath(gc, knight);
						} else {
							bounceMove(knight, gc);
						}
					}
				}
			}

			runMage(mages, rockets, gc);
			runRanger(rangers, rockets, gc);

			// factory code
			if (roundNum > 300 && gc.karbonite() < 150 && rockets.size() == 0) {
				if (troopSize < 15 || rockets.size() > 0) {
					runFactories(gc, factories, 1);
				} else if (troopSize < 20) {
					runFactories(gc, factories, roundNum / 100);
				} else {
					runFactories(gc, factories, roundNum / 100 * (troopSize / 10));
				}
			} else {
				runFactories(gc, factories, 1);
			}

			// Submit the actions we've done, and wait for our next turn.
			gc.nextTurn();
		}
	}

	private static void runAwayWorker(GameController gc, Unit worker, VecUnit enemies) {
		boolean hasMoved = false;
		int counter = 0;
		MapLocation myLoc = worker.location().mapLocation();
		while (!hasMoved && counter < enemies.size()) {
			Unit enemy = enemies.get(counter);
			MapLocation enemyLoc = enemy.location().mapLocation();
			long dist = myLoc.distanceSquaredTo(enemyLoc);
			if ((enemy.unitType().equals(UnitType.Ranger) ||
					enemy.unitType().equals(UnitType.Knight) ||
					enemy.unitType().equals(UnitType.Mage)) &&
					dist <= enemy.attackRange()) {
				MapLocation moveLoc = myLoc.subtract(myLoc.directionTo(enemyLoc));
				if (thisMap.onMap(moveLoc) && gc.canMove(worker.id(), myLoc.directionTo(moveLoc))) {
					gc.moveRobot(worker.id(), myLoc.directionTo(moveLoc));
					hasMoved = true;
				}
			}
			counter++;
		}

	}

	private static void harvestKarbonite(GameController gc, Unit worker, EarthDeposit[][] karboniteAmts) {
		// leon code

		int m = 0;
		int n = 0;
		int h = (int) earthMap.getHeight();
		int w = (int) earthMap.getWidth();
		boolean gotSomething = false;
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
						gotSomething = true;
					}
				}
			}
		}

		if(!gotSomething){
			bounceMove(worker, gc);
		}
		else{
			boolean harvested = false;
			MapLocation karbLoc = karboniteAmts[m][n].getLoc();
			if(karbLoc.equals(worker.location().mapLocation())){
				if(gc.canHarvest(worker.id(), Direction.Center)){
					harvested = true;
					gc.harvest(worker.id(), Direction.Center);
					karboniteAmts[m][n].changeCount(gc.karboniteAt(karbLoc));
				}
			}
			if(karbLoc.isAdjacentTo(worker.location().mapLocation())){
				if(gc.canHarvest(worker.id(), worker.location().mapLocation().directionTo(karbLoc))){
					if(gc.canHarvest(worker.id(), Direction.Center)){
						harvested = true;
						gc.harvest(worker.id(), Direction.Center);
						karboniteAmts[m][n].changeCount(gc.karboniteAt(karbLoc));
					}
				}
			}

			if(!harvested){
				moveToLoc(gc, worker, karbLoc);
			}
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

	private static void runBuildSequence(GameController gc, Unit worker, MapLocation buildLoc, UnitType buildType, int builtNum, int h) {
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
					MapLocation possibleLoc = null;
					if (buildType.equals(UnitType.Factory)) {
						possibleLoc = findOpenAdjacentSpot(gc, myLoc);
					} else {
						possibleLoc = findFarAwaySpot(gc, myLoc);
					}
					if (possibleLoc != null) {
						buildLoc.setX(possibleLoc.getX());
						buildLoc.setY(possibleLoc.getY());
						spreadPathfindingMapEarthBuildLoc = updatePathfindingMap(buildLoc, earthMap);
					}
					// stop building factories
					if (builtNum == 3 && buildType.equals(UnitType.Factory)) {
						finishedFactories = true;
					}
					// stop building rockets
					if (builtNum == (1 + gc.round() / 300) && buildType.equals(UnitType.Rocket)) {
						finishedRockets = true;
					}
				}
			}
		} else {
			// move towards build location
			// if still building factories, use moveToLoc
			// if building rockets, use bfs path
			if (!finishedFactories) {
				moveAlongBFSPath(gc, worker);
			} else {
				moveAlongBFSPath(gc, worker);
			}
		}
	}

	private static MapLocation findFarAwaySpot(GameController gc, MapLocation myLoc) {
		MapLocation possibleLoc = null;
		boolean foundOpenSpace = false;
		int distance = 2;
		while (!foundOpenSpace && distance < 5) {
			for (int i = 0; i < directions.length; i++) {
				MapLocation newLoc = myLoc.addMultiple(directions[i], distance);
				if (earthMap.onMap(newLoc) && gc.isOccupiable(newLoc) == 1) {
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
			if (thisMap.onMap(possibleLoc) && gc.isOccupiable(possibleLoc) == 1) {
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
			if (gc.canMove(unit.id(), targetDir) && shouldMoveTowards(myLoc, targetDir)) {
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
				// rotate to find viable direction
				int counter = 1;
				while (moveDir == null && counter < 4) {
					int dirPos = position - counter;
					if (dirPos < 0) {
						dirPos += 8;
					}
					if (gc.canMove(unit.id(), directions[dirPos]) && shouldMoveTowards(myLoc, directions[dirPos])) {
						moveDir = directions[dirPos];
					} else {
						dirPos = position + counter;
						if (dirPos > 7) {
							dirPos -= 8;
						}
						if (gc.canMove(unit.id(), directions[dirPos]) && shouldMoveTowards(myLoc, directions[dirPos])) {
							moveDir = directions[dirPos];
						}
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

	private static boolean shouldMoveTowards(MapLocation myLoc, Direction direction) {
		boolean shouldMove = true;
		if (buildLoc != null && myLoc.add(direction).equals(buildLoc)) {
			shouldMove = false;
		}
		return shouldMove;
	}

	/**
	 * Basic tryMove based on direction. assumes move is ready (isMoveReady() waas already checked and returns true
	 * @param toMove direction to move in
	 */
	public static void tryMove(GameController gc, Unit unit, Direction toMove){
		if (gc.isMoveReady(unit.id())) {
			//if can move forwards
			if (gc.canMove(unit.id(), toMove)){
				gc.moveRobot(unit.id(), toMove);
			}
			else{
				//tests moving one direction right, then one direction left
				//than a further direction right (total 2) then a further direction left
				Direction rightRotation = bc.bcDirectionRotateRight(toMove);
				if (gc.canMove(unit.id(), rightRotation)){
					gc.moveRobot(unit.id(), rightRotation);
				}
				else {
					Direction leftRotation = bc.bcDirectionRotateLeft(toMove);
					if (gc.canMove(unit.id(), leftRotation)) {
						gc.moveRobot(unit.id(), leftRotation);
					} else {
						rightRotation = bc.bcDirectionRotateRight(rightRotation);
						if (gc.canMove(unit.id(), rightRotation)) {
							gc.moveRobot(unit.id(), rightRotation);
						} else {
							leftRotation = bc.bcDirectionRotateLeft(leftRotation);
							if (gc.canMove(unit.id(), leftRotation)) {
								gc.moveRobot(unit.id(), leftRotation);
							} else {
							}
						}
					}
				}
			}
		}
	}

	public static void moveAlongBFSPath(GameController gc, Unit unit){
		MapLocation myLoc = unit.location().mapLocation();
		Direction oppositeDir;
		if (gc.planet().equals(Planet.Earth)){
			if (unit.unitType().equals(UnitType.Worker)) {
				oppositeDir = getValueInPathfindingMap(myLoc.getX(), myLoc.getY(), spreadPathfindingMapEarthBuildLoc);
			} else {
				oppositeDir = getValueInPathfindingMap(myLoc.getX(), myLoc.getY(), spreadPathfindingMapEarthSwarm);
			}
		}
		else{
			oppositeDir = getValueInPathfindingMap(myLoc.getX(), myLoc.getY(), spreadPathfindingMapMarsSwarm);
		}
		if (oppositeDir != null) {
			moveToLoc(gc, unit, myLoc.subtract(oppositeDir));
		} else {
			bounceMove(unit, gc);
		}
	}

	/**
    @param target assumes the target location is on the same planet as the planet currently being run
     @param mapToUpdate pathfinding map to update
     @param planetMap map of planet to create pathfinding for
	 */
	public static Direction[][] updatePathfindingMap(MapLocation target, PlanetMap planetMap){
		Direction[][] currentMap;
		Planet currentPlanet = planetMap.getPlanet();

		currentMap = new Direction[(int)planetMap.getHeight()][(int)planetMap.getWidth()];

		LinkedList<MapLocation> bfsQueue = new LinkedList<MapLocation>();
		MapLocation tempLocation;
		MapLocation currentBFSLocation;
		bfsQueue.add(target);
		while (bfsQueue.size() > 0){
			//gets first item in bfsQueue
			currentBFSLocation = bfsQueue.poll();
			for (int i = 0; i < 8; i++) {
				tempLocation = currentBFSLocation.add(directions[i]);
				//only runs calculations if the added part is actually on the map...
				if ( (tempLocation.getY() >= 0 && tempLocation.getY() < currentMap.length)
						&& (tempLocation.getX() >= 0 && tempLocation.getX() < currentMap[0].length)) {
					//only runs calculation if that area of the pathfindingMap hasn't been filled in yet
					if (getValueInPathfindingMap(tempLocation.getX(), tempLocation.getY(), currentMap) == null) {
						if (planetMap.isPassableTerrainAt(new MapLocation(currentPlanet, tempLocation.getX(), tempLocation.getY())) != 0) {
							bfsQueue.add(tempLocation);
							setValueInPathfindingMap(tempLocation.getX(), tempLocation.getY(), directions[i], currentMap);
						}
					}
				}
			}
		}

		return currentMap;
	}

	public static Direction getValueInPathfindingMap(int x, int y, Direction[][] map){
		return map[map.length-1-y][x];
	}

	public static void setValueInPathfindingMap(int x, int y, Direction myDirection, Direction[][] map){
		map[map.length-1-y][x] = myDirection;
	}

	public static void bounceMove(Unit u, GameController gc){
		int id = u.id();
		if(gc.round()%20 == 0){
			MapLocation selfLocation = u.location().mapLocation();
			if(robotChecker.get(id) == null){
				robotChecker.put(id, selfLocation);
			}
			else{
				if(robotChecker.get(id).distanceSquaredTo(selfLocation) <= 25){
					robotDirections.put(id, getRandomDiagonalDirection());
				}
			}
		}


		Integer currentDir = robotDirections.get(id);
		if(currentDir == null){

			currentDir = getRandomDiagonalDirection();
			robotDirections.put(id, currentDir);
		}
		if(gc.isMoveReady(id)){
			if(gc.canMove(id, directions[currentDir])){
				gc.moveRobot(id, directions[currentDir]);
			}
			else{
				if(!moveUnit(gc, id, currentDir, 2, directions)){
					if(!moveUnit(gc, id, currentDir, 1, directions)){
						if(!moveUnit(gc, id, currentDir, 3, directions)){
							if(gc.canMove(id, directions[getNumUp(currentDir, 4)])){
								gc.moveRobot(id, directions[getNumUp(currentDir, 4)]);
								robotDirections.put(id, getNumUp(currentDir, 4));
							}
						}
					}
				}
			}
		}
	}

	public static int getRandomDiagonalDirection(){
		return ((((int)(Math.random() * 4))*2)+1);
	}

	public static int getNumUp(int i, int n){
		if(i > 7-n){
			i -= 8-n;
		}
		else{
			i += n;
		}
		return i;
	}

	public static int getNumDown(int i, int n){
		if(i < n){
			i += 8-n;
		}
		else{
			i -= n;
		}
		return i;
	}

	//directions is class variable in other thingyyyyyyyyyy
	public static boolean moveUnit(GameController gc, int id, int currentDir, int n, Direction[] directions){
		int leftNum = getNumDown(currentDir, n);
		int rightNum = getNumUp(currentDir, n);
		boolean left = gc.canMove(id, directions[leftNum]);
		boolean right = gc.canMove(id, directions[rightNum]);
		if(left && !right){
			gc.moveRobot(id, directions[leftNum]);
			determineNewDirection(n, id, leftNum, true);
			return true;
		}
		if(!left && right){
			gc.moveRobot(id, directions[rightNum]);
			determineNewDirection(n, id, rightNum, false);
			return true;
		}
		if(left && right){
			if((int)(Math.random() * 2) % 2 == 0){
				gc.moveRobot(id, directions[leftNum]);
				determineNewDirection(n, id, leftNum, true);
			}
			else{
				gc.moveRobot(id, directions[rightNum]);
				determineNewDirection(n, id, rightNum, false);
			}
			return true;
		}
		return false;
	}

	public static void determineNewDirection(int n, int id, int num, boolean left){
		int determinant = num;
		if(num%2 == 0){

			if(left){
				determinant = getNumUp(num, 1);
			}
			else{
				determinant = getNumDown(num, 1);
			}

		}
		robotDirections.put(id, determinant);
	}



	private static void produceWorkers(GameController gc, Unit worker) {
		MapLocation myLoc = worker.location().mapLocation();
		MapLocation makeLoc = findOpenAdjacentSpot(gc, myLoc);
		//replicates worker in open direction
		if(makeLoc != null && gc.karbonite() >= 30 && gc.canReplicate(worker.id(), myLoc.directionTo(makeLoc))) {
			gc.replicate(worker.id(), myLoc.directionTo(makeLoc));
		}
	}

	private static void runRocket(GameController gc, Unit rocket) {
		MapLocation myLoc = rocket.location().mapLocation();
		VecUnitID garrison = rocket.structureGarrison();
		if (rocket.rocketIsUsed() == 0) {
			VecUnit adjacentUnits = gc.senseNearbyUnitsByTeam(myLoc, 1, myTeam);
			if (adjacentUnits.size() > 0) {
				for (int i = 0; i < adjacentUnits.size(); i++) {
					Unit unit = adjacentUnits.get(i);
					if (gc.canLoad(rocket.id(), unit.id()) && gc.isMoveReady(unit.id())) {
						gc.load(rocket.id(), unit.id());
					}
				}
			}

			// decide when to launch rockets
			findLandLoc(gc);
			if (gc.canLaunchRocket(rocket.id(), landLoc) && garrison.size() == 8) {
				gc.launchRocket(rocket.id(), landLoc);
				landLoc = new MapLocation(Planet.Mars, (int) (Math.random() * marsMap.getWidth()), (int) (Math.random() * marsMap.getHeight()));

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
		if (marsMap.onMap(landLoc) && marsMap.isPassableTerrainAt(landLoc) != 1) {
			landLoc = findFarAwaySpotOnMars(gc, landLoc);
		}
		if (landLoc == null) {
			landLoc = new MapLocation(Planet.Mars, (int) (Math.random() * marsMap.getWidth()), (int) (Math.random() * marsMap.getHeight()));
		}

	}

	private static MapLocation findFarAwaySpotOnMars(GameController gc, MapLocation myLoc) {
		MapLocation possibleLoc = null;
		boolean foundOpenSpace = false;
		int distance = 3;
		while (!foundOpenSpace && distance < 6) {
			for (int i = 0; i < directions.length; i++) {
				MapLocation newLoc = myLoc.addMultiple(directions[i], distance);
				if (marsMap.onMap(newLoc) && marsMap.isPassableTerrainAt(newLoc) == 1) {
					possibleLoc = newLoc;
					foundOpenSpace = true;
				}
			}
			distance++;
		}
		return possibleLoc;
	}

	private static boolean unitToRocket(Unit unit, MapLocation myLoc, ArrayList<Unit> rockets, int count, GameController gc) {
		boolean doRocketStuff = false;

		if (!thisPlanet.equals(Planet.Mars) && rockets.size() > 0) {
			Unit rocket = rockets.get(0);
			MapLocation rocketLoc = rocket.location().mapLocation();

			if (!myLoc.isAdjacentTo(rocketLoc) && rocket.health() == rocket.maxHealth()) {
				doRocketStuff = true;
				if (myLoc.distanceSquaredTo(rocketLoc) < 64 || count < 2) {
					moveToLoc(gc, unit, rocketLoc);
				}
			}
		}

		return doRocketStuff;
	}

	private static void rangedUnitAttack(Unit unit, MapLocation myLoc, GameController gc) {
		int visionRange = (int) unit.visionRange();
		VecUnit enemies = gc.senseNearbyUnitsByTeam(myLoc, visionRange, opponentTeam);
		MapLocation attackLoc = swarmLoc;
		boolean enemyInRange = false;
		// if reached target, stop swarming
		if (swarmLoc != null && myLoc.equals(swarmLoc)) {
			swarmLoc = null;
		}

		if (enemies.size() > 0) {
			// find closest enemy
			Unit closestEnemy = findClosestEnemy(gc, unit, enemies);
			long dist = myLoc.distanceSquaredTo(closestEnemy.location().mapLocation());
			enemyInRange = dist <= unit.attackRange();

			// attack closest enemy
			attackLoc = closestEnemy.location().mapLocation();
			MapLocation moveLoc = myLoc.subtract(myLoc.directionTo(attackLoc));

			if (thisMap.onMap(moveLoc) && moveLoc.distanceSquaredTo(attackLoc) <= unit.attackRange()) {
				moveToLoc(gc, unit, moveLoc);
			}

			if (unit.unitType().equals(UnitType.Ranger)) {
				// ranger attack
				if (dist <= unit.attackRange() && !(dist <= unit.rangerCannotAttackRange())
						&& gc.isAttackReady(unit.id())
						&& gc.canAttack(unit.id(), closestEnemy.id())) {
					gc.attack(unit.id(), closestEnemy.id());
				}
			} else {
				// mage attack
				if (enemyInRange && gc.isAttackReady(unit.id()) 
						&& gc.canAttack(unit.id(), closestEnemy.id())) {
					gc.attack(unit.id(), closestEnemy.id());
				}
			}

			if (swarmLoc == null && attackLoc != null) {
				swarmLoc = attackLoc;
				if (thisPlanet.equals(Planet.Earth)) {
					spreadPathfindingMapEarthSwarm = updatePathfindingMap(swarmLoc, thisMap);
				} else {
					spreadPathfindingMapMarsSwarm = updatePathfindingMap(swarmLoc, thisMap);
				}
			}

		}
		if (attackLoc == null) {
			// bounce if no enemies
			enemyInRange = false;
			bounceMove(unit, gc);
		} else {
			if (!enemyInRange) {
				if (troopSize > 15 && swarmLoc != null) {
					// move towards swarmLoc if enough troops
					moveAlongBFSPath(gc, unit);
				} else {
					bounceMove(unit, gc);
				}
			}
		}
	}

	private static void runRanger(ArrayList<Unit> rangers, ArrayList<Unit> rockets, GameController gc) {
		// TODO copy-pasted from mage code
		for (int i = 0; i < rangers.size(); i++) {

			Unit ranger = rangers.get(i);

			if (!ranger.location().isInGarrison() && !ranger.location().isInSpace()) {

				MapLocation myLoc = ranger.location().mapLocation();

				if (!unitToRocket(ranger, myLoc, rockets, i, gc)) {

					rangedUnitAttack(ranger, myLoc, gc);

				}
			}
		}
	}

	private static void runMage(ArrayList<Unit> mages, ArrayList<Unit> rockets, GameController gc) {
		for (int i = 0; i < mages.size(); i++) {

			Unit mage = mages.get(i);

			if (!mage.location().isInGarrison() && !mage.location().isInSpace()) {

				MapLocation myLoc = mage.location().mapLocation();

				if (!unitToRocket(mage,myLoc,rockets,i,gc)) {

					rangedUnitAttack(mage, myLoc, gc);
				}
			}
		}
	}

	private static void runHealer(Unit healer, ArrayList<Unit> rockets, GameController gc, VecUnit units) {
		if(!healer.location().isInGarrison() &&  !healer.location().isInSpace()){
			ArrayList<Unit> visibleUnits = new ArrayList<Unit>();
			MapLocation myLoc = healer.location().mapLocation();
			for(int j = 0; j < units.size(); j++){
				Unit unit = units.get(j);
				if(!unit.location().isInGarrison() &&  !unit.location().isInSpace()){
					if(healer.visionRange() >= units.get(j).location().mapLocation().distanceSquaredTo(myLoc)){
						visibleUnits.add(units.get(j));
					}
				}
			}

			if(visibleUnits.size() > 0){
				boolean seeInjured = false;
				Unit priority = visibleUnits.get(0);
				double priorityVal = getHealerVal(priority, myLoc);
				for(int j = 1; j < visibleUnits.size(); j++){
					double value = getHealerVal(visibleUnits.get(j), myLoc);
					if(priorityVal > value){
						priority = visibleUnits.get(j);
						priorityVal = value;
					}
				}
				int targetId = priority.id();
				int healerId = healer.id();
				if (priority.health() != priority.maxHealth()) {
					if(gc.canHeal(healerId, targetId)){
						if(gc.isHealReady(healerId)){
							gc.heal(healerId, targetId);
						}
					}
					else{
						moveToLoc(gc, healer, priority.location().mapLocation());
					}
				}
			}
			if (gc.isMoveReady(healer.id())) {
				if(swarmLoc != null){
					moveToLoc(gc, healer, swarmLoc);
				}
				else{
					bounceMove(healer, gc);
				}
			}
		}
	}

	private static double getHealerVal(Unit u, MapLocation healerLoc){
		long i = u.location().mapLocation().distanceSquaredTo(healerLoc);
		double percentageLeft = u.health()/(double)u.maxHealth();
		if(percentageLeft == 1){
			return 10000;
		}
		return i * percentageLeft;
	}

	private static void runFactories(GameController gc, ArrayList<Unit> factories, int slowDownRate) {
		if (factories.size() >= 2) {
			for (Unit factory : factories) {
				MapLocation myLoc = factory.location().mapLocation();
				MapLocation openLoc = findOpenAdjacentSpot(gc, myLoc);
				if (openLoc != null) {
					int factoryId = factory.id();

					Direction openDir = myLoc.directionTo(openLoc);
					if (gc.canUnload(factoryId, openDir)) {
						gc.unload(factoryId, openDir);
					}
					if (workforceSize == 0 && gc.canProduceRobot(factory.id(), UnitType.Worker)) {
						gc.produceRobot(factory.id(), UnitType.Worker);
					}
					if ((int) (Math.random() * slowDownRate) == 0) {
						int random = (int) (Math.random() * 10) + 1;
						if(gc.round() < 200){
							if (random <= knightFactoryEarlyChance && gc.canProduceRobot(factoryId, UnitType.Knight)) {
								gc.produceRobot(factoryId, UnitType.Knight);
							} else if (random <= rangerFactoryEarlyChance && gc.canProduceRobot(factoryId, UnitType.Ranger)) {
								gc.produceRobot(factoryId, UnitType.Ranger);
							} else if (random <= mageFactoryEarlyChance && gc.canProduceRobot(factoryId, UnitType.Mage)) {
								gc.produceRobot(factoryId, UnitType.Mage);
							}
						}
						else{
							if (random <= 4 && gc.canProduceRobot(factoryId, UnitType.Knight)) {
								gc.produceRobot(factoryId, UnitType.Knight);
							} else if (random <= 8 && gc.canProduceRobot(factoryId, UnitType.Ranger)) {
								gc.produceRobot(factoryId, UnitType.Ranger);
							} else if (random <= 10 && gc.canProduceRobot(factoryId, UnitType.Mage)) {
								gc.produceRobot(factoryId, UnitType.Mage);
							} else if (random <= 10 && gc.canProduceRobot(factoryId, UnitType.Healer)) {
								gc.produceRobot(factoryId, UnitType.Healer);
							}
						}

					}
				}
			}
		}
	}
}

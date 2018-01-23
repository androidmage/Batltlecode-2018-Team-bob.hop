//import the API.
//See xxx for the javadocs.
import java.util.ArrayList;
import java.util.HashMap;

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
	public static MapLocation landLoc = new MapLocation(Planet.Mars, 1, 1);
	public static int buildTeamSize;
	
	//can make robotDirections a class variable
	public static HashMap<Integer, Integer> robotDirections = new HashMap<Integer, Integer>();
		//directions already is a class variable, directions is the movement shit

	//make maplocation dootadoot for saving locations, checking if things get stuck and such
	public static HashMap<Integer, MapLocation> robotChecker = new HashMap<Integer, MapLocation>();

	public static void main(String[] args) {

		// Connect to the manager, starting the game
		GameController gc = new GameController();

		// get planet and planet map
		thisPlanet = gc.planet();
		earthMap = gc.startingMap(thisPlanet);
		marsMap = gc.startingMap(Planet.Mars);

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
		MapLocation buildLoc = null;
		myTeam = gc.team();
		if (myTeam.equals(Team.Blue)) {
			opponentTeam = Team.Red;
		} else {
			opponentTeam = Team.Blue;
		}

		// research code
		gc.queueResearch(UnitType.Worker);
		//round 25
		gc.queueResearch(UnitType.Rocket);
		//round 75
		gc.queueResearch(UnitType.Knight);
		//round 100
		gc.queueResearch(UnitType.Knight);
		//round 175
		gc.queueResearch(UnitType.Worker);
		//round 250
		gc.queueResearch(UnitType.Ranger);
		//round 275
		gc.queueResearch(UnitType.Mage);
		//round 300
		gc.queueResearch(UnitType.Worker);
		//round 375
		gc.queueResearch(UnitType.Rocket);
		//round 475
		gc.queueResearch(UnitType.Worker);
		//round 550
		gc.queueResearch(UnitType.Ranger);
		//round 650
		gc.queueResearch(UnitType.Mage);
		//round 725
		gc.queueResearch(UnitType.Mage);
		//round 825

		// karbonite finding stuff

		//map dimensions
		int h = (int) earthMap.getHeight();
		int w = (int) earthMap.getWidth();


		EarthDeposit[][] karboniteAmts = new EarthDeposit[w][h];
		//loads matrix of earth deposits that have an earth deposit
		for(int i = 0; i < w; i++){
			for(int j = 0; j < h; j++){
				MapLocation loca = new MapLocation(thisPlanet, i, j);
				long x = (int) earthMap.initialKarboniteAt(loca);
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
						runHealer();
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
							}
						}
						workers.add(unit);
						break;
					}
				}
			}

			// keep rocket building going
			if (buildLoc == null && workers.size() > 0) {
				buildLoc = findFarAwaySpot(gc, workers.get(0).location().mapLocation());
			}
			// worker code
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
					} else if (workers.size() < 8) {
						areBuilding = false;
						produceWorkers(gc, worker);
					} else if (roundNum > 75 && (rockets.size() == 0 || !finishedRockets)) {
						buildType = UnitType.Rocket;
						size = rockets.size();
					} else {
						areBuilding = false;
						harvestKarbonite(gc, worker, karboniteAmts);
						bounceMove(worker, gc);
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
					else if (workers.size() < 8 && factories.size() == 3) {
						produceWorkers(gc, worker);
					}
					else if (rockets.size() > 0 && i < 2) {
						MapLocation myLoc = worker.location().mapLocation();
						MapLocation rocketLoc = rockets.get(0).location().mapLocation();
						if (!myLoc.isAdjacentTo(rocketLoc)) {
							moveToLoc(gc, worker, rocketLoc);
						}
					}
					else {
						harvestKarbonite(gc, worker, karboniteAmts);
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
						MapLocation rocketLoc = rockets.get(0).location().mapLocation();
						if (myLoc.isAdjacentTo(rocketLoc)) {
							goingToMars = true;
						} else if (myLoc.distanceSquaredTo(rocketLoc) < 64 || i < 2) {
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
							if (dist == 1 && gc.isAttackReady(knight.id()) && gc.canAttack(knight.id(), closestEnemy.id())) {
								gc.attack(knight.id(), closestEnemy.id());
							}

							if (swarmLoc == null) {
								swarmLoc = attackLoc;
							}
						}
						if (attackLoc == null) {
							bounceMove(knight, gc);
						} else {
							moveToLoc(gc, knight, attackLoc);
						}
					}
				}
			}
			
			runMage(mages, rockets, gc);
			runRanger(rangers, rockets, gc);

			// factory code
			if (roundNum > 75 && gc.karbonite() <  75 && rockets.size() == 0) {
				runFactories(gc, factories, roundNum / 50);
			} else {
				runFactories(gc, factories, 1);
			}

			// Submit the actions we've done, and wait for our next turn.
			gc.nextTurn();
		}
	}

	private static void harvestKarbonite(GameController gc, Unit worker, EarthDeposit[][] karboniteAmts) {
		// leon code

		int m = 0;
		int n = 0;
		int h = (int) earthMap.getHeight();
		int w = (int) earthMap.getWidth();
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
				gc.harvest(worker.id(), Direction.Center);
				karboniteAmts[m][n].changeCount(gc.karboniteAt(karboniteAmts[m][n].getLoc()));
			}
		}

		else{
			moveToLoc(gc, worker, karboniteAmts[m][n].getLoc());
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
				if (blueprint.unitType().equals(buildType) && blueprint.health() == blueprint.maxHealth()) {
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
					if (builtNum == (1 + gc.round() / 300) && buildType.equals(UnitType.Rocket)) {
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
			if (earthMap.onMap(possibleLoc) && gc.isOccupiable(possibleLoc) == 1) {
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
					} else {
						dirPos = position + counter;
						if (dirPos > 7) {
							dirPos -= 8;
						}
						if (gc.canMove(unit.id(), directions[dirPos])) {
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
		//replicates worker in random direction
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
			
			
			if (enemyInRange
				&& gc.isAttackReady(unit.id())
				&& gc.canAttack(unit.id(), closestEnemy.id())) {
				
				gc.attack(unit.id(), closestEnemy.id());
				
			}

			if (swarmLoc == null) {
				swarmLoc = attackLoc;
			}

		}
		if (attackLoc == null) {
			enemyInRange = false;
			bounceMove(unit, gc);
		} else {
			if (!enemyInRange)
				moveToLoc(gc, unit, attackLoc);
		}
	}
	
	private static void runRanger(ArrayList<Unit> rangers, ArrayList<Unit> rockets, GameController gc) {
		// TODO copy-pasted from mage code
		for (int i = 0; i < rangers.size(); i++) {

			Unit ranger = rangers.get(i);

			if (!ranger.location().isInGarrison() && !ranger.location().isInSpace()) {
				boolean goingToMars = false;
				MapLocation myLoc = ranger.location().mapLocation();

				if (!thisPlanet.equals(Planet.Mars) && rockets.size() > 0) {
					MapLocation rocketLoc = rockets.get(0).location().mapLocation();
					if (myLoc.isAdjacentTo(rocketLoc)) {
						goingToMars = true;
					} else if (myLoc.distanceSquaredTo(rocketLoc) < 64 || i < 2) {
						moveToLoc(gc, ranger, rocketLoc);
					}
				}
				if (!goingToMars) {
					rangedUnitAttack(ranger, myLoc, gc);
				}
			}
		}

	}

	private static void runMage(ArrayList<Unit> mages, ArrayList<Unit> rockets, GameController gc) {
		// TODO basically copy-pasted from knight code
		for (int i = 0; i < mages.size(); i++) {

			Unit mage = mages.get(i);

			if (!mage.location().isInGarrison() && !mage.location().isInSpace()) {
				boolean goingToMars = false;
				MapLocation myLoc = mage.location().mapLocation();
				if (!thisPlanet.equals(Planet.Mars) && rockets.size() > 0) {
					MapLocation rocketLoc = rockets.get(0).location().mapLocation();
					if (myLoc.isAdjacentTo(rocketLoc)) {
						goingToMars = true;
					} else if (myLoc.distanceSquaredTo(rocketLoc) < 64 || i < 2) {
						moveToLoc(gc, mage, rocketLoc);
					}
				}
				if (!goingToMars) {
					rangedUnitAttack(mage, myLoc, gc);
				}
			}
		}
	}

	private static void runHealer() {
		// TODO Auto-generated method stub

	}

	private static void runFactories(GameController gc, ArrayList<Unit> factories, int slowDownRate) {
		if (factories.size() == 3) {
			for (Unit factory : factories) {
				MapLocation myLoc = factory.location().mapLocation();
				MapLocation openLoc = findOpenAdjacentSpot(gc, myLoc);
				if (openLoc != null) {
					Direction openDir = myLoc.directionTo(openLoc);
					if (gc.canUnload(factory.id(), openDir)) {
						gc.unload(factory.id(), openDir);
					}
					if ((int) (Math.random() * slowDownRate) == 0) {
						int random = (int) (Math.random() * 10) + 1;
						if (random <= 5 && gc.canProduceRobot(factory.id(), UnitType.Knight)) {
							gc.produceRobot(factory.id(), UnitType.Knight);
						} else if (random <= 8 && gc.canProduceRobot(factory.id(), UnitType.Ranger)) {
							gc.produceRobot(factory.id(), UnitType.Ranger);
						} else if (random <= 10 && gc.canProduceRobot(factory.id(), UnitType.Mage)) {
							gc.produceRobot(factory.id(), UnitType.Mage);
						}
					}
				}
			}
		}
	}
}

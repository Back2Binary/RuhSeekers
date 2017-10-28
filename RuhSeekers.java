
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import robocode.AlphaBot;
import robocode.BattleEndedEvent;
import robocode.Bullet;
import robocode.BulletHitBulletEvent;
import robocode.BulletHitEvent;
import robocode.BulletMissedEvent;
import robocode.DeathEvent;
import robocode.HitByBulletEvent;
import robocode.HitRobotEvent;
import robocode.HitWallEvent;
import robocode.RobotDeathEvent;
import robocode.RoundEndedEvent;
import robocode.ScannedRobotEvent;
import robocode.StatusEvent;
import robocode.WinEvent;
import java.awt.geom.*;
import java.util.*;

/**
 *
 * @author Pradeep Madushan
 */
public class RuhSeekers extends AlphaBot {

	Point2D.Double myLoc, prevLoc, nextLoc;
	HashMap<String,Enemy> enemies;
	Enemy target;
	static HashMap<String,int[][][][]> statStore = new HashMap<String,int[][][][]>();
	int direction = 1;
	double perpendicularDirection = 1;
	int hits;
	
	public void run() {
	
		setBodyColor(Color.white);
		setGunColor(Color.pink);
		setRadarColor(Color.black);
		setBulletColor(Color.pink);
		setScanColor(Color.pink);
		
	
		enemies = new HashMap<String,Enemy>();
		target = null;
		Rectangle2D battlefield = new Rectangle2D.Double(50, 50, getBattleFieldWidth() - 100, getBattleFieldHeight() - 100);
		nextLoc = null;
		hits = 0;
		
		while(true) {
			myLoc = new Point2D.Double(getX(), getY());
	
			if(target == null) {
				turnRadarRight(360);
			} else {
				double radarAngle = robocode.util.Utils.normalRelativeAngleDegrees(Math.toDegrees(calcAngle(myLoc, target.loc)) - getRadarHeading());
				target = null;
				turnRadarRight(radarAngle);
				if(target == null) {
					turnRadarRight(radarAngle < 0 ? -360 - radarAngle : 360 - radarAngle);
				}
			}
			if(target != null) {
				if(getOthers() > 1) {
					if(nextLoc == null) {
						nextLoc = prevLoc = myLoc;
					}
					for(int i = 0; i < 90; i++) {
						double d = (Math.random() * 100) + 100;
						Point2D.Double p = calcPoint(myLoc, Math.toRadians(Math.random() * 360), d);
						if(battlefield.contains(p) && (calcRisk(p) < calcRisk(nextLoc))) {
							nextLoc = p;
						}
					}
				} else {
				
					double d = (Math.random() * 100) + 150;
					if(!battlefield.contains(calcPoint(myLoc, calcAngle(myLoc, target.loc) + Math.PI / 3 * perpendicularDirection, d)) || ((Math.random() * (hits % 5) > 0.6))) {
						perpendicularDirection = -perpendicularDirection;
					}
					double angle = calcAngle(myLoc, target.loc) + (Math.PI / 2) * perpendicularDirection;
					while(!battlefield.contains(calcPoint(myLoc, angle, d))) {
						angle -= perpendicularDirection * 0.1;
					}
					nextLoc = calcPoint(myLoc, angle, d);
				}
			
				double distance = myLoc.distance(nextLoc);
				double moveAngle = robocode.util.Utils.normalRelativeAngleDegrees(Math.toDegrees(calcAngle(myLoc, nextLoc)) - getHeading());
				prevLoc = myLoc;
				
				if(Math.abs(moveAngle) > 90) {
					moveAngle = robocode.util.Utils.normalRelativeAngleDegrees(moveAngle + 180);
					distance = -distance;
				}
				turnRight(moveAngle);
				ahead(distance);
			}
		}
		
//	public void onRobotDeath(RobotDeathEvent e) {
//		Enemy en = (Enemy)targets.get(e.getName());
//		en.live = false;		
//	}	
//	
        }
	public void onScannedRobot(ScannedRobotEvent e) {
	
		String name = e.getName();
		Enemy enemy;
		if(enemies.get(name) == null) {
			enemy = new Enemy(name, calcPoint(myLoc, Math.toRadians(getHeading() + e.getBearing()), e.getDistance()), e.getEnergy(), e.getBearing(), e.getHeading(), new Vector<BulletWave>());
		} else {
			enemy = new Enemy(name, calcPoint(myLoc, Math.toRadians(getHeading() + e.getBearing()), e.getDistance()), e.getEnergy(), e.getBearing(), e.getHeading(), enemies.get(name).waves);
		}
		enemies.put(name, enemy);
		
		if((target == null) || (target.name.equals(enemy.name)) || (e.getDistance() < target.loc.distance(myLoc))) {
			target = enemy;
		}
		
		int[][][][] stats = statStore.get(name.split(" ")[0]);
		if(stats == null) {
			stats = new int[2][9][13][31];
			statStore.put(name.split(" ")[0], stats);
		}
		
		double power = getOthers() > 1 ? 3 : Math.min(3, Math.max(600 / e.getDistance(), 1));
		double absoluteBearing = Math.toRadians(getHeading() + enemy.bearing);
		
		if(e.getVelocity() != 0) {
			if(Math.sin(Math.toRadians(enemy.heading) - absoluteBearing) * e.getVelocity() < 0) {
				direction = -1;
			} else {
				direction = 1;
			}
		}
		
		int[] currentStats = stats[getOthers() > 1 ? 0 : 1][(int) (e.getVelocity() == 0 ? 8 : Math.abs(Math.sin(Math.toRadians(enemy.heading) - absoluteBearing) * e.getVelocity() / 3))][(int) (e.getDistance() / 100)];
		
		BulletWave newWave = new BulletWave(myLoc, enemy.loc, absoluteBearing, power, getTime(), direction, currentStats, getTime() - 1);
		enemy.waves.add(newWave);
		for(int i = 0; i < enemy.waves.size(); i++) {
			BulletWave currentWave = enemy.waves.get(i);
			if(currentWave.waveHit(enemy.loc, getTime())) {
				enemy.waves.remove(currentWave);
				i--;
			}
		}
		
		if((enemy == target) && (power < getEnergy())) {
			int bestindex = 15;
			for(int i = 0; i < 31; i++) {
				if(currentStats[bestindex] < currentStats[i]) {
					bestindex = i;
				}
			}
			
			double guessFactor = (double)(bestindex - (currentStats.length - 1) / 2) / ((currentStats.length - 1) / 2);
			double angleOffset = direction * guessFactor * newWave.maxEscapeAngle();
			double gunAdjust = Math.toDegrees(robocode.util.Utils.normalRelativeAngle(absoluteBearing - Math.toRadians(getGunHeading()) + angleOffset));

			turnGunRight(gunAdjust);
			fire(power);
		}
	}
	
	public void onHitByBullet(HitByBulletEvent e) {
		if(getOthers() == 1) {
			hits++;
		}
	}
	
	public void onRobotDeath(RobotDeathEvent e) {
		enemies.remove(e.getName());
		if((target != null) && (target.name.equals(e.getName()))) {
			target = null;
		}
	}
	
	public void onWin(WinEvent e) {
		turnRight(15);
		while(true) {
			turnLeft(30);
			turnRight(30);
		}
	}
	
	public double calcRisk(Point2D point) {
		double risk = 0;
		Iterator<Enemy> it = enemies.values().iterator();
	
		while(it.hasNext()) {
			Enemy enemy = it.next();
			risk += (enemy.energy + 50) / point.distanceSq(enemy.loc);
		}
	
		risk += 0.1 / point.distanceSq(prevLoc);
		risk += 0.1 / point.distanceSq(myLoc);
		
		return risk;
	}
	
	public Point2D.Double calcPoint(Point2D origin, double angle, double distance) {
		return new Point2D.Double(origin.getX() + distance * Math.sin(angle), origin.getY() + distance * Math.cos(angle));
	}
	
	public double calcAngle(Point2D p, Point2D q) {
		return Math.atan2(q.getX() - p.getX(), q.getY() - p.getY());
	}

public class Enemy {
	public String name;
	public Point2D.Double loc;
	public double energy, bearing, heading;
	public Vector<BulletWave> waves;	
	


	public Enemy(String name, Point2D.Double loc, double energy, double bearing, double heading, Vector<BulletWave> waves) {
		this.name = name;
		this.loc = loc;
		this.energy = energy;
		this.bearing = bearing;
		this.heading = heading;
		this.waves = waves;
	}
	
}

public class BulletWave {
	private Point2D.Double origin, lastKnown;
	private double bearing, power;
	private long fireTime;
	private int direction;
	private int[] returnSegment;
	private long lastTime;
	
	public BulletWave(Point2D.Double location, Point2D.Double enemyLoc, double bearing, double power, long fireTime, int direction, int[] segment, long time) {
		this.origin = location;
		this.lastKnown = enemyLoc;
		this.bearing = bearing;
		this.power = power;
		this.fireTime = fireTime;
		this.direction = direction;
		this.returnSegment = segment;
		lastTime = time;
	}
	
	public double getBulletSpeed() {
		return 20 - power * 3;
	}
	
	public double maxEscapeAngle() {
		return Math.asin(8 / getBulletSpeed());
	}
	
	public boolean waveHit(Point2D.Double enemy, long time) {
		long dt = time - lastTime;
		double dx = (enemy.getX() - lastKnown.getX()) / dt;
		double dy = (enemy.getY() - lastKnown.getY()) / dt;
		
		while(lastTime < time) {
			if(origin.distance(enemy) <= (lastTime - fireTime) * getBulletSpeed()) {
				double desiredDirection = Math.atan2(enemy.getX() - origin.getX(), enemy.getY() - origin.getY());
				double angleOffset = robocode.util.Utils.normalRelativeAngle(desiredDirection - bearing);
				double guessFactor = Math.max(-1, Math.min(1, angleOffset / maxEscapeAngle())) * direction;
				int index = (int) Math.round((returnSegment.length - 1) / 2 * (guessFactor + 1));
				returnSegment[index]++;
				return true;
			}
			lastTime++;
			lastKnown = new Point2D.Double(lastKnown.getX() + dx, lastKnown.getY() + dy);
		}
		return false;
	}
}

}



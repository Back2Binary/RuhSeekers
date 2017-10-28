
import robocode.*;
import java.awt.Color;


public class RuhSeekers extends BravoBot {
	
	

	boolean peek;
	double moveAmount; 
	

	public void run() {
		
		setBodyColor(Color.black);
		setGunColor(Color.black);
		setRadarColor(Color.orange);
		setBulletColor(Color.cyan);
		setScanColor(Color.cyan);

		
		moveAmount = Math.max(getBattleFieldWidth(), getBattleFieldHeight());
		
		peek = false;

		
		turnLeft(getHeading() % 90);
		ahead(moveAmount);
		peek = true;
		turnRight(90);
		turnRight(10);

		while (true) {
		
			peek = true;
			ahead(moveAmount);
			peek = false;
			turnRight(90);
		}
	}

	
//	public void onHitRobot(HitRobotEvent e) {
//		
//		if (e.getBearing() > -90 && e.getBearing() < 90) {
//			back(100);
//		}
//		else {
//			ahead(100);
//		}
//	}

	
	public void onScannedRobot(ScannedRobotEvent e) {
		
		if(e.getDistance()<100) {
			fire(3);
		}else {
			fire(2);
		}
		
		if (peek) {
			scan();
		}
	}
	
	public void onHitRobot(HitRobotEvent e) {
		if (e.getBearing() > -10 && e.getBearing() < 10) {
			fire(3);
		}
//		if (e.isMyFault()) {
//			turnRight(10);
//		}

		
//		if (e.getEnergy() > 16) {
//			fire(3);
//		} else if (e.getEnergy() > 10) {
//			fire(2);
//		} else if (e.getEnergy() > 4) {
//			fire(1);
//		} else if (e.getEnergy() > 2) {
//			fire(.5);
//		} else if (e.getEnergy() > .4) {
//			fire(.1);
//		}
//		ahead(40); 
	}



}

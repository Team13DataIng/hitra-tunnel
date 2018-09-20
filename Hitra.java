import lejos.hardware.motor.*;
import lejos.hardware.lcd.*;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.port.Port;
import lejos.hardware.Brick;
import lejos.hardware.Sound;
import lejos.hardware.BrickFinder;
import lejos.hardware.ev3.EV3;
import lejos.hardware.Keys;
import lejos.hardware.sensor.SensorModes;
import lejos.robotics.SampleProvider;
import lejos.hardware.sensor.NXTSoundSensor;
import lejos.hardware.sensor.*;
import lejos.hardware.Button;
import java.util.*;

public class Hitra {

	// Globale E3-klasser
	private static Brick brick;
	private static Port s1;
	private static Port s2;
	private static Port s3;
	private static Port s4;
	private static EV3 ev3;
	private static TextLCD lcd;
	private static Keys keys;
	private static EV3ColorSensor fargeSensor;
	private static SampleProvider fargeLeser;
	private static float[] fargeSample;
	private static SampleProvider lydSensor;
	private static float[] lydSample;
	private static SampleProvider trykksensor;
	private static float[] trykkSample;
	private static SampleProvider trykksensor2;
	private static float[] trykkSample2;

	// Andre globale variabler
	private static int svart;
	private static Random rand;

	// Konstanter
	private static final int SPEED = 450;
	private static final int BRUSH_SPEED = 1800;
	private static final int ANT_RUNDER = 8;
	private static final double MAKS_LYD = 0.7;

	public static void main(String[] args) {
		// Oppsett av robotens komponenter
		brick = BrickFinder.getDefault();		//Laster inn EV3-brikken

		// Porter
		s1 = brick.getPort("S1");		// Lydsensor
		s2 = brick.getPort("S2");		// Trykksensor 1
		s3 = brick.getPort("S3");		// Trykksensor 2
		s4 = brick.getPort("S4");		// Fargesensor

		ev3 = (EV3)BrickFinder.getLocal();
		lcd = ev3.getTextLCD();
		keys = ev3.getKeys();

		lydSensor = new NXTSoundSensor(s1);
		trykksensor = new EV3TouchSensor(s2);
		trykksensor2 = new EV3TouchSensor(s3);
		fargeSensor = new EV3ColorSensor(s4);

		// Definer random
		rand = new Random();

		// Sett opp en definisjon av svart
		definerSvart();

		// Sett motorfart
		Motor.A.setSpeed(SPEED);
		Motor.B.setSpeed(SPEED);
		Motor.C.setSpeed(BRUSH_SPEED);

		for(int i = 0; i < ANT_RUNDER; i++) {
			System.out.println("Runde " + i);
			kjor();
		}
	}

	private static void kjor() {
		// Start motoren
		startMotor(false);

		boolean fortsett = true;

		while (true) {
			// TODO: Sjekk trykk
			int retning = sjekkTrykk();
			if (retning != 0) {
				System.out.println("Traff hindring!");
				startMotor(true);
				vent(1000);
				sving((rand.nextInt((600 - 100) + 1) + 100), retning);
				break;
			}

			// TODO: Sjekk farge
			if (skannSvart()) {
				startMotor(false);
				vent(1000);
				sving(1080, 1);
				break;
			}
			// TODO: Sjekk lyd
			if (sjekkLyd()) {
				stoppMotor(3000);
				break;
			}
		}
	}

	// Svinger til et bestemt antall grader. Retning: 1 = venstre, -1 = høyre
	private static void sving(int grader, int retning) {
		if (retning < -1 || retning > 1)
			throw new IllegalArgumentException("Retning må være definert som -1 eller 1.");

		grader = grader * retning;
		Motor.B.stop();
		Motor.A.rotate(grader);
		//while (Motor.A.isMoving()) Motor.B.rotate(-grader);
	}

	private static void startMotor(boolean revers) {
		if (!revers) {
			Motor.A.backward();
			Motor.B.backward();
		}
		else {
			Motor.A.forward();
			Motor.B.forward();
		}

		Motor.C.forward();
	}

	private static void stoppMotor(int varighet) {
		if (varighet < 0)
			throw new IllegalArgumentException("Varighet kan ikke være i minus.");

		Motor.A.stop();
		Motor.B.stop();
		Motor.C.stop();

		try {
			Thread.sleep(varighet);
		}
		catch(InterruptedException e) {
		   	System.out.println("thread 2 interrupted");
        }
	}

	// Returnerer 0 for ingen, 1 for venstre og -1 for høyre
	private static int sjekkTrykk() {
		trykkSample = new float[trykksensor.sampleSize()];
		trykkSample2 = new float[trykksensor2.sampleSize()];

		if (trykkSample2 != null && trykkSample2.length > 0){
			trykksensor2.fetchSample(trykkSample2, 0);
			if (trykkSample2[0] > 0){
				return -1;
			}
		}
		if (trykkSample != null && trykkSample.length > 0){
			trykksensor.fetchSample(trykkSample, 0);
			if (trykkSample[0] > 0){
				return 1;
			}
		}

		return 0;
	}

	private static void vent(int varighet) {
		if (varighet < 0)
			throw new IllegalArgumentException("Varighet kan ikke være minus.");

		try {
			Thread.sleep(varighet);
		}
		catch(InterruptedException e) {
		   	System.out.println("thread 2 interrupted");
        }
	}

	private static boolean sjekkLyd() {
		lydSample = new float[lydSensor.sampleSize()];

		if(lydSample != null && lydSample.length > 0) {
			lydSensor.fetchSample(lydSample, 0);
			if (lydSample[0] > MAKS_LYD) {
				System.out.println("Lyd over " + MAKS_LYD + ": " + lydSample[0]);
				return true;
			}
		}

		return false;
	}

	// Finner en definisjon for 'svart' farge
	private static void definerSvart() {
		fargeLeser = fargeSensor.getMode("RGB");
		fargeSample = new float[fargeLeser.sampleSize()];
		svart = 0;

		for(int i = 0; i < 100; i++) {
			fargeLeser.fetchSample(fargeSample, 0);
			svart += fargeSample[0] * 100;
		}

		svart = svart / 100 + 5;
	}

	public static boolean skannSvart() {
		fargeLeser.fetchSample(fargeSample, 0);
		
		if (fargeSample[0]*100 > 5) {
			// TODO: Følg svart linje
			
			return false;
		} else {
			System.out.println("Fant svart linje!");
			return true;
		}
	}
}
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

public class Hitra
{
	// Globale variabler for aa kunne hente dem i kjor og setup
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
	private static int runde;
	private static int svart;

	private static Random rand;

	public static void main (String[] args)  throws Exception {

		// Oppsett av robotens komponenter
		brick = BrickFinder.getDefault();
		s1 = brick.getPort("S1"); // Lydsensor
		s2 = brick.getPort("S2"); // Trykksensor 1 
		s3 = brick.getPort("S3"); // Trykksensor 2
		s4 = brick.getPort("S4"); // Fargesensor
		ev3 = (EV3) BrickFinder.getLocal();
		lcd = ev3.getTextLCD();
		keys = ev3.getKeys();
		lydSensor = new NXTSoundSensor(s1);
		trykksensor = new EV3TouchSensor(s2);
		trykksensor2 = new EV3TouchSensor(s3);
		fargeSensor = new EV3ColorSensor(s4);

		rand = new Random();
		
		// Antall ganger roboten skal krasje foer den stopper
		runde = 2;

		// Sett opp en definisjon av svart
		fargeLeser = fargeSensor.getMode("RGB");
		fargeSample = new float[fargeLeser.sampleSize()];
		svart = 0;
		for(int i = 0; i < 100; i++) {
			fargeLeser.fetchSample(fargeSample, 0);
			svart += fargeSample[0] * 100;
		}

		svart = svart / 100 + 5;

		// Motorfart
	 	Motor.A.setSpeed(900);
	 	Motor.B.setSpeed(900);
	 	Motor.C.setSpeed(450);
	 	// Kjoer framover
		kjor();
	}

	public static void kjor() {
		lydSample = new float[lydSensor.sampleSize()];
		trykkSample = new float[trykksensor.sampleSize()];
		trykkSample2 = new float[trykksensor2.sampleSize()];

		System.out.println("Kjorer til trykk");
		Motor.A.backward();
		Motor.B.backward();
		Motor.C.forward();
		boolean fortsett  = true;
		int ret = 0;
		boolean lydStopp = false;
		boolean svartStopp = false;
		boolean sensorSving = false;

		while(fortsett) {
			lydStopp = false;
			sensorSving = false;
			svartStopp = false;
			// Dersom trykksensoren registrerer trykk, startes en prosess der bilen stoppes, rygger og svinger
			if (trykkSample2 != null && trykkSample2.length > 0){
				trykksensor2.fetchSample(trykkSample2, 0);
				if (trykkSample2[0] > 0){
					//System.out.println("Hindring2");
					fortsett = false;
					ret = 0;
					lydStopp = false;
				}
			}
			if (trykkSample != null && trykkSample.length > 0){
				trykksensor.fetchSample(trykkSample, 0);
				if (trykkSample[0] > 0){
					//System.out.println("Hindring");
					fortsett = false;
					ret = 1;
					lydStopp = false;
				}
			}
			// Sjekker om fargeleseren returnerer svart farge
			if (skannSvart()) {
				fortsett = false;
				svartStopp = true;
			}
			// Stopper ved en hoey lyd
			if(lydSample != null && lydSample.length > 0) {
				lydSensor.fetchSample(lydSample, 0);
				if (lydSample[0] > 0.7) {
					System.out.println("Lyd over 0.7: " + lydSample[0]);
					fortsett = false;
					lydStopp = true;
				}
			}
		}
		stopp();

		// Kjoer metodene rygg og sving samtidig

		if(svartStopp){
			rygg();
			ret =0;
			int deg = 1300;
			sving(ret, deg);
			kjor();
		}
		else if (!lydStopp) {
			rygg();
			int deg = rand.nextInt((600 - 100) + 1) + 100;
			sving(ret, deg);

			runde--;
			if(runde>0){
		 		kjor();
	 		}
		}
		else  {
			try {
				Thread.sleep(3000);
			}
			catch (InterruptedException e) {
				System.out.println("En feil skjedde.");
			}
			kjor();
		}
	}

	// Stopper begge hjulmotorene
	public static void stopp(){
		Motor.A.stop();
		Motor.B.stop();
		Motor.C.stop();
	}

	// Faar bilen til aa rygge
	public static void rygg(){
		Motor.A.forward();
		Motor.B.forward();
		try {
			Thread.sleep(1000);
		}
		catch(InterruptedException e){
		   	System.out.println("thread 2 interrupted");
        }
	}

	public static boolean skannSvart() {
		fargeLeser.fetchSample(fargeSample, 0);

		if (fargeSample[0]*100 > 4) {
			// TODO: Foelg svart linje
			System.out.println("Ikke svart! (" + fargeSample[0]*100 + "/" + svart + ")");
			return false;
		} else {
			System.out.println("Svart! (" + fargeSample[0]*100 + "/" + svart +")");
			return true;
		}
	}

	// Faar bilen til aa svinge. Dersom ret = 0, svinger bilen til venstre. Er ret = 1, hoeyre
	public static void sving(int ret, int deg){
		System.out.println("Svinger");
		if (ret==1){
			deg = deg*-1;
			Motor.B.forward();
		}
		else {
			Motor.B.backward();
		}
		Motor.A.rotate(deg);
		while (Motor.A.isMoving()) Thread.yield();
	}
}
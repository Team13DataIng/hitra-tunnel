import lejos.hardware.motor.*;
import lejos.hardware.lcd.*;
import lejos.hardware.sensor.EV3TouchSensor;
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
	//globale variabler for aa kunne hente dem i kjor og setup
	private static Brick brick;
	private static Port s1;
	private static Port s2;
	private static Port s3;
	private static EV3 ev3;
	private static TextLCD lcd;
	private static Keys keys;
	private static SampleProvider trykksensor;
	private static float[] trykkSample;
	private static SampleProvider trykksensor2;
	private static float[] trykkSample2;
	private static SampleProvider lydsensor;
	private static float[] lydSample;
	private static int runde;

	public static void main (String[] args)  throws Exception {

		//setup
		brick = BrickFinder.getDefault();
		s1 = brick.getPort("S1"); //lydsensor
		s2 = brick.getPort("S2"); // trykksensor
		s3 = brick.getPort("S3"); // trykksensor
		ev3 = (EV3) BrickFinder.getLocal();
		lcd = ev3.getTextLCD();
		keys = ev3.getKeys();
		/* Definerer trykksensor */
		trykksensor = new EV3TouchSensor(s2);
		trykksensor2 = new EV3TouchSensor(s3);
		//lydsensor
		lydsensor  = new NXTSoundSensor(s1);
		//teller runder
		runde = 8;

		//motorfast
	 	Motor.A.setSpeed(900);
	 	Motor.B.setSpeed(900);

	 	// Kjoer framover
		kjor();
	}

	public static void kjor() {
		float[] lydSample = new float[lydsensor.sampleSize()];
		float[] trykkSample = new float[trykksensor.sampleSize()];
		float[] trykkSample2 = new float[trykksensor2.sampleSize()];

		System.out.println("Kjorer til trykk eller lyd");
		Motor.A.backward();
		Motor.B.backward();
		boolean fortsett  = true;
		int ret = 0;
		boolean lydStopp = false;
		while(fortsett) {
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
			//stopper hvis lyden er hoy nok
			if(lydSample!= null && lydSample.length > 0){
				lydsensor.fetchSample(lydSample, 0);
				if(lydSample[0]>0.4){
					System.out.println("Lyd over 0.4: "+lydSample[0]);
					fortsett = false;
					lydStopp = true;
				}
			}
		}
		stopp();

		// Kjoer metodene rygg og sving samtidig
		if(!lydStopp){
			rygg();
			sving(ret);
		}
		else {
			try {
				Thread.sleep(3000);
			}
			catch(InterruptedException e){
				System.out.println("En feil skjedde");
        	}
			kjor();
		}
	}

	// Stopper begge hjulmotorene
	public static void stopp(){
		Motor.A.stop();
		Motor.B.stop();
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

	// Faar bilen til aa svinge. Dersom ret = 0, svinger bilen til venstre. Er ret = 1, hoeyre
	public static void sving(int ret){
		Random rand = new Random();
		int deg = rand.nextInt((600 - 100) + 1) + 100;
		if (ret==1){
			deg = deg*-1;
			Motor.B.forward();
		}
		else {
			Motor.B.backward();
	}
		//hjulene roterer
		Motor.A.rotate(deg);
		while (Motor.A.isMoving()) Thread.yield();
		/*Motor.C.rotate(deg);
		while (Motor.C.isMoving()) Thread.yield();  // vent til rotasjon er ferdig
		Motor.C.rotate(-deg);
		while (Motor.C.isMoving()) Thread.yield();  // vent til rotasjon er ferdig
		System.out.println("roterer "+deg);*/
		runde--;
		if(runde>0){
		 	kjor();
	 	}
	}
}
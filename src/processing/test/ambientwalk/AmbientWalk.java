package processing.test.ambientwalk;

import processing.core.*; 
import processing.data.*; 
 
import android.content.Context; 
import android.content.res.Resources;

import android.os.PowerManager; 
import android.hardware.Sensor; 
import android.hardware.SensorEvent; 
import android.hardware.SensorEventListener; 
import android.hardware.SensorManager;  

import java.io.File;  
import java.io.PrintWriter; 
import java.io.InputStream;  
import java.io.IOException;

import org.puredata.android.io.AudioParameters;
import org.puredata.android.io.PdAudio;
import org.puredata.android.utils.PdUiDispatcher;
import org.puredata.core.PdBase;
import org.puredata.core.PdListener;
import org.puredata.core.utils.IoUtils;

public class AmbientWalk extends PApplet {

//  **Be sure that your Sketch has activated these permissions:
//    INTERNET
//    RECORD_AUDIO
//    WRITE_EXTERNAL_STORAGE
//    WAKE_LOCK

    //Parameter initialisation: sensor parameter
    SensorManager sensorManager;       
    SensorListener sensorListener; 
    //--step counting with pedometer (for API 19+)
    /*Sensor pedometer; 
    float step, isStep, prev_step, abs_stepcount;
    */
    //--step counting with accelerometer (for API 16)--
    Sensor accelerometer;  
    float[] accelData; 
    //list of acc-magnitude values over time (of a window of 10--1sec)
    FloatList acc_magnitude;
    float magnitude,acc_sum,acc_avg;double pace;
    //totalcount, stepcount and windowsize for counting average
    int stepcount=0,windowsize=10,peak_index,prev_index=1,startframe=0;
    
    //--flag whether user tapped the screen
    boolean button_clicked=false,mouse_enabled=false;
//vars for finding peak of breath sound
    int current_peak=0,prev_peak=0,recstart=0;
    //breath period, received from pd patch
   float period=0,ratio=1,volume=0;//threshold=50;
 
//wakelock
PowerManager pm;
PowerManager.WakeLock wl;
//data logging
PrintWriter OUTPUT,OUTPUT2;
//initialscreen images
PImage title,instruction,buttonimg,dmlogo;
int start_time;

//Pd settings
private static final String ACCELERATE = "#accelerate";
private static final String PACE = "#pace";//count pace per 5 sec
private static final String BREATHPERIOD = "#breathperiod";
private static final String MAXVOL = "#maxvol";
//private static final String THRESHOLD = "#threshold";
private static final String RATIO = "#ratio";
private PdUiDispatcher dispatcher=new PdUiDispatcher();

	public PdListener dataListener=new PdListener() {
	    public void receiveBang(String source){
	    	
	    };
	    public void receiveFloat(String source, float x){
	    	if (source.equals("#breathperiod")){
	    	period=x*2/1000;
	    	}
	    	else if (source.equals("#maxvol")){
	    	volume=x;
	    	}
	    	
	    };
	    public void receiveSymbol(String source, String symbol){
	    	
	    };
	    public void receiveList(String source, Object... args){
	    	
	    };
	    public void receiveMessage(String source, String symbol, Object... args){
	    	
	    };
	};
	
 //processing code
public void setup() 
{       
       // start_time=millis();
        frameRate(10);//10 frame per sec
        smooth();
       
        //load images to be placed at front screen
        title = loadImage("title_logo.png");
        instruction = loadImage("instruction.png");
        buttonimg=loadImage("button.png");
        dmlogo=loadImage("dmlogo.png");
        //accelerometer setting
        acc_magnitude=new FloatList();//refresh magnitude list every frame
        prev_index=0;
        
 //wakelock setting       
pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "My Tag");
wl.acquire();
OUTPUT = createWriter("/sdcard/ambientwalk_data_"+month()+"_"+day()+"_"+hour()+"_"+minute()+"_"+second()+".txt");
OUTPUT.println("FrameCount"+","+"Breath Period (sec)"+","+"Walking Pace (Hz)");
OUTPUT2 = createWriter("/sdcard/ambientwalk_acc_"+hour()+"_"+minute()+"_"+second()+".txt");
OUTPUT2.println("FrameCount"+","+"X"+","+"Y"+","+"Z"+","+"Magnitude");

}

public void draw() //update per frame, stop working when screen locked
{
  
  background(50,10,10,50);
  //paint initial screen with start button
  paintfirstscreen();
 mouse_enabled=true;
  
  //if button clicked:
  if(button_clicked){
    background(220,220,220);
    fill(0,0,0,10);rect(0,0,width,height);
   
   //text("Time elapsed: "+millis()+" Frame: "+frameCount, width/10,height/10);
 mouse_enabled=false;
    try {
 fill(50,10,10);noStroke();
   //textSize(15);  
   //draw the slider bar
   /*
   text("Threshold", width*5/6,height/6);
   text(""+threshold, width*5/6,height/6+12);
   stroke(255);noFill();

   rect(width*5/6,height/6+20,20,400);
 rect(width*5/6,height/6+420-threshold*4,20,10);//slider
 */
  // textSize(20); 
  // rect(width*5/6,height/6+20,20,height*2/3);
 //rect(width*5/6,mouseY,20,10);//slider
   textSize(30); 

   String period2decimal = String.format("%.2f", period);
text("Breath Period: " + period2decimal+"s", width/10, height*4/5); 
 text("Number of Steps: "+stepcount, width/10,height*5/6);
 //text("Volume: "+volume, width/10,height*7/8);
  //text("Accelerometer: "+magnitude, width/10,height*7/8);
	//PdBase.sendFloat(THRESHOLD,threshold);
  //bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
         //part1: breath detection via microphone
  // thread("breathRecording");
             stroke(150,150,150);
             //noFill();
         // fill(100,100,100,volume*4-50);  
             for(int i=1;i<round(volume/10);i++){
            	 fill(50,10,10,volume*2);
 ellipse(width/2,height/2,i*25,i*25);
             }

           //part 2:step detection with accelerometer||android 4.1.2+ (API16-19)
           //get sensor data
            
    thread("stepDetection");

           //generate the tones based on two data
    /*
      if(frameCount%20==1){
           genTone(freqOfTone_breath,freqOfTone_step);
           playSound(); 
       }     */
  
      if (frameCount%10==1){
          export2Txt(period, pace);  
          ratio=period/(float)pace;
      	PdBase.sendFloat(RATIO,ratio);
          //maxvol=0;
        }
     // end if frameCount%10=1
      /*
     if(millis()-start_time>=1000||millis()-start_time==0){
     ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
     tg.startTone(ToneGenerator.TONE_PROP_BEEP);
     start_time=millis();
        }     
        */  
    }//end try
    catch (IllegalArgumentException e) 
      {
    	/*
        freqOfTone_breath=offset1;
        freqOfTone_step=2*offset2;
        genTone(freqOfTone_breath,freqOfTone_step);
        */
    	//send offset data to Pd
        e.printStackTrace();
      } 
    catch (IllegalStateException e) 
      {/*
        freqOfTone_breath=2*offset2;
        freqOfTone_step=3*offset3;
        genTone(freqOfTone_breath,freqOfTone_step);*/
        e.printStackTrace();
      } 
    catch (RuntimeException e) 
      {/*
        freqOfTone_breath=offset1+offset2+offset3;
        freqOfTone_step=offset1;
        genTone(freqOfTone_breath,freqOfTone_step);*/
        e.printStackTrace();
      } 

  }//end button clicked
} //end draw 

//get breath data (period, maxvol) PdBase.receiveList(BREATHPERIOD, period, maxvol);
//draw maxvol
/*
 public void breathRecording(){
   //part 1: breath recording
      bufferReadResult = audioRecord.read(buffer, 0, bufferSize);
       maxvol=0;
       for(int i=0;i<bufferReadResult-1;i++){
         if(Math.abs(buffer[i])==0){volume[i]=0;}
           else{
           volume[i]=20*log(Math.abs(buffer[i])/10)/log(10);//transfer to decibel
         }
          maxvol=Math.max(maxvol,volume[i]); 
          //println(maxvol);
          
//real-time debouncing
            if(i>=2&&i<=bufferReadResult-1){
              if (volume[i-2]<volume[i-1]&&volume[i-1]>volume[i]&&volume[i-1]>50){
                          //debouncing algorithm: only indicate foundmax when fulfilling (either of) these two criteria
                          current_peak=(frameCount-1)*320+i-1;//get absolute sample index of the maximum
          //need further editing****
          if(prev_peak==0){prev_peak=current_peak;}
          else if(current_peak-prev_peak>=320*5){
            period=(current_peak-prev_peak)*2/(320*10);
            //send period to pd
            //receive period data from Pd every 100ms
           
            //text period, calculate ratio and draw small/big group of circles
            prev_peak=current_peak;
          }  
          //println(prev_peak);
          //else:do nothing if found a point within debouncing period
        } //else: do nothing if not found a maximum
     }//end debouncing
   }//end for loop (transfer into decibel volume)

       /*
       if(period!=0){
     freqOfTone_breath=(double)1/period*10*offset1;
       }
       else{freqOfTone_breath=2*offset1;}
       
      }//end breath recording
   */   
      
           //part 2:step detection with accelerometer||android 4.1.2+ (API16-19)
           //get sensor data
  public void stepDetection(){
     if (accelData != null) {
//*******send acc data to Pd (test)********
    	 PdBase.sendList(ACCELERATE, accelData[0], accelData[1], -accelData[2]);
     //get magnitude
    	 magnitude=sqrt(accelData[0]*accelData[0]+accelData[1]*accelData[1]+accelData[2]*accelData[2]);
     //test accelerometer data logging (need to be commented for real app)
     OUTPUT2.println(frameCount+","+accelData[0]+","+accelData[1]+","+accelData[2]+","+magnitude);  // here we export the coordinates of the vector using String concatenation!
     OUTPUT2.flush();
     //append magnitude to analyse
     acc_magnitude.append(magnitude);//append new value at the end
     //acc_sum=acc_sum+magnitude;//add new value
     //see whether to subtract the first value
if(frameCount>=windowsize){
  acc_avg=(acc_magnitude.max()+acc_magnitude.min())/2;
//find peak within this range
for(int i=2;i<acc_magnitude.size()-1;i++){
//peak detection with debouncing
 if(acc_magnitude.get(i)>acc_magnitude.get(i-1)&&acc_magnitude.get(i)>acc_magnitude.get(i+1)&&acc_magnitude.get(i)>acc_avg&&acc_avg>1.5){
   peak_index=frameCount-10+i;
   if(peak_index-prev_index>=3||prev_index==1){
	   fill(50,10,10,100);
	   ellipse(width/4,height/3,25,25);
     stepcount++;
     //stroke(50,10,10);textSize(30);
     //text(stepcount, 0,height*5/6);
   prev_index=peak_index;
   if(stepcount==1){startframe=frameCount;}//start counting pace when the 1st step is detected

 }
   } //end if
}//end for (peak detection)
acc_magnitude.remove(1);//delete the first one at the beginning to shift the window
}//end if (framecount>windowsize)
  
}//end if accelData!=null
//text("Number of Steps: "+stepcount, width/10,height*5/6);
//generate a tone according to steps per 1 sec (10 frames)
       if(frameCount%10==1){
    if(stepcount!=0&&frameCount!=1){
              pace=(double)stepcount*50/(frameCount-startframe);//pace per 5sec
              //********send pace to Pd********
              //freqOfTone_step=pace*offset1;
            //stroke(50,10,10);textSize(30);
             // text(stepcount, 0,height*5/6);
        }
       else{
    	   //freqOfTone_step=random(220,2000);
    	   pace=1;
    	   }
    PdBase.sendList(PACE, pace);
       }
       
}//end stepDetection

  public void initSystemServices(){
	  sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
	  sensorListener = new SensorListener();
	  //Step counter for API 19+/Nexus5
	  /*pedometer = sensorManager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER);
	 sensorManager.registerListener(sensorListener, pedometer, SensorManager.SENSOR_DELAY_FASTEST); 
	*/
	//Use accelerometer for API 16+
	accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
	  sensorManager.registerListener(sensorListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST); 
	  
  }
  
public void stop() 
{ 
 wl.release();
 PdAudio.stopAudio();
 PdBase.release();
   //close data logging
 OUTPUT.close();
}
 
public void mousePressed()
{if(mouse_enabled){
  button_clicked=true;
//for stepcounter (API 19+)
//abs_stepcount=0;
//println(abs_stepcount);//check step count
frameCount=0;
start_time=millis();
//ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 60);
//tg.startTone(ToneGenerator.TONE_PROP_BEEP);
//start data logging (to sdcard)
//data logging for breath period and walking pace

try{initPd();}catch(IOException e){};
}
}
//slider UI
/*
public void mouseDragged() 
{if(mouseX>=width*5/6&&mouseY>=height/6&&mouseY<=height*5/6){
	threshold=(height-mouseY)*100/(height*2/3);
	//rect(width*5/6,mouseY,20,10);
}
}
*/
public void onResume() //register sensor listener
{
  super.onResume();
  initSystemServices();
  PdAudio.startAudio(this);
  //pdService.startAudio(new Intent(this, AmbientWalk.class), R.drawable.icon, "ambientwalk", "Return to ambientwalk.");
}

public void onPause() //unregister sensor listener
{
  sensorManager.unregisterListener(sensorListener);
  super.onPause();
  //close data logging
}

 //paint first screen
 public void paintfirstscreen()
 {
   background(50,10,10);
   //fill the bottom 1/3 with darker color
   if(title.width>=width){
   image(title, 0,height/3,width,title.height*width/title.width);
   }
  else{image(title, (width-title.width)/2,height/3);}
   if(instruction.width>=width){
     image(instruction, 0,height*3/5+buttonimg.height*width/buttonimg.width/2, width,instruction.height*width/instruction.width);
   }
   else{image(instruction, (width-instruction.width)/2,height*3/5+buttonimg.height);}
   if(buttonimg.width>=width){
     image(buttonimg,0,height/2, width, buttonimg.height*width/buttonimg.width);
   }
   else{image(buttonimg,(width-buttonimg.width)/2,height*5/9);}
   if(dmlogo.width>=width/2){
   image(dmlogo,width/2,height-dmlogo.height*width/2/dmlogo.width, width/2, dmlogo.height*width/2/dmlogo.width);
   }
   else{image(dmlogo,width-dmlogo.width,height-dmlogo.height);}

}

//data logging to sd card

public void export2Txt(float breath, double pace){
  OUTPUT.println(frameCount+","+breath+","+pace);  // here we export the coordinates of the vector using String concatenation!
  OUTPUT.flush();
  //println("data has been exported");
}

//initialize LibPd settings
public void initPd() throws IOException {
	AudioParameters.init(this);
	Resources res = getResources();
	//int inpch = AudioParameters.suggestInputChannels();
	PdAudio.initAudio(44100, 1, 2, 8, true); 
	File patchFile = null;
	try {
		PdBase.setReceiver(dispatcher);
		dispatcher.addListener(BREATHPERIOD, dataListener);
		dispatcher.addListener(MAXVOL, dataListener);
		//PdBase.subscribe("android");
		PdBase.subscribe("android");
		 //load pd patch
		InputStream in = res.openRawResource(R.raw.ambientwalk);
		patchFile = IoUtils.extractResource(in, "ambientwalk.pd", getCacheDir());
		PdBase.openPatch(patchFile);
		PdAudio.startAudio(this);
		recstart=1;
		PdBase.sendFloat("#rec", recstart);
	} catch (IOException e) {
		//Log.e(TAG, e.toString());
		finish();
	} finally {
		if (patchFile != null) patchFile.delete();
	}
	
}
	
class SensorListener implements SensorEventListener 
{
  public void onSensorChanged(SensorEvent event) 
  {//if step counter is available
  /*
    if (event.sensor.getType() == Sensor.TYPE_STEP_COUNTER)    {
      step = event.values[0];
    }
    */
    //if accelerometer only
    if (event.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) 
    {
      accelData = event.values;
    }
   
  }
  public void onAccuracyChanged(Sensor sensor, int accuracy) 
  {
       //todo 
  }
}
}



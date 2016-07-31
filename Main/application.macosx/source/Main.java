import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import ddf.minim.*; 
import ddf.minim.analysis.*; 
import ddf.minim.effects.*; 
import ddf.minim.signals.*; 
import ddf.minim.spi.*; 
import ddf.minim.ugens.*; 
import toxi.math.*; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class Main extends PApplet {









PFont font;

//Sample frequency
int Fs = 44100;

//Minimum and Maximum frequency range
int minFreq = 80;
int maxFreq = 1200;

//Calculate max and min number of samples
int maxSamples = Fs * 1/minFreq; // x2 for double maxSamples
int minSamples = Fs * 1/maxFreq;

float[] inputSamples = new float[maxSamples*2];
float finalFreq;

//Minim objects (audio inputs)
Minim minim;
AudioInput in;
AudioInput inLP;
LowPassFS lowPass;
//Self produced class objects
AMDF amdf;
Dial dial1;
Dial dial2;

/********* Mean Frequency Variables *********/
int numberOfFreq = 10; //How many frequency values to use for mode calculation
int numberOfModes = 5; //How many means should be used to calculate user performance
float [] currentFreq = new float[numberOfFreq]; //Array to store frequencies
float [] modeFrequencies = new float[numberOfModes]; //Array to store mode values
int modeCounter; //Tracks number of modes stored
int currentFreqN; //Tracks number of frequencies stored
int storeFreqTimer; //Timer variable for recording frequencies
int freqLog = 1; //Store frequency every 500ms
float [] getVariance = new float[2]; //Array for comparing variances

float difficulty = 0.0625f; //Initial difficulty multiplier
int diffKey = 1; //Initial user input

boolean debugModeBool = false;


/********* Graphics Variables *********/
//Image objects
PImage pointer;
PImage dial;
PImage ticks;
PImage varianceTitle;
PImage soloTitle;
PImage space;
PImage easy;
PImage average;
PImage hard;
PImage icon;

//Initial dial values
float deg = 0;
float dialVal = 0;
int [] compareDial = new int [2];

boolean isOn = false; //Bool to determine margin colour (green or red)
boolean draw = false; //Draw margin

//float dialMove = 0.5;
float varianceDial = 0;
float marginDial = 0;

/********* Circular Marker Variables *********/
int r = 140; //Radius
int a = -90; //Angle (degrees)
int startA = -135; //Angle offset
float a1 = a + startA + 8.75f; //Start position angle

//Arras to store 'tick marker' angles
float [] xAngle = new float[16];
float [] yAngle = new float[16];

//Screen dimensions
int xScreen = PApplet.parseInt(1440*0.9f);
int yScreen = PApplet.parseInt(800*0.9f);

boolean start = false;//Start program boolean
boolean reset = false;//Reset dial values boolean

float dialMoveSpeed = 0.5f;


public void settings(){//Set screen size using variables
  size(xScreen, yScreen);
}

public void setup(){

//Edit window frame
surface.setResizable(true);
surface.setVisible(true);

//Object instantiation
minim = new Minim(this);
in = minim.getLineIn(Minim.MONO, maxSamples*2);
amdf = new AMDF();
lowPass = new LowPassFS(900, Fs);
dial1 = new Dial(xScreen, yScreen, 0.25f, 0.4f,1);
dial2 = new Dial(xScreen, yScreen, 0.75f, 0.4f,1);

//Low pass filter the input audio
in.addEffect(lowPass);
//Add audioListener to the amdf class
in.addListener(amdf); 
//Time since program started
storeFreqTimer = millis();

//Set colour mode to HSB and set ranges (Hue,Saturation,Brightness,Alpha)
colorMode(HSB,360,360,100,270);

//Load images
pointer = loadImage("pointer3_V3.png");
ticks = loadImage("ticks3.png");
soloTitle = loadImage("soloTitle.png");
varianceTitle = loadImage("varTitle.png");
space = loadImage("start.png");
easy = loadImage("Easy.png");
average = loadImage("Average.png");
hard = loadImage("Hard2.png");
}

public void draw()
{
  if(debugModeBool == true){
    surface.setTitle("Sol-O-Meter: Debug Mode");
  } else {
    surface.setTitle("Sol-O-Meter");
  }
  //Redraw scene each loop
  background(0,0,100);
  stroke(255);

  if(start == false){//Display intro screen until user hits space bar
    introScreen();
  } else {
    if(reset == true){//Reset dial values and empty array
      dialVal = 0;
      varianceDial = 0;
      getVariance[0] = getVariance[1] = 0;
      dialMoveSpeed = 5;
    }

  int isSamples = amdf.calc(); //Calculate AMDF of input signal
  amdf.clear();//Reset AMDF variables

  if(isSamples == 1){ //If there are samples available to read

    //Initial variance margin variable
    float margin = 0;

    if(millis() - storeFreqTimer >= freqLog){//If the set update time has passed
      //Store current frequency (then calculate variance)
      storeFreqs();
      //Update frequency storing timer
      storeFreqTimer = millis(); 
    }

    //Variance variables
    int maxVariance = 110000;//Through experimentation
    float maxMargin = varianceDial + (270*difficulty);//Maximum limit dial value
    float minMargin = varianceDial - (270*difficulty);//Minimum limit dial value
    float maxMarginVar = map(maxMargin,0,270,0,maxVariance);//Maximum limit variance value
    float minMarginVar = map(minMargin,0,270,0,maxVariance);//Minimum limit variance value
    varianceDial = map(getVariance[0],0,maxVariance,0,270); //Variance to dial angle

    //When enough modes have been calculated
    if(modeCounter == numberOfModes){
      modeCounter = 0; //Reset the mean counter
      getVariance[1] = getVariance[0];//Store previous variance
      getVariance[0] = calculateVariance();//Calculate new variance

      /*
      For monitoring and debugging only
      printModes();
      printVariance();
      */

    if(getVariance[0] > maxMarginVar || getVariance[0] < minMarginVar){
      //Increase dial value if variance is outside margins
      dialVal = dialVal + 40;
      isOn = true;//Margin flash green
      draw = true;
    } else {
      //Decrease dial value if variance is outside margins
      dialVal = dialVal - 40;
      isOn = false;//Margin flash red
      draw = true;
    }
  }
    
    //Limit the dial values to 0-270 degrees
    dialVal = limiter(dialVal);
    varianceDial = limiter(varianceDial);

    //Call dial class functions
    dial1.drawDial(dialVal, dialMoveSpeed, width, height, 1); //(value to read, dial speed)
    dial2.drawDial(varianceDial, 5, width, height, 1);
    dial2.drawMargin(varianceDial, minMargin, maxMargin);

    if(dial1.getResetStatus() == true){
      reset = false;
      dialMoveSpeed = 0.5f;
    }
    } else {
      print("No Samples for calculations");
    }
    //Load title and label image
    loadTextImages();
  }

  if(debugModeBool == true){
    debugMode();
  }

}

public void debugMode(){

  //Check whether frequency > 0hz
  boolean freqStatus = amdf.getFrequencyStatus();

  if(freqStatus == true){
    //Draw signal indicator
    noStroke();
    fill(120,46.2f,86.7f);
    ellipse(width - 20, height - 20, 20, 20);
  } else {
    noStroke();
    fill(2.8f,91.8f,100);
    ellipse(width - 20, height - 20, 20, 20);
  }
}

//Introduction screen
public void introScreen(){
  imageMode(CENTER);
  soloTitle.resize(0,100); 
  image(soloTitle,xScreen*0.5f,yScreen*0.45f);
  space.resize(0,60);
  image(space,xScreen*0.5f,yScreen*0.55f);
}

//Limit dial values
public float limiter(float value){
  if(value <= 0){
    value = 0;
  }
  if(value >= 270){
    value = 270;
  }
  return value;
}

public void storeFreqs(){
  //Check current frequency
  float freq = amdf.getFrequency();
  //Only store frequency if it has been read correctly (ie !=0)
  if(freq != 0){
    currentFreq[currentFreqN] = freq;
    currentFreqN++;
  }
  if(currentFreqN == numberOfFreq){ //If frequency array is full calculate the mode
    currentFreqN = 0; //Reset currentFreqN counter
    modeFrequencies[modeCounter] = calculateMode(); //Store mode value
    modeCounter++;//Increment mode counter
  }
}

public float calculateMode(){
  float [] tempArray = new float[currentFreq.length];
  int most = 0;
  float compareMode = 0;

  System.arraycopy(currentFreq,0,tempArray,0,currentFreq.length);

  //Tally number of occurrences of each value
  float [] findMode = new float[tempArray.length];

  for(int k=0;k<currentFreq.length;k++){
    for(int i=0;i<tempArray.length;i++){
      if(currentFreq[k] == tempArray[i]){
        findMode[k] += 1;
      }
    }
  }
  //Now find which frequency comes up the most
  for(int i=0;i<findMode.length;i++){
    if(compareMode >= findMode[i]){
      //Do Nothing
    }else{
      compareMode = findMode[i];
      most = i;
    }
  }
  //Output the most common frequency
  return currentFreq[most];
}


public float calculateVariance(){
  float sumOfModes = 0;
  float meanOfModes = 0;
  float sumOfDeviations = 0;
  float variance = 0;
  float sumOfDifferences;

  //Mean of modes
  for(int i=0;i<modeFrequencies.length;i++){
    sumOfModes = sumOfModes + modeFrequencies[i];
  }
  meanOfModes = sumOfModes/modeFrequencies.length; //Mean of modes

  //Calculate deviations
  for(int i=0; i<modeFrequencies.length;i++){
    sumOfDeviations = sumOfDeviations + sq(modeFrequencies[i] - meanOfModes);
  }

  variance = sumOfDeviations/modeFrequencies.length;

  return variance;
}

//Place labels
public void loadTextImages(){
  //Load text images
  tint(360, 360);
  soloTitle.resize(0,60); 
  image(soloTitle,width*0.25f,height*0.80f);
  varianceTitle.resize(0,50); 
  image(varianceTitle,width*0.75f,height*0.80f);

  //Load different images for different difficulties
  if(diffKey ==1){
    easy.resize(0,50); 
    image(easy,width*0.5f,height*0.10f);
  } else if(diffKey ==2){
    average.resize(0,50); 
    image(average,width*0.5f,height*0.10f);
  } else if(diffKey ==3){
    hard.resize(0,50); 
    image(hard,width*0.5f,height*0.10f);
  }
}

//Get user input
public void keyPressed(){
  if(key == '1'){
    difficulty = 0.0625f;
    print("\n difficulty: ",difficulty);
    diffKey = 1;
  }
  if(key == '2'){
    difficulty = 0.125f;
    print("\n difficulty: ",difficulty);
    diffKey = 2;
  }
  if(key == '3'){
    difficulty = 0.25f;
    print("\n difficulty: ",difficulty);
    diffKey = 3;
  }
  if(key == ' '){
    start = true;
  }
  if(key == 'r'){
    reset = true;
  }  
  if(key == 'd'){
    debugModeBool = true;
  }
  if(key == 'f'){
    debugModeBool = false;
  }
  if(key == 's'){
    print("\n w: " + displayWidth + "h: " +displayHeight + "Width: "+width);
  } 
}

//Print stored modes
public void printModes(){
  print("\n ----------");
  for(int x = 0; x<modeFrequencies.length;x++){
    print("\n modeFrequencies["+x+"]: "+modeFrequencies[x]);
  }
  print("\n ----------");
}

//Print variance
public void printVariance(){
  print("\n Variance: ",getVariance[0]);
  print("\n ----------");
}
class AMDF implements AudioListener
{
  float [] difference = new float[maxSamples];
  float [] sum = {0,0};
  float [] total = new float[maxSamples];
  int largestValue;
  int n;
  float compare = 0.000001f;
  float[] freqArray;
  float frequency;  
  float freqLabel;
  String myText;
  boolean frequencyAvilable = false;

  int updateFreqCount;

  private float[] inMix;  
  private float[] left;
  private float[] right;

  int samplesFlag; //Indicates whether samples are available for mean calculations

  //Constructor
  AMDF(){
  	 inMix = null;
  	 left = null;
  	 right = null;
  }

//Get input buffer
public synchronized void samples(float[] samp)
{
	inMix = samp;
}
//Get input buffer (left and right mix)
  public synchronized void samples(float[] sampL, float[] sampR)
  {
    left = sampL;
    right = sampR;
  }  

//Calculates the AMDF
public synchronized int calc(){
//Store appropriate 
  if(inMix!= null) //If buffer contains audio
  {
    //Normalise all the samples
    for(int z=0; z<inMix.length; z++){
      inMix[z] = inMix[z] * 10;
    }

    for(int n = minSamples; n <= maxSamples; n++){
      for(int i = 0; i < n; i++){
       //Calculate inside Summation
       difference[i] = (abs(inMix[i] - inMix[n+i]));

       //Summation of all values
       sum[0] = difference[i] + sum[1];
       sum[1] = sum[0];
      }
      //Store the AMDF in an array for each iteration
      total[n - minSamples] = sum[0]/n; //Scale by number of samples used

      //Find value of n for least difference in magnitude
      if(total[n-minSamples] < 0.2f){
        largestValue = n;
        sum[0] = 0; //Clear buffers for next iteration
        sum[1] = 0;
        frequency = Fs/largestValue; //Return frequency value
        break;
      }
      sum[0] = 0; //Clear buffers for next iteration
      sum[1] = 0;
    }
   samplesFlag = 1; 
  } else {
    print("\n No Samples");
    samplesFlag = 0;
  }

  return samplesFlag;

}

public synchronized void plotDifference(){
  for(int i=0;i<total.length - minSamples;i++)
  {
    if(total[i] < 0.01f) //Colour the 'tips' differently
    {
    int c = color(0,0,0);
    fill(c);
    stroke(c);  
    } else{
    int c = color(2,82,76);
    fill(c);
    stroke(c);
  }
    ellipse(i*(total.length/width), height/2 + total[i]*500, 2, 2);
    stroke(0,0,100);
    ellipse(i*(total.length/width), height/2, 2, 2);

    smooth();
    fill(0,0,100);

    if(freqLabel <80 || freqLabel > 1200)
    {
      myText = "Frequency:";
    } else {
    myText = "Frequency: "+freqLabel+"Hz";
  }
    text(myText,100,50);
  }
}

//Return calculated frequency
public float getFrequency(){
  if(frequency > 1200){
    frequency = 0;
    frequencyAvilable = false;
  } else {
    frequencyAvilable = true;
  }
  return frequency;
}

public boolean getFrequencyStatus(){
  return frequencyAvilable;
}

//Clear all values for new reading
public synchronized void clear(){
  largestValue = 0;
}

public synchronized int getSampSize(){
  return inMix.length;
}

}
class Dial{

int xS ,yS;
float xPos, yPos;
int resizeVal = 350;
float val = 0;
float scale;
float colourTimer = 0;

boolean start = true;

PImage dotImage;
  //Constructor
  Dial(int tempXS, int tempYS, float tempXPos, float tempYPos , float tempScale){
    xS = tempXS;
    yS = tempYS;
    xPos = tempXPos;
    yPos = tempYPos;
    scale = tempScale;
  }  

  public void drawDial(float degree, float dialMove, int tempWidth, int tempHeight, float tempScale){
    start = true;
    //Scalable dimensions
    xS = tempWidth;
    yS = tempHeight;
    scale = tempScale;

   //Calculate how much to move by
   if(val < degree){
    if(val+dialMove > degree){
      dialMove = dialMove*0.1f;
    }
    val = val + dialMove;
    start = true;
  } 
   if(val > degree){
    if(val+dialMove < degree){
      dialMove = dialMove*0.1f;
    }
    val = val - dialMove;
    start = false;

  } else{
    //Do Nothing
  }

  //Position and draw dial (pointer) and gauge
  imageMode(CENTER);
  pushMatrix ();  // save transformation matrix
  translate (xS*xPos, yS*yPos);
  ticks.resize(0,round(380*scale)); 
  tint(360, 360);
  image(ticks,0,0);
  float rads = radians(val - 130);
  rotate (rads);
  pointer.resize(0,round(resizeVal*scale)); 
  image (pointer, 0, 0);
  popMatrix ();


  //Calculate where ticks (markers) sound be placed
  for(int i=0;i<xAngle.length;i++){
    xAngle[i] = xS*xPos + (r*scale)*cos(radians(a1+(16.8f*(i))));
    yAngle[i] = yS*yPos + (r*scale)*sin(radians(a1+(16.8f*(i))));

    if(i<7){
      dotImage = loadImage("tickG2.png");
      } else if(i > 6 && i < 13){
      dotImage = loadImage("tickA2.png");
      } else {
      dotImage = loadImage("tickR.png");
      }
    imageMode(CENTER);
    pushMatrix ();  // save transformation matrix
    translate (xAngle[i],yAngle[i]);
    dotImage.resize(0,round(30*scale)); 
    float tintVal = map(val-16.8f*(i+1),0,130,0,360);
    tint(360,tintVal);
    float randRotate = radians(-a1+16.8f*(i+1));
    rotate (randRotate);
    image (dotImage, 0, 0);
    popMatrix ();
  }
}

public boolean getResetStatus(){
  return start;
}

public void drawMargin(float tempC, float tempMin, float tempMax){

  float upperLim = tempMax;
  float lowerLim = tempMin;
  float r2 = scale*200;

  //Set limits
  if(tempC <= 0){
    tempC = 0;
  }
  if(tempC >= 270){
    tempC = 270;
  }

  if(upperLim <= 0){
    upperLim = 0;
  }
  if(upperLim >= 270){
    upperLim = 270;
  }

  if(lowerLim <= 0){
    lowerLim = 0;
  }
  if(lowerLim >= 270){
    lowerLim = 270;
  }

  //Margin offsets
  float centre = a1+tempC;
  float upper = a1+upperLim;
  float lower = a1+lowerLim;

  //Calculate x y positions
  float maxLimX = xS*xPos + r2*cos(radians(upper));
  float maxLimY = yS*yPos + r2*sin(radians(upper));

  float cenLimX = xS*xPos + r2*cos(radians(centre));
  float cenLimY = yS*yPos + r2*sin(radians(centre));

  float minLimX = xS*xPos + r2*cos(radians(lower));
  float minLimY = yS*yPos + r2*sin(radians(lower));

  //Local colour variable
  int c;

  //Determine colour of the margin
  if(draw == true){
    if(isOn == true){
     c = color(120, 46.2f, 86.7f); 
    } else {
     c = color(2.8f,91.8f,100); 
    }
  } else {
    c = color(0);
  }

  //Change colour back to black after 5 seconds
  if(millis() - colourTimer > 5000){
    draw = false;
    colourTimer = millis();
  }

  colorMode(HSB,360,100,100,135);
  noFill();
  stroke(c);
  strokeWeight(2);
  beginShape();
  for(float i=lower; i<upper; i++){
    float x = xS*xPos + (r2+random(-1,1))*cos(radians(i));
    float y = yS*yPos + (r2+random(-1,1))*sin(radians(i));
    curveVertex(x,y);
  }
  endShape();
}

}
  static public void main(String[] passedArgs) {
    String[] appletArgs = new String[] { "Main" };
    if (passedArgs != null) {
      PApplet.main(concat(appletArgs, passedArgs));
    } else {
      PApplet.main(appletArgs);
    }
  }
}

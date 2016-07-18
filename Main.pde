import ddf.minim.*;
import ddf.minim.analysis.*;
import ddf.minim.effects.*;
import ddf.minim.signals.*;
import ddf.minim.spi.*;
import ddf.minim.ugens.*;
import toxi.math.*;

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

float difficulty = 0.0625; //Initial difficulty multiplier
int diffKey = 1; //Initial user input

boolean debugModeBool = true;


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
float a1 = a + startA + 8.75; //Start position angle

//Arras to store 'tick marker' angles
float [] xAngle = new float[16];
float [] yAngle = new float[16];

//Screen dimensions
int xScreen = 1000;
int yScreen = 600;

boolean start = false;//Start program boolean
boolean reset = false;//Reset dial values boolean


void settings(){//Set screen size using variables
  size(xScreen, yScreen);
}

void setup(){

//Object instantiation
minim = new Minim(this);
in = minim.getLineIn(Minim.MONO, maxSamples*2);
amdf = new AMDF();
lowPass = new LowPassFS(900, Fs);
dial1 = new Dial(xScreen, yScreen, 0.25, 0.4,1);
dial2 = new Dial(xScreen, yScreen, 0.75, 0.4,1);

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

void draw()
{
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
    dial1.drawDial(dialVal, 0.5); //(value to read, dial speed)
    dial2.drawDial(varianceDial, 5);
    dial2.drawMargin(varianceDial, minMargin, maxMargin);

    reset = false;
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

void debugMode(){

  boolean freqStatus = amdf.getFrequencyStatus();

  if(freqStatus == true){
    //Draw signal indicator
    fill(120,46.2,86.7);
    ellipse(xScreen - 20, yScreen - 20, 20, 20);
  } else {
    fill(2.8,91.8,100);
    ellipse(xScreen - 20, yScreen - 20, 20, 20);
  }
}

//Introduction screen
void introScreen(){
  imageMode(CENTER);
  soloTitle.resize(0,100); 
  image(soloTitle,xScreen*0.5,yScreen*0.45);
  space.resize(0,60);
  image(space,xScreen*0.5,yScreen*0.55);
}

//Limit dial values
float limiter(float value){
  if(value <= 0){
    value = 0;
  }
  if(value >= 270){
    value = 270;
  }
  return value;
}

void storeFreqs(){
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

float calculateMode(){
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


float calculateVariance(){
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
void loadTextImages(){
  //Load text images
  tint(360, 360);
  soloTitle.resize(0,60); 
  image(soloTitle,xScreen*0.25,yScreen*0.80);
  varianceTitle.resize(0,50); 
  image(varianceTitle,xScreen*0.75,yScreen*0.80);

  //Load different images for different difficulties
  if(diffKey ==1){
    easy.resize(0,50); 
    image(easy,xScreen*0.5,yScreen*0.10);
  } else if(diffKey ==2){
    average.resize(0,50); 
    image(average,xScreen*0.5,yScreen*0.10);
  } else if(diffKey ==3){
    hard.resize(0,50); 
    image(hard,xScreen*0.5,yScreen*0.10);
  }
}

//Get user input
void keyPressed(){
  if(key == '1'){
    difficulty = 0.0625;
    print("\n difficulty: ",difficulty);
    diffKey = 1;
  }
  if(key == '2'){
    difficulty = 0.125;
    print("\n difficulty: ",difficulty);
    diffKey = 2;
  }
  if(key == '3'){
    difficulty = 0.25;
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
}

//Print stored modes
void printModes(){
  print("\n ----------");
  for(int x = 0; x<modeFrequencies.length;x++){
    print("\n modeFrequencies["+x+"]: "+modeFrequencies[x]);
  }
  print("\n ----------");
}

//Print variance
void printVariance(){
  print("\n Variance: ",getVariance[0]);
  print("\n ----------");
}
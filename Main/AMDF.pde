class AMDF implements AudioListener
{
  float [] difference = new float[maxSamples];
  float [] sum = {0,0};
  float [] total = new float[maxSamples];
  int largestValue;
  int n;
  float compare = 0.000001;
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
synchronized int calc(){
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
      if(total[n-minSamples] < 0.2){
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

synchronized void plotDifference(){
  for(int i=0;i<total.length - minSamples;i++)
  {
    if(total[i] < 0.01) //Colour the 'tips' differently
    {
    color c = color(0,0,0);
    fill(c);
    stroke(c);  
    } else{
    color c = color(2,82,76);
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
synchronized void clear(){
  largestValue = 0;
}

synchronized int getSampSize(){
  return inMix.length;
}

}
class Dial{

int xS ,yS;
float xPos, yPos;
int resizeVal = 350;
float val = 0;
float scale;
float colourTimer = 0;


PImage dotImage;
  //Constructor
  Dial(int tempXS, int tempYS, float tempXPos, float tempYPos , float tempScale){
    xS = tempXS;
    yS = tempYS;
    xPos = tempXPos;
    yPos = tempYPos;
    scale = tempScale;
  }  

  void drawDial(float degree, float dialMove){

   //Calculate how much to move by
   if(val < degree){
    if(val+dialMove > degree){
      dialMove = dialMove*0.1;
    }
    val = val + dialMove;
  } 
   if(val > degree){
    if(val+dialMove < degree){
      dialMove = dialMove*0.1;
    }
    val = val - dialMove;
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
    xAngle[i] = xS*xPos + (r*scale)*cos(radians(a1+(16.8*(i))));
    yAngle[i] = yS*yPos + (r*scale)*sin(radians(a1+(16.8*(i))));

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
    float tintVal = map(val-16.8*(i+1),0,130,0,360);
    tint(360,tintVal);
    float randRotate = radians(-a1+16.8*(i+1));
    rotate (randRotate);
    image (dotImage, 0, 0);
    popMatrix ();
  }

}

void drawMargin(float tempC, float tempMin, float tempMax){

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
  color c;

  //Determine colour of the margin
  if(draw == true){
    if(isOn == true){
     c = color(120, 46.2, 86.7); 
    } else {
     c = color(2.8,91.8,100); 
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
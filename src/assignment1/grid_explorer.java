package assignment1;

import lejos.robotics.RegulatedMotor;
import lejos.hardware.motor.Motor;
import lejos.hardware.BrickFinder;
import lejos.hardware.Keys;
import lejos.hardware.ev3.EV3;
import lejos.robotics.SampleProvider;
import lejos.hardware.lcd.TextLCD;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.port.SensorPort;
import lejos.robotics.Color;
import lejos.hardware.sensor.EV3IRSensor;
import lejos.utility.Delay;
import lejos.hardware.Sound;
import java.lang.String;



// If we have to MODIFY direction, then we should check this.
//     - int pos = 0 or 5
//     - int dir = 1 or -1 (the robot see initially only long side)
//     - go back to initial point as 0 or 5.
//     - output in lcd should be relative position.
public class grid_explorer {
      private static EV3ColorSensor color_left;
      private static EV3ColorSensor color_right;
      private static EV3IRSensor ir_sensor;
      static TextLCD lcd;
      static RegulatedMotor leftMotor = Motor.B;
      static RegulatedMotor rightMotor = Motor.D;

      static int[][] map = {{99, 99, 99, 99, 99, 99, 99, 99},
                            {99,  0,  0,  0,  0,  0,  0, 99},
                            {99,  0,  0,  0,  0,  0,  0, 99},
                            {99,  0,  0,  0,  0,  0,  0, 99},
                            {99,  0,  0,  0,  0,  0,  0, 99},
                            {99, 99, 99, 99, 99, 99, 99, 99}};
      /*
        map's pos of each position is inverted upside down.
        This is because we should start at (0, 0), but in real map it should be (6, 1), not (0, 0).
        So initial position and number assignment of each direction should be reflect this.

        map pos values :    {{-1, -1, -1, -1, -1, -1, -1, -1},
                             {-1, 18, 19, 20, 21, 22, 23, -1},
                             {-1, 12, 13, 14, 15, 16, 17, -1},
                             {-1,  6,  7,  8,  9, 10, 11, -1},
                             {-1,  0,  1,  2,  3,  4,  5, -1},
                             {-1, -1, -1, -1, -1, -1, -1, -1}};
      */

      // MODIFY when using another direction.
      static int initial_pos  = 0;
      // MODIFY when using another direction.
      static int dir = 1; // 1 : right, -1 : left,
                          // 6 : down, -6 : up
      static int pos = initial_pos;
      static int depth = 1;
      static int[] red_pos  = {99, 99};
      static int[] block_pos  = {99, 99};
      static float value[];
      static int red_count = 0;
      static int block_count = 0;
      static SampleProvider distanceMode;

      // main (): - All sensor initialized by this part.
      //          - Main algorithms of DFS must be producted.
      //          - At last, LCD should be give us some position informations
      //              such as red's and block's relative position.
      public static void main (String[] args){
            EV3 ev3 = (EV3) BrickFinder.getLocal();
            color_left = new EV3ColorSensor(SensorPort.S3);
            color_right = new EV3ColorSensor(SensorPort.S4);
            ir_sensor = new EV3IRSensor(SensorPort.S1);
            lcd = ev3.getTextLCD();
            Keys keys = ev3.getKeys();

            distanceMode = ir_sensor.getDistanceMode();
            value = new float[distanceMode.sampleSize()];

            while (red_count < 2 || block_count < 2)
            {
                  // Current Position is not searched
                  if (map[pos/6+1][pos%6+1] == 0)
                  {
                        if (color_right.getColorID() == Color.RED &&
                              color_left.getColorID() == Color.RED){
                              red_pos[red_count] = pos;
                              red_count++;
                        }
                        map[pos/6+1][pos%6+1] = depth;
                        depth++;
                  }

                  // DFS: Using DFS, the robot should search right first.
                  if (!right_is_searched ())
                  {
                        rotate_right ();
                        if (front_is_clear ())
                              one_forward ();
                        else
                              front_block ();
                  }
                  //
                  else if (!front_is_searched ())
                  {
                        if (front_is_clear())
                              one_forward ();
                        else
                              front_block ();
                  }
                  else if (!left_is_searched ())
                  {
                        rotate_left ();
                        if (front_is_clear())
                              one_forward ();
                        else
                              front_block();
                  }
                  else if (!back_is_searched ())
                  {
                        rotate_back ();
                        if (front_is_clear())
                              one_forward ();
                        else
                              front_block();
                  }
                  else
                  {
                        rotate_dfs_back ();
                        one_forward ();
                        depth--;
                  }
            }

            // go back to initial position
            while (pos != initial_pos)
            {
                  rotate_smallest_dir ();
                  one_forward ();
            }

            // Output all positions of red's and block's position.
            // MODIFY when using another direction.
            lcd.drawString(red_pos[0]%6 + "," + red_pos[0]/6 +"R",1,1);
            lcd.drawString(red_pos[1]%6 + "," + red_pos[1]/6 +"R",1,3);
            lcd.drawString(block_pos[0]%6 + "," + block_pos[0]/6 +"B",1,5);
            lcd.drawString(block_pos[1]%6 + "," + block_pos[1]/6 +"B",1,7);

            leftMotor.stop(true);
            rightMotor.stop(true);
            leftMotor.close();
            rightMotor.close();

            while(keys.getButtons() != Keys.ID_ESCAPE);
      }

      // right_is_searched (): Using temporary variables, check whether should we
      //                         have to check right side.
      public static boolean right_is_searched ()
      {
            int tmpdir, tmppos;
            switch (dir)
            {
                  case 1:
                        tmpdir = -6;
                        break;
                  case -1:
                        tmpdir = 6;
                        break;
                  case 6:
                        tmpdir = 1;
                        break;
                  case -6:
                        tmpdir = -1;
                        break;
                  default:
                       tmpdir = 0;
            }
            int ret;

            if (pos%6 == 5 && ((pos+tmpdir)%6 == 0 || (pos+tmpdir+6)%6 == 1))
                  ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+7];
            else
               ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+1];

            return ret != 0;
      }

      // front_is_searched (): Using temporary variables, check whether should we
      //                         have to check front.
      public static boolean front_is_searched ()
      {
            int tmppos;
            tmppos = pos + dir;
            int ret=0;
            if (pos%6 == 5 && ((pos+dir+6)%6 == 0 || (pos+dir+6)%6 == 1))
                  ret = map[(pos+dir)/6+1][(pos + dir+6)%6+7];
            else{
                  ret = map[(pos+dir)/6+1][(pos + dir+6)%6+1];
            }
            return ret != 0;
      }

      // left_is_searched (): Using temporary variables, check whether should we
      //                        have to check left side.
      public static boolean left_is_searched ()
      {
            int tmpdir, tmppos;
            switch (dir)
            {
                  case 1:
                        tmpdir = 6;
                        break;
                  case -1:
                        tmpdir = -6;
                        break;
                  case 6:
                        tmpdir = -1;
                        break;
                  case -6:
                        tmpdir = 1;
                        break;
                  default:
                         tmpdir = 0;
            }
            int ret;

            if (pos%6 == 5 && ((pos+tmpdir+6)%6 == 0 || (pos+tmpdir+6)%6 == 1))
                  ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+7];
            else
                  ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+1];

            return ret != 0;
      }

      // back_is_searched (): Using temporary variables, check whether should we
      //                        have to check back.
      public static boolean back_is_searched ()
      {
            int tmpdir, tmppos;
            tmpdir = -dir;
            int ret;

            if (pos%6 == 5 && ((pos+tmpdir+6)%6 == 0 || (pos+tmpdir+6)%6 == 1))
                  ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+7];
            else
                  ret = map[(pos+tmpdir)/6+1][(pos + tmpdir+6)%6+1];

            return ret != 0;
      }

      // front_is_clear (): Check whether there are no block in front.
      //                    Before calling this function, the program makes sure
      //                      the robot see forward. This is because our robot
      //                      don't have side checker.
      public static boolean front_is_clear ()
      {
            distanceMode.fetchSample (value, 0);
            float dist = value[0];
            lcd.clear();
            lcd.drawString(dist+" ",1,1);
            if (dist < 25 && dist >0)
               return false;
            return true;
      }

      // front_block (): If the robot detect block, this function deal with the
      //                   variables such as map[][], block_count, ....
      public static void front_block ()
      {
            int tmppos;
            tmppos = pos + dir;
            map[tmppos/6+1][tmppos%6+1] = 77;
            block_pos[block_count] = tmppos;
            block_count++;
            Sound.beep();
      }

      // rotate_dfs_back (): When all 4 sides have trackted, then go back to
      //                       bifurcation using depth in map[][].
      public static void rotate_dfs_back()
      {
            int smdir = 0;
            int posvalue = map[pos/6+1][pos%6+1];

            if (posvalue-1 == map[pos/6][pos%6+1])
            {
                  smdir = -6;
            }
            else if (posvalue-1 == map[pos/6+2][pos%6+1])
            {
                  smdir = +6;
            }
            else if (posvalue-1 == map[pos/6+1][pos%6])
            {
                  smdir = -1;
            }
            else if (posvalue-1 == map[pos/6+1][pos%6+2])
            {
                  smdir = +1;
            }

            set_direction (smdir);
      }

      // rotate_smallest_dir (): This function should be called when the robot
      //                           already searched 2 reds and 2 blocks.
      //                         Find smallest way to go home.
      public static void rotate_smallest_dir ()
      {
            int smdir = 0;
            int smallest = 99;

            if (smallest > map[pos/6][pos%6+1] && map[pos/6][pos%6+1] > 0 )
            {
                  smdir = -6;
                  smallest = map[pos/6][pos%6+1];
            }
            if (smallest > map[pos/6+2][pos%6+1] && map[pos/6+2][pos%6+1] > 0)
            {
                  smdir = +6;
                  smallest = map[pos/6+2][pos%6+1];
            }
            if (smallest > map[pos/6+1][pos%6] && map[pos/6+1][pos%6] > 0)
            {
                  smdir = -1;
                  smallest = map[pos/6+1][pos%6];
            }
            if (smallest > map[pos/6+1][pos%6+2] && map[pos/6+1][pos%6+2] > 0)
            {
                  smdir = +1;
                  smallest = map[pos/6+1][pos%6+2];
            }
            lcd.clear();
            lcd.drawInt(smdir, 1, 1);
            lcd.drawInt(smallest, 1, 3);

            set_direction (smdir);
      }

      // set_direction (set): Set robot's direction to argument 1.
      //                      Argument 1 must follow the rule of comments above
      //                      (definition of dir variable).
      public static void set_direction (int set)
      {
            if (set == -dir)
                  rotate_back();
            else if (set == dir)
                  return;
      
            else if (set == -1)
            {
                  switch(dir)
                  {
                        case 6:
                              rotate_left();
                              break;
                        case -6:
                              rotate_right();
                              break;
                  }
            }
            else if (set == 1)
            {
                  switch(dir)
                  {
                        case 6:
                              rotate_right();
                              break;
                        case -6:
                              rotate_left();
                              break;
                  }
            }
            else if (set == -6)
            {
                  switch(dir)
                  {
                        case -1:
                              rotate_left();
                              break;
                        case 1:
                              rotate_right();
                              break;
                  }
            }
            else
            {
                  switch(dir)
                  {
                        case -1:
                              rotate_right();
                              break;
                        case 1:
                              rotate_left();
                              break;
                  }
            }
      }


      //moves forward 23cms(rough estimate) - commented the whole function in case we need it for later.
      public static void one_forward (){
            Sound.buzz();

            EV3 ev3 = (EV3) BrickFinder.getLocal();
            TextLCD lcd = ev3.getTextLCD();
            //Keys keys = ev3.getKeys();

            //constants for checking if the sensor has reached the black line
            int right_met = 0;
            int left_met = 0;

            //first, it just moves until the do/while condition is met
            rightMotor.setSpeed(400);
            leftMotor.setSpeed(400);
            leftMotor.endSynchronization();
            rightMotor.forward();
            leftMotor.forward();

            //do while loop checks if black line is detected, every 10 ms.
            do{

                  //result string containing the current color
                  String str = "";

                  //left color id and right color id
                  int left_cid = color_left.getColorID();
                  int right_cid = color_right.getColorID();

                  //if left wheel has not run into black
                  if(left_met == 0){
                        switch(left_cid){
                              //if black line is detected, then stop
                              case Color.BLACK:
                                    leftMotor.stop(true);
                                    Delay.msDelay(40);
                                    left_met = 1;
                                    str+= "black";
                                    break;
                              //if white, just save white to the result string
                              case Color.WHITE:
                                    str += "white";
                                    break;
                              default:
                                    break;
                        }
                  }

                  //same algorithm from the left motor, but for the right motor.
                  if(right_met == 0){
                        switch(right_cid){
                              case Color.BLACK:
                                    rightMotor.stop(true);
                                    Delay.msDelay(40);
                                    right_met = 1;
                                    str+= "black";
                                    break;
                              case Color.WHITE:
                                    str += "white";
                                    break;
                              default:
                                    break;
                        }
                  }
                  //draws the result string, but it did not work when I tried last time. will have to try again
                  lcd.drawString(str,1,5);
                  Delay.msDelay(10);
            }
            while (left_met == 0 && right_met == 0);

            //Once it stops at the black line, it moves forward a little bit to center itself in the grid cell

            leftMotor.setSpeed(400);
            rightMotor.setSpeed(400);
            leftMotor.forward();
            rightMotor.forward();

            try{
                  Thread.sleep(1225);
            }catch(InterruptedException e){}

            leftMotor.stop(true);
            rightMotor.stop(true);

            pos += dir;
      }

      //function to rotate to the right side, 90 degrees
      public static void rotate_right (){
            leftMotor.setSpeed(200);
            rightMotor.setSpeed(200);
            leftMotor.forward();
            rightMotor.backward();

            try{
                  Thread.sleep(1155);
            }catch(InterruptedException e){}

            leftMotor.stop(true);
            rightMotor.stop(true);

            switch (dir)
            {
                  case 1:
                        dir = -6;
                        break;
                  case -1:
                        dir = 6;
                        break;
                  case 6:
                        dir = 1;
                        break;
                  case -6:
                        dir = -1;
                        break;
            }
      }

      //function to rotate to the right side, 90 degrees
      public static void rotate_left (){
            leftMotor.setSpeed(200);
            rightMotor.setSpeed(200);
            leftMotor.backward();
            rightMotor.forward();

            try{
                  Thread.sleep(1155);
            }catch(InterruptedException e){}

            leftMotor.stop(true);
            rightMotor.stop(true);

            switch (dir)
            {
                  case 1:
                        dir = 6;
                        break;
                  case -1:
                        dir = -6;
                        break;
                  case 6:
                        dir = -1;
                        break;
                  case -6:
                        dir = +1;
                        break;
            }
      }

      public static void rotate_back ()
      {
            rotate_left ();
            rotate_left ();
      }

/*
      //function to brake
      public static void flt (){
            leftMotor.flt();
            rightMotor.flt();
      }
*/

/*
      //function to stop
      public static void stop (){
            leftMotor.stop(true);
            rightMotor.stop(true);
            try{
                  Thread.sleep(1050);
            }
            catch(InterruptedException e){}
      }
*/
/*
      public static int search_map(int[][] map){
            for(int i = 0; i < map.length; i++)
                  for(int j = 0; j < map[i].length; j++)
                        return map[i][j];
            return 1;
      }
/*
      public static float block_check(EV3IRSensor IRsensor){
            SampleProvider distanceMode = IRsensor.getDistanceMode();
            float value[] = new float[distanceMode.sampleSize()];
            distanceMode.fetchSample(value, 0);
            float centimeter = value[0];
            return centimeter;
      }
*/
}


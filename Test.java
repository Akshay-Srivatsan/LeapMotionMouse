//This program should move the mouse according to the fingers of the user
//It features the following actions:
//Movement
//Click
//Right click
//Scroll
//Click and drag

//Careful using these packages together! There is Frame class ambiguity.
import com.leapmotion.leap.*;
import java.awt.*;
import java.awt.event.InputEvent;

public class Test
{
	/**
	 *This double sets the mouse sensitivity. The higher the number, the
	 *less the user needs to move his/her hand to reach the entire screen.
	 */
	public static double sensitivity = 2.5;
	
	public static void main(String[] args) throws AWTException
	{
		//Initialize robot
		Robot robot = new Robot();
		//Initialize controller
		Controller controller = new Controller();
		//Set up a frame, as well as a previous frame
		com.leapmotion.leap.Frame frame = controller.frame();
		com.leapmotion.leap.Frame oldFrame = controller.frame();
		
		//Continue indefinitely
		while(true)
		{
			//Refresh the frame
			frame = controller.frame();
			//Check that hands are present
			if(!frame.hands().empty())
			{
				//Get the first hand
				Hand hand = frame.hands().get(0);
				//Check that fingers are present
				FingerList fingers = hand.fingers();
				FingerList oldFingers = oldFrame.hands().get(0).fingers();
				
				//This chunk is where we actually detect and act on gestures
				if(!fingers.empty())
				{
					//Get the first finger
					Finger finger = fingers.get(0);
					//Hold the position information and print
					Vector position = finger.tipPosition();
					Vector direction = finger.direction();

					//If the position is far from the screen and only one finger is present, move the mouse
					if((position.get(2) > 10) && (fingers.count() == 1))
					{
						try
						{
							setMousePosition(position, direction, robot);
						}
						catch(AWTException e)
						{
							System.out.println("Error in moving mouse: " + e);
						}
					}

					//If the position is moderately close and the hand is open, right click
					else if((position.get(2) < 10) && (position.get(2) > 0) &&
						(fingers.count() > 1) && !(oldFingers.count() > 1))
					{
						clickMouse(InputEvent.BUTTON3_MASK, robot);
					}

					//If the position is very close, left click
					else if(position.get(2) < 0 && !(oldFingers.get(0).tipPosition().get(2) < 0))
					{
						clickMouse(InputEvent.BUTTON1_MASK, robot);
					}

					//If a second hand is present, click and hold
					if(frame.hands().count() > 1 && oldFrame.hands().count() <= 1)
					{
						hold(InputEvent.BUTTON1_MASK, robot);
					}

					//If a second hand is removed, release the mouse
					if(frame.hands().count() <= 1 && oldFrame.hands().count() > 1)
					{
						release(InputEvent.BUTTON1_MASK, robot);
					}

					//If two fingers are present, and are moving up or down at a decent rate, scroll
					if((fingers.count() > 1) &&
						(Math.abs(position.get(1) - oldFingers.get(0).tipPosition().get(1)) > 0) &&
						(position.get(2) > 10))
					{
						scrollMouse((int)(position.get(1) - oldFingers.get(0).tipPosition().get(1)), robot);
					}
				}
			}
			oldFrame = frame;
		}
	}

	/**
	 *This method scrolls the mouse one notch in the given direction.
	 *@param diff - The direction to scroll, given as the displacement of the fingers
	 *@param robot - The robot to use to scroll
	 */
	public static void scrollMouse(int diff, Robot robot)
	{
		if(diff != 0)
		{
			diff = diff / Math.abs(diff);
			robot.mouseWheel(diff);
		}
	}

	/**
	 *This method clicks and holds the mouse.
	 *@param mask - The button to hold
	 *@param robot - The robot to use to click
	 */
	public static void hold(int mask, Robot robot)
	{
		robot.mousePress(mask);
	}
	
	/**
	 *This method releases a held mouse
	 *@param mask - The button to release
	 *@param robot - The robot to use to release
	*/
	public static void release(int mask, Robot robot)
	{
		robot.mouseRelease(mask);
	}
	
	/**
	 *This method clicks the mouse once by clicking and releasing.
	 *@param mask - The button to click
	 *@param robot - The robot to use to click
	*/
	public static void clickMouse(int mask, Robot robot)
	{
		robot.mousePress(mask);
		robot.mouseRelease(mask);
	}
	
	/**
	 *This method moves the mouse to the location onscreen that the
	 *user is pointing towards.
	 *@param position - The finger position
	 *@param direction - The finger direction
	 *@param robot - THe robot to use to move the mouse
	*/
	public static void setMousePosition(Vector position, Vector direction, Robot robot) throws AWTException
	{
		//Get dimensions of screen in pixels
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		//Get screen resolution in pixels per inch
		int resolution = Toolkit.getDefaultToolkit().getScreenResolution();
		//Find scale to increase direction by
		double scale = Math.abs(position.get(2) / direction.get(2));
		//Initialize position arrays
		double[] superFinger = new double[3];
		double[] newPosition = new double[3];
		
		//Scale all 3 dimensions of the finger
		for(int ii = 0; ii < 3; ii++)
		{
			//Extend finger until it reaches Z = 0 plane
			superFinger[ii] = scale * direction.get(ii);
			//Get the position when it intersects Z = 0 plane and convert to pixels (mm * in/mm * px/in)
			newPosition[ii] = (superFinger[ii] + position.get(ii)) * 0.0393701 * resolution;
		}
		
		//Adjust for sensitivity
		newPosition[0] *= sensitivity;
		//Move the mouse
		int x = (int)(newPosition[0] + (screenSize.getWidth() / 2));
		int y = (int)(screenSize.getHeight() / 2 + (screenSize.getHeight() / 2 - newPosition[1])*sensitivity);
		robot.mouseMove(x, y);
	}
}

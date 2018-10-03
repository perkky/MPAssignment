
import java.io.File;
import net.sourceforge.tess4j.*;
import java.awt.Rectangle;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters; 
import org.opencv.features2d.*;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.ImageIcon;
import javax.swing.JLabel;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

import java.lang.Math;
import java.io.PrintWriter;

import java.util.LinkedList;
import java.util.Queue;


public class Label extends Thread
{
	private Thread pThread;				//the thread
	private String pThreadName;
	public boolean finished = false;

    private Mat pMat;

	//When channel is 2
    private String pc2Text1 = "";   //first word
    private String pc2Text2 = "";         //second word
    private Point pc2Text1Loc;            //the location of the first word
    private String pc2ClassNum = "(none)";

	//When channel is 3
	private String pc3Text1 = "";   //first word
    private String pc3Text2 = "";         //second word
    private Point pc3Text1Loc;            //the location of the first word
    private String pc3ClassNum = "(none)";

	private String pSymbol = "Flame";

	/*********Multithreading*************/
	public void start()
	{
		if (pThread == null)
		{
			pThread = new Thread(this, pThreadName);
			pThread.start();
		}
	}
	public void run()
	{
		detect();//detect class and text
        detectSymbol();
		try{ Thread.sleep(20); } catch (Exception e) { System.out.println(e.getMessage()); }
		
		finished = true;
	}
	public void setThreadName(String name)
	{
		pThreadName = name;
	}


    public Label(Mat mat)
    {
        pMat = mat; //will be a 500x500
    }

    //************* Colour Functions ****************

    public String getTopColour()
    {
        Mat hsvMat = ImgProcessing.convertImage(pMat, Imgproc.COLOR_BGR2HSV);

        return getColourInTriangle(hsvMat, new Point(pMat.cols()/2, 0), pMat.rows()/2, pMat.cols());
    }

    public String getBotColour()
    {
        Mat hsvMat = ImgProcessing.convertImage(pMat, Imgproc.COLOR_BGR2HSV);

        return getColourInTriangle(hsvMat, new Point(pMat.cols()/2, pMat.rows()), -pMat.rows()/2, pMat.cols());
    }

    //Takes a double array of size 3 and returns what colour it is
	//Based on colour definitions in notes.txt
	//To be used on HSV only
	private static String checkColourHSV(double[] col)
	{
		String s = "Black";	//default is black
        if (col.length == 3)
        {
            double hue = col[0], sat = col[1], val = col[2];


            if (val <= 30)//40
                s = "Black";
            else if (sat < 75 && val > 176)
                s = "White";
            else if (7 <= hue && hue < 14 )
                s = "Orange";
            else if ((0 <= hue && 7 > hue) || (148 <= hue && hue <= 180))
                s = "Red";
            else if ( 39 <= hue && 83 > hue)
                s = "Green";
            else if (83 <= hue && 148 > hue)
                s = "Blue";
            else if (14 <= hue && hue < 39)
                s = "Yellow";
        }

		return s;
	}

    //This function gets the average colour inside an isosceles triangle
	//It does this buy taking the median colour of 20 points (4 height positions with 5 width positions per height)
	//p1 refers to the apex of the triangle
	//height is in reference to apex - +ve is downwards and -ve is upwards
	private static String getColourInTriangle(Mat mat, Point p1, int height, int width)
	{
		String s = "Black"; //default is black

		if ((height < 7 && height > -7 ) || (width < 7)) //height and width have to be greater than 7
			throw new IllegalArgumentException("Unable to get colour, width and height need to be above 6 pixels");
		else
		{
            //Initialise colour array - 0-7 is black, white, red, orangle, green, blue and yellow respectively
			int[] cols = {0, 0, 0, 0, 0, 0, 0};
			String temp;

			for (int i = 2; i < 6; i++)
			{
				double newWidth = width * i/6;
				for (int j = 1; j < 6; j++)
				{
					temp = checkColourHSV(mat.get( (int)(p1.y + height*i/6), (int)(p1.x - newWidth/2 + newWidth*j/6)) ); //get the colour 

					if (temp.equals("Black"))
						cols[0]++;
					else if (temp.equals("White"))
						cols[1]++;
					else if (temp.equals("Red"))
						cols[2]++;
					else if (temp.equals("Orange"))
						cols[3]++;
					else if (temp.equals("Green"))
						cols[4]++;
					else if (temp.equals("Blue"))
						cols[5]++;
					else if (temp.equals("Yellow"))
						cols[6]++;
				}
			}

			//find the most common colour
			int index = 0;
			for (int i = 1; i < 7; i++)
			{
				if (cols[index] < cols[i])
				{
					index = i;
				}
			}

			//set the most common colour
			switch (index)
			{
				case 6:
					s = "Yellow";
					break;
				case 5:
					s = "Blue";
					break;
				case 4:
					s = "Green";
					break;
				case 3:
					s = "Orange";
					break;
				case 2:
					s = "Red";
					break;
				case 1:
					s = "White";
					break;
                case 0:
                    s = "Black";
                    break;
				default:
					s = "Black";
					break;
			}

		}

		return s;
	}

    //**************** Text Functions ******************
    //convert hsv - split to 3rd/2nd depending on text colour - threshold - blob detection - isolation of specific letters - combination of words -
    //- map each word to a point - return words that fit a point

    private class DetectedText
    {
        private String pText;
        private Point pLocation;

        public DetectedText(String text, Point location)
        {
            pText = text;
            pLocation = location;
        }

        public String getText() { return pText; }
        public Point getLocation() { return pLocation;}
    }

	public List<DetectedText> detectText(Mat hsvMat, boolean inverse)
	{
		int channel = 3;
		 //threshold the image

		Mat thresholdImg = ImgProcessing.adaptiveThresholdOnChannel(hsvMat, channel, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C );
		Mat grayThresholdImg = ImgProcessing.threshold(ImgProcessing.getChannel(thresholdImg, channel), 1);

		if (inverse)
		{
			grayThresholdImg = ImgProcessing.invertImage(grayThresholdImg);
			Imgproc.dilate(grayThresholdImg, grayThresholdImg, Mat.ones(2,2,grayThresholdImg.type()));
			Imgproc.medianBlur(grayThresholdImg, grayThresholdImg, 3);//DEBUG - not sure if should keep or not - needs testing
		}
		else
			Imgproc.dilate(grayThresholdImg, grayThresholdImg, Mat.ones(2,2,grayThresholdImg.type()));

        try{
				//FileIO.showImage(grayThresholdImg,"");//DEBUG
			}
			catch (Exception e) { }

        //blob detection
		FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
		MatOfKeyPoint keypoints = new MatOfKeyPoint();

		blobDetector.read("blob.xml");	//reads the properties

		blobDetector.detect(grayThresholdImg, keypoints);

        org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0,0,255);
        //org.opencv.features2d.Features2d.drawKeypoints(pMat, keypoints, pMat, cores, Features2d.DRAW_RICH_KEYPOINTS  );//DEBUG

        return getStrings(grayThresholdImg, keypoints.toArray(), 120, 24);
	}

    public void detect()
    {
        //convert to hsv
        Mat hsvMat = ImgProcessing.convertImage(pMat, Imgproc.COLOR_BGR2HSV);

		/*
        //threshold the image
        int channel = 3;//getBestChannel(hsvMat);
        Mat thresholdImg = ImgProcessing.adaptiveThresholdOnChannel(hsvMat, channel, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C );
        Mat grayThresholdImg = ImgProcessing.threshold(ImgProcessing.getChannel(thresholdImg, channel), 1);
		Imgproc.dilate(grayThresholdImg, grayThresholdImg, Mat.ones(2,2,grayThresholdImg.type()));

        try{
				FileIO.showImage(grayThresholdImg,"");//DEBUG
			}
			catch (Exception e) { }

        //blob detection
		FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
		MatOfKeyPoint keypoints = new MatOfKeyPoint();

		blobDetector.read("blob.xml");	//reads the properties

		blobDetector.detect(grayThresholdImg, keypoints);

        org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0,0,255);
        //org.opencv.features2d.Features2d.drawKeypoints(pMat, keypoints, pMat, cores, Features2d.DRAW_RICH_KEYPOINTS  );//DEBUG

        List<DetectedText> detectedTextList = getStrings(grayThresholdImg, keypoints.toArray(), 120, 24);
        DetectedText[] detectedTextArray = detectedTextList.toArray(new DetectedText[0]);
		*/
		List<DetectedText> detectedTextList2 = detectText(hsvMat, false);
		List<DetectedText> detectedTextList3 = detectText(hsvMat, true);

		DetectedText[] detectedTextArray2 = detectedTextList2.toArray(new DetectedText[0]);
		DetectedText[] detectedTextArray3 = detectedTextList3.toArray(new DetectedText[0]);

		//for the second channel
        for (DetectedText text : detectedTextArray2)
        {
            if (validTextPosition(text.getLocation()) && text.getText().length() > 2 )
            {
                if (!text.getText().equals("") && !text.getText().equals("O"))
                {
                    //If no text has been set yet
                    if (pc2Text1.equals(""))
                    {
                        pc2Text1 = text.getText();
                        pc2Text1Loc = text.getLocation();
                    }
                    //If a text has been set, compare the positions
                    //The text that is higher will come first
                    else if (pc2Text2.equals(""))
                    {
                        if (pc2Text1Loc.y > text.getLocation().y)
                        {
                            pc2Text2 = pc2Text1;
                            pc2Text1 = text.getText();
                        }
                        else
                            pc2Text2 =  text.getText();
                    }
                    else
                        pc2Text2 += text.getText();
                }
            }
            else if (validClassPosition(text.getLocation()))
                if (!text.getText().equals(""))
                    pc2ClassNum = text.getText();

        }

		//for the first channel
		for (DetectedText text : detectedTextArray3)
        {
            if (validTextPosition(text.getLocation()) && text.getText().length() > 2 )
            {
                if (!text.getText().equals("") && !text.getText().equals("O"))
                {
                    //If no text has been set yet
                    if (pc3Text1.equals(""))
                    {
                        pc3Text1 = text.getText();
                        pc3Text1Loc = text.getLocation();
                    }
                    //If a text has been set, compare the positions
                    //The text that is higher will come first
                    else if (pc3Text2.equals(""))
                    {
                        if (pc3Text1Loc.y > text.getLocation().y)
                        {
                            pc3Text2 = pc3Text1;
                            pc3Text1 = text.getText();
                        }
                        else
                            pc3Text2 =  text.getText();
                    }
                    else
                        pc3Text2 += text.getText();
                }
            }
            else if (validClassPosition(text.getLocation()))
                if (!text.getText().equals(""))
                    pc3ClassNum = text.getText();

        }
        

    }

    //Checks if it is a valid text position - may need to move from hard coded to xml reading
    private boolean validTextPosition(Point p)
    {
        boolean flag = false;
        Rect rect = new Rect(37, 193, 424, 131);//78,193,336,131
        if (rect.contains(p))
            flag = true;

        return flag;
    }
    private boolean validClassPosition(Point p)
    {
        boolean flag = false;
        Rect rect = new Rect(181, 343, 166, 126);
        if (rect.contains(p))
            flag = true;

        return flag;
    }

    //returns all the strings in an image, \n divides each string
	//maxdist is the max dist a letter can be from another letter (based on the keypoint)
	//takes a long time to do ocr per image
	private List<DetectedText> getStrings(Mat mat, KeyPoint[] kp, int maxdistX, int maxdistY)
	{
		List<DetectedText> detectedText = new ArrayList<>();

		//holds the letter for each keypoint (index's corresponding to kp's)
		Mat[] letters = new Mat[kp.length];
        Mat[] uncropperLetters = new Mat[kp.length];

        //get each letter mat
		for (int i = 0; i < kp.length; i++)
		{
			Mat[] mats =  getLetter(mat, kp[i]);
            letters[i] = mats[1];
            uncropperLetters[i] = mats[0];

            
            //check if it matches a letter already
            for (int j = 0; j < i; j++)
            {
                boolean erase = false;

                if (uncropperLetters[i].rows() == uncropperLetters[j].rows() && uncropperLetters[i].cols() == uncropperLetters[j].cols())
                {
                    Mat subtract = new Mat();
                    Core.subtract(uncropperLetters[i], uncropperLetters[j], subtract);

                    if (Core.countNonZero(subtract) < 5)
                        erase = true;
                }


                if (erase)
                {
                    letters[i] = Mat.zeros(1, 1, letters[i].type());
                    uncropperLetters[i] = Mat.zeros(1, 1, letters[i].type());
                }
            }

			//make sure is valid
			if (!validTextPosition(kp[i].pt) && !validClassPosition(kp[i].pt))
			{
				letters[i] = Mat.zeros(1, 1, letters[i].type());
                uncropperLetters[i] = Mat.zeros(1, 1, letters[i].type());
			}
			else if (validClassPosition(kp[i].pt) && ImgProcessing.getNumWhitePixels(letters[i]) < 350 )
			{
				double ratio = (double)ImgProcessing.getLongestVerticalLine(letters[i])/(double)ImgProcessing.getLongestHorizontalLine(letters[i]);

				//only if its not a square, delete it
				if (ratio < 0.9 || ratio > 1.1)
				{
					letters[i] = Mat.zeros(1, 1, letters[i].type());
					uncropperLetters[i] = Mat.zeros(1, 1, letters[i].type());
				}
			}
			

		}
		
		Queue<Integer> currentLetters = new LinkedList<>();
		LinkedList<Integer> pastLetters = new LinkedList<>();

		//remove any blobs that are blank
		for (int i = 0; i < kp.length; i++)
		{
			if (letters[i].empty())
				pastLetters.add(i);
		}

		//loop until every letter has been visited
		while (pastLetters.size() != kp.length)
		{
			LinkedList<Mat> currentLetterMats = new LinkedList<>();
			LinkedList<Integer> currentLetterIdx = new LinkedList<>();

			//find the furtherest left letter that hasnt been visited
			int minx = 10000;
			int idx = -1;
			int currIdx = 0;
			for (KeyPoint p : kp)
			{
				if ((int)p.pt.x < minx && !pastLetters.contains(currIdx))
				{
					minx = (int)p.pt.x;
					idx = currIdx;
				}

				currIdx++;
			}

			currentLetters.add(idx);
			pastLetters.add(idx);

			//find all leters that are within max dist from all letters that are within max dist of each other
			while (currentLetters.size() != 0)
			{
				int pointIndex = currentLetters.remove();
				currentLetterMats.add(letters[pointIndex]);
				currentLetterIdx.add(pointIndex);
				Point currentPoint = kp[pointIndex].pt;

				int i = 0;
				for (KeyPoint p : kp)
				{
					int distX = (int)currentPoint.x - (int)p.pt.x;
					int distY = (int)currentPoint.y - (int)p.pt.y;
					//System.out.println(Math.sqrt(distX*distX + distY*distY));
					if (distX*distX < maxdistX*maxdistX && distY*distY < maxdistY*maxdistY && !pastLetters.contains(i))
					{
						currentLetters.add(i);
						pastLetters.add(i);
					}

					i++;
				}
			}

			

			int width = 0;
			int maxHeight = 0;
			Mat[] ms = currentLetterMats.toArray(new Mat[0]);
			Integer[] indexs = currentLetterIdx.toArray(new Integer[0]);

			//after all leters within a certain range are found
			//sort the array to be in order of x values
			for (int i = 0; i < ms.length; i++)
			{
				for (int j = 0; j < ms.length-1; j++)
				{
					if (kp[indexs[j]].pt.x > kp[indexs[j+1]].pt.x)
					{
						Integer tempInt = indexs[j];
						Mat tempMat = ms[j];
						indexs[j] = indexs[j+1];
						ms[j] = ms[j+1];
						indexs[j+1] = tempInt;
						ms[j+1] = tempMat;
					}
				}
			}

			
			//Get the midpoint of the letter
			Point midpoint = new Point((kp[indexs[indexs.length-1]].pt.x + kp[indexs[0]].pt.x)/2, (kp[indexs[indexs.length-1]].pt.y + kp[indexs[0]].pt.y)/2);


			//only do this if the word is more than 3 letters
			if ((validTextPosition(midpoint) && ms.length > 2) ||  (validClassPosition(midpoint) ))//&& ms.length < 4
			{

				//create new mat contraining each found letter
				//Go through once to find the parameters of the new mat
				for (Mat m : ms ) 
				{
					width += m.cols();

					if (m.rows() > maxHeight)
						maxHeight = m.rows();
				}
				Mat currentMat = Mat.zeros(maxHeight, width, mat.type());
				
				width = 0;
				for (Mat m : ms )
				{
					//m.copyTo(currentMat.submat(0, m.rows()-1, width, width+m.cols()-1));

					for (int i = 0; i < m.cols(); i++)
						for (int j = 0; j < m.rows(); j++)
							currentMat.put(j, width +i, m.get(j,i));

					width += m.cols();
				}
				
				if ((validTextPosition(midpoint) && ImgProcessing.getNumWhitePixels(currentMat) > 240 ) ||  (validClassPosition(midpoint) && ImgProcessing.getNumWhitePixels(currentMat) > 400 ))
				{

					//do ocr on this new mat
					try{
						//FileIO.showImage(currentMat,"");//DEBUG
						//System.out.println(OCR.doOCR(currentMat)+" "+ midpoint);//DEBUG
						OCR ocr = new OCR();
						detectedText.add(new DetectedText(ocr.doOCR(currentMat), midpoint));
					}
					catch (Exception e) { }
				}
			}
		}

		return detectedText;
	}

    //gets and returns a mat containing the letter from a keypoint
    //returns both a cropped and uncropped letter
	private Mat[] getLetter(Mat mat, KeyPoint keypoint)
	{
        //Create a blank image the size of the mat as that is the maximum possible size the letter could be
		Mat image = Mat.zeros(mat.rows(), mat.cols(), mat.type());

		byte[] return_buff = new byte[(int) (mat.total() *mat.channels())]; //The image in array format
		int[] visited = new int[(int) (mat.total() *mat.channels())];       //hold whether each pixel has been visited
        mat.get(0, 0, return_buff);                                         //load the image into array format

		
		Point pt;                                               //Current point
		Queue<Point> queue = new LinkedList<>();                //Queue of visited black pixels
		double[] val = {255.0};
		int minx = 10000, miny = 10000, maxx = -1, maxy = -1;

		//Find the closest black pixel to the point
		if (return_buff[(int)keypoint.pt.y*mat.cols()+(int)keypoint.pt.x] == 0)
		{
			queue.add(new Point((int)keypoint.pt.x, (int)keypoint.pt.y));
			visited[(int)(keypoint.pt.y)*mat.cols()+(int)keypoint.pt.x] = 1;
		}
		else
		{
			int i = 1, j = 1;
			while (queue.size() == 0)
			{
				for (int k = -i; k < i+1; k++)
				{
					for (int l = -j; l < j+1; l++)
					{
						if (return_buff[(int)(keypoint.pt.y+l)*mat.cols()+(int)keypoint.pt.x+k] == 0)
						{
							queue.add(new Point((int)keypoint.pt.x+k, (int)keypoint.pt.y+l));
							visited[(int)(keypoint.pt.y+l)*mat.cols()+(int)keypoint.pt.x+k] = 1;
						}
					}
				}
				i++;
				j++;
			}
		}

		//loop through to find all black pixels that are connected, starting with the first pixel
		int size = 0;
		int found = 0;
		while (queue.size() != 0)
		{

			pt = queue.remove();
			image.put((int)pt.y, (int)pt.x, val);   //draw this pixel onto the new image

			for (int i = -1; i < 2; i++)
			{
				for (int j = -1; j < 2; j++)
				{
					if ((int)pt.y+j >-1 && (int)pt.y+j < mat.rows() && (int)pt.x+i >-1 && (int)pt.x+i < mat.cols() )
					{
						if (return_buff[(int)(pt.y+j)*mat.cols()+(int)pt.x+i] == 0 && visited[(int)(pt.y+j)*mat.cols()+(int)pt.x+i] != 1)
						{
							//set min/max points
							if ((int)pt.x+i < minx)
								minx = (int)pt.x+i;
							else if ((int)pt.x+i > maxx)
								maxx = (int)pt.x+i;
							if ((int)pt.y+j < miny)
								miny = (int)pt.y+j;
							else if ((int)pt.y+j > maxy)
								maxy = (int)pt.y+j;

							queue.add(new Point((int)pt.x+i, (int)pt.y+j));
							visited[(int)(pt.y+j)*mat.cols()+(int)pt.x+i] = 1;

							found++;
							size++;
						}

						if (visited[(int)(pt.y+j)*mat.cols()+(int)pt.x+i] != 1)
						{
							size++;
							visited[(int)(pt.y+j)*mat.cols()+(int)pt.x+i] = 1;
						}
					}
				}
			}

			
			
		}

        //The final image
		Mat newMat = new Mat();
		
		//if no points were found return a blank mat
        //or if a letter was bigger than 50% of the image
		if (maxx == -1 || maxy == -1 || minx == 10000 || miny == 10000 || (maxx-minx)*(maxx-minx) > 0.5*mat.rows()*mat.cols() || maxy-miny < 10 || maxx-minx < 5 || maxy-miny > 100 )//|| (double)found/(double)size < (double)(maxx-minx)*(maxy-miny)/( (maxx-minx)*(maxy-miny)+450) ) //|| maxy-miny > 90 )
		{
			newMat = Mat.zeros(10,10, mat.type());
		}
		else
		{
            //crop the image to just the letter
			Rect rect = getRect(minx, miny, maxx, maxy, mat.rows(), mat.cols(), 10);

			newMat = ImgProcessing.cropImage(image, rect);
		}

        Mat[] bothMats = { image, newMat }; //return both the original and the cropped letter

		return bothMats;

	}

    private Rect getRect(int minx, int miny, int maxx, int maxy, int rows, int cols, int boarder)
    {
        //set boundaries to get a thin boarder around letter
		if (minx > boarder)
			minx -= boarder;
		else
			minx = 0;
		if (miny >boarder)
			miny -= boarder;
		else
			miny = 0;
		if (maxx < cols-boarder-1)
			maxx += boarder;
		else
			maxx = cols-1;
		if (maxy < rows-boarder-1)
			maxy += boarder;
		else
			maxy = rows-1;

		//create the rectangle
		return new Rect(minx, miny , maxx-minx, maxy-miny);
    }
    

    public String getText()
    {
        return getBestText();
    }

	//compares the text obtained with channel 2 and 3 and 
	//returns the best text
	public String getBestText()
	{
		boolean c2TextValid = true, c3TextValid = true;
		String c2Text = pc2Text1 + " " + pc2Text2;
		String c3Text = pc3Text1 + " " + pc3Text2;
		String dest = "(none)";
		int c2numNums = 0;
		int c3numNums = 0;
		int c2numLower = 0;
		int c3numLower = 0;

		//System.out.println(c2Text + " " + c3Text);//DEBUG
		//A text will be invalid if -
		//More than 1 space
		//Less than 3 characters per word

		//Make sure the length is correct
		if (pc2Text1.length() < 3 || (pc2Text2.length() < 3 && !pc2Text2.equals("")))
		{
			c2TextValid = false;
		}
		if (pc3Text1.length() < 3 || (pc3Text2.length() < 3 && !pc3Text2.equals("")))
		{
			c3TextValid = false;
		}
		
		//check the amount of spaces
		int spaces = 0;
		boolean lastCharWasSpace = false;
		for (int i = 0; i < c2Text.length(); i++)
		{
			if (c2Text.charAt(i) == ' ' && !lastCharWasSpace && i != 0)
				spaces++;
			
			if ( (int)c2Text.charAt(i) > 47 && (int)c2Text.charAt(i) < 58 )
				c2numNums++;
			else if ( (int)c2Text.charAt(i) > 96 && (int)c2Text.charAt(i) < 123 )
				c2numLower++;
		
			if (c2Text.charAt(i) == ' ')
				lastCharWasSpace = true;
			else
				lastCharWasSpace = false;
		}
		if (spaces > 1)
		{
			c2TextValid = false;
		}

		spaces = 0;
		lastCharWasSpace = false;
		for (int i = 0; i < c3Text.length(); i++)
		{
			if (c3Text.charAt(i) == ' ' && !lastCharWasSpace && i != 0)
				spaces++;

			if ( (int)c3Text.charAt(i) > 47 && (int)c3Text.charAt(i) < 58 )
				c3numNums++;
			else if ( (int)c3Text.charAt(i) > 96 && (int)c3Text.charAt(i) < 123 )
				c3numLower++;
			
			if (c3Text.charAt(i) == ' ')
				lastCharWasSpace = true;
			else
				lastCharWasSpace = false;
		}
		if (spaces > 1)
			c3TextValid = false;
		
		//If both are valid, the text with the least amount of numbers is chosen
		//If it is still equal, the text with the least amount of lowercase letters is then chosen
		//if its still equal, chose the one with the longest text

		if (c2TextValid && !c3TextValid)
			dest = c2Text;
		else if (c3TextValid && !c2TextValid)
			dest = c3Text;
		else if (c2TextValid && c3TextValid)
		{
			if (c2numLower > c3numLower)
				dest = c3Text;
			else if (c3numLower > c2numLower)
				dest = c2Text;
			else
			{
				if (c2numNums > c3numNums)
					dest = c3Text;
				else if (c3numNums > c2numNums)
					dest = c2Text;
				else
				{
					if (c2Text.length() > c3Text.length())
						dest = c2Text;
					else
						dest = c3Text;
				}
			}
				
		}

		return dest;
	}

    public String getClassNum()
    {
        return getBestClassNum();
    }

	public String getBestClassNum()
	{
		boolean c2ClassValid = true, c3ClassValid = true;
		String c2Class = pc2ClassNum;
		String c3Class = pc3ClassNum;
		String dest = "(none)";

		//System.out.println(c2Class + " " + c3Class);//DEBUG
		//if its empty or greater than 3 characters, its invalid
		if (c2Class.length() > 3 || c2Class.equals(""))
			c2ClassValid = false;
		if (c3Class.length() > 3 || c3Class.equals(""))
			c3ClassValid = false;

		//if it has any letter its invalid
		for (int i = 0; i < c2Class.length(); i++)
		{
			if ( ((int)c2Class.charAt(i) < 48 || (int)c2Class.charAt(i) > 57) && (int)c2Class.charAt(i) != '.')
			{
				c2ClassValid = false;
			}
		}

		for (int i = 0; i < c3Class.length(); i++)
		{
			if ( ((int)c3Class.charAt(i) < 48 || (int)c3Class.charAt(i) > 57) && (int)c3Class.charAt(i) != '.')
			{
				c3ClassValid = false;
			}
		}

		if (c2ClassValid && !c3ClassValid)
			dest = c2Class;
		else if (c3ClassValid && !c2ClassValid)
			dest = c3Class;
		else if (c2ClassValid && c3ClassValid)
		{
			//give lower priority to 2 digit numbers over 1 or 3 digit numbers
			if (c2Class.length() != 1 || c2Class.length() != 3)
				dest = c3Class;
			else if (c3Class.length() != 1 || c3Class.length() != 3)
				dest = c2Class;
			else
				dest = c3Class;
		}

		return dest;
	}

	public String getSymbol()
    {
        return pSymbol;
    }

	public void detectSymbol()
	{
		//make it be able to be processed
		Mat cannyMat = ImgProcessing.edgeCanny(pMat);
		double[] val = { 0.0 };
		for (int i = 0; i < pMat.cols(); i++)
		{
			for (int j = 0; j < pMat.cols(); j++)
			{
				if (!isInside(250,40,130,190,360,190,i,j))//130->115, 360->385
					cannyMat.put(j, i, val);
			}
		}

		//preform shape context on them
		ShapeIdentifier si = new ShapeIdentifier(cannyMat, "Symbols/");


		pSymbol = si.findMatch();



		try
        {
           //FileIO.showImage(cannyMat,"SetD\\detect.png");//DEBUG
        }
        catch (Exception e) { System.out.println(e.getMessage()); 
        }
	}

	//https://www.geeksforgeeks.org/check-whether-a-given-point-lies-inside-a-triangle-or-not/
	boolean isInside(int x1, int y1, int x2, int y2, int x3, int y3, int x, int y) 
	{    
		/* Calculate area of triangle ABC */
		double A = area (x1, y1, x2, y2, x3, y3); 
		
		/* Calculate area of triangle PBC */   
		double A1 = area (x, y, x2, y2, x3, y3); 
		
		/* Calculate area of triangle PAC */   
		double A2 = area (x1, y1, x, y, x3, y3); 
		
		/* Calculate area of triangle PAB */    
		double A3 = area (x1, y1, x2, y2, x, y); 
			
		/* Check if sum of A1, A2 and A3 is same as A */ 
		return (A == A1 + A2 + A3); 
	} 
	double area(int x1, int y1, int x2, int y2, int x3, int y3) 
	{ 
		return Math.abs( (x1*(y2-y3) + x2*(y3-y1)+ x3*(y1-y2)) /2.0); 
	} 

    //releases mat memory back to the heap
    //who knows why this needs to be done in java
    public void release()
    {
        pMat.release();
    }

    //DEBUG
    public void showImage(String title)
    {
        try
        {
            FileIO.showImage(pMat, title);
        }
        catch (Exception e) { System.out.println(e.getMessage()); 
        }
    }

}
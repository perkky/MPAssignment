import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.utils.Converters; 

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

/*****************************************************
 *
 * ImgProcessing class
 * Inherits from: nil
 * Purpose: Provides the functions used to process images
            The class is an extension of opencvs
            Imgproc class but with some predefined constants
            and some assignment specific functions
 *
 *****************************************************/

public class ImgProcessing
{
    //***********Basic Image Functions*************

    //Converts an image from one type to another
    //mat - the input mat
    //type - the type to convert from/to - eg. Imgproc.COLOR_BGR2HSV
	public static Mat convertImage(Mat mat, int type)
	{
		Mat dest = new Mat();

		Imgproc.cvtColor(mat, dest, type);

		return dest;

	}

	public static Mat invertImage(Mat mat)
	{
		Mat dest = Mat.zeros(mat.rows(), mat.cols(), mat.type());

		for (int i = 0; i < mat.cols(); i++)
		{
			for (int j = 0; j < mat.rows();j++)
			{
				double[] val = {255 - mat.get(j,i)[0]};
				dest.put(j,i,val);
			}
		}

		return dest;
	}

    //Returns the Canny Edge version of an image
    //Contains predetermined presets
    public static Mat edgeCanny(Mat mat)
	{
		Mat edges = new Mat();
		
		Imgproc.Canny(mat, edges, 50, 150);    //originally 100 and 200
		
		return edges;
	}

    //Applys the preprocessing filters used
    //Contrains predetermined constants
    public static Mat applyPreprocessingFilters(Mat mat, double sigma)
	{
		Mat dest = new Mat();
		
		//median filter
		Imgproc.medianBlur(mat, dest, 5);	//use to be 3 instead of 5

		//Gausian blur
		Imgproc.GaussianBlur(dest, dest, new Size(7,7), sigma); //maybe change to 3? or 1?

		return dest;
	}

     //Adaptive threshold on a certain channel - index starts at 1
    public static Mat adaptiveThresholdOnChannel(Mat mat, int channel, int flag)
	{
		List<Mat> channels = new ArrayList<Mat>(3);
		Core.split(mat, channels);
		Mat newChannel = new Mat();
		Mat dest = mat.clone();

		int windowSize = 35;
		int constant = 5;

		Imgproc.adaptiveThreshold(channels.get(channel-1), newChannel, 255, flag, Imgproc.THRESH_BINARY, windowSize, constant);
		
		channels.set(channel-1, newChannel);
		Core.merge(channels,dest);

		return dest;
	}

    //thresholds an single channel image
	// everything above the cutoff gets set to 255
	//everything below gets set to 0
	public static Mat threshold(Mat mat, int cutoff)
	{
		Mat thresh = new Mat();
		
		Imgproc.threshold(mat, thresh, cutoff, 255, Imgproc.THRESH_BINARY);

		return thresh;
	}

    //gets the channel - index starts at 1
    public static Mat getChannel(Mat mat, int channel)
	{
		List<Mat> channels = new ArrayList<Mat>(3);
		Core.split(mat, channels);

		return channels.get(channel-1);
	}

    public static Mat cropImage(Mat mat, Rect rect)
    {
        return new Mat(mat, rect);
    }

    //Creates a masic for an image based on a contour
    //contours - list of contours
    //rows - rows of the image (should be mat.rows() of the source image)
    //cols - cols of the image (should be mat.cols() of the source image)
    //idx - index of the contour to make a mask from
    public static Mat createMask(List<MatOfPoint> contours, int rows, int cols, int idx)
	{
		Mat mask = Mat.zeros(rows, cols, CvType.CV_8UC1);
		Scalar color = new Scalar(255);

		if (idx >= contours.size())
		{
			System.out.println("Unable to properly mask - index is too big");
		}
		else
			Imgproc.drawContours(mask, contours, idx, color,  Core.FILLED);

		return mask;
	}

    //***********Contour Manipulation Functions*************

    public static List<MatOfPoint> getContours(Mat mat)
    {
        List<MatOfPoint> contours =  new ArrayList<>();
		Mat heirachy = new Mat();		

		//Find the contours
		Imgproc.findContours(ImgProcessing.edgeCanny(mat), contours, heirachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        return contours;
    }

    //Removes all contours from a list that are under a certain area
	public static List<MatOfPoint> removeMOPArea(List<MatOfPoint> contours, int minArea )
	{
		List<MatOfPoint> newContours = new ArrayList<>();

		for(MatOfPoint point : contours) 
		{
			if (Imgproc.contourArea(point) > minArea)
			{
				newContours.add(point);
			}
		
		}

		return newContours;
	}

    //Approximates all contours to polygons within a list of contours
	public static List<MatOfPoint> approxContours(List<MatOfPoint> contours )
	{
		double epsilon = 0;
		
		List<MatOfPoint> newContours = new ArrayList<>();
 		for(MatOfPoint point : contours) 
		 {
			//convert from MatOfPoint to MatOfPoint2f
			//This has to be done as arcLength() and approxPolyDp()
			//only take MatOfPoint2f, which is pretty much the same thing 
			//as MatOfPoint
     		MatOfPoint2f olfPoint2f = new MatOfPoint2f(point.toArray());	//This is the MatOfPoint2f version of the original contour
			MatOfPoint2f newPoint2f = new MatOfPoint2f();					//This holds the approximated poly of the contour
			

			epsilon = 0.05*Imgproc.arcLength(olfPoint2f, true);
			Imgproc.approxPolyDP(olfPoint2f, newPoint2f, epsilon,true);		//approximate the polygons	

			MatOfPoint finalPoint = new MatOfPoint(newPoint2f.toArray());	//This is the MatOfPoint version of the approximated poly.

    		newContours.add(finalPoint);
		 }

		return newContours;
	}

    //Converts all contours to quadrilaterals
	//Removes contours with less than 3 points
	//This may cause problems - needs to be tested more
	public static List<MatOfPoint> convertToQuad(List<MatOfPoint> contours)
	{
		List<MatOfPoint> newContours = new ArrayList<>();
		

 		for(MatOfPoint shape : contours) 
		 {
			int topIdx = 0, leftIdx = 0, botIdx = 0, rightIdx = 0;
			int top = 10000, left = 10000, right = 0, bot = 0;

			Point[] shapeArray = shape.toArray();
			//System.out.println(Arrays.toString(shapeArray));
			//Removes any shapes with 3 or less verticies 
			if ( shapeArray.length > 3)
			{
				int i = 0;
				for (Point point : shapeArray)
				{
					//see if a new left or right max was made
					if (point.x < left)
					{
						leftIdx = i;
						left = (int)point.x;
					}
					else if ( point.x > right)
					{
						rightIdx = i;
						right = (int)point.x;
					}

					//see if a new top or bot max was made
					if (point.y < top)
					{
						topIdx = i;
						top = (int)point.y;
					}
					else if ( point.y > bot)
					{
						botIdx = i;
						bot = (int)point.y;
					}

					i++;
				}

				Point[] newShapeArray = {shapeArray[topIdx], shapeArray[leftIdx], shapeArray[botIdx], shapeArray[rightIdx]};
				MatOfPoint newContour = new MatOfPoint(newShapeArray);
				//System.out.println(Arrays.toString(newShapeArray));//DEBUG

				newContours.add(newContour);
			}
		 }

		 return newContours;
	}


}
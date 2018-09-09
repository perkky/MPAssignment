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


public class Assignment
{
	public static void main(String[] args)
	{
		for (String arg : args)
		{
			long startTime = System.currentTimeMillis();


			System.loadLibrary("opencv_java330");
			
			
			Mat mat = readImage(arg, Imgcodecs.CV_LOAD_IMAGE_COLOR);
			/*-----------used to find image colour---------------------
			Mat n = convertImage(mat, Imgproc.COLOR_BGR2HSV );

			System.out.println("Top: " + getColourInTriangle(n, new Point(250, 0), 250, 500) );
			System.out.println("Bottom: " + getColourInTriangle(n, new Point(250, 500), -250, 500) );
			-----------------------------------------------------------*/

			//System.out.println(mat.get(146,159)[0] + " " + mat.get(146,159)[1] + " " + mat.get(146,159)[2] + " ");	
			//System.out.println(n.get(146,159)[0] + " " + n.get(146,159)[1] + " " + n.get(146,159)[2] + " ");
			//System.out.println(checkColourHSV(n.get(100,100)));

			Mat processed = applyPreprocessingFilters(mat);

			testContours(processed);
			
			//saveImage(n, "Canny edge7.png");
			
			//System.out.println( "mat = " + mat.dump() );

			long endTime = System.currentTimeMillis();
			System.out.println("Took "+(endTime - startTime) + " ms");

			try
			{
				Mat nm= new Mat();
				Imgproc.resize(processed, nm, new Size(processed.cols()/2,processed.rows()/2));
				showImage(nm, "Original");
				//showImage(convertImage(n, Imgproc.COLOR_HSV2BGR), "New");
			}
			catch (Exception e) { System.out.println(e.getMessage()); }

		}
	}

	private static Mat applyPreprocessingFilters(Mat mat)
	{
		Mat dest = new Mat();
		
		//median filter
		Imgproc.medianBlur(mat, dest, 3);

		//Gausian blur
		Imgproc.GaussianBlur(dest, dest, new Size(5,5), 1);

		return dest;
	}

	private static void testContours(Mat mat)
	{
		List<MatOfPoint> contours =  new ArrayList<>();
		Mat heirachy = new Mat();		

		//find the contours
		Imgproc.findContours(edgeCanny(mat), contours, heirachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		
		//System.out.println(Arrays.toString(approx.toArray()));
		//System.out.println(Imgproc.contourArea(approx));

		contours = approxContours(contours); //approx the contours
		contours = removeMOPArea(contours, 10000); //removes contours with an area less than 10000 - is the minimum as it will be at least 100x100
		//contours = removeContoursWithParents(contours, heirachy); //remove any contours within a contour //may not be needed
		

		for (int i = 0; i < contours.size(); i++)
		{
			//create the mask for this specific contour
			Mat mask = createMask(contours, mat.rows(), mat.cols(), i);

			//extract that shape
			Mat shapeMat = extractShape(mat, mask, contours.get(i), new Size(500,500));

			//find colour
			Mat n = convertImage(shapeMat, Imgproc.COLOR_BGR2HSV );

			System.out.println("Top: " + getColourInTriangle(n, new Point(n.cols()/2, 0), n.rows()/2, n.cols()) );
			System.out.println("Bottom: " + getColourInTriangle(n, new Point(n.cols()/2, n.rows()), -n.rows()/2, n.cols()) );

			try 
			{

				if (Core.countNonZero(convertImage(shapeMat, Imgproc.COLOR_BGR2GRAY )) > 1)
					showImage(shapeMat,"Mask Applied");


			} catch (Exception e) { }

		}

	}

	//Extracts the shape from an image, warps it into a diamond and returns a Mat
	//mat - the input image
	//mask - the mask to be applied to the image before extracting the shape
	//shape - the coordinates of the shape
	//size - the size of the mat to be returned (and as such the size of the diamond
	public static Mat extractShape(Mat mat, Mat mask, MatOfPoint shape, Size size)
	{
		Mat maskedMat = new Mat(); 							//The masked image
		List<Point> sourcePoints = new ArrayList<Point>();	//List of the 4 source points in the original image
		Mat sP = new Mat();									//The mat version of the source points
		List<Point> destPoints = new ArrayList<Point>();	//List of the 4 corresponding dest points in the new image
		Mat dP = new Mat();									//The mat version of the dest points
		Point[] shapeArray = shape.toArray();				//Array of the shapes points
		Mat newMat = Mat.zeros((int)size.height, (int)size.width, mat.type());	//The final Mat
		Mat transformationMatrix = new Mat();				//The transformation matrix

		//Apply the mask
		mat.copyTo(maskedMat, mask);

		//Fill the source points list from the shape array
		int i = 0;
		for (Point point : shapeArray)
		{
			sourcePoints.add(new Point(point.x, point.y));
			i++;
		}

		//only run if the shape has 4 points
		if (i == 4)
		{
			//converting source points from list to mat
			sP = Converters.vector_Point2f_to_Mat(sourcePoints); 

			//Create the dest points based on the inputed size
			destPoints.add(new Point(size.width/2, 0));				//top
			destPoints.add(new Point(0, size.height/2));			//left
			destPoints.add(new Point(size.width/2, size.height));	//bottom
			destPoints.add(new Point(size.width, size.height/2));	//right

			//converting dest points from list to mat
			dP = Converters.vector_Point2f_to_Mat(destPoints); 

			//calculate the transformatin matrix
			transformationMatrix = Imgproc.getPerspectiveTransform(sP, dP);

			//apply the transformation matrix
			Imgproc.warpPerspective(maskedMat, newMat , transformationMatrix, size);
		}

		return newMat;
	}

	/*-------------Old Extract shape function-----------------
	----------this function did not rotate the image---------

	//This function extracts a shape from a mat and returns it
	//mat - source mat file
	//mask - mask image to be applied before extraction
	//shape - shape coordinates
	public static Mat extractShape(Mat mat, Mat mask, MatOfPoint shape)
	{
		Mat maskedMat = new Mat();
		mat.copyTo(maskedMat, mask);
		Point[] shapeArray = shape.toArray();


		int maxX = 0, maxY = 0, minX = 100000, minY = 100000;

		for (Point point : shapeArray)
		{
			//set max and min x
			if ((int)point.x < minX)
				minX = (int)point.x;
			else if ( (int)point.x > maxX)
				maxX = (int)point.x;

			//set max and min y
			if ((int)point.y < minY)
				minY = (int)point.y;
			else if ((int)point.y > maxY)
				maxY = (int)point.y;
		}

		//create the rectangle
		Rect rect = new Rect(minX, minY , maxX-minX, maxY-minY);

		Mat newMat = new Mat(maskedMat, rect);

		return newMat;
	}
	-------------------------------------------------------*/

	//creates a mask from a list of contours
	//use mat.copyTo(yess, mask) to apply the mask
	public static Mat createMask(List<MatOfPoint> contours, int rows, int cols)
	{
		Mat mask = Mat.zeros(rows, cols, CvType.CV_8UC1); //new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1); - this for some reason would give an image with noise
		Scalar color = new Scalar(255);

		Imgproc.drawContours(mask, contours, -1, color,  Core.FILLED);

		return mask;
	}

	//creates a mask from a certain contour
	public static Mat createMask(List<MatOfPoint> contours, int rows, int cols, int idx)
	{
		Mat mask = Mat.zeros(rows, cols, CvType.CV_8UC1); //new Mat(mat.rows(), mat.cols(), CvType.CV_8UC1); - this for some reason would give an image with noise
		Scalar color = new Scalar(255);

		if (idx >= contours.size())
		{
			System.out.println("Unable to properly mask - index is too big");
		}
		else
			Imgproc.drawContours(mask, contours, idx, color,  Core.FILLED);

		return mask;
	}

	//removes any contour that has a parent
	//This may cause a problem if a a bigger non related contour is present
	//May need to revise this
	//May not be needed as you can just fill it in to make a mask
	private static List<MatOfPoint> removeContoursWithParents(List<MatOfPoint> contours, Mat heirachy)
	{
		List<MatOfPoint> newContours = new ArrayList<>();

		int len = contours.size();
		for (int i = 1; i < len; i++)
		{
			if (heirachy.get(0, i)[3] == -1)
			{
				newContours.add(contours.get(i));
			}
		}

		return newContours;
	}

	//approximate all contours to polys
	private static List<MatOfPoint> approxContours(List<MatOfPoint> contours )
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


	//takes away all contours under a certain area
	private static List<MatOfPoint> removeMOPArea(List<MatOfPoint> contours, int minArea )
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


	//Returns the Canny Edge of the inputted Mat
	private static Mat edgeCanny(Mat mat)
	{
		Mat edges = new Mat();
		
		Imgproc.Canny(mat, edges, 100, 200);
		
		return edges;
	}

	//This function gets the average colour inside an isosceles triangle
	//It does this buy taking the colour of 25 points (5 height positions with 5 width positions per height)
	//p1 refers to the top of the triangle
	//height can be +ve or -ve depending on triangles orientation - +ve id downwards and -ve is upwards
	private static String getColourInTriangle(Mat mat, Point p1, int height, int width)
	{
		String s = "Black"; //default is black

		if ((height < 7 && height > -7 ) || (width < 7)) //height and width have to be greater than 7
			throw new IllegalArgumentException("Unable to get colour, width and height need to be above 6 pixels");
		else
		{
			int[] cols = {0, 0, 0, 0, 0, 0, 0};
			String temp;

			for (int i = 1; i < 6; i++)
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
				default:
					s = "Black";
					break;
			}


		}

		return s;
	}

	//Takes a double array of size 3 and returns what colour it is
	//Based on colour definitions in notes.txt
	//To be used on HSV only
	private static String checkColourHSV(double[] col)
	{
		String s = "Black";	//default is black
		double hue = col[0], sat = col[1], val = col[2];


		if (val <= 13)
			s = "Black";
		else if (sat < 26 && val > 176)
			s = "White";
		else if (9 <= hue && hue < 23 )
			s = "Orange";
		else if ((0 <= hue && 9 > hue) || (148 <= hue && hue <= 180))
			s = "Red";
		else if ( 39 <= hue && 83 > hue)
			s = "Green";
		else if (83 <= hue && 148 > hue)
			s = "Blue";
		else if (23 <= hue && hue < 39)
			s = "Yellow";

		return s;
	}

	//converts the current mat file into a different one
	//eg rgb -> hsv
	private static Mat convertImage(Mat mat, int type)
	{
		Mat dest = new Mat();

		Imgproc.cvtColor(mat, dest, type);

		return dest;

	}


	private static Mat applyFilter2d(Mat mat, Mat kernel)
	{
		Mat dest = new Mat();
		
		Imgproc.filter2D(mat, dest, -1, kernel);
		
		return dest;
	}
	
	private static Mat convertTo(Mat mat, int code)
	{
		Mat dest = new Mat();
		
		Imgproc.cvtColor(mat, dest, code);
		
		return dest;
	}
	
	private static Mat readImage(String location)
	{
		Mat img = Imgcodecs.imread(location);
		
		if (!img.empty())
			System.out.println("Successfully read image: " + location);
		else
			throw new IllegalArgumentException("Error reading image: " + location);
		return img;
	}
	
	private static Mat readImage(String location, int type)
	{
		Mat img = Imgcodecs.imread(location, type);
		
		if (!img.empty())
			System.out.println("Successfully read image: " + location);
		else
			throw new IllegalArgumentException("Error reading image: " + location);

		return img;
	}
	
	private static void saveImage(Mat mat, String location)
	{
		if (!mat.empty())
		{
			Imgcodecs.imwrite(location, mat);
			System.out.println("Successfully saved image: " + location);
		}
		else
			System.out.println("Error saving image: " + location);
		
	}



	//DEBUGGING METHODS
	//https://stackoverflow.com/questions/27086120/convert-opencv-mat-object-to-bufferedimage
	private static BufferedImage matToBufferedImage(Mat mat)throws Exception 
	{        
		MatOfByte mob=new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, mob);
		byte ba[]=mob.toArray();

		BufferedImage bi=ImageIO.read(new ByteArrayInputStream(ba));
		return bi;
	}

	private static void showImage(Mat mat,  String title) throws Exception
	{
		JLabel image = new JLabel(new ImageIcon(matToBufferedImage(mat)));
		JFrame frame = new JFrame(title);
		frame.setSize(mat.cols()+50, mat.rows()+50);
		frame.setVisible(true);
		frame.add(image);
	}

}
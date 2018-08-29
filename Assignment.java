import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.CvType;
import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
 
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;

import java.io.*;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class Assignment
{
	public static void main(String[] args)
	{
		System.loadLibrary("opencv_java330");
		
		Mat mat = readImage("3.png");
		Mat n = convertImage(mat, Imgproc.COLOR_BGR2HSV );
	
		saveImage(n, "Canny edge7.png");
		
		//System.out.println( "mat = " + mat.dump() );
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
			System.out.println("Error reading image: " + location);
		return img;
	}
	
	private static Mat readImage(String location, int type)
	{
		Mat img = Imgcodecs.imread(location, type);
		
		if (!img.empty())
			System.out.println("Successfully read image: " + location);
		else
			System.out.println("Error reading image: " + location);
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
}
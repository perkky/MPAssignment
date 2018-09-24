
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

public class TesseractExample {
    public static void main(String[] args) {
		long startTime = System.currentTimeMillis();
        // System.setProperty("jna.library.path", "32".equals(System.getProperty("sun.arch.data.model")) ? "lib/win32-x86" : "lib/win32-x86-64");

        //System.loadLibrary("libtesseract305");
        System.loadLibrary("opencv_java330");

        File imageFile = new File(args[0]);
        ITesseract instance = new Tesseract();  // JNA Interface Mapping
        // ITesseract instance = new Tesseract1(); // JNA Direct Mapping
        // File tessDataFolder = LoadLibs.extractTessResources("tessdata"); // Maven build bundles English data
        // instance.setDatapath(tessDataFolder.getPath());
        Scanner sc = new Scanner(System.in);
        for (String arg : args)
        {
            Mat mat = readImage(arg, CvType.CV_8U);
			Mat colourMat = readImage(arg);
			Mat guasthresh = new Mat();
			Mat medthresh = new Mat();
			Mat normthresh = edgeCanny(mat);
			Mat kp = Mat.zeros(mat.rows(), mat.cols(), mat.type());
			//Mat test = readImage("combustable.png");
			Mat cons = Mat.zeros(mat.rows(), mat.cols(), mat.type());
		
			int windowSize = 25;
			int constant = 5;

			//Imgproc.threshold(mat, normthresh, 10, 255, Imgproc.THRESH_BINARY);
			//Imgproc.adaptiveThreshold(mat, guasthresh, 255, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C, Imgproc.THRESH_BINARY, windowSize, constant);
			//Imgproc.adaptiveThreshold(mat, medthresh, 255, Imgproc.ADAPTIVE_THRESH_MEAN_C, Imgproc.THRESH_BINARY, windowSize, constant);

			//Using adaptive threshold on the 3rd channel is good for identifying black letters, while
			//using it on the 2nd channel is good for white letters
			Mat threshHSV3 = adaptiveThresholdOnChannel(convertImage(colourMat,Imgproc.COLOR_BGR2HSV), 3, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
			Mat threshBGR3 = convertImage(threshHSV3, Imgproc.COLOR_HSV2BGR);

			Mat threshHSV2 = adaptiveThresholdOnChannel(convertImage(colourMat,Imgproc.COLOR_BGR2HSV), 2, Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C);
			Mat threshBGR2 = convertImage(threshHSV2, Imgproc.COLOR_HSV2BGR);
			
			//blob detection
			FeatureDetector blobDetector = FeatureDetector.create(FeatureDetector.SIMPLEBLOB);
			MatOfKeyPoint keypoints = new MatOfKeyPoint();

			blobDetector.read("blob.xml");	//saves the properties

			blobDetector.detect(threshBGR3, keypoints);
			org.opencv.core.Scalar cores = new org.opencv.core.Scalar(0,0,255);
			org.opencv.features2d.Features2d.drawKeypoints(threshBGR3, keypoints, threshBGR3, cores, Features2d.DRAW_RICH_KEYPOINTS  );

			//saveImage(mat, "test.png");
            try {
                //String result1 = instance.doOCR(matToBufferedImage(threshBGR3), new Rectangle(50, 188,448-50,345-188));//Rectangle(103, 262,395-103,320-262)
                //String result2 = instance.doOCR(matToBufferedImage(threshBGR2), new Rectangle(50, 188,448-50,345-188));

				showImage(threshBGR3, "Custom Black Text");
				showImage(threshold(getChannel(threshHSV3, 3), 1), "Custom Black Threshold");

				//Used for white text
				showImage(threshBGR2, "Custom White Text");
				showImage(threshold(getChannel(threshHSV2, 2), 1), "Custom White Threshold");

				//get the letter
				KeyPoint[] kps = keypoints.toArray();
				Mat testLetter = getLetter(threshold(getChannel(threshHSV3, 3), 1), kps[0]);
				showImage(testLetter, "test letter");

				System.out.println(getStrings(threshold(getChannel(threshHSV3, 3), 1), kps, 120, 20));

                //System.out.println(result1);
                //System.out.println(result2);
            } catch (TesseractException e) {
                System.err.println(e.getMessage());
            }
            catch (Exception e) {
                System.err.println(e.getMessage());
            }
			
			long endTime = System.currentTimeMillis();
			System.out.println("Took "+(endTime - startTime) + " ms");
            sc.next();
        }
    }

	//returns all the strings in an image, \n divides each string
	//maxdist is the max dist a letter can be from another letter (based on the keypoint)
	//takes a long time to do ocr per image
	private static String getStrings(Mat mat, KeyPoint[] kp, int maxdistX, int maxdistY)
	{
		String s = "";
		ITesseract instance = new Tesseract();  // JNA Interface Mapping

		//holds the letter for each keypoint (index's corresponding to kp)
		Mat[] letters = new Mat[kp.length];

		for (int i = 0; i < kp.length; i++)
		{
			letters[i] = getLetter(mat, kp[i]);
			//showImage(testLetter, "test letter");

		}
		
		Queue<Integer> currentLetters = new LinkedList<>();
		LinkedList<Integer> pastLetters = new LinkedList<>();


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

			//find all leters that are within max dist from each letter
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
			//sort the array to be in order
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

			//create new mat contraining each found letter
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
			
			//do ocr on this new mat
			try{
				showImage(currentMat,"");
				s += instance.doOCR(matToBufferedImage(currentMat)) + "\n";
			}
			catch (Exception e) { }
		}

		return s;
	}
	
	//gets and returns a mat containing the letter from a keypoint
	private static Mat getLetter(Mat mat, KeyPoint keypoint)
	{

		Mat image = Mat.zeros(mat.rows(), mat.cols(), mat.type());

		byte[] return_buff = new byte[(int) (mat.total() *mat.channels())];
		int[] visited = new int[(int) (mat.total() *mat.channels())];
        mat.get(0, 0, return_buff);

		
		Point pt;
		Queue<Point> queue = new LinkedList<>();
		LinkedList<Point> pastPoints = new LinkedList<>();
		double[] val = {255.0};
		int minx = 10000, miny = 10000, maxx = -1, maxy = -1;

		//find the closest black pixel to the point
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

		//loop through to find all black pixels connected to black pixels
		while (queue.size() != 0)
		{
			pt = queue.remove();
			//System.out.println(pastPoints.size());
			image.put((int)pt.y, (int)pt.x, val);

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
						}
					}
				}
			}

			
			
		}

		Mat newMat = new Mat();

		
		//if no points were found return a blank mat
		if (maxx == -1 || maxy == -1 || minx == 10000 || miny == 10000)
		{
			newMat = Mat.zeros(10,10, mat.type());
		}
		else
		{
			//set boundaries to get a thin boarder around letter
			int boarder = 20;
			if (minx > boarder)
				minx -= boarder;
			else
				minx = 0;
			if (miny >boarder)
				miny -= boarder;
			else
				miny = 0;
			if (maxx < mat.cols()-boarder-1)
				maxx += boarder;
			else
				maxx = mat.cols()-1;
			if (maxy < mat.rows()-boarder-1)
				maxy += boarder;
			else
				maxy = mat.rows()-1;

			//create the rectangle
			Rect rect = new Rect(minx, miny , maxx-minx, maxy-miny);

			newMat = new Mat(image, rect);
		}

		return newMat;

	}

	//preforms adaptive threshold on a specific channel
	private static Mat adaptiveThresholdOnChannel(Mat mat, int channel, int flag)
	{
		List<Mat> channels = new ArrayList<Mat>(3);
		Core.split(mat, channels);
		Mat newChannel = new Mat();
		Mat dest = mat.clone();

		int windowSize = 25;
		int constant = 5;

		Imgproc.adaptiveThreshold(channels.get(channel-1), newChannel, 255, flag, Imgproc.THRESH_BINARY, windowSize, constant);
		
		channels.set(channel-1, newChannel);
		Core.merge(channels,dest);

		return dest;
	}

	//thresholds an single channel image
	// everything above the cutoff gets set to 255
	//everything below gets set to 0
	private static Mat threshold(Mat mat, int cutoff)
	{
		Mat thresh = new Mat();
		
		Imgproc.threshold(mat, thresh, cutoff, 255, Imgproc.THRESH_BINARY);

		return thresh;
	}

	//returns the specific channel only - eg a 8 bit image
	private static Mat getChannel(Mat mat, int channel)
	{
		List<Mat> channels = new ArrayList<Mat>(3);
		Core.split(mat, channels);

		return channels.get(channel);
	}

	//takes away all contours OVER a certain area
	private static List<MatOfPoint> removeMOPArea(List<MatOfPoint> contours, int maxArea )
	{
		List<MatOfPoint> newContours = new ArrayList<>();

		for(MatOfPoint point : contours) 
		{
			if (Imgproc.contourArea(point) < maxArea && Imgproc.contourArea(point) > 0)
			{
				newContours.add(point);
			}
		
		}

		return newContours;
	}

	private static Mat edgeCanny(Mat mat)
	{
		Mat edges = new Mat();
		
		Imgproc.Canny(mat, edges, 10, 50);
		
		return edges;
	}

	private static Mat equaliseHist(Mat mat, int channel)
	{
		List<Mat> channels = new ArrayList<Mat>(3);
		Core.split(mat, channels);
		Mat newChannel = new Mat();
		Mat dest = mat.clone();



		Imgproc.equalizeHist(channels.get(channel-1), newChannel);
		channels.set(channel-1, newChannel);
		Core.merge(channels,dest);

		return dest;
	}

	private static Mat convertImage(Mat mat, int type)
	{
		Mat dest = new Mat();

		Imgproc.cvtColor(mat, dest, type);

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
			;//System.out.println("Successfully read image: " + location);
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
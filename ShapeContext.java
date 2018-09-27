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

import java.lang.Math;
import java.io.PrintWriter;

public class ShapeContext
{
	private static double[][] data;
	private static String[] tags;
	public static void main(String[] args)
	{
		System.loadLibrary("opencv_java330");
		
		/*
		data = readCSVData("LetterData.txt");
		tags = readCSVTags("LetterData.txt");

		Mat mat = readImage(args[0], CvType.CV_8U);
		Mat extra = Mat.zeros(mat.rows(), mat.cols(), CvType.CV_8U);

		//find the countours
		List<MatOfPoint> contours =  new ArrayList<>();
		Mat heirachy = new Mat();

		//find the contours
		Imgproc.findContours(edgeCanny(mat), contours, heirachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);
		Imgproc.drawContours(extra, contours, -1, new Scalar(255));
		//contours = new ArrayList<>();
		
		//Imgproc.findContours(extra, contours, heirachy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);


		for (int i = 0; i < contours.size(); i++)
		{
			
			//create the mask for this specific contour to remove the background 
			Mat mask = createMask(contours, mat.rows(), mat.cols(), i);

			//extract that shape
			Mat shapeMat = extractShape(mat, mask, contours.get(i), new Size(300,400));

			try{
				showImage(edgeCanny(shapeMat),"let.png");
			} catch(Exception e) {}

			findLetter(edgeCanny(shapeMat));

		}*/

		
		double lowestScore = 100000;
		String label = "";
		
		for (String arg : args)
		{
			Mat mat1 = readImage(arg, CvType.CV_8U);
			Mat rsz1 = new Mat();
			Imgproc.resize(mat1,rsz1, new Size(300, 400));
			Mat mat2 = readImage("SetD\\detect.png", CvType.CV_8U);
			Mat rsz2 = new Mat();
			Imgproc.resize(mat2,rsz2, new Size(300, 400));

			double avgScore = 0;
			for (int i = 0; i < 5; i++)
				avgScore += testSC3D(edgeCanny(rsz1), edgeCanny(rsz2))/5;

			if (avgScore < lowestScore)
			{
				lowestScore = avgScore;
				label = arg;
			}
		
		}
		System.out.println(label + " " + lowestScore);
	
	}

	/*
	To create a data file
	try {
			PrintWriter writer = new PrintWriter("LetterData.txt", "UTF-8");

		for (String arg : args)
		{
		
			Mat mat1 = readImage(arg, CvType.CV_8U);
			Mat rsz1 = new Mat();
			Imgproc.resize(mat1,rsz1, new Size(300, 400));
		
			
			writer.println(convertToCSV(mat1,arg));

		}
		writer.close();
		} catch (Exception e) {}
	*/

	public static Mat extractShape(Mat mat, Mat mask, MatOfPoint shape, Size size)
	{
		Mat maskedMat = new Mat(); 							//The Mat to store the masked image
		int minX = 10000, minY = 10000, maxX = 0, maxY = 0;


		//Apply the mask
		mat.copyTo(maskedMat, mask);

		Point[] shapeArray = shape.toArray();				//Array of the shapes points
		//set the boundaries of the shape
		for (Point point : shapeArray)
		{
				if (point.x < minX)
					minX = (int)point.x;
				else if (point.x > maxX)
					maxX = (int)point.x;

				if (point.y < minY)
					minY = (int)point.y;
				else if (point.y > maxY)
					maxY = (int)point.y;
		}

		if (minX < 50)
			minX = 0;
		else
			minX -=50;

		if (maxX > mat.cols()-50)
			maxX = mat.cols()-1;
		else
			maxX +=50;
		
		if (minY < 50)
			minY = 0;
		else
			minY -=50;

		if (maxY > mat.rows()-50)
			maxY = mat.rows()-1;
		else
			maxY +=50;

		Rect rect = new Rect(minX, minY , maxX-minX, maxY-minY);

		Mat newMat = new Mat(maskedMat, rect);

		Mat resize = new Mat();
		Imgproc.resize(newMat,resize, size);

		return resize;
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

	//input a mat file that has had an edge canny filter applied to it
	private static void findLetter(Mat mat)
	{
		double[][] histogram = new double[6][12];
		for (int i = 0; i < 100; i++)
		{
			List<Point> points = getPoints(mat, 5);
			double[][] h1 = getSCValue(points);
			for (int k = 0; k < 6; k++)
				for (int j = 0; j < 12; j++)
					histogram[k][j] += h1[k][j];
		}

		
		
		double min = 10000;
		int idx = 0;
		for (int k = 0; k < tags.length; k++)
		{
			//System.out.println(Arrays.toString(data[k]));
			double score = 0;
			for (int i = 0; i < 6; i++)
				for (int j = 0; j < 12; j++)
				{
					score += Math.pow(Math.abs(histogram[i][j]/100-data[k][i*12+j]) , 1.2);//maybe change this value
				}
			//System.out.println(tags[k]+ " " +score);
			if (score < min)
			{
				min = score;
				idx = k;
			}
		}

		System.out.println(tags[idx]);
	}

	private static String[] readCSVTags(String fileName)
	{
		String line;
		int length = 0;
		try
		{
			FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) 
			{
                length++;
			}
		} catch (Exception e) {}

		String[] data = new String[length];
		try
		{
			FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

			int l = 0;
            while((line = bufferedReader.readLine()) != null) 
			{
                String[] split = line.split(",");
				data[l] = split[0];
				l++;
			}
		} catch (Exception e) {}

		return data;
	}

	private static double[][] readCSVData(String fileName)
	{
		String line;
		int length = 0;
		try
		{
			FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            while((line = bufferedReader.readLine()) != null) 
			{
                length++;
			}
		} catch (Exception e) {}

		double[][] data = new double[length][72];

		try
		{
			FileReader fileReader = new FileReader(fileName);

            // Always wrap FileReader in BufferedReader.
            BufferedReader bufferedReader = new BufferedReader(fileReader);

			int l = 0;
            while((line = bufferedReader.readLine()) != null) 
			{
                String[] split = line.split(",");
				for (int i = 1; i < split.length; i++)
				{
					data[l][i-1] = Double.parseDouble(split[i]);
				}
				l++;
			}
		} catch (Exception e) {}

		return data;

	}

	private static String convertToCSV(Mat mat, String fileName)
	{
		String s = "";
		String[] nameSplit = fileName.split("\\.");
		s += nameSplit[0].split("\\\\")[1];
		int timesToRun =100;

		//create the arrays
		double[][] histogram = new double[6][12];
		for (int i = 0; i < 6; i++)
				for (int j = 0; j <12; j++)
					histogram[i][j] = 0;
		
		for (int run = 0; run < timesToRun; run++)
		{
			List<Point> points = getPoints(mat, 5);
			double[][] h1 = getSCValue(points);
			for (int i = 0; i < 6; i++)
				for (int j = 0; j <12; j++)
				{
					histogram[i][j] += h1[i][j];
				}
		}
		//average out and add to string
		for (int i = 0; i < 6; i++)
				for (int j = 0; j <12; j++)
				{
					histogram[i][j] /= timesToRun;
					s += ","+histogram[i][j];
				}

		
		return s;
	}

	private static double testSC(Mat mat1, Mat mat2)
	{
		List<Point> points1 = getPoints(mat1, 5);
		List<Point> points2 = getPoints(mat2, 5);

		double[][] h1 = getSCValue(points1);
		double[][] h2 = getSCValue(points2);

		
		double score = 0;

		return score;

	}

	public static double testSC3D(Mat mat1, Mat mat2)
	{
		List<Point> points1 = getPoints(mat1, 5);
		List<Point> points2 = getPoints(mat2, 5);

		double[][][] h1 = getSCValue3D(points1);
		double[][][] h2 = getSCValue3D(points2);
		
		double score = getMinCost(h1, h2);

		return score;

	}

	private static double getMinCost(double[][][] h1, double[][][] h2)
	{
		//the score of matching two points together h1-i and h2-j
		double scores[][] = new double[h1.length][h2.length];
		double scoresCopy[][] = new double[h1.length][h2.length];

		//create the scores for mathing each point together
		for (int i = 0; i < h1.length; i++)
		{
			for (int j = 0; j < h2.length; j++)
			{
				double score = 0;
				//get each part of the histogram to create the score
				for (int k = 0; k < 6; k++)
					for (int l = 0; l < 12; l++)
					{
						if (!(h1[i][k][l] == 0 && h2[j][k][l] ==0))
							score += Math.pow(h1[i][k][l] - h2[j][k][l],2)/(h1[i][k][l] + h2[j][k][l]); //maybe wrong?? look at wikipedia article - not between 0 and 1
						
					}
				score /= 2;

				scores[i][j] = score;
				scoresCopy[i][j] = score;
				//System.out.println(score);
			}
		}

		
		//Greedly match points up to minimise cost (greedy minimise that is)
		int num = 0;
		boolean[] usedH1 = new boolean[h1.length]; //all initialised to false
		boolean[] usedH2 = new boolean[h2.length]; //all initialised to false
		double totalScore = 0;
		while( num < h1.length)
		{
			double lowestScore = 10000;
			int lowestH1idx = 0;
			int lowestH2idx = 0;
			for (int i = 0; i < h1.length; i++)
			{
				if (!usedH1[i]) //if havent been picked yet
					for (int j = 0; j < h2.length; j++)
					{
						if (!usedH2[j]) //if havent been picked yet
						{
							if (scores[i][j] < lowestScore)
							{
								lowestScore = scores[i][j];
								lowestH1idx = i;
								lowestH2idx = j;
							}
						}
					}	
			}
			totalScore += lowestScore;
			usedH1[lowestH1idx] = true;
			usedH2[lowestH2idx] = true;
			num++;
		}/*



		//Hungarian method to find cost efficient path
		//subtract the row min
		for (int i = 0; i < h1.length; i++)
		{	
			double min = 10000;
			//find min
			for (int j = 0; j < h1.length; j++)
			{
				if (scoresCopy[i][j] < min)
					min = scoresCopy[i][j];
			}
			//subtract min
			for (int j = 0; j < h1.length; j++)
			{
				scoresCopy[i][j] -= min;
				if (scoresCopy[i][j] < 0.01)
					scoresCopy[i][j] = 0;
			}
		}
		//subtract col min
		for (int i = 0; i < h1.length; i++)
		{	
			double min = 10000;
			//find min
			for (int j = 0; j < h1.length; j++)
			{
				if (scoresCopy[j][i] < min)
					min = scoresCopy[j][i];
			}
			//subtract min
			for (int j = 0; j < h1.length; j++)
			{
				scoresCopy[j][i] -= min;
				if (scoresCopy[j][i] < 0.01)
					scoresCopy[j][i] = 0;
			}
		}

		int n = 0;
		List<Integer> usedCols = new ArrayList();
		List<Integer> usedRows = new ArrayList();

		while (n < scoresCopy.length)
		{
			int numRow = 0;	//max num of 0s in a row
			int rowIdx = 0;//row index
			int numCol = 0;	//max num of 0s in a col
			int colIdx = 0;//col index

			Integer[] rowsArray = usedRows.toArray();
			Integer[] colsArray = usedCols.toArray();

			//find row with most 0's
			for (int i = 0; i < scoresCopy.length; i++)
			{
				
				boolean runRow = true;
				for (int row : rowsArray)
					if (row == i)
						runRow = false;

				if (runRow)
				{
					int temprow = 0;
					for (int j = 0; j < scoresCopy.length; j++)
					{
						boolean runCol = true;
						for (int col : colsArray)
							if (col == j)
								runCol = false;

						if (scoresCopy[i][j] == 0 && runCol)
							temprow++;

					}

					if (temprow > numRow)
					{
						numRow = temprow;
						rowIdx = i;
					}
				}
			}

			//find col with most 0's
			for (int i = 0; i < scoresCopy.length; i++)
			{
				
				boolean runCol = true;
				for (int col : colsArray)
					if (col == i)
						runCol = false;

				if (runCol)
				{

					int tempcol = 0;
					for (int j = 0; j < scoresCopy.length; j++)
					{
						boolean runRow = true;
						for (int row : rowsArray)
							if (row == j)
								runRow = false;

						if (scoresCopy[j][i] == 0 && runRow)
							tempcol++;

					}

					if (tempcol > numCol)
					{
						numCol = tempcol;
						colIdx = i;
					}
				}
			}

			if (numCol > numRow)
				usedCols.add(colIdx);
			else
				usedRows.add(rowIdx);

			n++;
		
		}*/

		return totalScore;
	}

	//get the shape context histogram for each point
	private static double[][][] getSCValue3D(List<Point> points)
	{
		double[][][] values = new double[points.size()][points.size()-1][2]; //[][][0] is the distance, [][][1] is the angle in radians
		double mean = 0;
		int num = 0;
		int total = 0;
		

		int i = 0;
		for (Point p1 : points)
		{
			int j = 0;
			for (Point p2: points)
			{
				if (p1 != p2)
				{
					values[i][j][0] = Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
					values[i][j][1] = Math.atan((p2.y-p1.y)/(p2.x-p1.x));

					if (p1.x < p2.x && p1.y < p2.y) //diagonal to bot right
						values[i][j][1] = -values[i][j][1];
					else if (p1.x < p2.x && p1.y > p2.y) //diagonal to top right
						values[i][j][1] = -values[i][j][1];
					else if (p2.x < p1.x && p2.y < p1.y) //diagonal to top left
						values[i][j][1] = Math.PI/2 - values[i][j][1];
					else								//diagonal to bot left
						values[i][j][1] = -Math.PI/2 + values[i][j][1];


					mean += values[i][j][0];
					num++;
					j++;
				}
				
			}

			i++;
		}

		double[][][] histogram = new double[points.size()][6][12]; 	// 6 - 0-0.125-0.25-0.5-1-2-4
													//12 - pi/12
		mean /= num;

		for (int k = 0; k < i; k++)
		{
			for (int l = 0; l < i-1; l++)
			{
				values[k][l][0] /= mean;
				int r = 0;	//can be 0-5 depending on where the radius is
				int angle = 0; //can be 0-11 depending on where the radius is



				//find the radius
				if (values[k][l][0] <= 0.125)
					r = 0;
				else if (values[k][l][0] <= 0.25)
					r = 1;
				else if (values[k][l][0] <= 0.5)
					r = 2;
				else if (values[k][l][0] <= 1)
					r = 3;
				else if (values[k][l][0] <= 2)
					r = 4;
				else
					r = 5;

				//find the angle
				if (values[k][l][1] <= -5*Math.PI/12)
					angle = 0;
				else if (values[k][l][1] <= -4*Math.PI/12)
					angle = 1;
				else if (values[k][l][1] <= -3*Math.PI/12)
					angle = 2;
				else if (values[k][l][1] <= -2*Math.PI/12)
					angle = 3;
				else if (values[k][l][1] <= -1*Math.PI/12)
					angle = 4;
				else if (values[k][l][1] <= -0*Math.PI/12)
					angle = 5;
				else if (values[k][l][1] <= 1*Math.PI/12)
					angle = 6;
				else if (values[k][l][1] <= 2*Math.PI/12)
					angle = 7;
				else if (values[k][l][1] <= 3*Math.PI/12)
					angle = 8;
				else if (values[k][l][1] <= 4*Math.PI/12)
					angle = 9;
				else if (values[k][l][1] <= 5*Math.PI/12)
					angle = 10;
				else
					angle = 11;

				histogram[k][r][angle]++;

			}
		}
	
		/*
		for (int m = 0; m < 6; m++)
		{
			System.out.println(histogram[m][0]+"\t"+histogram[m][1]+"\t"+histogram[m][2]+"\t"+histogram[m][3]+"\t"+
								histogram[m][4]+"\t"+histogram[m][5]+"\t"+histogram[m][6]+"\t"+histogram[m][7]+"\t"+
								histogram[m][8]+"\t"+histogram[m][9]+"\t"+histogram[m][10]+"\t"+histogram[m][11]);
		}
		System.out.println("");*/

		return histogram;
	}

	private static double[][] getSCValue(List<Point> points)
	{
		double[][][] values = new double[points.size()][points.size()-1][2]; //[][][0] is the distance, [][][1] is the angle in radians
		double mean = 0;
		int num = 0;
		int total = 0;

		int i = 0;
		for (Point p1 : points)
		{
			int j = 0;
			for (Point p2: points)
			{
				if (p1 != p2)
				{
					values[i][j][0] = Math.sqrt((p1.x-p2.x)*(p1.x-p2.x) + (p1.y-p2.y)*(p1.y-p2.y));
					values[i][j][1] = Math.atan((p2.y-p1.y)/(p2.x-p1.x));

					if (p1.x < p2.x && p1.y < p2.y) //diagonal to bot right
						values[i][j][1] = -values[i][j][1];
					else if (p1.x < p2.x && p1.y > p2.y) //diagonal to top right
						values[i][j][1] = -values[i][j][1];
					else if (p2.x < p1.x && p2.y < p1.y) //diagonal to top left
						values[i][j][1] = Math.PI/2 - values[i][j][1];
					else								//diagonal to bot left
						values[i][j][1] = -Math.PI/2 + values[i][j][1];


					mean += values[i][j][0];
					num++;
					j++;
				}
				
			}

			i++;
		}

		double[][] histogram = new double[6][12]; 	// 6 - 0-0.125-0.25-0.5-1-2-4
													//12 - pi/12
		mean /= num;

		for (int k = 0; k < i; k++)
		{
			for (int l = 0; l < i-1; l++)
			{
				values[k][l][0] /= mean;
				int r = 0;	//can be 0-5 depending on where the radius is
				int angle = 0; //can be 0-11 depending on where the radius is



				//find the radius
				if (values[k][l][0] <= 0.125)
					r = 0;
				else if (values[k][l][0] <= 0.25)
					r = 1;
				else if (values[k][l][0] <= 0.5)
					r = 2;
				else if (values[k][l][0] <= 1)
					r = 3;
				else if (values[k][l][0] <= 2)
					r = 4;
				else
					r = 5;

				//find the angle
				if (values[k][l][1] <= -5*Math.PI/12)
					angle = 0;
				else if (values[k][l][1] <= -4*Math.PI/12)
					angle = 1;
				else if (values[k][l][1] <= -3*Math.PI/12)
					angle = 2;
				else if (values[k][l][1] <= -2*Math.PI/12)
					angle = 3;
				else if (values[k][l][1] <= -1*Math.PI/12)
					angle = 4;
				else if (values[k][l][1] <= -0*Math.PI/12)
					angle = 5;
				else if (values[k][l][1] <= 1*Math.PI/12)
					angle = 6;
				else if (values[k][l][1] <= 2*Math.PI/12)
					angle = 7;
				else if (values[k][l][1] <= 3*Math.PI/12)
					angle = 8;
				else if (values[k][l][1] <= 4*Math.PI/12)
					angle = 9;
				else if (values[k][l][1] <= 5*Math.PI/12)
					angle = 10;
				else
					angle = 11;

				histogram[r][angle]++;

			}
		}
	
		/*
		for (int m = 0; m < 6; m++)
		{
			System.out.println(histogram[m][0]+"\t"+histogram[m][1]+"\t"+histogram[m][2]+"\t"+histogram[m][3]+"\t"+
								histogram[m][4]+"\t"+histogram[m][5]+"\t"+histogram[m][6]+"\t"+histogram[m][7]+"\t"+
								histogram[m][8]+"\t"+histogram[m][9]+"\t"+histogram[m][10]+"\t"+histogram[m][11]);
		}
		System.out.println("");*/

		return histogram;
	}

	private static Mat edgeCanny(Mat mat)
	{
		Mat edges = new Mat();
		
		Imgproc.Canny(mat, edges, 100, 200);
		
		return edges;
	}

	//gets 16 points of a grayscale canny edge image
	private static List<Point> getPoints(Mat mat, int min)
	{
		List<Point> points = new ArrayList<>();
		int maxy = mat.rows(), maxx = mat.cols();

		//try {showImage(mat, "ss");} catch (Exception e) {}

		while (points.size() < 64)
		{
			int x = (int)(Math.random()*maxx);
			int y = (int)(Math.random()*maxy);

			if (x == maxx)
				x = maxx-1;
			if (y == maxy)
				y = maxy-1;

			if (mat.get(y,x)[0] != 0)
			{
				boolean invalid = false;
				for (Point p : points)
				{
					//if it already exists
					if (p.x == x && p.y == y)
						invalid = true;
					//if its within the min distance
					if ((x-p.x)*(x-p.x) + (y-p.y)*(y-p.y) < min*min )
						invalid = true;

				}

				if (!invalid)
					points.add(new Point(x,y));
			}


		}

		return points;
		
	}


	
	//affines the pixels of a gray scale image - can be modified for rgb
	//used for prac 2 question 4
	private static Mat affinePIxel(Mat mat, double contrast, double brightness)
	{
		Mat dest = new Mat(mat.size(), CvType.CV_8U);
		
		int width = (int)mat.size().width;
		int height = (int)mat.size().height;
		
		for (int i = 0; i < width; i++)
		{
			for (int j = 0; j < height; j++)
			{

				if (mat.get(j,i) != null)
				{
					double[] data = { contrast*mat.get(j,i)[0] + brightness};
					dest.put(j,i, data);
				}
				else
					System.out.println(i + " " + j);
			}
		}
		
		return dest;
	}
	
	//returns the mat of a median filter
	//size refers to the size of the filter eg size=3 is a 3x3
	//used in question 3 of prac 2
	private static Mat medianFilter(Mat mat, int size)
	{
		Mat dest = new Mat();
		
		Imgproc.medianBlur(mat, dest, size);
		
		return dest;
	}
	
	//returns the mat of a gaussian blur
	//used in question 2 of prac 2
	private static Mat gaussianBlur(Mat mat, double sigmaX)
	{
		Mat dest = new Mat();
		
		Imgproc.GaussianBlur(mat, dest, new Size(5,5), sigmaX);
		
		return dest;
	}
	
	//holds the code used for linear filtering in question 2 prac 2
	private static void linearFiltering()
	{
		Mat mat = readImage("gray.jpg");
		
		//Kernel x
		double[] a = {	1.0, 4.0, 7.0, 4.0, 1.0,
						4.0, 16.0, 26.0, 16.0, 4.0,
						7.0, 26.0, 41.0, 26.0, 7.0,
						4.0, 16.0, 26.0, 16.0, 4.0,
						1.0, 4.0, 7.0, 4.0, 1.0 };	
		Mat kernelX = new Mat(5,5, CvType.CV_32F);
		kernelX.put(0,0, a);
		Core.multiply(kernelX, new Scalar(1/(double)(273.0)), kernelX);

		System.out.println(kernelX.dump());
		
		Mat dest1 = applyFilter2d(mat, kernelX);
		
		saveImage(dest1, "Guassian Kernel.jpg");
		
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
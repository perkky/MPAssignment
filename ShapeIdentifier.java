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

public class ShapeIdentifier
{
    private Mat pMat;
    private String pDirectory = "";
    public ShapeIdentifier(Mat inputMat, String directory)
    {
        pMat = inputMat;
        pDirectory = directory;
    }

    public String findMatch()
    {
        File folder = new File(pDirectory);
        File[] listOfFiles = folder.listFiles();

        double minAvg = 10000;
		String name = "";
        for (File file : listOfFiles) 
        {
            if (file.isFile()) 
			{
                Mat test = FileIO.readImage(pDirectory+file.getName());

                double avgScore = 0;
                for (int i = 0; i < 15; i++)
				      avgScore += compareShapes(pMat, ImgProcessing.edgeCanny(test))/15;

				if (avgScore < minAvg)
				{
					minAvg = avgScore;
					name = file.getName();
				}
            } 
        }

        return name;
    }

    private double compareShapes(Mat mat1, Mat mat2)
	{
		List<Point> points1 = getPoints(mat1, 0);
		List<Point> points2 = getPoints(mat2, 0);

		double[][][] h1 = getSCValue3D(points1);
		double[][][] h2 = getSCValue3D(points2);
		
		double score = getMinCost(h1, h2);

		return score;

	}

    //gets 64 points of a grayscale canny edge image
    private List<Point> getPoints(Mat mat, int min)
	{
		List<Point> points = new ArrayList<>();
		int maxy = mat.rows(), maxx = mat.cols();

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

    //get the shape context histogram for each point
	private double[][][] getSCValue3D(List<Point> points)
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

		return histogram;
	}

    private double getMinCost(double[][][] h1, double[][][] h2)
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

		
		//Greedly match points up to minimise cost (greedy minimise)
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
		}

		return totalScore;
	}
}
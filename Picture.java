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


public class Picture
{
    private Mat pMat;
    private double pSigma;

    public Picture(String location)
    {
        pMat = FileIO.readImage(location);
        pSigma = 1;
    }

    public void setSigma(double sigma)
    {
        pSigma = sigma;
    }

    //Returns true if the Mat is all black
    //False otherwise
    public boolean isEmpty()
    {
        boolean flag = true;

        if (Core.countNonZero(ImgProcessing.convertImage(pMat, Imgproc.COLOR_BGR2GRAY)) > 1)
			flag = true;

        return false;
    }

    public List<Label> getLabels()
    {
        Mat preprocessedMat = ImgProcessing.applyPreprocessingFilters(pMat, pSigma);

        List<MatOfPoint> contours =  ImgProcessing.getContours(preprocessedMat);
        List<Label> labels = new ArrayList<>();
		
        //contours = ImgProcessing.convertToQuad(contours);         //convert contours to quads - This may cause problems - needs to be tested more
		contours = ImgProcessing.approxContours(contours); 		    //approx the contours
		contours = ImgProcessing.removeMOPArea(contours, 10000);    //removes contours with an area less than 10000 - is the minimum as it will be at least 100x100
		

		//for each contour
		for (int i = 0; i < contours.size(); i++)
		{
			//create the mask for this specific contour to remove the background 
			Mat mask = ImgProcessing.createMask(contours, pMat.rows(), pMat.cols(), i);

			//extract that label
			Mat shapeMat = extractLabel(pMat, mask, contours.get(i), new Size(500,500));    //use to be preprocessedMat instead of pmat

            if (Core.countNonZero(ImgProcessing.convertImage(shapeMat, Imgproc.COLOR_BGR2GRAY )) > 1)
            {
                Label label = new Label(shapeMat);

                labels.add(label);
            }
        }

        return labels;
    }


    //Extracts a label from the mat and warps it to fit an image of Size size
    //mat - input mat
    //mask - mask to be applied to the mat
    //shape - shape contraining the 4 points of the label
    //size - the output size of the mat - generally 500x500
    private Mat extractLabel(Mat mat, Mat mask, MatOfPoint shape, Size size)
	{
		Mat maskedMat = new Mat(); 							//The Mat to store the masked image
		List<Point> sourcePoints = new ArrayList<Point>();	//List of the 4 source points in the original image
		Mat sP = new Mat();									//The mat version of the source points
		List<Point> destPoints = new ArrayList<Point>();	//List of the 4 corresponding dest points in the new image
		Mat dP = new Mat();									//The mat version of the dest points
		Point[] shapeArray = shape.toArray();				//Array of the shapes points
		Mat transformationMatrix = new Mat();				//The transformation matrix
		Mat newMat = Mat.zeros((int)size.height, (int)size.width, mat.type());	//The final Mat

		//Apply the mask
		mat.copyTo(maskedMat, mask);

		//Fill the source points list from the shape array
		int i = 0;
		for (Point point : shapeArray)
		{
				sourcePoints.add(new Point(point.x, point.y));
				i++;
		}

		//only run if the shape has 4 points - a quadrilateral
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

}
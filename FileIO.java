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


/*****************************************************
 *
 * FileIO class
 * Inherits from: nil
 * Purpose: Handles the input and output of image data
 *
 *****************************************************/


public class FileIO
{
	private FileReader fileReader;
	BufferedReader bufferedReader;
	String line = null;

	public FileIO(String fileName)
	{
		 try {

            fileReader = new FileReader(fileName);
			bufferedReader = new BufferedReader(fileReader);
			line = bufferedReader.readLine();
		}
		catch(FileNotFoundException ex) { System.out.println("Unable to open file '" + fileName + "'"); }  
		catch (IOException e) { line = null;}
	}

	public boolean isEmpty() { return (line == null);}
	public String getLine()
	{
		String currLine = line;
		try{
		line =  bufferedReader.readLine();
		}catch (Exception e) { line = null;}

		return currLine;
	}
	public void close()
	{
		try{
			 bufferedReader.close();
		} catch (Exception e) {}
	}

	/**Static functions****/
    public static Mat readImage(String location)
	{
		Mat img = Imgcodecs.imread(location);
		
		if (!img.empty())
			;//System.out.println("Successfully read image: " + location);//debug
		else
			System.out.println("Error reading image: " + location);
		return img;
	}
	
	public static Mat readImage(String location, int type)
	{
		Mat img = Imgcodecs.imread(location, type);
		
		if (!img.empty())
			System.out.println("Successfully read image: " + location);
		else
			throw new IllegalArgumentException("Error reading image: " + location);

		return img;
	}
	
	public static void saveImage(Mat mat, String location)
	{
		if (!mat.empty())
		{
			Imgcodecs.imwrite(location, mat);
			System.out.println("Successfully saved image: " + location);
		}
		else
			System.out.println("Error saving image: " + location);
		
	}
	
	//https://stackoverflow.com/questions/27086120/convert-opencv-mat-object-to-bufferedimage
	public static BufferedImage matToBufferedImage(Mat mat)throws Exception 
	{        
		MatOfByte mob=new MatOfByte();
		Imgcodecs.imencode(".jpg", mat, mob);
		byte ba[]=mob.toArray();

		BufferedImage bi=ImageIO.read(new ByteArrayInputStream(ba));
		return bi;
	}

    //DEBUGGING METHODS
	public static void showImage(Mat mat,  String title) throws Exception
	{
		JLabel image = new JLabel(new ImageIcon(matToBufferedImage(mat)));
		JFrame frame = new JFrame(title);
		frame.setSize(mat.cols()+50, mat.rows()+50);
		frame.setVisible(true);
		frame.add(image);
	}



}
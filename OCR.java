
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


public class OCR
{
    private static ITesseract instance = new Tesseract();

    public static String doOCR(Mat mat)
    {
        //instance.setTessVariable("tessedit_char_whitelist", "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ-'");
        String s = "";
        try
        {
            s = instance.doOCR(FileIO.matToBufferedImage(mat));
        }catch (Exception e) { }

        return cleanTextUp(s);
    }

    //Cleans the text up by removing any non standard characters
    public static String cleanTextUp(String s)
    {
        String newString = "";

        for (int i = 0; i < s.length(); i++)
        {
            //if its a ' ` or -
            if ( (int)s.charAt(i) == 39 || (int)s.charAt(i) == 45 || (int)s.charAt(i) == 96 || (int)s.charAt(i) == 46)
            {
                if (i != 0 )
                {
                    if ((int)newString.charAt(newString.length()-1) > 47 && (int)newString.charAt(newString.length()-1) < 58 ) //replace ' with . as it mistakes the two
                    {
                         newString +=".";
                    }
                }
            }
            //remove any useless characters
            else if ( ((int)s.charAt(i) > 47 && (int)s.charAt(i) < 58) || ( (int)s.charAt(i) > 64 && (int)s.charAt(i) < 91) || ( (int)s.charAt(i) > 96 && (int)s.charAt(i) < 123) || (int)s.charAt(i) == 32)
                newString += s.charAt(i);
        }

        return newString;
    }


}
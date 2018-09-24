import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main
{
    public static void main(String args[])
    {
        System.loadLibrary("opencv_java330");

        for (String arg : args)
        {

            Picture pic = new Picture(arg);

            //FileIO.saveImage(ImgProcessing.edgeCanny(FileIO.readImage(arg)), "wtf.png");

            List<Label> labelsList = new ArrayList<>();

            int maxLabels = 0;
            double maxSigma = 1.0;
            for (double sigma = 1.0; sigma < 2.5; sigma += 0.05)
            {
                pic.setSigma(sigma);
                labelsList = pic.getLabels();

                if (labelsList.size() > maxLabels)
                {
                    maxSigma = sigma;
                    maxLabels = labelsList.size();
                }
            }

            pic.setSigma(maxSigma);
            labelsList = pic.getLabels();
            Label[] labelsArray = labelsList.toArray(new Label[0]);

            System.out.println(arg);
            for (Label label : labelsArray)
            {
                label.detect();//detect class and text

                System.out.println("Top:\t" + label.getTopColour());
                System.out.println("Bot:\t" + label.getBotColour());
                System.out.println("Class:\t" + label.getClassNum());
                System.out.println("Text:\t" + label.getText());
                System.out.println();

                label.showImage("label");

                label.release();
            }
            
            Scanner sc = new Scanner(System.in);
            sc.next();
        }
    }

}
import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class Main
{
    public static void main(String args[])
    {
        System.loadLibrary("opencv_java330");
        long startTime = System.currentTimeMillis();

        for (String arg : args)
        {

            Picture pic = new Picture(arg);

            //FileIO.saveImage(ImgProcessing.edgeCanny(FileIO.readImage(arg)), "wtf.png");

            List<Label> labelsList = new ArrayList<>();

            int maxLabels = 0;
            double maxSigma = 1.0;
            for (double sigma = 0.0; sigma < 2.5; sigma += 0.25)
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
                label.detectSymbol();

                System.out.println("Top:\t" + label.getTopColour());
                System.out.println("Bot:\t" + label.getBotColour());
                System.out.println("Class:\t" + label.getClassNum());
                System.out.println("Text:\t" + label.getText());
                System.out.println("Symbol:\t" + label.getSymbol());
                System.out.println();

                //label.showImage("label");

                label.release();
            }
            
            Scanner sc = new Scanner(System.in);
            //sc.next();
        }

        long endTime = System.currentTimeMillis();
        System.out.println("Took "+ (endTime - startTime) + " ms total");
		System.out.println("Took "+(endTime - startTime)/args.length + " ms per image");
    }

}
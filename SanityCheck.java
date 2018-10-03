


//Class which contains static functions used to sanity check various things
public class SanityCheck
{
    //Sanity checks the word against all words in fileLocation, seperated by a line
    //Basically just makes the words match what is needed for the assingment spec, not too much correcting is preformed
    public static String sanityCheckText(String word, String fileLocation)
    {
        FileIO sanityWords = new FileIO(fileLocation);
        String newWord = "";

        //First check if any of the words match any in the dictionary file
        while (!sanityWords.isEmpty())
        {
            String line = sanityWords.getLine();
            String[] readWords = line.split(",");
            int lcs = getLCS(word, readWords[0]);
            //System.out.println("Comparing " + word + " " + readWords[0] + " gives " + lcs);

            boolean added = false;
            //If the words is 3 or less characters or if it requires it to be exact
            //only add it if it is exact
            if (!word.contains("RADIOACTIVE"))
            {
                if ((readWords.length ==2) || readWords[0].length() < 4)
                {
                    
                    if (readWords.length >= 2)
                    {
                        //must be exact if it has E as the second word
                        if (readWords[1].equals("E"))
                        {
                            if (word.contains(readWords[0]))
                            {
                                newWord += readWords[0] + " ";
                                added = true;
                            }
                        }
                        else
                        {
                            boolean add = true;
                            if (!word.contains(readWords[0]))
                                add = false;
                            for (int i = 1; i < readWords.length; i++)
                                if ( newWord.contains(readWords[i]) )  
                                    add = false;

                            if (add)
                            {
                                newWord += readWords[0] + " ";
                                added = true;
                            }
                        }
                    }
                    else
                    {
                        if (word.contains(readWords[0]))
                            {
                                newWord += readWords[0] + " ";
                                added = true;
                            }
                    }
                }
                if ( !added && readWords[0].length() >3 && ((readWords[0].length() < 7 && lcs >=  readWords[0].length()-1) || (lcs >=  readWords[0].length()-2)))
                {
                    //do nothing if the second word is contained
                    if (readWords.length >= 2 && !readWords[1].equals("E"))
                    {
                        boolean add = true;
                        for (int i =1; i < readWords.length; i++)
                            if ( newWord.contains(readWords[i]) )  
                                add = false;

                        if (add)
                            newWord += readWords[0] + " ";
                    }
                    else
                        newWord += readWords[0] + " ";
                }
            }

        }
        if (newWord.equals(""))
            newWord = word;

        sanityWords.close();

        //Finally replace the words with their propper words (eg NONFLAMMABLE with NON-FLAMMABLE)
        FileIO correctWords = new FileIO("correctwords.data");
        
        while (!correctWords.isEmpty())
        {
            String line = correctWords.getLine();
            String[] words = line.split(",");
            
            newWord = newWord.replace(words[0], words[1]);
        }
        
        return newWord;
    }

    //https://www.programcreek.com/2014/04/longest-common-subsequence-java/
    public static int getLCS(String a, String b)
    {
        int m = a.length();
        int n = b.length();
        int[][] dp = new int[m+1][n+1];
    
        for(int i=0; i<=m; i++){
            for(int j=0; j<=n; j++){
                if(i==0 || j==0){
                    dp[i][j]=0;
                }else if(a.charAt(i-1)==b.charAt(j-1)){
                    dp[i][j] = 1 + dp[i-1][j-1];
                }else{
                    dp[i][j] = Math.max(dp[i-1][j], dp[i][j-1]);
                }
            }
        }
    
        return dp[m][n];
    }

    public static String sanityCheckSymbol(String symbol, String fileLocation)
    {
        String newSymbol = "";

        FileIO sanitySymbols = new FileIO(fileLocation);

        while (!sanitySymbols.isEmpty())
        {
            String line = sanitySymbols.getLine();
            String[] words = line.split(",");

            if (symbol.equals(words[0]))
            {
                newSymbol = words[1];
                break;
            }
        }
        sanitySymbols.close();
        return newSymbol;
    }
}
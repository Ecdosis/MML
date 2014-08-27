/*
 * This file is part of MML.
 *
 *  MML is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  MML is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with MML.  If not, see <http://www.gnu.org/licenses/>.
 *  (c) copyright Desmond Schmidt 2014
 */

/**
 * This package requires that the server installs aspell, dictionaries
 * and the aesespeller library. It has to be called calliope or we need 
 * to rebuild the supporting C library and rerun javah.
 */
package calliope;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Look words up in the aspell dictionary
 * @author desmond
 */
public class AeseSpeller 
{
    String lang;
    // Default algorithm in isHardHyphen works pretty well
    // Italian doesn't use hyphenated words much
    static String[] it_exceptions = {};
    static HashMap<String,HashSet> compounds;
    static 
    {  
        System.loadLibrary("AeseSpeller");
        System.loadLibrary("aspell");
        compounds = new HashMap<>();
        HashSet<String> italian = new HashSet<>();
        for ( String comp: it_exceptions )
            italian.add(comp);
        compounds.put( "it", italian );
        // load other language exceptions here
    }
    public AeseSpeller( String lang ) throws Exception
    {
        this.lang = lang;
        if ( !initialise(lang) )
            throw new Exception("failed to initialise language: "+lang );
    }
    /** must call this to initialise at least one language */
    native boolean initialise( String lang );
    /** must cleanup or waste memory */
    public native void cleanup();
    /** tells you if word is in that dictionary */
    public native boolean hasWord( String word, String lang );
    /** lists valid language codes */
    public native String[] listDicts();
    /**
     * For testing
     * @param args 
     */
    public static void main( String[] args )
    {
        try
        {
            AeseSpeller as = new AeseSpeller("en_GB");
            if ( as.hasWord( "housing", "en_GB") )
                System.out.println("Dictionary (en_GB) has housing");
            if ( as.hasWord( "pratiche", "it") )
                System.out.println("Dictionary (it) has practiche");
            if ( as.hasWord( "progetto", "it") )
                System.out.println("Dictionary (it) has progetto");
            if ( as.hasWord( "automobile", "en_GB") )
                System.out.println("Dictionary (en_GB) has automobile");
            String[] dicts = as.listDicts();
            for ( int i=0;i<dicts.length;i++ )
            {
                System.out.println(dicts[i]);
            }
            if ( dicts.length==0 )
                System.out.println("no dicts!");
            as.cleanup();
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
        }
    }
    /**
     * Should we hard-hyphenate two words or part-words? We require:
     * 1. The leading and trailing halves of the "word" are also words
     * 2. The compound of the two halves is *not* a word
     * 3. Or, the compound is a word but is listed in the exceptions table
     * @param last the previous 'word'
     * @param next the word on the next line
     * @return true for a hard hyphen else soft
     */
    public boolean isHardHyphen( String last, String next )
    {
        String compound = last+next;
        if ( this.hasWord(last,lang)
            &&this.hasWord(next,lang)
            &&(!this.hasWord(compound,lang)
            ||compounds.get(lang).contains(compound)) )
            return true;
        else
            return false;
    }
}

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
 *  (c) copyright Desmond Schmidt 2016
 */

package mml.handler.get;
import java.util.ArrayList;
import java.io.File;
import java.util.Arrays;
import calliope.core.DocType;

/**
 * Compute metadata from bare file names
 * @author desmond
 */
public class Metadata {
    String volume;
    ArrayList<String> pages;
    /**
     * Get the name of the REAL enclosing directory
     * @param file the file perhaps an alias
     * @return the name of its true parent dir
     */
    final String getRealParentDirName( File file )
    {
        try
        {
            String path = file.getCanonicalPath();
            File f = new File(path);
            return f.getParentFile().getName();
        }
        catch ( Exception e )
        {
            return file.getParentFile().getName();
        }
    }
    /**
     * Store metadata about an image file
     * @param file 
     */
    public Metadata( File file )
    {
        String name = file.getName();
        String pageNo = "";
        if ( DocType.isLetter(name) )
        {
            volume = DocType.getDescription(name);
            pageNo = DocType.getPageNo(name, DocType.LETTER);
        }
        else if ( DocType.isNewspaper(name) )
        {
            volume = DocType.getDescription(name);
            String parent = getRealParentDirName(file);
            if ( parent.length()>0 )
                volume = parent + " "+volume;
            pageNo = DocType.getPageNo(name,DocType.NEWSPAPER);
        }
        else
        {
            volume = getRealParentDirName(file);
            pageNo = DocType.getPageNo(name,DocType.MSORBOOK);
        }
        pages = new ArrayList<String>();
        pages.add(pageNo);
    }
    /**
     * Add a file to the collection 
     * @param file the additional file
     */
    void add( File file )
    {
        String name = file.getName();
        String pageNo;
        if ( DocType.isLetter(name) )
        {
            pageNo = DocType.getPageNo(name, DocType.LETTER);
        }
        else if ( DocType.isNewspaper(name) )
        {
            pageNo = DocType.getPageNo(name,DocType.NEWSPAPER);
        }
        else
        {
            pageNo = DocType.getPageNo(name,DocType.MSORBOOK);
        }
        pages.add( pageNo );
    }
    /**
     * Is this page no ref a real page number (fails on roman numerals)
     * @param page the page number
     * @return true if it was digits followed by optional letters
     */
    boolean isPage( String page )
    {
        int state = 0;
        for ( int i=0;i<page.length();i++ )
        {
            char token = page.charAt(i);
            switch ( state )
            {
                case 0:
                    if ( Character.isDigit(token) )
                        state = 1;
                    else
                        return false;
                    break;
                case 1:
                    if ( Character.isLetter(token) )
                        state = 2;
                    else if ( !Character.isDigit(token) )
                        return false;
                    break;
                case 2:
                    if ( !Character.isLetter(token) )
                        return false;
                    break;
            }
        }
        return true;
    }
    /**
     * Convert to a plain text string as metadata
     * @return a string
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        if ( pages.size() == 1 )
        {
            String page = pages.get(0);
            if ( isPage(page) )
                sb.append("p.");
            sb.append(page);
        }
        else 
        {
            String[] sorted = new String[pages.size()];
            pages.toArray(sorted);
            Arrays.sort(sorted);
            if ( isPage(pages.get(0)) )
                sb.append("pp.");
            sb.append(sorted[0]);
            sb.append("-");
            sb.append(sorted[sorted.length-1]);
        }
        sb.append(" of ");
        sb.append(volume);
        return sb.toString();
    }
}

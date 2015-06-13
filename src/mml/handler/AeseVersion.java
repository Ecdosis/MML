/* This file is part of calliope.
 *
 *  calliope is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  calliope is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with calliope.  If not, see <http://www.gnu.org/licenses/>.
 */
package mml.handler;
import edu.luc.nmerge.mvd.MVD;

/**
 * Represent a version returned from an MVD
 * @author desmond
 */
public class AeseVersion 
{
    /** format of the MVD if any */
    String format;
    /** format the text is in */
    String content;
    /** actual text of the version */
    byte[] version;
    /** if CorCode the default style */
    String defaultStyle;
    /** description if only one version */
    String description;
    /** default first version */
    String version1;
    /** the original mvd data or null */
    MVD mvd;
    /**
     * Set the format of this version
     * @param format the format to set
     */
    public void setFormat( String format )
    {
        int slashPos = format.indexOf("/");
        if ( slashPos != -1 )
        {
            content = format.substring( slashPos+1 );
            this.format = format.substring(0,slashPos);
        }
        else
            this.content = this.format = format;
    }
    public void setDescription( String description )
    {
        this.description = description;
    }
    public void setVersion1( String version1 )
    {
        this.version1 = version1;
    }
    /**
     * Set the default style of this version
     * @param style the style to set
     */
    public void setStyle( String style )
    {
        this.defaultStyle = style;
    }
    /**
     * Set the version data of this version
     * @param format the version contents to set
     */
    public void setVersion( byte[] version )
    {
        this.version = version;
    }
    /**
     * Remember the MVD used to get the version
     * @param mvd 
     */
    public void setMVD( MVD mvd )
    {
        this.mvd = mvd;
    }
     /**
     * Set the format of this version
     * @return the name of the format 
     */
    public String getFormat()
    {
        return format;
    }
    /**
     * Set the format of this version's content
     * @return the name of the format 
     */
    public String getContentFormat()
    {
        return content;
    }
     /**
     * Set the mvd used to fetch this version
     * @return the original MVD
     */
    public MVD getMVD()
    {
        return mvd;
    }
    /**
     * Get the version data of this version
     * @return the version contents 
     */
    public byte[] getVersion()
    {
        return version;
    }
    /**
     * Get the encoding of the version data
     * @return an encoding name
     */
    public String getEncoding()
    {
        if ( mvd != null )
            return mvd.getEncoding();
        else
            return "UTF-8";
    }
    /**
     * Get the MVD version as a String
     * @return the string or an "invalid encoding" message
     */
    public String getVersionString()
    {
        try
        {
            if ( mvd != null )
                return new String( version, mvd.getEncoding());
            else    // plain text
                return new String( version, "UTF-8" );
        }
        catch ( Exception e )
        {
            return "invalid encoding "+mvd.getEncoding();
        }
    }
    /**
     * Get the default style of this version
     * @return the default style if any
     */
    public String getStyle()
    {
        return this.defaultStyle;
    }
    /**
     * Get the length of the data version
     * @return the version's length
     */
    public int getVersionLength()
    {
        return version.length;
    }
    /**
     * Get the full versionIDs of all the versions
     * @return an array of versionIDs
     */
    public String[] listVersions()
    {
        if ( mvd != null )
        {
            int nversions = mvd.numVersions();
            String[] array = new String[nversions];
            for ( int i=0;i<nversions;i++ )
            {
                short id = (short)(i+1);
                array[i] = mvd.getGroupPath(id)+"/"+mvd.getVersionShortName(id);
            }
            return array;
        }
        else
        {
            String[] array = new String[1];
            array[0] = this.version1;
            return array;
        }
    }
    /**
     * Get the long name for the given version int id
     * @param id the identifier as an index+1 into the version table
     * @return a String
     */
    public String getVersionLongName( int id )
    {
        if ( mvd != null )
            return mvd.getVersionLongName(id);
        else
            return this.description;
    }
}

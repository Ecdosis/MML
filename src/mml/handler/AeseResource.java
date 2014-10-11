/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler;

/**
 * An MVD or plain text file wrapped up as a resource
 * @author desmond
 */
public class AeseResource 
{
    String format;
    String content;
    String description;
    String version1;
    public void setFormat( String format )
    {
        this.format = format;
    }
    public void setContent( String content )
    {
        this.content = content;
    }
    public void setDescription( String description )
    {
        this.description = description;
    }
    public void setVersion1( String version1 )
    {
        this.version1 = version1;
    }
    public String getVersion1( )
    {
        return this.version1;
    }
    public String getFormat()
    {
        return this.format;
    }
    public String getContent( )
    {
        return this.content;
    }
    public String getDescription( )
    {
        return this.description;
    }
}

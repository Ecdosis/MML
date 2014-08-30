/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.database;

/**
 * Basic metadata about an image
 * @author desmond
 */
public class ImgInfo 
{
    public int width;
    public int height;
    public String mimeType;
    public ImgInfo( int width, int height, String mimeType )
    {
        this.width = width;
        this.height = height;
        this.mimeType = mimeType;
    }
}

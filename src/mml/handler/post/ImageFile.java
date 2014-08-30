/* This file is part of MML.
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
 */
package mml.handler.post;
import mml.exception.*;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.ByteArrayInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
/**
 * An image file
 * @author desmond
 */
public class ImageFile
{
    String type;
    String name;
    byte[] data;
    int width;
    int height;
    public ImageFile( String name, String type, byte[] data ) 
        throws MMLImageException
    {
        this.data = data;
        this.name = name;
        this.type = type;
        try
        {
            ByteArrayInputStream bis = new ByteArrayInputStream(data);
            MemoryCacheImageInputStream mciis = new MemoryCacheImageInputStream(bis);
            BufferedImage image = ImageIO.read(mciis);
            this.width = image.getWidth();
            this.height = image.getHeight();

        } catch (Exception ex){
            ex.printStackTrace();
        }
    }
    public byte[] getData()
    {
        return this.data;
    }
    public String getName()
    {
        return this.name;
    }
    public String getType()
    {
        return this.type;
    }
    public int getHeight()
    {
        return height;
    }
    public int getWidth()
    {
        return width;
    }
}

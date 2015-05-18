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
package mml.handler.get;
import calliope.core.image.MimeType;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import calliope.core.image.Corpix;
import mml.exception.MMLException;
import javax.servlet.ServletOutputStream;
import mml.MMLWebApp;

/**
 * Fetch an image from the corpix collection
 * @author desmond
 */
public class MMLCorpixHandler extends MMLGetHandler
{
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn )
        throws MMLException
    {
        try
        {
            MimeType type = new MimeType();
            byte[] data = Corpix.getImage( MMLWebApp.webRoot, urn, type );
            if ( data != null)
            {
                response.setContentType(type.mimeType);
                ServletOutputStream sos = response.getOutputStream();
                sos.write( data );
                sos.close();
            }
            else
            {
                response.getOutputStream().println("image "+urn+" not found");
            }
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}

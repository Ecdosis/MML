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

package mml.handler.get;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import java.io.File;
import java.io.FileInputStream;
import java.net.URLConnection;
import javax.servlet.ServletOutputStream;
import mml.handler.MMLHandler;

/**
 * Handle requests for ordinary files like scripts
 * @author desmond
 */
public class MMLFileHandler extends MMLHandler 
{
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        File f = new File(urn);
        try
        {
            FileInputStream fis = new FileInputStream(f);
            byte[] data = new byte[(int)f.length()];
            fis.read(data);
            String mimeType = URLConnection.guessContentTypeFromStream(fis);
            response.setContentType(mimeType);
            ServletOutputStream sos = response.getOutputStream();
            sos.write( data );
            sos.close();
        }
        catch ( Exception e )
        {
            System.out.println(e.getMessage());
            throw new MMLException(e);
        }
    }
}

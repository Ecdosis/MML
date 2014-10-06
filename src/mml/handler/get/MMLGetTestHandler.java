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
import calliope.core.Utils;
import mml.constants.Subdir;
import mml.exception.MMLException;
import mml.test.Test;

/**
 * Handle a request to instantiate a Test class
 * @author desmond
 */
public class MMLGetTestHandler extends MMLGetHandler
{
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            String first = Utils.first(urn);
            try
            {
                Subdir sd = Subdir.valueOf(first.toUpperCase());
//                urn = Utils.pop(urn);
                new MMLFileHandler().handle(request,response, urn );
            }
            catch ( IllegalArgumentException e )
            {
                if (first == null || first.length() == 0) {
                    first = "Post";
                } else if (first.length() > 0) {
                    first = Character.toUpperCase(first.charAt(0))
                        + first.substring(1);
                }
                String className = "mml.test." + first;
                Class tClass = Class.forName(className);
                Test t = (Test) tClass.newInstance();
                t.handle(request, response, Utils.pop(urn));
            }
        } catch (Exception e) {
            throw new MMLException(e);
        }
    }
}

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

package mml.handler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.Utils;
import mml.exception.*;
import mml.constants.*;
import mml.test.Test;

/**
 * Handle GET request for the MML service
 * @author desmond
 */
public class MMLGetHandler extends MMLHandler {

    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        try {
            String service = Utils.first(urn);
            System.out.println(urn );
            if (service.equals(Service.TEST.toString())) {
                try {
                    String second = Utils.second(urn);
                    try
                    {
                        Subdir sd = Subdir.valueOf(second.toUpperCase());
                        urn = Utils.pop(urn);
                        new MMLFileHandler().handle(request,response, urn );
                    }
                    catch ( IllegalArgumentException e )
                    {
                        if (second == null || second.length() == 0) {
                            second = "Post";
                        } else if (second.length() > 0) {
                            second = Character.toUpperCase(second.charAt(0))
                                + second.substring(1);
                        }
                        String className = "mml.test." + second;
                        Class tClass = Class.forName(className);
                        Test t = (Test) tClass.newInstance();
                        t.handle(request, response, Utils.pop(urn));
                    }
                } catch (Exception e) {
                    throw new MMLException(e);
                }
            } 
        } catch (Exception e) {
            throw new MMLException(e);
        }
    }
}

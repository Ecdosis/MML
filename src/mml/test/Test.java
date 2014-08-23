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

package mml.test;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.handler.MMLHandler;
import mml.exception.MMLException;
import html.*;

/**
 * Abstract Test handler
 * @author desmond
 */
public abstract class Test extends MMLHandler
{
    HTML doc;
    public Test()
    {
        doc = new HTML();
    }
    /**
     * Display the test GUI, selecting the default Home tab
     * @param request the request to read from
     * @param urn the original URN - ignored
     */
    @Override
    public abstract void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException;
}

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
 *  (c) copyright Desmond Schmidt 2015
 */
package mml.handler.get;

import calliope.core.constants.Database;
import calliope.core.Utils;
import calliope.core.constants.JSONKeys;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;
import mml.constants.Params;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import org.json.simple.JSONValue;
import org.json.simple.JSONObject;

/**
 * Get a dialect
 * @author desmond
 */
public class MMLGetDialectHandler extends MMLGetHandler
{
    static String DEFAULT_DIALECT = "{ \"body\": \"{\\\"paragraph\\\":{\\\"prop\\\":\\\"\\\"},\\\"softhyphens\\\":true,\\\"smartquotes\\\":true,\\\"codeblocks\\\":[{\\\"tag\\\":\\\"pre\\\",\\\"prop\\\":\\\"level1\\\"}], \\\"section\\\":{\\\"prop\\\":\\\"section\\\"},\\\"headings\\\":[{\\\"tag\\\":\\\"=\\\",\\\"prop\\\":\\\"h1\\\"},{\\\"tag\\\":\\\"-\\\",\\\"prop\\\":\\\"h2\\\"},{\\\"tag\\\":\\\"_\\\",\\\"prop\\\":\\\"h3\\\"}],\\\"milestones\\\":[{\\\"rightTag\\\":\\\"]\\\",\\\"leftTag\\\":\\\"[\\\",\\\"prop\\\":\\\"page\\\"}],\\\"quotations\\\":[{\\\"prop\\\":\\\"level1\\\"}],\\\"description\\\":\\\"Default dialect\\\",\\\"language\\\":\\\"en\\\",\\\"paraformats\\\":[{\\\"rightTag\\\":\\\"<-\\\",\\\"leftTag\\\":\\\"->\\\",\\\"prop\\\":\\\"centered\\\"}],\\\"charformats\\\":[{\\\"tag\\\":\\\"*\\\",\\\"prop\\\":\\\"italics\\\"},{\\\"tag\\\":\\\"`\\\",\\\"prop\\\":\\\"letter-spacing\\\"},{\\\"tag\\\":\\\"@\\\",\\\"prop\\\":\\\"small-caps\\\"}]}\" }";
    @Override
    public void handle( HttpServletRequest request, 
        HttpServletResponse response, String urn ) throws MMLException
    {
        try
        {
            String docid = request.getParameter(Params.DOCID);
            String version1 = request.getParameter(Params.VERSION1);
            if ( version1 != null && version1.length()>0 && !docid.endsWith(version1) )
                docid += version1;
            // look for a dialect file with the full path then
            // if not found progressively pop off segments from the end
            String res = null;
            Connection conn = Connector.getConnection();
            while ( res == null && docid.length() > 0 )
            {
                res = conn.getFromDb(Database.DIALECTS,docid);
                if ( res == null )
                    docid = Utils.chomp( docid );
            }
            if ( res == null )
                res = DEFAULT_DIALECT;
            JSONObject jObj = (JSONObject)JSONValue.parse(res);
            String dialect = (String)jObj.get(JSONKeys.BODY);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.getWriter().write(dialect);
        } catch (Exception e) {
            throw new MMLException(e);
        }
    }
}

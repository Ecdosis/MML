/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mml.handler.get;

import calliope.core.constants.Database;
import calliope.core.database.Connection;
import calliope.core.database.Connector;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import mml.exception.MMLException;

/**
 * Get metadata about a document
 * @author desmond
 */
public class MMLMetadataHandler extends MMLGetHandler 
{
    String database;
    public MMLMetadataHandler( String database )
    {
        this.database = database;
    }
    public void handle(HttpServletRequest request,
        HttpServletResponse response, String urn) throws MMLException {
        try
        {
            Connection conn = Connector.getConnection();
            String md = conn.getMetadata( Database.CORTEX, urn );
            if ( md != null)
            {
                response.setContentType("application/json");
                response.setCharacterEncoding(encoding);
                response.getWriter().println(md.toString());
            }
            else
            {
                response.getOutputStream().println("metadata at "
                    +urn+" not found");
            }
        }
        catch ( Exception e )
        {
            throw new MMLException( e );
        }
    }
}

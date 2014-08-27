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
package mml.database;


import mml.exception.MMLDbException;

/**
 * Handle connections with a database
 * @author desmond
 */
public class Connector 
{
    static Connection connection = null;
    /**
     * Initialise once per instantiation
     * @param repository the repository type
     * @param user the user name
     * @param password the user's password
     * @param host the domain name of the host
     * @param dbPort the database port
     * @param wsPort the web-service port
     * @param webRoot the full path to the web-root
     * @throws MMLDbException 
     */
    public static void init( Repository repository, String user, 
        String password, String host, int dbPort, int wsPort, String webRoot ) 
        throws MMLDbException
    {
        switch ( repository )
        {
            case MONGO:
                connection = new MongoConnection(
                    user,password,host, dbPort,wsPort );
                break;
            default:
                throw new MMLDbException( "Unknown repository type "
                    +repository );
        }
    }
    /**
     * Get the connection object
     * @return an active Connection
     */
    public static Connection getConnection() throws MMLDbException
    {
        if ( connection == null )
            throw new MMLDbException( "connection to database was null" );
        return connection;
    }
    /**
     * Is the connection open?
     * @return true if so
     */
    public static boolean isOpen()
    {
        return connection != null;
    }
    /**
     * Get the type of this connection
     * @return a Repository type
     */
    public static Repository getRepository()
    {
        if ( connection != null )
        {
            if ( connection instanceof MongoConnection )
                return Repository.MONGO;
        }
        return Repository.UNSET;
    }
}

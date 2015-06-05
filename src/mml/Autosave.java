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
package mml;

/**
 * Maintain a constant vigil waiting for files to appear in scratch, then 
 * writing them out permanently to the proper collections after a certain time.
 * @author desmond
 */
public class Autosave {
    /** when reaping in progress don't accept new files to scratch */
    public static boolean inProgress;
    /** when scratch being written to don't do any reaping */
    public static boolean lock;
    /** Set up reaping */
    static 
    {
        Reaper reaper = new Reaper();
        reaper.start();
    }
}

/* This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
 * 02111-1307, USA.
 *
 * http://www.gnu.org/copyleft/gpl.html
 */
package ru.agecold.gameserver.taskmanager.tasks;

import ru.agecold.gameserver.Shutdown;
import ru.agecold.gameserver.taskmanager.Task;
import ru.agecold.gameserver.taskmanager.TaskManager.ExecutedTask;

/**
 * @author Layane
 *
 */
public final class TaskRestart extends Task
{
    public static final String NAME = "restart";

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.tasks.Task#getName()
     */
    @Override
    public String getName()
    {
        return NAME;
    }

    /* (non-Javadoc)
     * @see ru.agecold.gameserver.tasks.Task#onTimeElapsed(ru.agecold.gameserver.tasks.TaskManager.ExecutedTask)
     */
    @Override
    public void onTimeElapsed(ExecutedTask task)
    {
        Shutdown handler = new Shutdown(Integer.valueOf(task.getParams()[2]),true);
        handler.start();
    }

}

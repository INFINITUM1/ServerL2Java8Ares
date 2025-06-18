/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ru.agecold.gameserver.taskmanager;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.taskmanager.utils.RunnableImpl;
import ru.agecold.gameserver.taskmanager.utils.SteppingRunnableQueueManager;

/**
 *
 * @author Администратор
 */
public class LazyPrecisionTaskManager extends SteppingRunnableQueueManager {

    private static final LazyPrecisionTaskManager _instance = new LazyPrecisionTaskManager();

    public static final LazyPrecisionTaskManager getInstance() {
        return _instance;
    }

    private LazyPrecisionTaskManager() {
        super(1000L);
        ThreadPoolManager.getInstance().scheduleGeneralAtFixedRate(this, 1000L, 1000L);
        //Очистка каждые 60 секунд
        ThreadPoolManager.getInstance().scheduleAiAtFixedRate(new RunnableImpl() {
            @Override
            public void runImpl() throws Exception {
                LazyPrecisionTaskManager.this.purge();
            }

        }, 60000L, 60000L);
    }

}

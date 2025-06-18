package ru.agecold.util.log;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.awt.*;
import javax.swing.*;

import ru.agecold.gameserver.ThreadPoolManager;
import ru.agecold.gameserver.model.L2World; 

public class DefaultLogger extends Logger
{
	public DefaultLogger(String name)
	{		
		super(name, null);
	}

	public Logger get(String name)
	{
		return getLogger(name);
	}
}
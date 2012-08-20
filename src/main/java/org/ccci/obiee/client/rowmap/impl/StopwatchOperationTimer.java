package org.ccci.obiee.client.rowmap.impl;

import org.apache.log4j.Logger;

import com.google.common.base.Stopwatch;

public class StopwatchOperationTimer implements OperationTimer
{
    
    private Stopwatch stopwatch;

    private final Logger log = Logger.getLogger(getClass());
    
    @Override
    public void start()
    {
        stopwatch = new Stopwatch();
        stopwatch.start();
    }

    @Override
    public void stopAndLog(String operationDescription)
    {
        stopwatch.stop();
        log.debug(operationDescription + " in " + stopwatch);
    }

}

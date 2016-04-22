package org.ccci.obiee.client.rowmap.impl;


import com.google.common.base.Stopwatch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopwatchOperationTimer implements OperationTimer
{
    
    private Stopwatch stopwatch;

    private final Logger log = LoggerFactory.getLogger(getClass());
    
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

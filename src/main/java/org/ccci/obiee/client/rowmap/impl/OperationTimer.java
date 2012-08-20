package org.ccci.obiee.client.rowmap.impl;

public interface OperationTimer
{
    public void start();
    public void stopAndLog(String operationDescription);

}

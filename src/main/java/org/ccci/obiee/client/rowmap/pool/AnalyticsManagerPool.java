package org.ccci.obiee.client.rowmap.pool;

import java.util.concurrent.TimeUnit;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.commons.pool.impl.GenericObjectPool;
import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;

import com.google.common.base.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AnalyticsManagerPool
{
    
    private final GenericObjectPool pool;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    
    public AnalyticsManagerPool(AnalyticsManagerFactory analyticsManagerFactory)
    {
        PoolableObjectFactory factory = new AnalyticsManagerObjectFactory(analyticsManagerFactory);
        pool = new GenericObjectPool(factory);
        pool.setMaxActive(50);
        pool.setMaxIdle(25);
        pool.setMaxWait(TimeUnit.SECONDS.toMillis(5));
        pool.setTestOnBorrow(true);
    }

    public AnalyticsManager borrowAnalyticsManager()
    {
        try
        {
            return (AnalyticsManager) pool.borrowObject();
        }
        catch (Exception e)
        {
            Throwables.propagateIfPossible(e);
            throw new RuntimeException(e);
        }
    }
    
    public void returnAnalyticsManager(AnalyticsManager analyticsManager)
    {
        try
        {
            pool.returnObject(analyticsManager);
        }
        catch (Exception e)
        //GeneriObjectPool#returnObject(Object) doesn't actually throw exceptions
        {
            Throwables.propagateIfPossible(e);
            throw new RuntimeException(e);
        }
    }
    
    public void shutdown()
    {
        try
        {
            pool.close();
        }
        catch (Exception e)
        {
            log.warn("exception shutting down analytics manager pool; ignoring", e);
        }
    }
    
    public void setMaxIdle(int maxIdle)
    {
        pool.setMaxIdle(maxIdle);
    }
    
    public void setMaxActive(int maxActive)
    {
        pool.setMaxActive(maxActive);
    }
    
    public static class AnalyticsManagerObjectFactory implements PoolableObjectFactory
    {

        private final AnalyticsManagerFactory analyticsManagerFactory;

        public AnalyticsManagerObjectFactory(AnalyticsManagerFactory analyticsManagerFactory)
        {
            this.analyticsManagerFactory = analyticsManagerFactory;
        }

        @Override
        public AnalyticsManager makeObject() throws Exception
        {
            return analyticsManagerFactory.createAnalyticsManager();
        }

        @Override
        public void destroyObject(Object obj) throws Exception
        {
            AnalyticsManager manager = (AnalyticsManager) obj;
            manager.close();
        }

        @Override
        public boolean validateObject(Object obj)
        {
            AnalyticsManager manager = (AnalyticsManager) obj;
            try
            {
                manager.validate();
                return true;
            }
            catch (IllegalStateException e)
            {
                return false;
            }
        }

        @Override
        public void activateObject(Object obj) throws Exception
        {
        }

        @Override
        public void passivateObject(Object obj) throws Exception
        {
        }

    }

}

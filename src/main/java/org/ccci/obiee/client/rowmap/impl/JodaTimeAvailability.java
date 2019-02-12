package org.ccci.obiee.client.rowmap.impl;

class JodaTimeAvailability
{

    static boolean isJodaAvailable()
    {
        try
        {
            JodaTimeAvailability.class.getClassLoader().loadClass("org.joda.time.LocalDate");
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }
}

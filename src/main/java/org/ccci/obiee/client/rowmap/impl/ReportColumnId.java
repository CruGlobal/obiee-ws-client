package org.ccci.obiee.client.rowmap.impl;

/**
 * A value object that represents a unique column within an Analytics report.
 * 
 * @author Matt Drees
 */
class ReportColumnId
{
    public final String tableHeading;
    public final String columnHeading;
    
    public ReportColumnId(String tableHeading, String columnHeading)
    {
        this.tableHeading = tableHeading;
        this.columnHeading = columnHeading;
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((columnHeading == null) ? 0 : columnHeading.hashCode());
        result = prime * result + ((tableHeading == null) ? 0 : tableHeading.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        ReportColumnId other = (ReportColumnId) obj;
        if (columnHeading == null)
        {
            if (other.columnHeading != null) return false;
        }
        else if (!columnHeading.equals(other.columnHeading)) return false;
        if (tableHeading == null)
        {
            if (other.tableHeading != null) return false;
        }
        else if (!tableHeading.equals(other.tableHeading)) return false;
        return true;
    }
    
    @Override
    public String toString()
    {
        return tableHeading + " : " + columnHeading;
    }
    
}
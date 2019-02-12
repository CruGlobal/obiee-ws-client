package org.ccci.obiee.client.rowmap;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.ccci.obiee.client.rowmap.annotation.Column;
import org.ccci.obiee.client.rowmap.annotation.ReportParamVariable;
import org.ccci.obiee.client.rowmap.annotation.ReportPath;
import org.ccci.obiee.client.rowmap.annotation.Scale;

@ReportPath("/shared/CCCi/SSW/SAI Donations")
public class SaiDonationRow
{
    public static ReportDefinition<SaiDonationRow> report = new ReportDefinition<SaiDonationRow>(SaiDonationRow.class);

    public static class SaiDonationParameters
    {
        @ReportParamVariable
        public String designationNumber;
        @ReportParamVariable
        public String accountNumber;
        @ReportParamVariable
        public LocalDate donationRangeBegin;
        @ReportParamVariable
        public LocalDate donationRangeEnd;
    }

    @Column(tableHeading = "Designation")
    private String designationNumber;
    
    @Column(tableHeading = "Source Account Profile")
    private String accountNumber;
    
    @Column(tableHeading = "Source Account Profile")
    private String accountName;
    
    @Column(tableHeading = "Transaction Date")
    private LocalDate transactionDate;
    
    @Column(tableHeading = "Transaction")
    private String subType;
    
    @Column(tableHeading = "Transaction Item")
    @Scale(2)
    private BigDecimal transactionAmount;
    
    @Column(tableHeading = "Transaction")
    private String transactionNumber;
    
    @Column(tableHeading = "Fact - Transaction Items", columnHeading = "# Transaction Items")
    private Integer numberOfTransactionItems;
    
    

    public String getDesignationNumber()
    {
        return designationNumber;
    }

    public void setDesignationNumber(String designationNumber)
    {
        this.designationNumber = designationNumber;
    }

    public String getAccountNumber()
    {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber)
    {
        this.accountNumber = accountNumber;
    }

    public String getAccountName()
    {
        return accountName;
    }

    public void setAccountName(String accountName)
    {
        this.accountName = accountName;
    }

    public LocalDate getTransactionDate()
    {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate)
    {
        this.transactionDate = transactionDate;
    }

    public String getSubType()
    {
        return subType;
    }

    public void setSubType(String subType)
    {
        this.subType = subType;
    }

    public BigDecimal getTransactionAmount()
    {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount)
    {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionNumber()
    {
        return transactionNumber;
    }

    public void setTransactionNumber(String transactionNumber)
    {
        this.transactionNumber = transactionNumber;
    }

    public Integer getNumberOfTransactionItems()
    {
        return numberOfTransactionItems;
    }

    public void setNumberOfTransactionItems(Integer numberOfTransactionItems)
    {
        this.numberOfTransactionItems = numberOfTransactionItems;
    }
    
    
}

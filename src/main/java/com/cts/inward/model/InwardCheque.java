package com.cts.inward.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public class InwardCheque {

    private final String chequeNumber;
    private final String batchId;
    private final String accountNumber;
    private final String drawerName;
    private final BigDecimal amount;
    private final String micrCode;
    private final LocalDate chequeDate;
    private final LocalDate presentingDate;

    private InwardCheque(
            String chequeNumber,
            String batchId,
            String accountNumber,
            String drawerName,
            BigDecimal amount,
            String micrCode,
            LocalDate chequeDate,
            LocalDate presentingDate) {

        this.chequeNumber = chequeNumber;
        this.batchId = batchId;
        this.accountNumber = accountNumber;
        this.drawerName = drawerName;
        this.amount = amount;
        this.micrCode = micrCode;
        this.chequeDate = chequeDate;
        this.presentingDate = presentingDate;
    }

    public static InwardCheque of(
            String chequeNumber,
            String batchId,
            String accountNumber,
            String drawerName,
            BigDecimal amount,
            String micrCode,
            LocalDate chequeDate,
            LocalDate presentingDate) {

        return new InwardCheque(
                chequeNumber,
                batchId,
                accountNumber,
                drawerName,
                amount,
                micrCode,
                chequeDate,
                presentingDate);
    }

    public String getChequeNumber() {
        return chequeNumber;
    }

    public String getBatchId() {
        return batchId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public String getDrawerName() {
        return drawerName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getMicrCode() {
        return micrCode;
    }

    public LocalDate getChequeDate() {
        return chequeDate;
    }

    public LocalDate getPresentingDate() {
        return presentingDate;
    }

	@Override
	public String toString() {
		return "InwardCheque [chequeNumber=" + chequeNumber + ", batchId=" + batchId + ", accountNumber="
				+ accountNumber + ", drawerName=" + drawerName + ", amount=" + amount + ", micrCode=" + micrCode
				+ ", chequeDate=" + chequeDate + ", presentingDate=" + presentingDate + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(accountNumber, amount, batchId, chequeDate, chequeNumber, drawerName, micrCode,
				presentingDate);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		InwardCheque other = (InwardCheque) obj;
		return Objects.equals(accountNumber, other.accountNumber) && Objects.equals(amount, other.amount)
				&& Objects.equals(batchId, other.batchId) && Objects.equals(chequeDate, other.chequeDate)
				&& Objects.equals(chequeNumber, other.chequeNumber) && Objects.equals(drawerName, other.drawerName)
				&& Objects.equals(micrCode, other.micrCode) && Objects.equals(presentingDate, other.presentingDate);
	}
    
    
}
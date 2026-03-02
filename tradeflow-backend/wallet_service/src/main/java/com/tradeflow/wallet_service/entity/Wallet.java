package com.tradeflow.wallet_service.entity;

import java.math.BigDecimal;

public class Wallet {

    private Long id;
    private Long userId;
    private BigDecimal balance = BigDecimal.ZERO;
    private Long version;

    public Wallet() {}

    public Wallet(Long userId) {
        this.userId = userId;
        this.balance = BigDecimal.ZERO;
    }

    public Long getId() { 
        return id; 
    }
    
    public Long getUserId() { 
        return userId; 
    }
    
    public void setUserId(Long userId) { 
        this.userId = userId; 
    }
    
    public BigDecimal getBalance() { 
        return balance; 
    }
    
    public void setBalance(BigDecimal balance) { 
        this.balance = balance; 
    }
    
    public Long getVersion() { 
        return version; 
    }
}
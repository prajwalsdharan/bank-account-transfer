package com.db.awmd.challenge.constants;

public class ExceptionConstants {
    public static final String ACCOUNT_NOT_FOUND_EXCEPTION = "The requested account is not found - ";
    public static final String IDENTICAL_ACCOUNT_TRANSFER_DENIAL_EXCEPTION = "The sender and receiver cannot be same";
    public static final String BAD_AMOUNT_TRANSFER_DENIAL_EXCEPTION = "The amount to transfer is not valid";
    public static final String INSUFFICIENT_BALANCE_EXCEPTION = "You do not have sufficient balance in your account";
    public static final String INSUFFICIENT_DATA_EXCEPTION = "The required details are not met to initiate the transfer";
}

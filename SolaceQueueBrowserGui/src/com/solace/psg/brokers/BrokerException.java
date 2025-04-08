package com.solace.psg.brokers;
/* Copyright 2024 Solace Systems, Inc. All rights reserved.
 *
 * http://www.solacesystems.com
 *
 * This source is distributed under the terms and conditions
 * of any contract or contracts between Solace Systems, Inc.
 * ("Solace") and you or your company. If there are no 
 * contracts in place use of this source is not authorized.
 * No support is provided and no distribution, sharing with
 * others or re-use of this source is authorized unless 
 * specifically stated in the contracts referred to above.
 *
 * This product is provided as is and is not supported by Solace 
 * unless such support is provided for under an agreement 
 * signed between you and Solace.
 */


/*
 * This exception may be thrown by the SempSession class.
 */
public class BrokerException extends Exception 
{
	static final long serialVersionUID = 1; 
	public BrokerException(String strE) 
	{ 
		super(strE); 
	}
	public BrokerException(Exception e) 
	{ 
		super(e); 
	}
}

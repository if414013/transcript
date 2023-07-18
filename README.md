# Midtrans Status Helper
Java helper for Midtrans status

## Install

Add the following line to build.gradle dependencies.
```bash
implementation 'com.gopay:midtrans-status-mapper:0.0.6-RELEASE'
```
call map method
```bash
MidtransStatusMapper.map(order);
```
### MERCHANT_TRANSACTION mapping
| OMS State                                                                                                                                                                                                                               | Callback Status |
|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|:---------------:|
| order.status in (“created”) AND (qris OR is PaymentReference) **isQRIS** = order.payer.payment_instruction_details.length = 0, **isPaymentReference** = order.payer.payment_instruction_details[0].instruction_type="PAYMENT_REFERENCE" |     pending     |
| order.status in (“rejected”)                                                                                                                                                                                                            |      deny       |
| order.status in (“expired”)                                                                                                                                                                                                             |     expire      |
| order.status in (“cancelled”)                                                                                                                                                                                                           |     cancel      |
| order.status in (“queued”)                                                                                                                                                                                                              |   settlement    |
| order.status in (“cancelled”)                                                                                                                                                                                                           |     cancel      |
| order.status in (“created”) AND order.payer.payment_instructions[].reservation_status = RESERVED                                                                                                                                        |   authorized    |
| order.status in (“created”) AND order.payer.payment_instructions.length>0 AND order.payer.payment_instructions.each(status IN (“void”))                                                                                                 |     failure     |

### CASHBACK mapping
| OMS State                  | Callback Status  |
|----------------------------|:----------------:|
| order.status in (“queued”) | promo_settlement |

### REFUND/REVERT mapping
| OMS State                     |    Callback Status     |
|-------------------------------|:----------------------:|
| order.status in (“queued”)    | refund, refund_to_bank |
| order.status in (“fulfilled”) | refund, refund_to_bank |

### FRAUD STATUS mapping
| Gopay Status |  Midtrans Fraud Status |
|--------------|------------------------|
| REJECTED     | DENY                   |
| others       | ACCEPT                 |

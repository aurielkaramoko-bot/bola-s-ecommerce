package com.bolas.ecommerce.dto;

import com.bolas.ecommerce.model.DeliveryOption;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class NewOrderDto {

    @NotBlank @Size(max = 200)
    private String customerName;

    @NotBlank @Size(max = 40)
    private String customerPhone;

    @Size(max = 500)
    private String customerAddress;

    @NotNull
    private DeliveryOption deliveryOption = DeliveryOption.HOME;

    @NotNull @Min(0)
    private Long totalAmountCfa;

    @Min(0)
    private long deliveryFeeCfa;

    // getters/setters
    public String getCustomerName() { return customerName; }
    public void setCustomerName(String v) { this.customerName = v; }
    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String v) { this.customerPhone = v; }
    public String getCustomerAddress() { return customerAddress; }
    public void setCustomerAddress(String v) { this.customerAddress = v; }
    public DeliveryOption getDeliveryOption() { return deliveryOption; }
    public void setDeliveryOption(DeliveryOption v) { this.deliveryOption = v; }
    public Long getTotalAmountCfa() { return totalAmountCfa; }
    public void setTotalAmountCfa(Long v) { this.totalAmountCfa = v; }
    public long getDeliveryFeeCfa() { return deliveryFeeCfa; }
    public void setDeliveryFeeCfa(long v) { this.deliveryFeeCfa = v; }
}

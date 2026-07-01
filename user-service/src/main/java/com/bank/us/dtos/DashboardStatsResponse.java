package com.bank.us.dtos;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalCustomers;

    private long activeCustomers;

    private long inactiveCustomers;

    private long registeredToday;

    private long deletedCustomers;
}
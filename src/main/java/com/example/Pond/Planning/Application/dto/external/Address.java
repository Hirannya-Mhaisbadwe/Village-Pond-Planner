package com.example.Pond.Planning.Application.dto.external;

public record Address(
        String village,
        String town,
        String city,
        String county,
        String state,
        String postcode
) {
}

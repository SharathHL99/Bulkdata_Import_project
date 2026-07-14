package com.bulkimport.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ImportRecordDto {

    @NotBlank(message = "Name is required.")
    private String name;

    @Email(message = "Invalid email address.")
    private String email;

    @Pattern(regexp = "\\d{10}",message = "Phone number must contain exactly 10 digits.")
    private String phone;

    @NotNull
    @Positive(message = "Salary must be greater than zero.")
    private Double salary;

    @NotBlank(message = "Department is required.")
    private String department;

}
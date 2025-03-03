package com.yc.eshop.common.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * @author 余聪
 * @date 2020/12/2
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Error implements Serializable {

    private String message;
}

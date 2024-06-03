package com.becoze.biback.common;

import com.becoze.biback.constant.CommonConstant;
import lombok.Data;

/**
 * Pagination request / config
 *
 */
@Data
public class PageRequest {

    /**
     * Current page number
     */
    private long current = 1;

    /**
     * Number of items per page
     */
    private long pageSize = 10;

    /**
     * Sorting field
     */
    private String sortField;

    /**
     * Sorting order (default ascending)
     */
    private String sortOrder = CommonConstant.SORT_ORDER_ASC;
}

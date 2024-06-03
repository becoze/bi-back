package com.becoze.biback.common;

import java.io.Serializable;
import lombok.Data;

/**
 * Delete request
 *
 */
@Data
public class DeleteRequest implements Serializable {

    /**
     * id
     */
    private Long id;

    private static final long serialVersionUID = 1L;
}
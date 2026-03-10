package com.fleet.bms.infrastructure.hardware.protocol

import com.fleet.bms.domain.model.CanFrame
import com.fleet.bms.domain.service.ParsedCanData

/**
 * Interface: CAN Protocol Parser
 * 
 * Defines the contract for parsing CAN frames
 * into domain-specific data structures.
 */
interface CanProtocolParser {
    
    /**
     * Parse a CAN frame into domain data
     * Returns null if the frame is not recognized or invalid
     */
    fun parse(frame: CanFrame): ParsedCanData?
}

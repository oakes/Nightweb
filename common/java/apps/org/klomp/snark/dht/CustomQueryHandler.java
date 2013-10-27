package org.klomp.snark.dht;

import java.util.Map;

import org.klomp.snark.bencode.BEValue;

/**
 * Callback used when an unrecognized DHT query arrives.
 */
public interface CustomQueryHandler
{
    Map<String, Object> receiveQuery(String method, Map<String, BEValue> args);
    void receiveResponse(Map<String, BEValue> args);
}

 package com.nursing.common.util;
 
 public class SnowflakeIdWorker {
 
     private final long workerId;
     private final long datacenterId;
     private final long epoch = 1288834974657L;
     private long sequence = 0L;
     private long lastTimestamp = -1L;
 
     public SnowflakeIdWorker(long workerId, long datacenterId) {
         this.workerId = workerId;
         this.datacenterId = datacenterId;
     }
 
     public synchronized long nextId() {
         long timestamp = System.currentTimeMillis();
         if (timestamp < lastTimestamp) {
             throw new RuntimeException("Clock moved backwards");
         }
         if (timestamp == lastTimestamp) {
             sequence = (sequence + 1) & 4095;
             if (sequence == 0) {
                 timestamp = tilNextMillis(lastTimestamp);
             }
         } else {
             sequence = 0L;
         }
         lastTimestamp = timestamp;
         return ((timestamp - epoch) << 22)
                | (datacenterId << 17)
                | (workerId << 12)
                | sequence;
     }
 
     private long tilNextMillis(long lastTimestamp) {
         long timestamp = System.currentTimeMillis();
         while (timestamp <= lastTimestamp) {
             timestamp = System.currentTimeMillis();
         }
         return timestamp;
     }
 }

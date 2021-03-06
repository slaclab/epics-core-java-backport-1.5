package org.epics.util.compat.jdk5.net;

public enum NetClass {
    CLASS_A,        // IPV4, Class A address : 10.0.0.0/8
    CLASS_B,        // IPV4, Class B address : 172.16.0.0/12
    CLASS_C,        // IPV4, Class C address : 192.168.0.0/16
    CLASS_D,        // IPV4, Class D address : 224.0.0.0/24
    IPV4,           // Any other IPV4 address: 0.0.0.0/0
    IPV6_MULTICAST, // ff00::.../8
    IPV6;           // Any other IPV6 address: 0000::.../0
}

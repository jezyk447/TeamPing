package pl.teamping.net;

public final class RelayAddress {

    private RelayAddress() {}

    public static final String HOST = "budynshop.pl";
    public static final int PORT = 25599;

    public static final byte[] SERVER_STATIC_PUB = {
        (byte)0x07, (byte)0x2C, (byte)0x04, (byte)0x49, (byte)0x5C, (byte)0xBE, (byte)0x51, (byte)0x43,
        (byte)0x29, (byte)0xC2, (byte)0x0D, (byte)0x71, (byte)0x61, (byte)0xE2, (byte)0xF5, (byte)0xF0,
        (byte)0x36, (byte)0x7E, (byte)0xC6, (byte)0xD1, (byte)0x05, (byte)0x44, (byte)0x3D, (byte)0x0E,
        (byte)0xEB, (byte)0x88, (byte)0x93, (byte)0x91, (byte)0x14, (byte)0x9E, (byte)0xBF, (byte)0x21
    };
}

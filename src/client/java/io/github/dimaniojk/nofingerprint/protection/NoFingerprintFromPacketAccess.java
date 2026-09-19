package io.github.dimaniojk.nofingerprint.protection;

public interface NoFingerprintFromPacketAccess {
    void nofingerprint$setFromPacket();
    default void nofingerprint$setSilent() {}
}

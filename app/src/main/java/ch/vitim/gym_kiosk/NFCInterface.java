package ch.vitim.gym_kiosk;

public interface NFCInterface {
    public void onNFCConnectionStateChange(boolean connected);
    public void onNFCMessage(String cardUid, String message);
}

package pixy.meta.iptc;

public interface IPTCTag {
	public int getRecordNumber();
	public int getTag();
	public String getName();
	public boolean allowMultiple();
	public String getDataAsString(byte[] data);
	
	public static final int MAX_STRING_REPR_LEN = 10;
}

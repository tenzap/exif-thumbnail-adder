package pixy.meta;

import pixy.util.Reader;

public interface MetadataReader extends Reader {
	public MetadataType getType();
	public void ensureDataRead();
	public boolean isDataRead();
}
